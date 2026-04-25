import java.util.Base64
import java.io.FileOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.indolearn.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.indolearn.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "1.0.10"
    }

    buildFeatures {
        buildConfig = true
    }

    // ── Release signing ────────────────────────────────────────────────────────
    // CI sets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_PASSWORD as env vars.
    // The keystore is decoded to a temp file at build time so the raw .jks
    // file never needs to be committed to the repo.
    val keystoreB64 = System.getenv("KEYSTORE_BASE64")
    if (keystoreB64 != null) {
        val ksFile = file("${buildDir}/release.keystore")
        ksFile.parentFile.mkdirs()
        FileOutputStream(ksFile).use { it.write(Base64.getDecoder().decode(keystoreB64)) }

        signingConfigs {
            create("release") {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = "indolearn"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Use release signing if secrets are available, otherwise fall back to debug key
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null) signingConfig = releaseSigning
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.browser)   // Chrome Custom Tabs
}
