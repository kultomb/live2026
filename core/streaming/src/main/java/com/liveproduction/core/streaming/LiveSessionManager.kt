package com.liveproduction.core.streaming

import android.util.Log
import com.liveproduction.core.streaming.model.LiveSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveSessionManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _sessionState = MutableStateFlow(LiveSessionState.IDLE)
    val sessionState: StateFlow<LiveSessionState> = _sessionState.asStateFlow()

    private val _reconnectAttempt = MutableStateFlow(0)
    val reconnectAttempt: StateFlow<Int> = _reconnectAttempt.asStateFlow()

    private var reconnectJob: Job? = null

    @Synchronized
    fun startLiveSession(): Boolean {
        val currentState = _sessionState.value
        if (currentState != LiveSessionState.IDLE && currentState != LiveSessionState.READY) {
            Log.w(TAG, "GO LIVE ignored: Session state is already $currentState")
            return false
        }

        Log.i(TAG, "Transitioning state: $currentState -> STARTING")
        _sessionState.value = LiveSessionState.STARTING
        
        // Simulating async RTMP handshake connection transition to LIVE
        _sessionState.value = LiveSessionState.LIVE
        Log.i(TAG, "Transitioning state: STARTING -> LIVE")
        return true
    }

    @Synchronized
    fun onNetworkInterrupted() {
        if (_sessionState.value != LiveSessionState.LIVE) return

        Log.w(TAG, "Network interrupted during live broadcast! Initiating Bounded Exponential Backoff Reconnect...")
        _sessionState.value = LiveSessionState.RECONNECTING
        _reconnectAttempt.value = 0

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delaysSec = listOf(0, 2, 4, 8, 16)
            var success = false

            for (attempt in 1..delaysSec.size) {
                _reconnectAttempt.value = attempt
                val waitTimeSec = delaysSec[attempt - 1]
                Log.i(TAG, "Exponential Backoff Reconnect Attempt $attempt/5. Waiting $waitTimeSec seconds...")
                delay(waitTimeSec * 1000L)

                // Attempt reconnect
                val reconnected = StreamManager.getInstance().attemptReconnect()
                if (reconnected) {
                    success = true
                    break
                }
            }

            if (success) {
                Log.i(TAG, "Stream connection successfully recovered!")
                _sessionState.value = LiveSessionState.LIVE
            } else {
                Log.e(TAG, "All 5 Exponential Backoff Reconnect attempts failed. Shifting state to FAILED.")
                _sessionState.value = LiveSessionState.FAILED
            }
        }
    }

    @Synchronized
    fun stopLiveSession(): Boolean {
        reconnectJob?.cancel()
        val currentState = _sessionState.value
        if (currentState != LiveSessionState.LIVE && currentState != LiveSessionState.RECONNECTING) {
            Log.w(TAG, "STOP LIVE ignored: Session state is $currentState")
            return false
        }

        Log.i(TAG, "Transitioning state: $currentState -> STOPPING")
        _sessionState.value = LiveSessionState.STOPPING
        _sessionState.value = LiveSessionState.IDLE
        Log.i(TAG, "Transitioning state: STOPPING -> IDLE")
        return true
    }

    companion object {
        private const val TAG = "LiveSessionManager"

        @Volatile
        private var INSTANCE: LiveSessionManager? = null

        fun getInstance(): LiveSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LiveSessionManager().also { INSTANCE = it }
            }
        }
    }
}
