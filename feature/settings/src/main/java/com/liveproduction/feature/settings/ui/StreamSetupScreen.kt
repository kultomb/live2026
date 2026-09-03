package com.liveproduction.feature.settings.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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

private val SurfacePanel = Color(0xFF14181F)
private val CardBackground = Color(0xFF1E242E)
private val BorderDivider = Color(0xFF2E3846)
private val TextPrimary = Color(0xFFF0F4F8)
private val TextSecondary = Color(0xFF94A3B8)
private val ReadyGreen = Color(0xFF10B981)
private val AccentBlue = Color(0xFF3B82F6)

private enum class SettingsTab(val label: String) {
    VIDEO("📹 VIDEO"),
    AUDIO("🎙 AUDIO"),
    STREAM("📡 STREAM"),
    OVERLAY("🎨 OVERLAY")
}

@Composable
fun StreamSetupScreen(
    viewModel: StreamSetupViewModel,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(SettingsTab.VIDEO) }
    var selectedPlatform by remember { mutableStateOf(SocialPlatformType.YOUTUBE) }
    var selectedProfile by remember { mutableStateOf(StreamProfile.PROFILE_1080P_30) }
    var customBitrateMbps by remember { mutableFloatStateOf(4.5f) }
    var selectedAudioBitrateKbps by remember { mutableIntStateOf(128) }
    var rtmpUrlInput by remember { mutableStateOf(selectedPlatform.defaultUrl) }
    var streamKeyInput by remember { mutableStateOf("") }
    var bannerTextInput by remember { mutableStateOf("LIVE PRODUCTION HD") }
    var isKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPlatform) {
        rtmpUrlInput = selectedPlatform.defaultUrl
        streamKeyInput = viewModel.getSavedStreamKey(selectedPlatform)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onBack() },
        contentAlignment = Alignment.CenterEnd
    ) {
        // Professional Broadcast Slide-Over Modal Drawer Panel
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 540.dp)
                .fillMaxWidth(0.65f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(top = 32.dp, bottom = 8.dp, end = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderDivider, RoundedCornerShape(12.dp)),
            color = SurfacePanel,
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙ BROADCAST ENCODER SETTINGS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Categorized Tab Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SettingsTab.entries.forEach { tab ->
                        val isSelected = (activeTab == tab)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AccentBlue else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content Area for Active Tab
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (activeTab) {
                        SettingsTab.VIDEO -> {
                            item {
                                SectionCard(title = "1. RESOLUTION & FRAME RATE") {
                                    StreamProfile.entries.forEach { prof ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedProfile = prof
                                                    customBitrateMbps = prof.bitrateBps / 1_000_000f
                                                }
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
                                            Text(
                                                text = prof.displayName,
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                SectionCard(title = "2. DYNAMIC VIDEO BITRATE ADJUSTMENT") {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Target Bitrate:", color = TextSecondary, fontSize = 12.sp)
                                            Text(
                                                text = "${String.format("%.1f", customBitrateMbps)} Mbps (${(customBitrateMbps * 1000).toInt()} Kbps)",
                                                color = AccentBlue,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
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
                                            Text(text = "1.0 Mbps", fontSize = 10.sp, color = TextSecondary)
                                            Text(text = "4.5 Mbps (Rec)", fontSize = 10.sp, color = ReadyGreen)
                                            Text(text = "12.0 Mbps", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        SettingsTab.AUDIO -> {
                            item {
                                SectionCard(title = "AUDIO BITRATE (AAC-LC STEREO)") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(128, 160, 320).forEach { bitrateKbps ->
                                            FilterChip(
                                                selected = (selectedAudioBitrateKbps == bitrateKbps),
                                                onClick = { selectedAudioBitrateKbps = bitrateKbps },
                                                label = { Text("$bitrateKbps Kbps", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AccentBlue,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsTab.STREAM -> {
                            item {
                                SectionCard(title = "PLATFORM PRESET") {
                                    SocialPlatformType.entries.forEach { platform ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedPlatform = platform
                                                    rtmpUrlInput = platform.defaultUrl
                                                    streamKeyInput = viewModel.getSavedStreamKey(platform)
                                                }
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
                                            Text(text = platform.displayName, color = TextPrimary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                SectionCard(title = "SERVER RTMP & ENCRYPTED STREAM KEY") {
                                    OutlinedTextField(
                                        value = rtmpUrlInput,
                                        onValueChange = { rtmpUrlInput = it },
                                        label = { Text(text = "Server RTMP URL", color = TextSecondary, fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = streamKeyInput,
                                        onValueChange = { streamKeyInput = it },
                                        label = { Text(text = "Stream Key / Token", color = TextSecondary, fontSize = 11.sp) },
                                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            TextButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                                Text(text = if (isKeyVisible) "HIDE" else "SHOW", fontSize = 10.sp, color = AccentBlue)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                                    )
                                }
                            }
                        }

                        SettingsTab.OVERLAY -> {
                            item {
                                SectionCard(title = "GRAPHIC BANNER OVERLAY") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = bannerTextInput,
                                            onValueChange = { bannerTextInput = it },
                                            label = { Text(text = "Lower Third Banner Text", color = TextSecondary, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.updateLowerThird(true, bannerTextInput, "LIVE PRODUCTION") },
                                            colors = ButtonDefaults.buttonColors(containerColor = ReadyGreen)
                                        ) {
                                            Text(text = "APPLY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Save Action Button
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE CONFIGURATION", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
