package com.liveproduction.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.liveproduction.core.network.model.NetworkStatus
import com.liveproduction.core.network.model.NetworkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkManager private constructor() {

    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private var activeTargetBitrateBps: Int = 4_500_000

    fun updateNetworkState(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager unavailable")
            return
        }

        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

        if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR_5G
                else -> NetworkType.NONE
            }
            val bandwidthKbps = caps.linkUpstreamBandwidthKbps.toLong()

            _networkStatus.value = NetworkStatus(
                type = type,
                isConnected = true,
                rttMs = 25L,
                estimatedUploadBandwidthKbps = if (bandwidthKbps > 0) bandwidthKbps else 5000L
            )

            // Adaptive Bitrate Feedback Loop: If estimated upload bandwidth is low, adjust video bitrate
            val estimatedBps = (bandwidthKbps * 1000).toInt()
            if (estimatedBps > 0 && estimatedBps < activeTargetBitrateBps) {
                val newTargetBitrate = (estimatedBps * 0.80).toInt().coerceAtLeast(1_000_000)
                Log.w(TAG, "Adaptive Bitrate Triggered: Network bandwidth ($estimatedBps bps) is lower than target. Adjusting target to $newTargetBitrate bps")
                activeTargetBitrateBps = newTargetBitrate
            }

            Log.i(TAG, "Network updated: Type=$type, Bandwidth=${_networkStatus.value.estimatedUploadBandwidthKbps} kbps")
        } else {
            _networkStatus.value = NetworkStatus(type = NetworkType.NONE, isConnected = false)
            Log.w(TAG, "Network disconnected")
        }
    }

    companion object {
        private const val TAG = "NetworkManager"

        @Volatile
        private var INSTANCE: NetworkManager? = null

        fun getInstance(): NetworkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkManager().also { INSTANCE = it }
            }
        }
    }
}
