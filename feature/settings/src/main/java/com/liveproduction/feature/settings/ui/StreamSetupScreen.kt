package com.liveproduction.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liveproduction.core.streaming.model.SocialPlatformType
import com.liveproduction.core.streaming.model.StreamProfile
import com.liveproduction.feature.settings.StreamSetupViewModel

private val BackgroundCanvas = Color(0xFF0F1216)
private val SurfacePanel = Color(0xFF181C22)
private val BorderDivider = Color(0xFF2E3542)
private val TextPrimary = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF94A3B8)
private val ReadyGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSetupScreen(
    viewModel: StreamSetupViewModel,
    onBack: () -> Unit
) {
    var selectedPlatform by remember { mutableStateOf(SocialPlatformType.YOUTUBE) }
    var selectedProfile by remember { mutableStateOf(StreamProfile.PROFILE_1080P_30) }
    var customBitrateMbps by remember { mutableFloatStateOf(4.5f) }
    var selectedAudioBitrateKbps by remember { mutableIntStateOf(128) }
    var rtmpUrlInput by remember { mutableStateOf(selectedPlatform.defaultUrl) }
    var streamKeyInput by remember { mutableStateOf("") }
    var bannerTextInput by remember { mutableStateOf("LIVE PRODUCTION HD") }
    var isKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BROADCAST SETTINGS & ENCODER CONFIG",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePanel)
            )
        },
        bottomBar = {
            Surface(
                color = SurfacePanel,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            viewModel.saveDestinationConfig(
                                platformType = selectedPlatform,
                                customUrl = rtmpUrlInput,
                                streamKey = streamKeyInput,
                                profile = selectedProfile
                            )
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE CONFIGURATION", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = BackgroundCanvas
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CardPanel(title = "1. VIDEO RESOLUTION & QUALITY PROFILE") {
                    StreamProfile.entries.forEach { prof ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedProfile == prof),
                                onClick = {
                                    selectedProfile = prof
                                    customBitrateMbps = prof.bitrateBps / 1_000_000f
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = prof.displayName, color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            item {
                CardPanel(title = "2. DYNAMIC VIDEO BITRATE ADJUSTMENT (DYNAMIC ENCODER)") {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Video Bitrate Target:", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${String.format("%.1f", customBitrateMbps)} Mbps (${(customBitrateMbps * 1000).toInt()} Kbps)",
                                color = AccentBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = customBitrateMbps,
                            onValueChange = { customBitrateMbps = it },
                            valueRange = 1.0f..12.0f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentBlue,
                                activeTrackColor = AccentBlue
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "1.0 Mbps (Low)", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "4.5 Mbps (1080p)", fontSize = 11.sp, color = ReadyGreen)
                            Text(text = "12.0 Mbps (Ultra)", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                CardPanel(title = "3. AUDIO QUALITY BITRATE (AAC-LC STEREO)") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(128, 160, 320).forEach { bitrateKbps ->
                            FilterChip(
                                selected = (selectedAudioBitrateKbps == bitrateKbps),
                                onClick = { selectedAudioBitrateKbps = bitrateKbps },
                                label = { Text("$bitrateKbps Kbps", fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            item {
                CardPanel(title = "4. STREAM DESTINATION PLATFORM") {
                    SocialPlatformType.entries.forEach { platform ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedPlatform == platform),
                                onClick = {
                                    selectedPlatform = platform
                                    rtmpUrlInput = platform.defaultUrl
                                    streamKeyInput = viewModel.getSavedStreamKey(platform)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = platform.displayName, color = TextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                CardPanel(title = "5. RTMP ENDPOINT & ENCRYPTED STREAM KEY") {
                    OutlinedTextField(
                        value = rtmpUrlInput,
                        onValueChange = { rtmpUrlInput = it },
                        label = { Text(text = "Server RTMP/RTMPS URL", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = streamKeyInput,
                        onValueChange = { streamKeyInput = it },
                        label = { Text(text = "Stream Key / Token (Encrypted)", color = TextSecondary) },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Text(text = if (isKeyVisible) "HIDE" else "SHOW", fontSize = 11.sp, color = AccentBlue)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                    )
                }
            }

            item {
                CardPanel(title = "6. LOWER-THIRD GRAPHIC BANNER OVERLAY") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = bannerTextInput,
                            onValueChange = { bannerTextInput = it },
                            label = { Text(text = "Banner Overlay Text", color = TextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { viewModel.updateLowerThird(true, bannerTextInput, "LIVE PRODUCTION") },
                            colors = ButtonDefaults.buttonColors(containerColor = ReadyGreen)
                        ) {
                            Text(text = "APPLY", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfacePanel)
            .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}
