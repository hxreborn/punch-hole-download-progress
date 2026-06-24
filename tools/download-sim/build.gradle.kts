plugins {
    alias(libs.plugins.agp.app)
}

android {
    namespace = "eu.hxreborn.downloadsim"
    compileSdk = 37

    defaultConfig {
        applicationId = "eu.hxreborn.downloadsim"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        disable += setOf("UnusedAttribute", "OldTargetApi")
    }
}
