package com.gaminghub.musify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gaminghub.musify.ui.viewmodels.AudioEngineViewModel

@Composable
fun PremiumEqualizerDialog(
    audioEngineViewModel: AudioEngineViewModel,
    onDismiss: () -> Unit
) {
    val isEqualizerEnabled by audioEngineViewModel.isEqualizerEnabled.collectAsState()
    val bands by audioEngineViewModel.equalizerBands.collectAsState()
    val bassLevel by audioEngineViewModel.bassBoostLevel.collectAsState()
    val virtualizerLevel by audioEngineViewModel.virtualizerLevel.collectAsState()

    val themeColor = Color(0xFF00E5FF)
    val surfaceColor = Color(0xFF121212)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Equalizer",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isEqualizerEnabled,
                        onCheckedChange = { audioEngineViewModel.toggleEqualizer(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = themeColor,
                            checkedTrackColor = themeColor.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Equalizer Bands ─────────────────────────────
                if (bands.isNotEmpty()) {
                    Text(
                        text = "Frequency Bands",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                    )
                    
                    bands.forEach { (band, level) ->
                        val label = when(band) {
                            0 -> "60 Hz"
                            1 -> "230 Hz"
                            2 -> "910 Hz"
                            3 -> "3.6 kHz"
                            4 -> "14 kHz"
                            else -> "Band $band"
                        }
                        
                        EqualizerSlider(
                            label = label,
                            value = level.toFloat(),
                            range = -1500f..1500f, // typically milliBels
                            enabled = isEqualizerEnabled,
                            onValueChange = { audioEngineViewModel.updateBandLevel(band, it.toInt()) },
                            themeColor = themeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Bass Boost ──────────────────────────────────
                Text(
                    text = "Bass Boost",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )
                EqualizerSlider(
                    label = "Strength",
                    value = bassLevel.toFloat(),
                    range = 0f..1000f,
                    enabled = isEqualizerEnabled,
                    onValueChange = { audioEngineViewModel.updateBassBoost(it.toInt()) },
                    themeColor = themeColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Virtualizer ──────────────────────────────────
                Text(
                    text = "Surround Sound",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )
                EqualizerSlider(
                    label = "Strength",
                    value = virtualizerLevel.toFloat(),
                    range = 0f..1000f,
                    enabled = isEqualizerEnabled,
                    onValueChange = { audioEngineViewModel.updateVirtualizer(it.toInt()) },
                    themeColor = themeColor
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EqualizerSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    themeColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White.copy(alpha = if (enabled) 0.9f else 0.4f), fontSize = 13.sp)
            Text(
                "${value.toInt()}", 
                color = themeColor.copy(alpha = if (enabled) 1f else 0.4f), 
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = themeColor,
                activeTrackColor = themeColor,
                inactiveTrackColor = Color.DarkGray,
                disabledThumbColor = Color.Gray,
                disabledActiveTrackColor = Color.DarkGray
            )
        )
    }
}
