package com.liveproduction.core.streaming.model

enum class LiveSessionState {
    IDLE,
    PREPARING,
    READY,
    STARTING,
    LIVE,
    RECONNECTING,
    STOPPING,
    COMPLETED,
    FAILED
}
