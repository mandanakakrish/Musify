# Musify Project Rules & Guidelines

## Repository Separation Guidelines (CRITICAL)
- **Code Repository**: `https://github.com/mandanakakrish/Music-Player`
  - Used **strictly** for project source code, Git commits, branches, and code management.
  - Never publish APK release assets directly here.
- **APK Release Repository**: `https://github.com/mandanakakrish/Musify`
  - Used **strictly** for publishing app releases, release notes/changelogs, and hosting APK assets (`Musify.apk`, `Musify_vX.X.X.apk`).
  - GitHub update checks in `AppUpdateManager.kt` query this repository (`mandanakakrish/Musify`) for latest releases and APK downloads.

## Contact Us & Feedback Link
- **Contact Us / Feedback URL**: `https://github.com/mandanakakrish/Musify/discussions/3`
  - Used for any "Contact Us", user feedback, bug reports, and community support links in the app (e.g. `AboutSettingsScreen.kt`) and documentation.
  - Kept saved for upcoming releases.

## Release Process
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` and `version.json`.
2. Commit and push changes to `Music-Player` (`git push origin master`).
3. Compile release APK: `.\gradlew.bat assembleRelease`.
4. Copy `app/build/outputs/apk/release/app-release.apk` to `Musify.apk` and `Musify_v<version>.apk`.
5. Create GitHub Release on `mandanakakrish/Musify` with the version tag (e.g. `v1.7.3`) and upload `Musify.apk` & `Musify_v<version>.apk`.
