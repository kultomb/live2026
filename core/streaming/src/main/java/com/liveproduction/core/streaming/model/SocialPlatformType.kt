package com.liveproduction.core.streaming.model

enum class SocialPlatformType(val displayName: String, val defaultUrl: String) {
    YOUTUBE("YouTube Live", "rtmps://a.rtmps.youtube.com/live2"),
    FACEBOOK("Facebook Live", "rtmps://live-api-s.facebook.com:443/rtmp/"),
    TWITCH("Twitch", "rtmp://live.twitch.tv/app/"),
    CUSTOM_RTMP("Custom RTMP", "rtmp://"),
    CUSTOM_RTMPS("Custom RTMPS (Secure)", "rtmps://")
}
