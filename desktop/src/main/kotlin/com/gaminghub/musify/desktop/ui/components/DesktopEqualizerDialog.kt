package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gaminghub.musify.desktop.ui.theme.*
import com.gaminghub.musify.desktop.viewmodel.DesktopMusicViewModel
import kotlin.math.roundToInt

@Composable
fun DesktopEqualizerDialog(viewModel: DesktopMusicViewModel) {
    val bands by viewModel.equalizerBands.collectAsState()
    val bandLabels = listOf("32Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    val presets = listOf(
        "Flat", "Bass Boost", "Treble Boost", "Rock", "Pop",
        "Jazz", "Electronic", "Classical", "Vocal Boost", "Acoustic"
    )
    var selectedPreset by remember { mutableStateOf("Flat") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { viewModel.closeEqualizer() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier
                .width(860.dp)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Graphic Equalizer",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                color = MusifyEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "10-BAND STUDIO DSP",
                                    color = MusifyEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Precision audio tone curves with zero clipping & dynamic range preservation",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Close Button ('X')
                    IconButton(
                        onClick = { viewModel.closeEqualizer() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Equalizer",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Presets Bar & Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Preset:",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        Box {
                            OutlinedButton(
                                onClick = { expandedDropdown = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceDark,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(selectedPreset, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .border(1.dp, SurfaceElevated, RoundedCornerShape(8.dp))
                            ) {
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                preset,
                                                color = if (preset == selectedPreset) MusifyEmerald else TextPrimary,
                                                fontWeight = if (preset == selectedPreset) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedPreset = preset
                                            viewModel.audioPlayer.setEqualizerPreset(preset)
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                selectedPreset = "Flat"
                                viewModel.audioPlayer.setEqualizerPreset("Flat")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MusifyEmerald
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset (0 dB)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Studio Vertical Faders Console
                Surface(
                    color = Color(0xFF141414),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Reference Scale Column
                        Column(
                            modifier = Modifier
                                .width(46.dp)
                                .height(270.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("+12 dB", color = MusifyEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("+6 dB", color = TextTertiary, fontSize = 10.sp)
                            Text("0 dB", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("-6 dB", color = TextTertiary, fontSize = 10.sp)
                            Text("-12 dB", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 10 Band Vertical Sliders with Long Sliders
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 10) {
                                val gain = if (i < bands.size) bands[i] else 0f
                                VerticalEqualizerFader(
                                    gain = gain,
                                    label = bandLabels[i],
                                    onGainChange = { newGain ->
                                        selectedPreset = "Custom"
                                        viewModel.audioPlayer.setBandGain(i, newGain)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom hint & status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tip: Drag the faders up or down, or click any band to adjust. Double-click any fader to reset to 0 dB.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = { viewModel.closeEqualizer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MusifyEmerald,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity Studio Vertical Equalizer Fader.
 * Features a tall 240dp track, 0 dB center reference line,
 * bi-directional active gain bar, precision draggable knob, and tap-to-set.
 */
@Composable
fun VerticalEqualizerFader(
    gain: Float,
    label: String,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val trackHeight = 220.dp
    val knobHeight = 18.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain dB readout on top
        val gainText = when {
            gain > 0.05f -> "+${"%.1f".format(gain)}"
            gain < -0.05f -> "%.1f".format(gain)
            else -> "0.0"
        }
        val gainColor = when {
            gain > 0.05f -> MusifyEmerald
            gain < -0.05f -> Color(0xFF64B5F6)
            else -> TextSecondary
        }

        Text(
            text = gainText,
            color = gainColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Tall Vertical Track with custom gesture handling
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(trackHeight)
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            onGainChange(0f)
                        },
                        onTap = { offset ->
                            val heightPx = size.height.toFloat()
                            if (heightPx > 0f) {
                                val fraction = 1f - (offset.y / heightPx).coerceIn(0f, 1f)
                                val newGain = -12f + fraction * 24f
                                val roundedGain = ((newGain * 2).roundToInt() / 2f).coerceIn(-12f, 12f)
                                onGainChange(roundedGain)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val heightPx = size.height.toFloat()
                        if (heightPx > 0f) {
                            val fraction = 1f - (change.position.y / heightPx).coerceIn(0f, 1f)
                            val newGain = -12f + fraction * 24f
                            val roundedGain = ((newGain * 2).roundToInt() / 2f).coerceIn(-12f, 12f)
                            onGainChange(roundedGain)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Background groove track
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF222222))
                    .border(0.5.dp, Color(0xFF333333), RoundedCornerShape(3.dp))
            )

            // Center 0 dB marker line
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(1.dp)
                    .background(Color(0xFF555555))
            )

            // Active Level Bar (from center 0dB upwards for positive, downwards for negative)
            val centerFraction = 0.5f
            val currentFraction = ((gain + 12f) / 24f).coerceIn(0f, 1f)

            if (gain > 0.05f) {
                // Fills from center (50%) up to currentFraction
                val fillHeightFraction = (currentFraction - centerFraction).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight(fillHeightFraction)
                        .align(Alignment.Center)
                        .offset(y = (-((trackHeight / 2) * fillHeightFraction)))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(MusifyEmerald, MusifyGreen)
                            )
                        )
                )
            } else if (gain < -0.05f) {
                // Fills from center (50%) down to currentFraction
                val fillHeightFraction = (centerFraction - currentFraction).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight(fillHeightFraction)
                        .align(Alignment.Center)
                        .offset(y = ((trackHeight / 2) * fillHeightFraction))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
                            )
                        )
                )
            }

            // Draggable Fader Knob / Thumb
            // Calculate knob offset: top is fraction 1f (-trackHeight/2 + knobHeight/2), bottom is 0f
            val travelDistance = trackHeight - knobHeight
            val knobOffsetY = (travelDistance / 2) - (travelDistance * currentFraction)

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF2E2E2E),
                border = BorderStroke(
                    1.dp,
                    if (gain != 0f) MusifyEmerald else Color(0xFF555555)
                ),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .width(34.dp)
                    .height(knobHeight)
                    .offset(y = knobOffsetY)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Center indicator line on the knob
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (gain != 0f) MusifyEmerald else Color.White)
                    )
                }
            }
        }

        // Frequency Label at bottom
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
