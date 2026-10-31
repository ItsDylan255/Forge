plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.fitnesstracker.watch"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Gleiche App-Id wie auf dem Handy, sonst finden sich die beiden Apps über die Data Layer nicht
        applicationId = "com.example.fitnesstracker"
        minSdk = 30
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.1"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)

    // Verbindung zum Handy
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // Laufendes Training auf dem Zifferblatt, Kachel und Komplikation
    implementation(libs.androidx.wear.ongoing)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.wear.complications.data.source)
    implementation(libs.androidx.concurrent.futures)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
