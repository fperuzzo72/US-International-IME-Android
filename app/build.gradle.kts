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
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isDebuggable = false

            // Left off deliberately. The app is a few hundred lines and one
            // generated table; R8 would save little, and it would do it to the
            // one component the framework instantiates by name, which is not a
            // trade worth making on a build that cannot be tested here.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // Signed with the debug key on purpose: this is sideloaded, never
            // published, and there is no store identity to protect. The cost is
            // that CI mints a fresh debug key per run, so a new APK will not
            // install over an older one; uninstall first. If in-place updates
            // ever matter, put a real keystore in repo secrets and point this
            // at it instead.
            signingConfig = signingConfigs.getByName("debug")
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
