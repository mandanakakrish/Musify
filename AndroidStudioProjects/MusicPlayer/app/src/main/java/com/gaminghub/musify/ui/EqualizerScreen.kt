package com.gaminghub.musify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.gaminghub.musify.ui.viewmodels.AudioEngineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun EqualizerScreen(
    audioEngineViewModel: AudioEngineViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fftData by audioEngineViewModel.fftData.collectAsState()
    val waveformData by audioEngineViewModel.waveformData.collectAsState()
    val isEqualizerEnabled by audioEngineViewModel.isEqualizerEnabled.collectAsState()
    val bands by audioEngineViewModel.equalizerBands.collectAsState()
    val bassBoost by audioEngineViewModel.bassBoostLevel.collectAsState()
    val virtualizer by audioEngineViewModel.virtualizerLevel.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    // Start visualizer using session ID from Service
    val isPlaying by audioEngineViewModel.isPlaying.collectAsState()
    LaunchedEffect(isPlaying, hasPermission) {
        if (isPlaying && hasPermission) {
            val sessionId = com.gaminghub.musicplayer.MusicPlaybackService.currentAudioSessionId
            if (sessionId != -1) {
                audioEngineViewModel.startVisualizer(sessionId)
            }
        } else {
            audioEngineViewModel.stopVisualizer()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioEngineViewModel.stopVisualizer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer & Visualizer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Switch(
                        checked = isEqualizerEnabled,
                        onCheckedChange = { audioEngineViewModel.toggleEqualizer(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visualizer Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                if (hasPermission) {
                    VisualizerView(
                        fftData = fftData,
                        waveformData = waveformData,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) {
                            @Suppress("DEPRECATION")
                            Text("Grant Audio Permission")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Equalizer Bands
            Text(
                "Frequency Bands",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Using Row with horizontalScroll for responsiveness on all devices
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp) // Much taller for 'long height' look
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                bands.forEach { (band, level) ->
                    EqualizerBandItem(
                        band = band,
                        level = level,
                        enabled = isEqualizerEnabled,
                        onValueChange = { audioEngineViewModel.updateBandLevel(band, it.toInt()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Enhancements (Bass & Virtualizer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bass Boost
                EnhancementControl(
                    label = "Bass Boost",
                    value = bassBoost.toFloat(),
                    onValueChange = { audioEngineViewModel.updateBassBoost(it.toInt()) },
                    enabled = isEqualizerEnabled,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )

                // Virtualizer
                EnhancementControl(
                    label = "Virtualizer",
                    value = virtualizer.toFloat(),
                    onValueChange = { audioEngineViewModel.updateVirtualizer(it.toInt()) },
                    enabled = isEqualizerEnabled,
                    color = Color(0xFFFF4081),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EqualizerBandItem(
    band: Int,
    level: Int,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp) // Wider for better horizontal spacing
            .fillMaxHeight()
            .padding(vertical = 12.dp) // Space for labels
    ) {
        // Level Indicator
        Text(
            "${level / 100} dB",
            color = if (enabled) Color(0xFF00E5FF) else Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Use weight for maximum vertical space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = level.toFloat(),
                onValueChange = onValueChange,
                valueRange = -1500f..1500f,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                    }
                    .width(320.dp), // Height of the slider when vertical (Extra Long Height)
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                )
            )
        }
        
        Text(
            "Band $band",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EnhancementControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { value / 1000f },
                modifier = Modifier.size(54.dp),
                color = color,
                strokeWidth = 5.dp,
                trackColor = color.copy(alpha = 0.1f)
            )
            Text(
                "${(value / 10).toInt()}%",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1000f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}

