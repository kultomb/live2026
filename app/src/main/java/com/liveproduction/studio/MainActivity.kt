package com.liveproduction.studio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.liveproduction.feature.diagnostics.DiagnosticsViewModel
import com.liveproduction.feature.diagnostics.ui.DiagnosticsScreen
import com.liveproduction.feature.live.LiveStudioViewModel
import com.liveproduction.feature.live.ui.LiveStudioScreen
import com.liveproduction.feature.settings.StreamSetupViewModel
import com.liveproduction.feature.settings.ui.StreamSetupScreen

private enum class AppScreen {
    STUDIO,
    SETTINGS,
    DIAGNOSTICS
}

class MainActivity : ComponentActivity() {

    private val studioViewModel: LiveStudioViewModel by viewModels()
    private val settingsViewModel: StreamSetupViewModel by viewModels()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (!cameraGranted || !audioGranted) {
            Toast.makeText(
                this,
                "Camera and Microphone permissions are required for Live Production",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        requestRequiredPermissions()

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.STUDIO) }

            when (currentScreen) {
                AppScreen.STUDIO -> {
                    LiveStudioScreen(
                        viewModel = studioViewModel,
                        onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                        onOpenDiagnostics = { currentScreen = AppScreen.DIAGNOSTICS }
                    )
                }
                AppScreen.SETTINGS -> {
                    StreamSetupScreen(
                        viewModel = settingsViewModel,
                        onBack = { currentScreen = AppScreen.STUDIO }
                    )
                }
                AppScreen.DIAGNOSTICS -> {
                    DiagnosticsScreen(
                        viewModel = diagnosticsViewModel,
                        onBack = { currentScreen = AppScreen.STUDIO }
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
