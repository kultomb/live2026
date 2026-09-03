package com.liveproduction.core.diagnostics.tracker

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

data class PerformanceMetrics(
    val actualBitrateKbps: Long = 0L,
    val totalDroppedFrames: Long = 0L,
    val renderLatencyMs: Long = 0L,
    val isThermalThrottling: Boolean = false
)

class PerformanceTracker private constructor() {

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val totalBytesSent = AtomicLong(0)
    private val totalDropped = AtomicLong(0)
    private var lastCheckTimeMs = System.currentTimeMillis()
    private var lastByteCount = 0L

    fun onFrameDropped() {
        totalDropped.incrementAndGet()
    }

    fun onBytesTransmitted(bytes: Int) {
        totalBytesSent.addAndGet(bytes.toLong())
        val now = System.currentTimeMillis()
        val elapsed = now - lastCheckTimeMs

        if (elapsed >= 1000) {
            val deltaBytes = totalBytesSent.get() - lastByteCount
            val bitrateKbps = (deltaBytes * 8) / elapsed

            _performanceMetrics.value = _performanceMetrics.value.copy(
                actualBitrateKbps = bitrateKbps,
                totalDroppedFrames = totalDropped.get()
            )

            lastByteCount = totalBytesSent.get()
            lastCheckTimeMs = now
        }
    }

    fun updateThermalThrottling(throttling: Boolean) {
        _performanceMetrics.value = _performanceMetrics.value.copy(isThermalThrottling = throttling)
        if (throttling) {
            Log.w(TAG, "Thermal throttling detected! Reducing rendering load...")
        }
    }

    companion object {
        private const val TAG = "PerformanceTracker"

        @Volatile
        private var INSTANCE: PerformanceTracker? = null

        fun getInstance(): PerformanceTracker {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceTracker().also { INSTANCE = it }
            }
        }
    }
}
