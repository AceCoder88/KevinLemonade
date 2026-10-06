plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kevin.lemonade"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kevin.lemonade"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // One fixed key for every build (this machine and GitHub Actions), so a new APK installs as an update
    // over the last one. It only identifies this hobby app; the repo is private.
    signingConfigs {
        create("kevin") {
            storeFile = file("kevin.keystore")
            storePassword = "kevinlemonade"
            keyAlias = "kevin"
            keyPassword = "kevinlemonade"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("kevin")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("kevin")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
