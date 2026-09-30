package com.gaminghub.musicplayer.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gaminghub.musicplayer.SettingsViewModel
import com.gaminghub.musicplayer.ui.theme.MusifyGreen

private val DarkSheetBackground = Color(0xFF101014)
private val DarkInputBackground = Color(0xFF18181D)
private val DarkInputBorder = Color(0xFF282830)
private val DarkSquircleBackground = Color(0xFF0F3520)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OthersSettingsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    val context = LocalContext.current
    val appLanguage by settingsViewModel.appLanguage.collectAsState()
    val minAudioLength by settingsViewModel.minAudioLengthSeconds.collectAsState()
    val excludedFolders by settingsViewModel.excludedFolders.collectAsState()
    val includedFolders by settingsViewModel.includedFolders.collectAsState()
    val liveSearch by settingsViewModel.liveSearch.collectAsState()
    val streamDownloaded by settingsViewModel.streamDownloaded.collectAsState()
    val searchLocalLyrics by settingsViewModel.searchLocalLyrics.collectAsState()
    val supportEqualizer by settingsViewModel.supportEqualizer.collectAsState()
    val stopOnClose by settingsViewModel.stopOnClose.collectAsState()
    val useProxy by settingsViewModel.useProxy.collectAsState()
    val proxyAddress by settingsViewModel.proxyAddress.collectAsState()
    val cacheSizeMb by settingsViewModel.cacheSizeMb.collectAsState()

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showFoldersSheet by remember { mutableStateOf(false) }
    var showMinLengthSheet by remember { mutableStateOf(false) }
    var showProxySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Others",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── 1. Language (App Text Language) ─────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLanguageSheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Language",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "App Text Language",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            Text(
                text = appLanguage,
                color = MusifyGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ── 2. Include/Exclude Folders ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showFoldersSheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Include/Exclude Folders",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Includes/Excludes selected folders from 'My Music' Section",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.size(22.dp)
            )
        }

        // ── 3. Min Audio Length to search music ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMinLengthSheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Min Audio Length to search music",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Audios with length smaller than this will not be shown in 'My Music' Section",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.size(22.dp)
            )
        }

        // ── 4. Live Search ──────────────────────────────────────
        ThemeSwitchRow(
            title = "Live Search",
            subtitle = "Search songs as soon as user stops typing",
            checked = liveSearch,
            onCheckedChange = { settingsViewModel.setLiveSearch(it) }
        )

        // ── 5. Stream Downloaded Songs, If available ────────────
        ThemeSwitchRow(
            title = "Stream Downloaded Songs, If available",
            subtitle = "If song is already downloaded, downloaded song will be played instead of streaming online",
            checked = streamDownloaded,
            onCheckedChange = { settingsViewModel.setStreamDownloaded(it) }
        )

        // ── 6. Search lyrics of local songs ─────────────────────
        ThemeSwitchRow(
            title = "Search lyrics of local songs",
            subtitle = "Search online if lyrics aren't available/downloaded for any offline song",
            checked = searchLocalLyrics,
            onCheckedChange = { settingsViewModel.setSearchLocalLyrics(it) }
        )

        // ── 7. Support Equalizer ────────────────────────────────
        ThemeSwitchRow(
            title = "Support Equalizer",
            subtitle = "Keep this off if you are unable to play songs (in both online and offline mode)",
            checked = supportEqualizer,
            onCheckedChange = { settingsViewModel.setSupportEqualizer(it) }
        )

        // ── 8. Stop music on App Close ──────────────────────────
        ThemeSwitchRow(
            title = "Stop music on App Close",
            subtitle = "Stop playback when application is closed from Recents/Task Manager",
            checked = stopOnClose,
            onCheckedChange = { settingsViewModel.setStopOnClose(it) }
        )

        // ── 9. Use Proxy ────────────────────────────────────────
        ThemeSwitchRow(
            title = "Use Proxy",
            subtitle = "Route search and streaming requests through custom proxy server",
            checked = useProxy,
            onCheckedChange = { settingsViewModel.setUseProxy(it) }
        )

        // ── 10. Proxy Settings ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showProxySheet = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Proxy Settings",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Change Proxy IP and Port",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            Text(proxyAddress, color = MusifyGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // ── 11. Clear Cached Details ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    settingsViewModel.clearCache()
                    Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Clear Cached Details", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Deletes cached album arts, stream data & temporary files",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(cacheSizeMb, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // ── Bottom Sheet: Proxy Settings ─────────────────────────────
    if (showProxySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val parts = remember(proxyAddress) {
            val s = proxyAddress.split(":")
            Pair(s.getOrNull(0) ?: "103.47.67.134", s.getOrNull(1) ?: "8080")
        }
        var ipAddress by remember { mutableStateOf(parts.first) }
        var portNumber by remember { mutableStateOf(parts.second) }

        ModalBottomSheet(
            onDismissRequest = { showProxySheet = false },
            sheetState = sheetState,
            containerColor = DarkSheetBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4A4A52))
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Squircle Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSquircleBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = null,
                        tint = MusifyGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Proxy Settings",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Change Proxy IP and Port",
                    color = Color(0xFF9E9EA6),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // IP Address Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "IP ADDRESS",
                        color = Color(0xFF8E8E98),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CustomDarkInputField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = Color(0xFF757580),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardType = KeyboardType.Text
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Port Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PORT",
                        color = Color(0xFF8E8E98),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CustomDarkInputField(
                        value = portNumber,
                        onValueChange = { portNumber = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                tint = Color(0xFF757580),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardType = KeyboardType.Number
                    )
                }

                var testStatus by remember { mutableStateOf<String?>(null) }
                var isTesting by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Spacer(modifier = Modifier.height(14.dp))

                // Test Connection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val cleanIp = ipAddress.trim()
                            val port = portNumber.trim().toIntOrNull() ?: 8080
                            if (cleanIp.isNotBlank()) {
                                isTesting = true
                                testStatus = "Testing proxy connection..."
                                scope.launch {
                                    val res = com.gaminghub.musicplayer.util.NetworkProxyManager.testProxyConnection(cleanIp, port)
                                    isTesting = false
                                    res.onSuccess { ms ->
                                        testStatus = "✓ Proxy reachable (${ms}ms)"
                                    }.onFailure { err ->
                                        testStatus = "✗ Unreachable: ${err.message?.take(30)}"
                                    }
                                }
                            }
                        },
                        enabled = !isTesting
                    ) {
                        Icon(
                            Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = MusifyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTesting) "Testing..." else "Test Connection",
                            color = MusifyGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (testStatus != null) {
                        Text(
                            text = testStatus!!,
                            color = if (testStatus!!.startsWith("✓")) MusifyGreen else Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Full-width Ok button
                Button(
                    onClick = {
                        val cleanIp = ipAddress.trim()
                        val cleanPort = portNumber.trim()
                        if (cleanIp.isNotBlank() && cleanPort.isNotBlank()) {
                            val combined = "$cleanIp:$cleanPort"
                            settingsViewModel.setProxyAddress(combined)
                            settingsViewModel.setUseProxy(true)
                            Toast.makeText(context, "Proxy active and routed: $combined", Toast.LENGTH_SHORT).show()
                        }
                        showProxySheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Ok",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ── Bottom Sheet: Language (App Text Language) ────────────────
    if (showLanguageSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val allLanguages = remember {
            listOf(
                "EN" to "English",
                "AR" to "Arabic",
                "BN" to "Bangla",
                "BE" to "Belarusian",
                "ZH" to "Chinese",
                "FR" to "French",
                "DE" to "German",
                "HI" to "Hindi",
                "ID" to "Indonesian",
                "IT" to "Italian",
                "JA" to "Japanese",
                "KO" to "Korean",
                "MR" to "Marathi",
                "PA" to "Punjabi",
                "PT" to "Portuguese",
                "RU" to "Russian",
                "ES" to "Spanish",
                "TA" to "Tamil",
                "TE" to "Telugu",
                "TR" to "Turkish",
                "UK" to "Ukrainian",
                "UR" to "Urdu",
                "VI" to "Vietnamese"
            )
        }
        var searchQuery by remember { mutableStateOf("") }
        val filteredLanguages = remember(searchQuery) {
            if (searchQuery.isBlank()) allLanguages
            else allLanguages.filter { it.second.contains(searchQuery, ignoreCase = true) || it.first.contains(searchQuery, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = sheetState,
            containerColor = DarkSheetBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4A4A52))
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Squircle Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSquircleBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MusifyGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Language",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "App Text Language",
                    color = Color(0xFF9E9EA6),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkInputBackground)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF757580),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search", color = Color(0xFF6B6B75), fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(MusifyGreen),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF9E9EA6), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Languages List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLanguages) { (code, name) ->
                        val isSelected = appLanguage.equals(name, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF0F3520) else Color.Transparent)
                                .clickable {
                                    settingsViewModel.setAppLanguage(name)
                                    Toast.makeText(context, "Language set to $name", Toast.LENGTH_SHORT).show()
                                    showLanguageSheet = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF202026)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = code,
                                    color = if (isSelected) MusifyGreen else Color(0xFFB0B0BA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = name,
                                color = if (isSelected) MusifyGreen else Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MusifyGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Bottom Sheet: Include/Exclude Folders ─────────────────────
    if (showFoldersSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedTab by remember { mutableIntStateOf(0) } // 0 = Excluded, 1 = Included
        var showAddFolderDialog by remember { mutableStateOf(false) }
        var manualFolderPath by remember { mutableStateOf("") }

        val folderPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                val path = uri.path ?: uri.toString()
                if (selectedTab == 0) {
                    settingsViewModel.setExcludedFolders(excludedFolders + path)
                } else {
                    settingsViewModel.setIncludedFolders(includedFolders + path)
                }
                Toast.makeText(context, "Folder added", Toast.LENGTH_SHORT).show()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showFoldersSheet = false },
            sheetState = sheetState,
            containerColor = DarkSheetBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4A4A52))
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Squircle Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSquircleBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MusifyGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Include/Exclude Folders",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Tabs: Excluded vs Included
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1B1B22))
                        .padding(3.dp)
                ) {
                    // Excluded Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 0) MusifyGreen else Color.Transparent)
                            .clickable { selectedTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Excluded",
                            color = if (selectedTab == 0) Color.White else Color(0xFF9E9EA6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Included Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 1) MusifyGreen else Color.Transparent)
                            .clickable { selectedTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Included",
                            color = if (selectedTab == 1) Color.White else Color(0xFF9E9EA6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (selectedTab == 0) "Songs from selected folders will not be shown" else "Only songs from selected folders will be shown",
                    color = Color(0xFF9E9EA6),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Add New Button Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAddFolderDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSquircleBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add New",
                            tint = MusifyGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Add New",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Folders List
                val currentFolders = if (selectedTab == 0) excludedFolders.toList() else includedFolders.toList()
                val defaultExcluded = listOf(
                    "/Ringtones/",
                    "/Notifications/",
                    "/WhatsApp/Media/WhatsApp Audio/",
                    "/Recordings/"
                )
                val displayList = if (selectedTab == 0 && currentFolders.isEmpty()) defaultExcluded else currentFolders

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayList) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkInputBackground)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MusifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = folder,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentFolders.contains(folder)) {
                                IconButton(
                                    onClick = {
                                        if (selectedTab == 0) {
                                            settingsViewModel.setExcludedFolders(excludedFolders - folder)
                                        } else {
                                            settingsViewModel.setIncludedFolders(includedFolders - folder)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Folder Dialog (Directory picker or manual text)
        if (showAddFolderDialog) {
            AlertDialog(
                onDismissRequest = { showAddFolderDialog = false },
                containerColor = DarkSheetBackground,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = if (selectedTab == 0) "Exclude Folder" else "Include Folder",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter folder name or keyword (e.g. WhatsApp, Podcasts, Ringtones):",
                            color = Color(0xFF9E9EA6),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = manualFolderPath,
                            onValueChange = { manualFolderPath = it },
                            placeholder = { Text("e.g. Call_Records", color = Color(0xFF666670)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkInputBackground,
                                unfocusedContainerColor = DarkInputBackground,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MusifyGreen,
                                focusedIndicatorColor = MusifyGreen,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                showAddFolderDialog = false
                                folderPickerLauncher.launch(null)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MusifyGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Storage")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clean = manualFolderPath.trim()
                            if (clean.isNotBlank()) {
                                if (selectedTab == 0) {
                                    settingsViewModel.setExcludedFolders(excludedFolders + clean)
                                } else {
                                    settingsViewModel.setIncludedFolders(includedFolders + clean)
                                }
                                manualFolderPath = ""
                                Toast.makeText(context, "Folder added", Toast.LENGTH_SHORT).show()
                            }
                            showAddFolderDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MusifyGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFolderDialog = false }) {
                        Text("Cancel", color = Color(0xFF9E9EA6))
                    }
                }
            )
        }
    }

    // ── Bottom Sheet: Min Audio Length ───────────────────────────
    if (showMinLengthSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val options = listOf(10, 20, 30, 60, 120)

        ModalBottomSheet(
            onDismissRequest = { showMinLengthSheet = false },
            sheetState = sheetState,
            containerColor = DarkSheetBackground,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4A4A52))
                )
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSquircleBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MusifyGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Min Audio Length",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Ignore short recordings, ringtones and sound clips",
                    color = Color(0xFF9E9EA6),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { sec ->
                    val isSelected = minAudioLength == sec
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF0F3520) else Color.Transparent)
                            .clickable {
                                settingsViewModel.setMinAudioLengthSeconds(sec)
                                Toast.makeText(context, "Min length set to $sec seconds", Toast.LENGTH_SHORT).show()
                                showMinLengthSheet = false
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                settingsViewModel.setMinAudioLengthSeconds(sec)
                                showMinLengthSheet = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MusifyGreen,
                                unselectedColor = Color(0xFF6B6B75)
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "$sec seconds",
                            color = if (isSelected) MusifyGreen else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MusifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomDarkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: @Composable () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkInputBackground)
            .border(1.dp, DarkInputBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon()
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(MusifyGreen),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
