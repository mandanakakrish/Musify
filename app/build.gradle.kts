@file:Suppress("DEPRECATION")

import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

configurations.all {
    resolutionStrategy {
        force("com.google.protobuf:protobuf-javalite:3.25.5")
    }
}

android {
    namespace = "com.gaminghub.musicplayer"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.gaminghub.musify"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.7.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.newpipe.extractor)
    implementation(libs.coil.compose)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Media3 (ExoPlayer) for Music Playback
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    // ViewModel and Coroutines
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.kotlinx.coroutines.play.services)

    // Palette, Auth, Ads, Analytics
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.palette)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.ads)
}

tasks.register("exportSha1") {
    description = "SHA1"
    doLast {
        val userHome = System.getProperty("user.home")
        val keystore = File(userHome, ".android/debug.keystore")
        val outputFile = file("sha1_output.txt")
        if (keystore.exists()) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("keytool", "-list", "-v", "-keystore", keystore.absolutePath, "-alias", "androiddebugkey", "-storepass", "android", "-keypass", "android"))
                val output = process.inputStream.bufferedReader().readText()
                outputFile.writeText(output)
            } catch (e: Exception) {
                outputFile.writeText("Error executing keytool: ${e.message}")
            }
        } else {
            outputFile.writeText("Keystore not found at ${keystore.absolutePath}")
        }
    }
}
