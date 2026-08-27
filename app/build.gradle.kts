plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.fperuzzo72.usintlime"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.fperuzzo72.usintlime"
        // Android 11. Covers both target devices (Boox Mini C on 11, Bigme
        // HiBreak Pro on 14). Nothing here needs a newer API; this is a chosen
        // baseline, not a technical floor.
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    testImplementation(libs.junit)
}
