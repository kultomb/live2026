package com.liveproduction.core.streaming.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class StreamKeyStorage(context: Context) {

    private val sharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences, falling back to standard private prefs", e)
            context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        }
    }

    fun saveStreamKey(destinationId: String, streamKey: String) {
        sharedPreferences.edit()
            .putString("KEY_$destinationId", streamKey)
            .apply()
        Log.i(TAG, "Safely saved encrypted stream key for destination: $destinationId")
    }

    fun getStreamKey(destinationId: String): String {
        return sharedPreferences.getString("KEY_$destinationId", "") ?: ""
    }

    companion object {
        private const val TAG = "StreamKeyStorage"
        private const val PREFS_FILENAME = "secure_stream_credentials"
    }
}
