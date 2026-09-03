package com.liveproduction.core.streaming.flv

import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class FlvPackager {

    enum class FlvTagType(val value: Int) {
        AUDIO(0x08),
        VIDEO(0x09),
        SCRIPT_METADATA(0x12)
    }

    data class FlvTag(
        val type: FlvTagType,
        val ptsMs: Int,
        val payload: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FlvTag
            if (type != other.type) return false
            if (ptsMs != other.ptsMs) return false
            if (!payload.contentEquals(other.payload)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + ptsMs
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }

    fun packageVideoTag(nalBuffer: ByteBuffer, isKeyFrame: Boolean, ptsMs: Int): FlvTag {
        val bos = ByteArrayOutputStream()
        // Video Tag Header: FrameType (1=KeyFrame, 2=InterFrame) + CodecID (7=AVC)
        val headerByte = if (isKeyFrame) 0x17 else 0x27
        bos.write(headerByte)
        // AVCPacketType: 1 = AVC NALU
        bos.write(0x01)
        // CompositionTime (3 bytes offset)
        bos.write(0x00)
        bos.write(0x00)
        bos.write(0x00)

        // Write NALU length prefix & data
        val nalSize = nalBuffer.remaining()
        bos.write((nalSize shr 24) and 0xFF)
        bos.write((nalSize shr 16) and 0xFF)
        bos.write((nalSize shr 8) and 0xFF)
        bos.write(nalSize and 0xFF)

        val nalArray = ByteArray(nalSize)
        nalBuffer.get(nalArray)
        bos.write(nalArray)

        return FlvTag(FlvTagType.VIDEO, ptsMs, bos.toByteArray())
    }

    fun packageAudioTag(aacBuffer: ByteBuffer, ptsMs: Int): FlvTag {
        val bos = ByteArrayOutputStream()
        // Audio Tag Header: SoundFormat (10=AAC) + SoundRate (3=44k/48k) + SoundSize (1=16bit) + SoundType (1=Stereo) -> 0xAF
        bos.write(0xAF)
        // AACPacketType: 1 = Raw AAC frame data
        bos.write(0x01)

        val dataSize = aacBuffer.remaining()
        val dataArray = ByteArray(dataSize)
        aacBuffer.get(dataArray)
        bos.write(dataArray)

        return FlvTag(FlvTagType.AUDIO, ptsMs, bos.toByteArray())
    }

    fun createHeader(): ByteArray {
        // "FLV", Version 1, Flags (Audio + Video), HeaderSize 9
        return byteArrayOf(
            0x46, 0x4C, 0x56,
            0x01,
            0x05, // Audio + Video
            0x00, 0x00, 0x00, 0x09,
            0x00, 0x00, 0x00, 0x00 // PreviousTagSize0
        )
    }

    companion object {
        private const val TAG = "FlvPackager"
    }
}
