package com.liveproduction.core.usb.parser

import android.util.Log
import com.liveproduction.core.media.model.VideoFrame
import com.liveproduction.core.media.model.VideoSourceType
import com.liveproduction.core.usb.buffer.UvcRingBuffer
import com.liveproduction.core.usb.model.UvcFrameFormat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class UvcFrameParser(private val ringBuffer: UvcRingBuffer) {

    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            Log.i(TAG, "UvcFrameParser background worker started")
        }
    }

    fun parseFrame(
        rawBuffer: ByteArray,
        length: Int,
        format: UvcFrameFormat,
        width: Int,
        height: Int
    ) {
        if (!isRunning.get()) return

        executor.execute {
            try {
                val frameTimeNs = System.nanoTime()
                // In production native build, MJPEG byte payload is decompressed via libjpeg-turbo
                // Here we wrap the frame into VideoFrame for ring buffer routing
                val frame = VideoFrame(
                    textureId = 0,
                    dataBuffer = rawBuffer.copyOf(length),
                    width = width,
                    height = height,
                    timestampNs = frameTimeNs,
                    sourceType = VideoSourceType.SOURCE_EXTERNAL_HDMI
                )

                ringBuffer.push(frame)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing UVC frame", e)
            }
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            executor.shutdownNow()
            Log.i(TAG, "UvcFrameParser background worker stopped")
        }
    }

    companion object {
        private const val TAG = "UvcFrameParser"
    }
}
