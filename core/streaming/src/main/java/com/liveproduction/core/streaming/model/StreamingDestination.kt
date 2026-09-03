package com.liveproduction.core.streaming.model

enum class StreamProfile(val displayName: String, val width: Int, val height: Int, val fps: Int, val bitrateBps: Int) {
    PROFILE_720P_30("720p HD (30 FPS) - 2.5 Mbps", 1280, 720, 30, 2_500_000),
    PROFILE_1080P_30("1080p Full HD (30 FPS) - 4.5 Mbps", 1920, 1080, 30, 4_500_000),
    PROFILE_1080P_60("1080p Pro (60 FPS) - 7.5 Mbps", 1920, 1080, 60, 7_500_000)
}

data class StreamingDestination(
    val id: String,
    val platformType: SocialPlatformType,
    val rtmpUrl: String,
    val profile: StreamProfile = StreamProfile.PROFILE_1080P_30
)
