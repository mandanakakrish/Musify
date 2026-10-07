package com.gaminghub.musify.desktop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
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
import java.util.Locale

@Composable
fun DesktopSpeedControlDialog(viewModel: DesktopMusicViewModel) {
    val currentSpeed by viewModel.playbackSpeed.collectAsState()
    val isSpeedLocked by viewModel.isSpeedLocked.collectAsState()

    var sliderSpeed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }
    var localSpeedLocked by remember(isSpeedLocked) { mutableStateOf(isSpeedLocked) }

    val row1 = listOf(0.50f, 0.75f, 1.0f, 1.25f)
    val row2 = listOf(1.50f, 1.75f, 2.0f)

    Dialog(onDismissRequest = { viewModel.closeSpeedDialog() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E28),
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MusifyEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Playback Speed",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Speed Readout
                Text(
                    text = String.format(Locale.US, "%.2fx", sliderSpeed),
                    color = MusifyEmerald,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Slider
                Slider(
                    value = sliderSpeed,
                    onValueChange = { v ->
                        sliderSpeed = (Math.round(v / 0.05f) * 0.05f).coerceIn(0.50f, 2.00f)
                        viewModel.setPlaybackSpeed(sliderSpeed)
                    },
                    valueRange = 0.50f..2.00f,
                    colors = SliderDefaults.colors(
                        thumbColor = MusifyEmerald,
                        activeTrackColor = MusifyEmerald,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.50x", color = Color.Gray, fontSize = 11.sp)
                    Text("1.00x", color = Color.Gray, fontSize = 11.sp)
                    Text("2.00x", color = Color.Gray, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row1.forEach { preset ->
                        val active = Math.abs(sliderSpeed - preset) < 0.01f
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) MusifyEmerald else SurfaceElevated,
                            border = BorderStroke(1.dp, if (active) MusifyEmerald else SurfaceHighlight),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    sliderSpeed = preset
                                    viewModel.setPlaybackSpeed(preset)
                                }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", preset).trimEnd('0').trimEnd('.') + "x",
                                    color = if (active) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2.forEach { preset ->
                        val active = Math.abs(sliderSpeed - preset) < 0.01f
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) MusifyEmerald else SurfaceElevated,
                            border = BorderStroke(1.dp, if (active) MusifyEmerald else SurfaceHighlight),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    sliderSpeed = preset
                                    viewModel.setPlaybackSpeed(preset)
                                }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", preset).trimEnd('0').trimEnd('.') + "x",
                                    color = if (active) Color.Black else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Speed Lock Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF262636),
                    border = BorderStroke(1.dp, if (localSpeedLocked) MusifyEmerald.copy(alpha = 0.5f) else SurfaceHighlight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Speed Lock",
                                tint = if (localSpeedLocked) MusifyEmerald else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Speed Lock",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Remember speed across songs",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = localSpeedLocked,
                            onCheckedChange = {
                                localSpeedLocked = it
                                viewModel.setSpeedLock(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = MusifyEmerald,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sliderSpeed = 1.0f
                            viewModel.setPlaybackSpeed(1.0f)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Reset (1.0x)")
                    }

                    Button(
                        onClick = {
                            viewModel.setPlaybackSpeed(sliderSpeed)
                            viewModel.setSpeedLock(localSpeedLocked)
                            viewModel.closeSpeedDialog()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyEmerald, contentColor = Color.Black)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
