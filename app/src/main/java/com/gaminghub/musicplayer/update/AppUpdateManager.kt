package com.gaminghub.musicplayer.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import com.gaminghub.musicplayer.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

    private var lastCheckedEpochMs: Long = 0L
    private const val MIN_CHECK_INTERVAL_MS = 60_000L // 1 minute throttle unless force = true

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
     * Testing utility: simulates a strict update prompt on the device so the user/developer
     * can verify the UI and strict behavior immediately.
     */
    fun triggerTestPrompt(context: Context) {
        val (curVersion, curCode) = getCurrentVersion(context)
        _updateInfo.value = AppUpdateInfo(
            isUpdateAvailable = true,
            isForceUpdate = true,
            currentVersion = curVersion,
            latestVersion = "v9.9.9",
            currentVersionCode = curCode,
            latestVersionCode = 999L,
            releaseTitle = "Strict Update Required (Test Mode)",
            changelog = "• Fixed audio cracking & improved sound quality\n• Fixed Up Next queue recommendation\n• Real-time settings verification\n• Strict mandatory GitHub update prompt active",
            downloadUrl = "https://github.com/$releaseRepoOwner/$releaseRepoName/releases/latest",
            source = UpdateSource.TEST_MODE
        )
    }

    /**
     * Clears test update prompt state.
     */
    fun clearUpdateInfo() {
        _updateInfo.value = null
    }
}
