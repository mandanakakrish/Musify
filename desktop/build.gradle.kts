plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Networking & Extractor
    implementation(libs.newpipe.extractor)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // Local Database
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // Audio Playback Engine (JavaFX Media for Windows)
    implementation("org.openjfx:javafx-base:21.0.2:win")
    implementation("org.openjfx:javafx-graphics:21.0.2:win")
    implementation("org.openjfx:javafx-controls:21.0.2:win")
    implementation("org.openjfx:javafx-media:21.0.2:win")
}

compose.desktop {
    application {
        mainClass = "com.gaminghub.musify.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Musify"
            packageVersion = "1.7.5"
            description = "Musify Music Player for Windows"
            copyright = "© 2026 Musify Team"
            vendor = "GamingHub"
        }
    }
}
