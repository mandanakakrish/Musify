package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel

@Composable
fun DesktopEqualizerDialog(viewModel: DesktopMusicViewModel) {
    val bands by viewModel.equalizerBands.collectAsState()
    val bandLabels = listOf("32Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    val presets = listOf("Flat", "Bass Boost", "Treble Boost", "Rock", "Pop", "Jazz", "Electronic", "Classical", "Vocal Boost", "Acoustic")
    var selectedPreset by remember { mutableStateOf("Flat") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { viewModel.toggleEqualizer() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier.width(620.dp).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Graphic Equalizer",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "10-Band DSP Audio Equalizer",
                            color = MusifyEmerald,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = { viewModel.toggleEqualizer() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Dropdown & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Preset: $selectedPreset")
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.background(SurfaceElevated)
                        ) {
                            presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset, color = TextPrimary) },
                                    onClick = {
                                        selectedPreset = preset
                                        viewModel.audioPlayer.setEqualizerPreset(preset)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    TextButton(onClick = {
                        selectedPreset = "Flat"
                        viewModel.audioPlayer.setEqualizerPreset("Flat")
                    }) {
                        Text("Reset to Flat", color = MusifyEmerald)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 10 Band Vertical Sliders
                Row(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 10) {
                        val gain = if (i < bands.size) bands[i] else 0f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${gain.toInt()} dB",
                                color = if (gain != 0f) MusifyEmerald else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Slider(
                                value = gain,
                                onValueChange = { newGain ->
                                    viewModel.audioPlayer.setBandGain(i, newGain)
                                },
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = TextPrimary,
                                    activeTrackColor = MusifyEmerald,
                                    inactiveTrackColor = SurfaceElevated
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = bandLabels[i],
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
