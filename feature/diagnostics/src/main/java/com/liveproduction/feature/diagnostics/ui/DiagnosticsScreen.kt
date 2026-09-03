package com.liveproduction.feature.diagnostics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liveproduction.feature.diagnostics.DiagnosticsViewModel

private val BackgroundCanvas = Color(0xFF0F1216)
private val SurfacePanel = Color(0xFF181C22)
private val BorderDivider = Color(0xFF2E3542)
private val TextPrimary = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF94A3B8)
private val ReadyGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onBack: () -> Unit
) {
    val report by viewModel.capabilityReport.collectAsState()
    val cameraCaps by viewModel.cameraCapabilities.collectAsState()
    val usbStatus by viewModel.usbStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "HARDWARE DIAGNOSTICS & CAPABILITY REPORT",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDiagnostics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePanel)
            )
        },
        containerColor = BackgroundCanvas
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            report?.let { rep ->
                item {
                    DiagnosticCard(title = "SYSTEM & HARDWARE OVERVIEW") {
                        MetricRow("Device Model", rep.deviceModel)
                        MetricRow("Android Version", rep.androidVersion)
                        MetricRow("SoC Hardware", rep.socName)
                        MetricRow("USB Host Support", if (rep.hasUsbHostFeature) "SUPPORTED" else "NOT_SUPPORTED", if (rep.hasUsbHostFeature) ReadyGreen else Color.Red)
                        MetricRow("System Memory (RAM)", "${rep.availableRamMb} MB free / ${rep.totalRamMb} MB total")
                        MetricRow("Battery Level", "${rep.batteryPercent}% (${if (rep.isCharging) "Charging" else "Discharging"})")
                        MetricRow("Thermal Status", rep.thermalStatus, if (rep.thermalStatus == "NORMAL") ReadyGreen else Color.Yellow)
                    }
                }

                item {
                    DiagnosticCard(title = "USB UVC HDMI CAPTURE CARD STATUS") {
                        MetricRow("USB Status", usbStatus.name)
                    }
                }

                item {
                    DiagnosticCard(title = "CAMERA2 HARDWARE CHARACTERISTICS") {
                        cameraCaps.forEach { (sourceType, cap) ->
                            MetricRow(
                                label = sourceType.name,
                                value = "ID: ${cap.cameraId} | Focal: ${cap.focalLength}mm | MultiCam: ${cap.isLogicalMultiCamera}"
                            )
                        }
                    }
                }

                item {
                    DiagnosticCard(title = "MEDIACODEC HARDWARE ENCODERS") {
                        rep.h264Codec?.let { videoCodec ->
                            MetricRow("H.264 Video Encoder", if (videoCodec.isHardwareAccelerated) "HARDWARE (OMX/C2)" else "SOFTWARE", ReadyGreen)
                            MetricRow("Max Resolution", "${videoCodec.maxSupportedWidth} x ${videoCodec.maxSupportedHeight} @ ${videoCodec.maxSupportedFrameRate} FPS")
                            MetricRow("Max Bitrate", "${videoCodec.maxBitrateBps / 1_000_000} Mbps")
                        }
                        rep.aacCodec?.let { audioCodec ->
                            MetricRow("AAC Audio Encoder", if (audioCodec.isHardwareAccelerated) "HARDWARE" else "SOFTWARE", ReadyGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfacePanel, RoundedCornerShape(8.dp))
            .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = AccentBlue,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
