import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.focuspath.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.focuspath.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 113
        versionName = "1.2.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiKey = localProperties.getProperty("GEMINI_API_KEY", "")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    baselineProfile(project(":baselineprofile"))
    implementation(platform("androidx.compose:compose-bom:2025.04.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.androidx.core.splashscreen)
    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
    // Network & Database
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    val room_version = "2.7.0-alpha11"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Bazen gerekir
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Hilt Dependency Injection
    val hilt_version = "2.57.1"
    implementation("com.google.dagger:hilt-android:$hilt_version")
    ksp("com.google.dagger:hilt-compiler:$hilt_version")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Gemini AI Client
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Google Sign-In with Credential Manager
    implementation("androidx.credentials:credentials:1.5.0-rc01")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0-rc01")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Google Play Billing (Real Money Purchases)
    implementation("com.android.billingclient:billing:8.0.0")
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Lottie for Animations
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Glance for App Widgets
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // In-App Updates
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")
}
