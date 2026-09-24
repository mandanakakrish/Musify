package com.gaminghub.musicplayer.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.gaminghub.musicplayer.SettingsViewModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

@Composable
fun ThemeSettingsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val useSystemTheme by settingsViewModel.useSystemTheme.collectAsState()
    val accentColorName by settingsViewModel.accentColorName.collectAsState()
    val accentColorHex by settingsViewModel.accentColorHex.collectAsState()
    val useAmoled by settingsViewModel.useAmoled.collectAsState()
    val canvasColor by settingsViewModel.canvasColor.collectAsState()
    val cardColor by settingsViewModel.cardColor.collectAsState()
    val currentTheme by settingsViewModel.currentTheme.collectAsState()
    val backgroundGradient by settingsViewModel.backgroundGradient.collectAsState()
    val cardGradient by settingsViewModel.cardGradient.collectAsState()
    val bottomSheetGradient by settingsViewModel.bottomSheetGradient.collectAsState()

    var showAccentPalette by remember { mutableStateOf(false) }
    var showGradientDialog by remember { mutableStateOf(false) }
    var activeGradientTarget by remember { mutableStateOf("Background") }

    var showCanvasDropdown by remember { mutableStateOf(false) }
    var showCardDropdown by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }

    val currentColor = remember(accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (_: Exception) {
            Color(0xFF1DB954)
        }
    }

    val paletteItems = remember {
        listOf(
            Pair("#1DB954", "Spotify Green"),
            Pair("#00C853", "Vibrant Green"),
            Pair("#64DD17", "Lime Green"),
            Pair("#2196F3", "Material Blue"),
            Pair("#00BCD4", "Cyan"),
            Pair("#009688", "Teal"),
            Pair("#9C27B0", "Purple"),
            Pair("#673AB7", "Deep Purple"),
            Pair("#3F51B5", "Indigo"),
            Pair("#E91E63", "Neon Pink"),
            Pair("#FF5722", "Deep Orange"),
            Pair("#FF9800", "Orange"),
            Pair("#FFC107", "Amber"),
            Pair("#FFD600", "Electric Yellow"),
            Pair("#D50000", "Crimson Red"),
            Pair("#FFFFFF", "Pure White")
        )
    }

    val gradientPresets = remember {
        listOf(
            listOf(Color(0xFF1E1E1E), Color(0xFF0D0D0D)),
            listOf(Color(0xFF282828), Color(0xFF121212)),
            listOf(Color(0xFF1A1A24), Color(0xFF0A0A0F)),
            listOf(Color(0xFF1F1B24), Color(0xFF121212)),
            listOf(Color(0xFF000000), Color(0xFF000000))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (useAmoled || canvasColor == "Black") Color.Black else Color(0xFF121212))
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top Header ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "Theme",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 1. Dark Mode ─────────────────────────────────────────
        ThemeSwitchRow(
            title = "Dark Mode",
            subtitle = if (useSystemTheme) "Controlled by System Theme" else null,
            checked = isDarkMode,
            onCheckedChange = { settingsViewModel.setDarkMode(it) }
        )

        // ── 2. Use System Theme ──────────────────────────────────
        ThemeSwitchRow(
            title = "Use System Theme",
            subtitle = "Follow system dark/light mode automatically",
            checked = useSystemTheme,
            onCheckedChange = { settingsViewModel.setUseSystemTheme(it) }
        )

        // ── 3. Accent Color & Hue ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAccentPalette = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Accent Color & Hue", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(accentColorName, color = Color.Gray, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }

        // ── 4. Background Gradient ───────────────────────────────
        GradientOptionRow(
            title = "Background Gradient",
            subtitle = "Gradient used as background everywhere (Preset #${backgroundGradient + 1})",
            onClick = {
                activeGradientTarget = "Background"
                showGradientDialog = true
            }
        )

        // ── 5. Card Gradient ─────────────────────────────────────
        GradientOptionRow(
            title = "Card Gradient",
            subtitle = "Gradient used in Cards (Preset #${cardGradient + 1})",
            onClick = {
                activeGradientTarget = "Cards"
                showGradientDialog = true
            }
        )

        // ── 6. Bottom Sheets Gradient ────────────────────────────
        GradientOptionRow(
            title = "Bottom Sheets Gradient",
            subtitle = "Gradient used in Bottom Sheets (Preset #${bottomSheetGradient + 1})",
            onClick = {
                activeGradientTarget = "Bottom Sheets"
                showGradientDialog = true
            }
        )

        // ── 7. Canvas Color ──────────────────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCanvasDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Canvas Color", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Color of Background Canvas", color = Color.Gray, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(canvasColor, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = Color.Gray, fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showCanvasDropdown,
                onDismissRequest = { showCanvasDropdown = false },
                modifier = Modifier.background(Color(0xFF242424))
            ) {
                listOf("Black", "Grey").forEach { option ->
                    val isSelected = canvasColor == option
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                color = if (isSelected) currentColor else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setCanvasColor(option)
                            showCanvasDropdown = false
                        }
                    )
                }
            }
        }

        // ── 8. Card Color ────────────────────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCardDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Card Color", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Color of Search Bar, Alert Dialogs, Cards", color = Color.Gray, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cardColor, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = Color.Gray, fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showCardDropdown,
                onDismissRequest = { showCardDropdown = false },
                modifier = Modifier.background(Color(0xFF242424))
            ) {
                listOf("Grey800", "Grey850", "Grey900", "Black").forEach { option ->
                    val isSelected = cardColor == option
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                color = if (isSelected) currentColor else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setCardColor(option)
                            showCardDropdown = false
                        }
                    )
                }
            }
        }

        // ── 9. Use Amoled Dark Mode Settings ─────────────────────
        ThemeSwitchRow(
            title = "Use Amoled Dark Mode Settings",
            subtitle = "Deep pure black background for AMOLED screens to save battery",
            checked = useAmoled,
            onCheckedChange = {
                settingsViewModel.setUseAmoled(it)
                Toast.makeText(context, "AMOLED mode ${if (it) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
            }
        )

        // ── 10. Current Theme ────────────────────────────────────
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current Theme", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Active preset theme style", color = Color.Gray, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentTheme, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▼", color = Color.Gray, fontSize = 10.sp)
                }
            }

            DropdownMenu(
                expanded = showThemeDropdown,
                onDismissRequest = { showThemeDropdown = false },
                modifier = Modifier.background(Color(0xFF242424))
            ) {
                listOf("Default", "Custom").forEach { option ->
                    val isSelected = currentTheme == option
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                color = if (isSelected) currentColor else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            settingsViewModel.setCurrentTheme(option)
                            showThemeDropdown = false
                        }
                    )
                }
            }
        }

        // ── 11. Save Theme ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    settingsViewModel.setCurrentTheme("Custom")
                    Toast.makeText(context, "Theme settings saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Save Theme", color = currentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // ── Accent Color Palette Modal Dialog ────────────────────────
    if (showAccentPalette) {
        Dialog(onDismissRequest = { showAccentPalette = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF181818),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Choose Accent Color",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    val rows = paletteItems.chunked(4)
                    rows.forEach { rowColors ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            rowColors.forEach { (hex, name) ->
                                val color = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (_: Exception) {
                                    Color(0xFF1DB954)
                                }
                                val isSelected = hex.equals(accentColorHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable {
                                            settingsViewModel.setAccentColor(hex, name)
                                            showAccentPalette = false
                                            Toast.makeText(context, "Accent color set to $name", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Gradient Selection Dialog ────────────────────────────────
    if (showGradientDialog) {
        val currentGradIdx = when (activeGradientTarget) {
            "Background" -> backgroundGradient
            "Cards" -> cardGradient
            else -> bottomSheetGradient
        }

        Dialog(onDismissRequest = { showGradientDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select $activeGradientTarget Gradient",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showGradientDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    gradientPresets.forEachIndexed { index, grad ->
                        val isSelected = index == currentGradIdx
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(vertical = 4.dp)
                                .clickable {
                                    when (activeGradientTarget) {
                                        "Background" -> settingsViewModel.setBackgroundGradient(index)
                                        "Cards" -> settingsViewModel.setCardGradient(index)
                                        else -> settingsViewModel.setBottomSheetGradient(index)
                                    }
                                    showGradientDialog = false
                                    Toast.makeText(context, "$activeGradientTarget gradient updated", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(grad), shape = RoundedCornerShape(14.dp))
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) currentColor else Color(0x33FFFFFF),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color.Gray, fontSize = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MusifyGreen,
                uncheckedThumbColor = Color(0xFFB0B0B0),
                uncheckedTrackColor = Color(0xFF383838)
            )
        )
    }
}

@Composable
fun GradientOptionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Normal)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF282828), Color(0xFF101010))
                    )
                )
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
        )
    }
}
