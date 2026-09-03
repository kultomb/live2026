package com.liveproduction.core.streaming.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class StreamKeyStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val appContext = context.applicationContext
        try {
            appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening SharedPreferences", e)
            appContext.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        }
    }

    fun saveStreamKey(destinationId: String, streamKey: String) {
        try {
            prefs.edit()
                .putString("KEY_$destinationId", streamKey)
                .apply()
            Log.i(TAG, "Safely saved stream key for destination: $destinationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stream key for $destinationId", e)
        }
    }

    fun getStreamKey(destinationId: String): String {
        return try {
            prefs.getString("KEY_$destinationId", "") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error reading stream key for $destinationId", e)
            ""
        }
    }

    companion object {
        private const val TAG = "StreamKeyStorage"
        private const val PREFS_FILENAME = "secure_stream_credentials"
    }
}
