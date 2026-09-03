package com.liveproduction.core.media.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import com.liveproduction.core.media.recording.RecordingManager
import java.nio.ByteBuffer

class VideoEncoder(
    private val width: Int = 1280,
    private val height: Int = 720,
    private val fps: Int = 30,
    private val bitrateBps: Int = 3_000_000,
    private val keyframeIntervalSec: Int = 2
) {

    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    @Volatile private var isEncoderRunning = false
    private var drainThread: Thread? = null

    fun start(onEncodedFrame: ((ByteBuffer, MediaCodec.BufferInfo) -> Unit)? = null): Surface {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyframeIntervalSec)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = codec.createInputSurface()
        codec.start()

        mediaCodec = codec
        inputSurface = surface
        isEncoderRunning = true

        drainThread = Thread {
            drainLoop(codec, onEncodedFrame)
        }.apply {
            name = "H264EncoderDrainThread"
            start()
        }

        Log.i(TAG, "VideoEncoder H.264 started: ${width}x${height} @ $fps fps, ${bitrateBps / 1000} kbps")
        return surface
    }

    private fun drainLoop(
        codec: MediaCodec,
        onEncodedFrame: ((ByteBuffer, MediaCodec.BufferInfo) -> Unit)?
    ) {
        val bufferInfo = MediaCodec.BufferInfo()

        while (isEncoderRunning) {
            try {
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when (outputBufferIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = codec.outputFormat
                        Log.i(TAG, "H.264 Encoder Output Format Changed: $newFormat")
                        RecordingManager.getInstance().addVideoFormat(newFormat)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No buffer available yet
                    }
                    else -> {
                        if (outputBufferIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    bufferInfo.size = 0
                                }

                                if (bufferInfo.size > 0) {
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                                    // 1. Write sample to MediaMuxer local recording
                                    RecordingManager.getInstance().writeVideoSample(outputBuffer, bufferInfo)

                                    // 2. Pass frame to RTMP callback if set
                                    onEncodedFrame?.invoke(outputBuffer, bufferInfo)
                                }
                            }
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isEncoderRunning) {
                    Log.e(TAG, "Error in VideoEncoder drainLoop", e)
                }
            }
        }
    }

    fun adjustBitrate(newBitrateBps: Int) {
        if (!isEncoderRunning) return
        val params = Bundle().apply {
            putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrateBps)
        }
        mediaCodec?.setParameters(params)
        Log.i(TAG, "Adjusted dynamic video bitrate to ${newBitrateBps / 1000} kbps")
    }

    fun stop() {
        if (isEncoderRunning) {
            isEncoderRunning = false
            try {
                drainThread?.join(500)
                drainThread = null
            } catch (e: Exception) {}

            try {
                mediaCodec?.stop()
                mediaCodec?.release()
                inputSurface?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping VideoEncoder", e)
            }
            mediaCodec = null
            inputSurface = null
            Log.i(TAG, "VideoEncoder H.264 stopped")
        }
    }

    companion object {
        private const val TAG = "VideoEncoder"
    }
}
