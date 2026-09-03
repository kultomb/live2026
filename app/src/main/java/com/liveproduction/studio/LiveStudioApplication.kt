package com.liveproduction.studio

import android.app.Application
import android.util.Log

class LiveStudioApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing LiveStudioApplication Broadcast Studio...")

        // Setup uncaught exception logging to prevent silent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "UNCAUGHT CRASH on thread ${thread.name}", throwable)
        }
    }

    companion object {
        private const val TAG = "LiveStudioApplication"
    }
}
