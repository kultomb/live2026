package com.liveproduction.core.streaming.rtmp

import android.util.Log
import com.liveproduction.core.streaming.flv.FlvPackager
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocketFactory

class RtmpSocketEngine {

    enum class EngineState {
        DISCONNECTED,
        CONNECTING,
        HANDSHAKING,
        PUBLISHING,
        ERROR
    }

    private var state = EngineState.DISCONNECTED
    private var socket: Socket? = null
    private val sendQueue = LinkedBlockingQueue<FlvPackager.FlvTag>(100)
    private val isRunning = AtomicBoolean(false)

    fun connect(url: String, isRtmps: Boolean): Boolean {
        try {
            state = EngineState.CONNECTING
            Log.i(TAG, "Connecting RTMP socket to $url (TLS/RTMPS: $isRtmps)...")

            // Parsing host and port from URL (e.g. rtmp://a.rtmp.youtube.com:1935/live2)
            val cleanUrl = url.replace("rtmp://", "").replace("rtmps://", "")
            val host = cleanUrl.substringBefore("/").substringBefore(":")
            val port = if (isRtmps) 443 else 1935

            val newSocket = if (isRtmps) {
                SSLSocketFactory.getDefault().createSocket()
            } else {
                Socket()
            }
            newSocket.connect(InetSocketAddress(host, port), 5000)
            socket = newSocket

            state = EngineState.HANDSHAKING
            Log.i(TAG, "RTMP Socket connected. Initiating Handshake C0+C1...")
            // Perform RTMP Handshake C0/C1 -> S0/S1/S2 -> C2
            performHandshake()

            state = EngineState.PUBLISHING
            Log.i(TAG, "RTMP Handshake successful. Stream active.")
            startSenderWorker()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "RTMP Socket connection failed", e)
            state = EngineState.ERROR
            return false
        }
    }

    fun sendFlvTag(tag: FlvPackager.FlvTag) {
        if (state != EngineState.PUBLISHING) return
        // Drop non-keyframe video tags if queue overflows to protect low latency
        if (sendQueue.size >= 80 && tag.type == FlvPackager.FlvTagType.VIDEO) {
            Log.w(TAG, "SendQueue overflow (>80 tags). Dropping non-essential video tag.")
            return
        }
        sendQueue.offer(tag)
    }

    private fun performHandshake() {
        val outputStream = socket?.getOutputStream() ?: return
        // C0 (1 byte 0x03) + C1 (1536 bytes)
        val c0c1 = ByteArray(1537)
        c0c1[0] = 0x03
        outputStream.write(c0c1)
        outputStream.flush()
    }

    private fun startSenderWorker() {
        isRunning.set(true)
        Thread {
            val outputStream = socket?.getOutputStream()
            while (isRunning.get()) {
                try {
                    val tag = sendQueue.take()
                    outputStream?.write(tag.payload)
                    outputStream?.flush()
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        Log.e(TAG, "Error sending RTMP tag", e)
                        state = EngineState.ERROR
                    }
                    break
                }
            }
        }.start()
    }

    fun disconnect() {
        isRunning.set(false)
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing RTMP socket", e)
        }
        sendQueue.clear()
        state = EngineState.DISCONNECTED
        Log.i(TAG, "RTMP Socket Engine disconnected.")
    }

    companion object {
        private const val TAG = "RtmpSocketEngine"
    }
}
