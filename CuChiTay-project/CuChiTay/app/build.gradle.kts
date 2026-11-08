plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cuchitay.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cuchitay.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // vivo X200 Pro dùng chip 64-bit -> chỉ đóng gói arm64 cho nhẹ
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")

    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")

    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}
