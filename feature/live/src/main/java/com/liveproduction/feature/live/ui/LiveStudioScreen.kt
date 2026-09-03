package com.liveproduction.feature.live.ui

import android.app.Activity
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.liveproduction.core.media.VideoPipelineManager
import com.liveproduction.core.media.model.VideoSourceType
import com.liveproduction.core.media.recording.RecordingState
import com.liveproduction.core.streaming.model.LiveSessionState
import com.liveproduction.feature.live.LiveStudioViewModel

// Dark Broadcast Theme Tokens
private val BackgroundCanvas = Color(0xFF0F1216)
private val SurfacePanel = Color(0xFF181C22)
private val BorderDivider = Color(0xFF2E3542)
private val TextPrimary = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF94A3B8)
private val LiveRed = Color(0xFFEF4444)
private val ReadyGreen = Color(0xFF10B981)
private val WarningAmber = Color(0xFFF59E0B)
private val AccentBlue = Color(0xFF3B82F6)

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun LiveStudioScreen(
    viewModel: LiveStudioViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {}
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val activeSource by viewModel.activeSource.collectAsState()
    val currentFps by viewModel.currentFps.collectAsState()
    val audioMeter by viewModel.audioMeterState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val recordedDurationMs by viewModel.recordedDurationMs.collectAsState()
    val healthMetrics by viewModel.healthMetrics.collectAsState()

    var isFullscreenPreview by remember { mutableStateOf(false) }

    // Toggle Android System Status Bar & Navigation Bar Immersive Mode during Fullscreen Preview
    val context = LocalContext.current
    DisposableEffect(isFullscreenPreview) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreenPreview) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
    ) {
        if (isFullscreenPreview) {
            // 100% Edge-to-Edge True Immersive Fullscreen Monitor (All buttons & bars hidden)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 1-Tap anywhere in Fullscreen mode immediately returns to main studio screen
                        isFullscreenPreview = false
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    val surface = holder.surface
                                    VideoPipelineManager.getInstance().onPreviewSurfaceAvailable(surface)
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    VideoPipelineManager.getInstance().onPreviewSurfaceDestroyed()
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .background(BackgroundCanvas)
            ) {
                // Main Production Column (Left / Middle 80%)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // Top Broadcast Bar
                    TopBroadcastBar(
                        sessionState = sessionState,
                        recordingState = recordingState,
                        recordedDurationMs = recordedDurationMs,
                        currentFps = currentFps,
                        batteryPercent = healthMetrics.batteryPercent,
                        thermalStatus = healthMetrics.thermalStatus,
                        onOpenSettings = onOpenSettings,
                        onOpenDiagnostics = onOpenDiagnostics
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Persistent Main Video Surface Viewport Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfacePanel)
                            .border(1.dp, BorderDivider, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Single Immutable SurfaceView Native Hardware Layer with Exact 16:9 Broadcast Aspect Ratio
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx).apply {
                                    holder.addCallback(object : SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            val surface = holder.surface
                                            VideoPipelineManager.getInstance().onPreviewSurfaceAvailable(surface)
                                        }

                                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                                            VideoPipelineManager.getInstance().onPreviewSurfaceDestroyed()
                                        }
                                    })
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        // Fullscreen Toggle Icon Button (⛶)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, BorderDivider, CircleShape)
                                .clickable {
                                    isFullscreenPreview = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⛶", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom Video Source Switcher Dock
                    SourceSwitcherDock(
                        activeSource = activeSource,
                        onSourceSelected = { viewModel.onSourceSelected(it) }
                    )
                }

                // Right Control Sidebar (VU Meters, REC Button & Go Live Button 20%)
                ControlSidebar(
                    sessionState = sessionState,
                    recordingState = recordingState,
                    recordedDurationMs = recordedDurationMs,
                    audioMeterMuted = audioMeter.isMuted,
                    audioLevelPercent = audioMeter.levelPercent,
                    onGoLiveClicked = { viewModel.onGoLiveToggled() },
                    onRecordClicked = { viewModel.onRecordToggled() },
                    onMuteToggled = { viewModel.onAudioMuteToggled() }
                )
            }
        }
    }
}

