plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.rishabh.gymbros"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rishabh.gymbros"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "Ignite"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Spotify Auth redirect
        // Must match: https://com.rishabh.gymbros/callback
        manifestPlaceholders["redirectSchemeName"] = "https"
        manifestPlaceholders["redirectHostName"] = "com.rishabh.gymbros"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Spotify App Remote
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))

    // Spotify Authentication
    implementation("com.spotify.android:auth:2.1.0")

    // Required by Spotify App Remote
    implementation("com.google.code.gson:gson:2.13.2")

    // Encrypted Google Drive appDataFolder backup + Google authorization
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}