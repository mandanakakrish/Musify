package com.gaminghub.musicplayer.update

import java.io.File

/**
 * Metadata representing an app update discovered from GitHub.
 */
data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val isForceUpdate: Boolean = true,
    val currentVersion: String,
    val latestVersion: String,
    val currentVersionCode: Long = 1L,
    val latestVersionCode: Long = 1L,
    val releaseTitle: String = "New Version Available",
    val changelog: String = "",
    val downloadUrl: String = "",
    val publishedAt: String = "",
    val source: UpdateSource = UpdateSource.GITHUB_RELEASE
)

enum class UpdateSource {
    GITHUB_RELEASE,
    GITHUB_VERSION_JSON,
    TEST_MODE
}

/**
 * State of in-app download and installation progress.
 */
sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()

    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateDownloadState() {
        val downloadedMb: String
            get() = "%.1f".format(downloadedBytes / (1024f * 1024f))
        val totalMb: String
            get() = if (totalBytes > 0) "%.1f MB".format(totalBytes / (1024f * 1024f)) else "Unknown"
    }

    data class Downloaded(val apkFile: File) : UpdateDownloadState()
    data class Installing(val apkFile: File) : UpdateDownloadState()
    data class PermissionRequired(val apkFile: File) : UpdateDownloadState()
    data class Failed(val error: String) : UpdateDownloadState()
}

