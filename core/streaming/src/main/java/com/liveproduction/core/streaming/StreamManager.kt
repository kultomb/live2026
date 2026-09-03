package com.liveproduction.core.streaming

import android.util.Log
import com.liveproduction.core.streaming.flv.FlvPackager
import com.liveproduction.core.streaming.rtmp.RtmpSocketEngine

class StreamManager private constructor() {

    private val flvPackager = FlvPackager()
    private val rtmpEngine = RtmpSocketEngine()
    private var currentEndpointUrl: String = ""

    fun configureEndpoint(rtmpUrl: String, streamKey: String) {
        currentEndpointUrl = if (streamKey.isNotEmpty()) "$rtmpUrl/$streamKey" else rtmpUrl
        Log.i(TAG, "Configured RTMP endpoint: $rtmpUrl/****HIDDEN_KEY****")
    }

    fun startStream(): Boolean {
        if (currentEndpointUrl.isEmpty()) {
            Log.e(TAG, "Cannot start stream: Endpoint URL is empty")
            return false
        }
        val isRtmps = currentEndpointUrl.startsWith("rtmps://")
        return rtmpEngine.connect(currentEndpointUrl, isRtmps)
    }

    fun attemptReconnect(): Boolean {
        Log.i(TAG, "Attempting stream reconnect to $currentEndpointUrl...")
        rtmpEngine.disconnect()
        val isRtmps = currentEndpointUrl.startsWith("rtmps://")
        return rtmpEngine.connect(currentEndpointUrl, isRtmps)
    }

    fun stopStream() {
        rtmpEngine.disconnect()
        Log.i(TAG, "Stopped stream session.")
    }

    companion object {
        private const val TAG = "StreamManager"

        @Volatile
        private var INSTANCE: StreamManager? = null

        fun getInstance(): StreamManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StreamManager().also { INSTANCE = it }
            }
        }
    }
}
