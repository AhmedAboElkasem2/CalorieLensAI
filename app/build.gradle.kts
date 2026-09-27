plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.calorielens.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.calorielens.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0-free"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Bundled on-device image-labeling model: no API key, no per-use fees.
    implementation("com.google.mlkit:image-labeling:17.0.9")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
