plugins {
    id("com.android.application")
}

android {
    namespace = "id.pakkom.exambro"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.pakkom.exambro"
        minSdk = 26
        targetSdk = 36
        versionCode = 522
        versionName = "5.2.2"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
}
