import java.util.Base64
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cuchitay.app"
    compileSdk = 34

    defaultConfig {
        // Separate package so 3.0 can be installed alongside the current 2.1 app.
        applicationId = "com.cuchitay.app.v32"
        minSdk = 28
        targetSdk = 34
        versionCode = 34
        versionName = "3.4"
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
    val bundleParts = fileTree("webui-source") { include(
        "part-00.b64", "part-01.b64", "part-02.b64", "part-03.b64",
        "part-04.b64", "part-05.b64", "part-06.b64", "part-07.b64"
    ) }
    val webRoot = layout.projectDirectory.dir("src/main/assets/www")
    inputs.files(bundleParts)
    outputs.dir(webRoot)
    doLast {
        val orderedParts = bundleParts.files.sortedBy { it.name }
        check(orderedParts.size == 8) { "Expected eight parts for the Web UI bundle" }
        val bundleText = orderedParts.joinToString("") { it.readText().trim() }
        val zipBytes = Base64.getDecoder().decode(bundleText)
        val root = webRoot.asFile.canonicalFile
        root.mkdirs()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val target = File(root, entry.name).canonicalFile
                require(target.toPath().startsWith(root.toPath())) {
                    "Unsafe Web UI archive entry: ${entry.name}"
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        check(File(root, "index.html").isFile &&
            File(root, "app.js").isFile &&
            File(root, "styles.css").isFile) {
            "Web UI bundle is incomplete"
        }
    }
}
tasks.named("preBuild").configure { dependsOn(inflateWebUi) }
