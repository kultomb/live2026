package com.liveproduction.core.network.model

enum class NetworkType {
    WIFI,
    CELLULAR_5G,
    CELLULAR_4G,
    ETHERNET,
    NONE
}

data class NetworkStatus(
    val type: NetworkType = NetworkType.NONE,
    val isConnected: Boolean = false,
    val rttMs: Long = 0L,
    val estimatedUploadBandwidthKbps: Long = 0L
)
