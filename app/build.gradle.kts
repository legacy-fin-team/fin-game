plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Whether this build is a demo one, i.e. whether the game shows the tools a demo is given instead
// of the wait the player is given — the time button, for one (see `DemoMode`).
//
// The debug build is a demo build and the release one is not, which is what a demo is usually built
// from; `-Pfingame.demoMode=true` (or `false`) overrides that for either of them, so a demo can be
// handed over as a release build as well without touching this file.
val demoModeOverride: Boolean? = providers.gradleProperty("fingame.demoMode")
    .orNull
    ?.toBooleanStrictOrNull()

android {
    namespace = "com.legacy.fingame"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.legacy.fingame"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEMO_MODE", (demoModeOverride ?: true).toString())
        }
        release {
            buildConfigField("boolean", "DEMO_MODE", (demoModeOverride ?: false).toString())
            optimization {
                enable = false
            }
        }
        create("releaseDebuggable") {
            initWith(getByName("release"))

            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")

            buildConfigField("boolean", "DEMO_MODE", (demoModeOverride ?: false).toString())
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}