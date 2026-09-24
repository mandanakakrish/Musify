package com.gaminghub.musicplayer.update

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
