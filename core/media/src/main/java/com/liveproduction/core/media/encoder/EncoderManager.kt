package com.liveproduction.core.media.encoder

import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

class EncoderManager private constructor() {

    private var videoEncoder: VideoEncoder? = null
    private var audioEncoder: AudioEncoder? = null
    private var isEncodersActive = false
    private var encoderSurface: Surface? = null
    private var onEncoderSurfaceListener: ((Surface?) -> Unit)? = null

    fun getEncoderSurface(): Surface? = encoderSurface

    fun setOnEncoderSurfaceListener(listener: ((Surface?) -> Unit)?) {
        this.onEncoderSurfaceListener = listener
    }

    fun encodeAudioFrame(pcmData: ShortArray, length: Int) {
        audioEncoder?.encodePcmFrame(pcmData, length)
    }

    fun startEncoders(
        width: Int = 1280,
        height: Int = 720,
        fps: Int = 30,
        videoBitrateBps: Int = 3_000_000
    ): Surface? {
        if (isEncodersActive && encoderSurface != null) {
            return encoderSurface
        }

        Log.i(TAG, "EncoderManager starting Hardware Video & Audio Encoders...")
        val vEnc = VideoEncoder(width, height, fps, videoBitrateBps)
        val aEnc = AudioEncoder()

        val inputSurface = vEnc.start { buffer, info ->
            // Output NAL units routed to StreamManager FLV packager
        }
        aEnc.start()

        videoEncoder = vEnc
        audioEncoder = aEnc
        encoderSurface = inputSurface
        isEncodersActive = true

        // Notify listener (e.g. ViewModel -> CameraManager) of new encoder surface
        onEncoderSurfaceListener?.invoke(inputSurface)

        return inputSurface
    }

    fun adjustVideoBitrate(newBitrateBps: Int) {
        videoEncoder?.adjustBitrate(newBitrateBps)
    }

    fun stopEncoders() {
        if (isEncodersActive) {
            Log.i(TAG, "EncoderManager stopping Video & Audio Encoders...")
            onEncoderSurfaceListener?.invoke(null)

            videoEncoder?.stop()
            audioEncoder?.stop()
            videoEncoder = null
            audioEncoder = null
            encoderSurface = null
            isEncodersActive = false
        }
    }

    companion object {
        private const val TAG = "EncoderManager"

        @Volatile
        private var INSTANCE: EncoderManager? = null

        fun getInstance(): EncoderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EncoderManager().also { INSTANCE = it }
            }
        }
    }
}
