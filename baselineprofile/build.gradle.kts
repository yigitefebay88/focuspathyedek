plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.focuspath.baselineprofile"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
}

baselineProfile {
    useConnectedDevices = true
}