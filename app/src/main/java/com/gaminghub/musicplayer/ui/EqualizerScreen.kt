package com.gaminghub.musicplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.gaminghub.musicplayer.ui.theme.MusifyDarkBg
import com.gaminghub.musicplayer.ui.theme.MusifyGlassBorder
import com.gaminghub.musicplayer.ui.theme.MusifyGlassSurface
import com.gaminghub.musicplayer.ui.theme.MusifyGreen
import com.gaminghub.musicplayer.util.EqualizerManager

@Composable
fun EqualizerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val eqManager = remember { EqualizerManager.getInstance(context) }
    val isEnabled by eqManager.isEnabled.collectAsState()
    val selectedPreset by eqManager.selectedPreset.collectAsState()
    val bandLevels by eqManager.bandLevels.collectAsState()
    val bassBoost by eqManager.bassBoostStrength.collectAsState()
    val virtualizer by eqManager.virtualizerStrength.collectAsState()

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Equalizer & Audio Effects",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { eqManager.setEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = MusifyGreen,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF333333)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Presets Selector ──────────────────────────────────────
            Text(
                text = "Presets",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eqManager.presets.forEach { preset ->
                    val isSelected = selectedPreset == preset.name
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MusifyGreen else MusifyGlassSurface,
                        border = BorderStroke(1.dp, if (isSelected) MusifyGreen else MusifyGlassBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = isEnabled) { eqManager.setPreset(preset.name) }
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isSelected) Color.Black else if (isEnabled) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ── 5-Band Equalizer Sliders ──────────────────────────────
            Surface(
                color = MusifyGlassSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MusifyGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Graphic Equalizer",
                        color = if (isEnabled) Color.White else Color.Gray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    bandFrequencies.forEachIndexed { index, freqLabel ->
                        val level = if (index < bandLevels.size) bandLevels[index] else 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = freqLabel,
                                color = if (isEnabled) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(60.dp)
                            )
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { eqManager.setBandLevel(index, it.toInt()) },
                                valueRange = -15f..15f,
                                enabled = isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = MusifyGreen,
                                    activeTrackColor = MusifyGreen,
                                    inactiveTrackColor = Color(0xFF333344)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${if (level > 0) "+" else ""}$level dB",
                                color = if (!isEnabled) Color.Gray else if (level != 0) MusifyGreen else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // ── Bass Boost & 3D Virtualizer ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
                Surface(
                    color = MusifyGlassSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bass Boost",
                                color = if (isEnabled) Color.White else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$bassBoost%",
                                color = MusifyGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { eqManager.setBassBoost(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MusifyGreen,
                                activeTrackColor = MusifyGreen,
                                inactiveTrackColor = Color(0xFF333344)
                            )
                        )
                    }
                }

                // 3D Virtualizer Card
                Surface(
                    color = MusifyGlassSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MusifyGlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3D Surround",
                                color = if (isEnabled) Color.White else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$virtualizer%",
                                color = MusifyGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = virtualizer.toFloat(),
                            onValueChange = { eqManager.setVirtualizer(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MusifyGreen,
                                activeTrackColor = MusifyGreen,
                                inactiveTrackColor = Color(0xFF333344)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun EqualizerSimpleDialog(onDismiss: () -> Unit) = QuickEqualizerDialog(onDismiss)

@Composable
fun QuickEqualizerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val eqManager = remember { EqualizerManager.getInstance(context) }
    val isEnabled by eqManager.isEnabled.collectAsState()
    val selectedPreset by eqManager.selectedPreset.collectAsState()
    val bandLevels by eqManager.bandLevels.collectAsState()
    val bassBoost by eqManager.bassBoostStrength.collectAsState()
    val virtualizer by eqManager.virtualizerStrength.collectAsState()

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E1E28),
            border = BorderStroke(1.dp, MusifyGlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MusifyGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Equalizer",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { eqManager.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = MusifyGreen,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF333344)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Text("Presets:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    eqManager.presets.forEach { preset ->
                        val isSelected = selectedPreset == preset.name
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MusifyGreen else Color(0xFF282836),
                            modifier = Modifier.clickable(enabled = isEnabled) { eqManager.setPreset(preset.name) }
                        ) {
                            Text(
                                text = preset.name,
                                color = if (isSelected) Color.Black else if (isEnabled) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 5 Frequency Bands ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frequency Bands",
                        color = if (isEnabled) Color.White else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { eqManager.setPreset("Flat") },
                        enabled = isEnabled,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Reset", color = if (isEnabled) MusifyGreen else Color.Gray, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                bandFrequencies.forEachIndexed { index, freqLabel ->
                    val level = if (index < bandLevels.size) bandLevels[index] else 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freqLabel,
                            color = if (isEnabled) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(55.dp)
                        )
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { eqManager.setBandLevel(index, it.toInt()) },
                            valueRange = -15f..15f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MusifyGreen,
                                activeTrackColor = MusifyGreen,
                                inactiveTrackColor = Color(0xFF333344)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${if (level > 0) "+" else ""}$level dB",
                            color = if (!isEnabled) Color.Gray else if (level != 0) MusifyGreen else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Bass Boost & 3D Surround ──────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bass Boost", color = if (isEnabled) Color.White else Color.Gray, fontSize = 12.sp)
                    Text("$bassBoost%", color = if (isEnabled) MusifyGreen else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bassBoost.toFloat(),
                    onValueChange = { eqManager.setBassBoost(it.toInt()) },
                    valueRange = 0f..100f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(thumbColor = MusifyGreen, activeTrackColor = MusifyGreen, inactiveTrackColor = Color(0xFF333344))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("3D Surround", color = if (isEnabled) Color.White else Color.Gray, fontSize = 12.sp)
                    Text("$virtualizer%", color = if (isEnabled) MusifyGreen else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = virtualizer.toFloat(),
                    onValueChange = { eqManager.setVirtualizer(it.toInt()) },
                    valueRange = 0f..100f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(thumbColor = MusifyGreen, activeTrackColor = MusifyGreen, inactiveTrackColor = Color(0xFF333344))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
