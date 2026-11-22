import java.util.Base64
import java.util.zip.GZIPInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cuchitay.app"
    compileSdk = 34

    defaultConfig {
        // Separate package so 3.0 can be installed alongside the current 2.1 app.
        applicationId = "com.cuchitay.app.v3"
        minSdk = 28
        targetSdk = 34
        versionCode = 30
        versionName = "3.0"
        ndk { abiFilters.add("arm64-v8a") }
    }

    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.webkit:webkit:1.11.0")

    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}

// Keep the UI compact in Git while automatically expanding it before Android asset packaging.
val inflateWebUi by tasks.registering {
    val compressedUi = layout.projectDirectory.file("src/main/assets/www/index.html.gz.b64")
    val expandedUi = layout.projectDirectory.file("src/main/assets/www/index.html")
    inputs.file(compressedUi)
    outputs.file(expandedUi)
    doLast {
        val payload = Base64.getDecoder().decode(compressedUi.asFile.readText().trim())
        GZIPInputStream(payload.inputStream()).use { input ->
            expandedUi.asFile.apply { parentFile.mkdirs() }.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
tasks.named("preBuild").configure { dependsOn(inflateWebUi) }
