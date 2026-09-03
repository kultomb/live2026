package com.liveproduction.core.streaming

import android.content.Context
import android.util.Log
import com.liveproduction.core.streaming.model.SocialPlatformType
import com.liveproduction.core.streaming.model.StreamProfile
import com.liveproduction.core.streaming.model.StreamingDestination
import com.liveproduction.core.streaming.security.StreamKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SocialPlatformManager private constructor() {

    private val _activeDestination = MutableStateFlow(
        StreamingDestination("dest_yt", SocialPlatformType.YOUTUBE, SocialPlatformType.YOUTUBE.defaultUrl)
    )
    val activeDestination: StateFlow<StreamingDestination> = _activeDestination.asStateFlow()

    private var keyStorage: StreamKeyStorage? = null

    fun initialize(context: Context) {
        if (keyStorage == null) {
            keyStorage = StreamKeyStorage(context.applicationContext)
        }
    }

    fun configureActiveDestination(
        platformType: SocialPlatformType,
        customUrl: String,
        streamKey: String,
        profile: StreamProfile
    ) {
        val finalUrl = if (customUrl.isNotEmpty()) customUrl else platformType.defaultUrl
        val destId = "dest_${platformType.name.lowercase()}"

        // Save stream key securely into EncryptedSharedPreferences
        keyStorage?.saveStreamKey(destId, streamKey)

        val destination = StreamingDestination(
            id = destId,
            platformType = platformType,
            rtmpUrl = finalUrl,
            profile = profile
        )

        _activeDestination.value = destination

        // Update StreamManager endpoint
        StreamManager.getInstance().configureEndpoint(finalUrl, streamKey)
        Log.i(TAG, "Configured Active Streaming Destination: ${platformType.displayName} ($finalUrl), Profile=${profile.displayName}")
    }

    fun getSavedStreamKey(platformType: SocialPlatformType): String {
        val destId = "dest_${platformType.name.lowercase()}"
        return keyStorage?.getStreamKey(destId) ?: ""
    }

    companion object {
        private const val TAG = "SocialPlatformManager"

        @Volatile
        private var INSTANCE: SocialPlatformManager? = null

        fun getInstance(): SocialPlatformManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocialPlatformManager().also { INSTANCE = it }
            }
        }
    }
}
