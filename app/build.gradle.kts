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
        versionCode = 6
        versionName = "1.0.6"
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
