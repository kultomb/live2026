package com.liveproduction.core.media.model

data class VideoFrame(
    val textureId: Int = 0,
    val dataBuffer: ByteArray? = null,
    val width: Int,
    val height: Int,
    val timestampNs: Long,
    val sourceType: VideoSourceType,
    val rotationDegrees: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VideoFrame
        if (textureId != other.textureId) return false
        if (dataBuffer != null) {
            if (other.dataBuffer == null) return false
            if (!dataBuffer.contentEquals(other.dataBuffer)) return false
        } else if (other.dataBuffer != null) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (timestampNs != other.timestampNs) return false
        if (sourceType != other.sourceType) return false
        if (rotationDegrees != other.rotationDegrees) return false
        return true
    }

    override fun hashCode(): Int {
        var result = textureId
        result = 31 * result + (dataBuffer?.contentHashCode() ?: 0)
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + timestampNs.hashCode()
        result = 31 * result + sourceType.hashCode()
        result = 31 * result + rotationDegrees
        return result
    }
}
