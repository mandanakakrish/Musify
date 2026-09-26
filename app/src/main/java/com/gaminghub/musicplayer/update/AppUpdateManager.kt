package com.gaminghub.musicplayer.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.gaminghub.musicplayer.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"

    // Primary Release Repository: https://github.com/mandanakakrish/Musify
    var releaseRepoOwner: String = "mandanakakrish"
    var releaseRepoName: String = "Musify"

    // Code Repository: https://github.com/mandanakakrish/Music-Player
    var codeRepoOwner: String = "mandanakakrish"
    var codeRepoName: String = "Music-Player"

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _isOfflineModeActive = MutableStateFlow(false)
    val isOfflineModeActive: StateFlow<Boolean> = _isOfflineModeActive.asStateFlow()

    private val _pendingOfflineNavigationRoute = MutableStateFlow<String?>(null)
    val pendingOfflineNavigationRoute: StateFlow<String?> = _pendingOfflineNavigationRoute.asStateFlow()

    private var downloadJob: Job? = null

    private var lastCheckedEpochMs: Long = 0L
    private const val MIN_CHECK_INTERVAL_MS = 60_000L // 1 minute throttle unless force = true

    fun enterOfflineModeAndNavigate(route: String) {
        _isOfflineModeActive.value = true
        _pendingOfflineNavigationRoute.value = route
    }

    fun clearPendingOfflineNavigation() {
        _pendingOfflineNavigationRoute.value = null
    }

    fun exitOfflineMode() {
        _isOfflineModeActive.value = false
    }

    /**
     * Retrieves current installed app's versionName and versionCode.
     */
    fun getCurrentVersion(context: Context): Pair<String, Long> {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pInfo.versionName ?: BuildConfig.VERSION_NAME
            val vCode = PackageInfoCompat.getLongVersionCode(pInfo)
            Pair(vName, vCode)
        } catch (_: Exception) {
            Pair(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())
        }
    }

    /**
     * Checks the GitHub repository for updates asynchronously.
     * Looks at both GitHub Releases API and raw version.json file.
     */
    fun checkForUpdates(
        context: Context,
        force: Boolean = false,
        onComplete: ((AppUpdateInfo?) -> Unit)? = null
    ) {
        val now = System.currentTimeMillis()
        val isOnline = com.gaminghub.musicplayer.util.NetworkMonitor.getInstance(context).isOnline.value
        if (!isOnline) {
            Log.d(TAG, "Device is offline. Skipping update check to allow offline music.")
            onComplete?.invoke(null)
            return
        }

        if (!force && (now - lastCheckedEpochMs) < MIN_CHECK_INTERVAL_MS && _updateInfo.value != null) {
            onComplete?.invoke(_updateInfo.value)
            return
        }

        if (_isChecking.value) return
        _isChecking.value = true

        val (curVersion, curCode) = getCurrentVersion(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Read any custom repo overrides from SharedPreferences if configured
                val prefs = context.getSharedPreferences("Musify_settings", Context.MODE_PRIVATE)
                val primaryOwner = prefs.getString("github_release_owner", releaseRepoOwner)?.trim()?.ifBlank { releaseRepoOwner } ?: releaseRepoOwner
                val primaryRepo = prefs.getString("github_release_name", releaseRepoName)?.trim()?.ifBlank { releaseRepoName } ?: releaseRepoName

                Log.d(TAG, "Checking release updates on GitHub repo: $primaryOwner/$primaryRepo (Current: $curVersion, code: $curCode)")

                // 1. Try checking raw version.json on release repo first
                var detectedUpdate = checkVersionJson(primaryOwner, primaryRepo, curVersion, curCode)

                // 2. If not found, check GitHub Releases API on release repo
                if (detectedUpdate == null) {
                    detectedUpdate = checkGitHubReleases(primaryOwner, primaryRepo, curVersion, curCode)
                }

                // 3. Fallback: check code repo (Music-Player) if release repo had no release yet
                if (detectedUpdate == null && (primaryOwner != codeRepoOwner || primaryRepo != codeRepoName)) {
                    detectedUpdate = checkVersionJson(codeRepoOwner, codeRepoName, curVersion, curCode)
                    if (detectedUpdate == null) {
                        detectedUpdate = checkGitHubReleases(codeRepoOwner, codeRepoName, curVersion, curCode)
                    }
                }

                lastCheckedEpochMs = System.currentTimeMillis()

                withContext(Dispatchers.Main) {
                    _isChecking.value = false
                    _updateInfo.value = detectedUpdate
                    onComplete?.invoke(detectedUpdate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isChecking.value = false
                    onComplete?.invoke(null)
                }
            }
        }
    }

    /**
     * Checks raw version.json hosted on GitHub repository.
     */
    private fun checkVersionJson(
        owner: String,
        repo: String,
        currentVersion: String,
        currentCode: Long
    ): AppUpdateInfo? {
        val branches = listOf("main", "master")
        for (branch in branches) {
            try {
                val urlString = "https://raw.githubusercontent.com/$owner/$repo/$branch/version.json"
                val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "MusifyApp-UpdateChecker")
                }

                if (connection.responseCode == 200) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)

                    val remoteVersionName = json.optString("versionName", "").trim()
                    val remoteVersionCode = json.optLong("versionCode", currentCode)
                    val minVersionCode = json.optLong("minVersionCode", 0L)
                    val forceUpdate = json.optBoolean("forceUpdate", true)
                    val title = json.optString("title", "Important Update Required")
                    val changelog = json.optString("changelog", "Bug fixes and performance improvements.")
                    val downloadUrl = json.optString("downloadUrl", "https://github.com/$owner/$repo/releases/latest")

                    val isNewer = (remoteVersionCode > currentCode) ||
                            (remoteVersionName.isNotBlank() && isNewerVersion(currentVersion, remoteVersionName))
                    val isMandatory = forceUpdate || (minVersionCode > currentCode)

                    if (isNewer) {
                        return AppUpdateInfo(
                            isUpdateAvailable = true,
                            isForceUpdate = isMandatory,
                            currentVersion = currentVersion,
                            latestVersion = if (remoteVersionName.isNotBlank()) remoteVersionName else "v$remoteVersionCode",
                            currentVersionCode = currentCode,
                            latestVersionCode = remoteVersionCode,
                            releaseTitle = title,
                            changelog = changelog,
                            downloadUrl = downloadUrl,
                            source = UpdateSource.GITHUB_VERSION_JSON
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "version.json check on branch $branch failed: ${e.message}")
            }
        }
        return null
    }

    /**
     * Checks the GitHub Releases API for the latest published release.
     */
    private fun checkGitHubReleases(
        owner: String,
        repo: String,
        currentVersion: String,
        currentCode: Long
    ): AppUpdateInfo? {
        try {
            val urlString = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "MusifyApp-UpdateChecker")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val tagName = json.optString("tag_name", "").trim()
                val releaseName = json.optString("name", "New Version Available").trim()
                val releaseBody = json.optString("body", "").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo/releases/latest")
                val publishedAt = json.optString("published_at", "")

                // Find direct APK download url in assets if available
                var apkDownloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                if (tagName.isNotBlank() && isNewerVersion(currentVersion, tagName)) {
                    val formattedTag = if (tagName.startsWith("v", ignoreCase = true)) tagName else "v$tagName"
                    return AppUpdateInfo(
                        isUpdateAvailable = true,
                        isForceUpdate = true, // Strictly prompt update as requested
                        currentVersion = currentVersion,
                        latestVersion = formattedTag,
                        currentVersionCode = currentCode,
                        latestVersionCode = currentCode + 1,
                        releaseTitle = if (releaseName.isNotBlank()) releaseName else "Update Required: $formattedTag",
                        changelog = if (releaseBody.isNotBlank()) releaseBody else "A newer version of Musify is required to continue.",
                        downloadUrl = apkDownloadUrl,
                        publishedAt = publishedAt,
                        source = UpdateSource.GITHUB_RELEASE
                    )
                }
            } else {
                Log.d(TAG, "GitHub Releases API returned HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Releases check failed: ${e.message}")
        }
        return null
    }

    /**
     * Robust semantic version comparison.
     * Compares "1.7.0" > "1.6.4", "v2.0" > "1.9.9", "1.0.1" > "1.0", etc.
     */
    fun isNewerVersion(current: String, remote: String): Boolean {
        try {
            val cleanCurrent = current.trim()
                .removePrefix("v").removePrefix("V")
                .split("-")[0]
            val cleanRemote = remote.trim()
                .removePrefix("v").removePrefix("V")
                .split("-")[0]

            val curParts = cleanCurrent.split(".").mapNotNull { it.trim().toIntOrNull() }
            val remParts = cleanRemote.split(".").mapNotNull { it.trim().toIntOrNull() }

            if (curParts.isEmpty() || remParts.isEmpty()) {
                return cleanRemote != cleanCurrent && cleanRemote > cleanCurrent
            }

            val maxLen = maxOf(curParts.size, remParts.size)
            for (i in 0 until maxLen) {
                val c = curParts.getOrElse(i) { 0 }
                val r = remParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Resolves direct APK download URL from GitHub release assets if a generic URL was provided.
     */
    private fun resolveDirectApkUrl(owner: String, repo: String, fallbackUrl: String): String {
        try {
            val urlString = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "MusifyApp-UpdateChecker")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            val apkUrl = asset.optString("browser_download_url", "")
                            if (apkUrl.isNotBlank()) return apkUrl
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve direct APK URL from GitHub API: ${e.message}")
        }
        return fallbackUrl
    }

    /**
     * Starts in-app downloading of update APK with progress tracking,
     * and triggers system PackageInstaller upon completion.
     */
    fun startInAppDownloadAndInstall(context: Context, rawDownloadUrl: String) {
        if (_downloadState.value is UpdateDownloadState.Downloading) {
            Log.d(TAG, "Download already in progress")
            return
        }

        downloadJob?.cancel()
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                _downloadState.value = UpdateDownloadState.Downloading(
                    progressPercent = 0,
                    downloadedBytes = 0L,
                    totalBytes = -1L
                )

                val effectiveUrl = if (rawDownloadUrl.endsWith(".apk", ignoreCase = true)) {
                    rawDownloadUrl
                } else {
                    resolveDirectApkUrl(releaseRepoOwner, releaseRepoName, rawDownloadUrl)
                }

                Log.d(TAG, "Starting APK download from: $effectiveUrl")

                if (!effectiveUrl.endsWith(".apk", ignoreCase = true) && !effectiveUrl.contains("/download/")) {
                    // Fallback to browser if direct APK asset not found
                    withContext(Dispatchers.Main) {
                        _downloadState.value = UpdateDownloadState.Failed("Direct APK not found. Opening browser...")
                        openDownloadUrl(context, effectiveUrl)
                    }
                    return@launch
                }

                val client = OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(effectiveUrl)
                    .header("User-Agent", "MusifyApp-Updater")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("HTTP error: ${response.code} ${response.message}")
                }

                val body = response.body ?: throw IOException("Empty response body")
                val contentLength = body.contentLength()

                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updatesDir, "Musify_update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesCopied = 0L
                        var read: Int
                        var lastProgressTime = 0L

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            val now = System.currentTimeMillis()
                            if (now - lastProgressTime >= 100 || bytesCopied == contentLength) {
                                lastProgressTime = now
                                val percent = if (contentLength > 0) {
                                    ((bytesCopied * 100) / contentLength).toInt().coerceIn(0, 100)
                                } else 0
                                _downloadState.value = UpdateDownloadState.Downloading(
                                    progressPercent = percent,
                                    downloadedBytes = bytesCopied,
                                    totalBytes = contentLength
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (!apkFile.exists() || apkFile.length() == 0L) {
                    throw IOException("Downloaded file is empty or missing")
                }

                apkFile.setReadable(true, false)

                withContext(Dispatchers.Main) {
                    _downloadState.value = UpdateDownloadState.Downloaded(apkFile)
                    promptInstallApk(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "In-app download failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _downloadState.value = UpdateDownloadState.Failed(e.message ?: "Download failed")
                }
            }
        }
    }

    /**
     * Prompts the system to install the downloaded APK file.
     * Handles Unknown App Sources permission for Android 8.0+.
     */
    fun promptInstallApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                _downloadState.value = UpdateDownloadState.Failed("APK file not found. Please re-download.")
                return
            }

            // Android 8.0+ Unknown App Sources Permission Check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Log.w(TAG, "REQUEST_INSTALL_PACKAGES not granted. Prompting user to allow in Settings.")
                    _downloadState.value = UpdateDownloadState.PermissionRequired(apkFile)
                    requestInstallPermission(context)
                    return
                }
            }

            _downloadState.value = UpdateDownloadState.Installing(apkFile)

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            _downloadState.value = UpdateDownloadState.Failed("Installation failed: ${e.message}")
        }
    }

    /**
     * Opens system Settings screen for "Install unknown apps" for Musify.
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot open install permission settings: ${e.message}", e)
            }
        }
    }

    /**
     * Automatically resumes install if permission was pending and user returned after granting.
     */
    fun checkAndResumePendingInstall(context: Context) {
        val state = _downloadState.value
        if (state is UpdateDownloadState.PermissionRequired) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                Log.d(TAG, "Install permission now granted, resuming APK installation.")
                promptInstallApk(context, state.apkFile)
            }
        }
    }

    /**
     * Resets download state to Idle and cancels any ongoing download job.
     */
    fun cancelOrResetDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = UpdateDownloadState.Idle
    }

    /**
     * Opens the download URL in user's browser or download manager.
     */
    fun openDownloadUrl(context: Context, url: String) {
        try {
            val effectiveUrl = if (url.isNotBlank()) url else "https://github.com/$releaseRepoOwner/$releaseRepoName/releases/latest"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open download page: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Dismisses the update dialog ONLY if the update is not forced/strict.
     */
    fun dismissOptionalUpdate() {
        if (_updateInfo.value?.isForceUpdate == false) {
            _updateInfo.value = null
        }
    }


    /**
     * Clears test update prompt state.
     */
    fun clearUpdateInfo() {
        cancelOrResetDownload()
        _updateInfo.value = null
    }
}