@Composable
private fun TopBroadcastBar(
    sessionState: LiveSessionState,
    recordingState: RecordingState,
    recordedDurationMs: Long,
    currentFps: Float,
    batteryPercent: Int,
    thermalStatus: String,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(SurfacePanel, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Live Status Badge
        val badgeColor = when (sessionState) {
            LiveSessionState.LIVE -> LiveRed
            LiveSessionState.STARTING, LiveSessionState.RECONNECTING -> WarningAmber
            LiveSessionState.READY -> ReadyGreen
            else -> TextSecondary
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(badgeColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sessionState.name,
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Recording Live Ticking Timer Badge
        if (recordingState == RecordingState.RECORDING) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(LiveRed.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(LiveRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REC ${formatDuration(recordedDurationMs)}",
                    color = LiveRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        // FPS Counter (Clean Integer format like 30 FPS / 60 FPS)
        val displayFps = if (currentFps > 0f) currentFps.toInt() else 30
        Text(
            text = "$displayFps FPS",
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.weight(1f))

        // Health Indicators
        Text(
            text = "BAT: $batteryPercent%",
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onOpenDiagnostics,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BorderDivider)
        ) {
            Text("DIAG", fontSize = 11.sp, color = TextPrimary)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Button(
            onClick = onOpenSettings,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BorderDivider)
        ) {
            Text("SETTINGS", fontSize = 11.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun SourceSwitcherDock(
    activeSource: VideoSourceType,
    onSourceSelected: (VideoSourceType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(SurfacePanel, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val sources = listOf(
            VideoSourceType.SOURCE_FRONT_CAMERA to "FRONT CAM",
            VideoSourceType.SOURCE_REAR_MAIN to "REAR MAIN (1x)",
            VideoSourceType.SOURCE_REAR_ULTRAWIDE to "ULTRA WIDE (0.5x)",
            VideoSourceType.SOURCE_REAR_TELEPHOTO to "TELEPHOTO (3x)",
            VideoSourceType.SOURCE_EXTERNAL_HDMI to "HDMI CAM"
        )

        sources.forEach { (source, label) ->
            val isSelected = activeSource == source
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else BorderDivider)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) AccentBlue else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSourceSelected(source) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) AccentBlue else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ControlSidebar(
    sessionState: LiveSessionState,
    recordingState: RecordingState,
    recordedDurationMs: Long,
    audioMeterMuted: Boolean,
    audioLevelPercent: Int,
    onGoLiveClicked: () -> Unit,
    onRecordClicked: () -> Unit,
    onMuteToggled: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
            .background(SurfacePanel, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (audioMeterMuted) "AUDIO: MUTED" else "VU METER",
            color = if (audioMeterMuted) WarningAmber else TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Professional 14-Segment Multi-Colored LED VU Meter Ladder
        Row(
            modifier = Modifier
                .weight(1f)
                .width(48.dp)
                .background(BackgroundCanvas, RoundedCornerShape(6.dp))
                .border(1.dp, BorderDivider, RoundedCornerShape(6.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProfessionalVuMeterBar(
                levelPercent = audioLevelPercent,
                isMuted = audioMeterMuted,
                modifier = Modifier.weight(1f)
            )
            ProfessionalVuMeterBar(
                levelPercent = (audioLevelPercent * 0.95f).toInt(),
                isMuted = audioMeterMuted,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onMuteToggled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (audioMeterMuted) WarningAmber else BorderDivider
            )
        ) {
            Text(if (audioMeterMuted) "UNMUTE" else "MUTE", fontSize = 11.sp, color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // REC Local MP4 Recording Button with Live Timer Counter
        val isRecording = recordingState == RecordingState.RECORDING
        Button(
            onClick = onRecordClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) LiveRed else BorderDivider
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isRecording) Color.White else LiveRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRecording) formatDuration(recordedDurationMs) else "REC MP4",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // GO LIVE Main Production Button
        val isLive = sessionState == LiveSessionState.LIVE || sessionState == LiveSessionState.STARTING
        Button(
            onClick = onGoLiveClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLive) LiveRed else ReadyGreen
            )
        ) {
            Text(
                text = if (isLive) "STOP LIVE" else "GO LIVE",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Broadcast-Grade 14-Segment LED VU Meter Bar (Green -> Yellow -> Red Peak)
 */
@Composable
private fun ProfessionalVuMeterBar(
    levelPercent: Int,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val totalSegments = 14
    val effectivePercent = if (isMuted) 0 else levelPercent.coerceIn(0, 100)
    val activeSegmentCount = (effectivePercent * totalSegments / 100)

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
    ) {
        for (index in (totalSegments - 1) downTo 0) {
            val isLit = index < activeSegmentCount && !isMuted
            val segmentColor = when {
                index >= 12 -> LiveRed          // Top 2 segments (85% - 100%): RED PEAK CLIPPING
                index >= 9 -> WarningAmber     // Middle 3 segments (65% - 85%): YELLOW WARNING
                else -> ReadyGreen              // Bottom 9 segments (0% - 65%): GREEN NORMAL
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isLit) segmentColor else segmentColor.copy(alpha = 0.12f))
            )
        }
    }
}
