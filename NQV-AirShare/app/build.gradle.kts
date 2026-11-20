plugins {
    id("com.android.application")
}

android {
    namespace = "com.nongquangvinh.airshare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nongquangvinh.airshare"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.google.zxing:core:3.5.4")
}
