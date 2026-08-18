package com.gaminghub.musify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onStartTimer: (minutes: Int, stopAtEnd: Boolean) -> Unit,
    isTimerRunning: Boolean,
    onStopTimer: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(30) }
    var stopAtEnd by remember { mutableStateOf(false) }
    
    val timeOptions = listOf(5, 15, 30, 45, 60, 90)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sleep Timer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                if (isTimerRunning) {
                    Text("Timer is running", color = Color(0xFF00E5FF), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onStopTimer(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Text("Stop Timer", color = Color.White)
                    }
                } else {
                    // Time Selection Grid
                    Text("Select Duration", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column {
                        timeOptions.chunked(3).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEach { minutes ->
                                    val isSelected = selectedMinutes == minutes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMinutes = minutes },
                                        label = { Text("$minutes m") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00E5FF),
                                            selectedLabelColor = Color.Black,
                                            labelColor = Color.White,
                                            containerColor = Color.Gray.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // "Stop at end of song" toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stop at end of current song", color = Color.White, fontSize = 14.sp)
                        Switch(
                            checked = stopAtEnd,
                            onCheckedChange = { stopAtEnd = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00E5FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onStartTimer(selectedMinutes, stopAtEnd); onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Text("Start Timer", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
