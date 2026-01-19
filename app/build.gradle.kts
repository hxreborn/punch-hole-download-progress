import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

val gitCommitCount: Int by lazy {
    ByteArrayOutputStream()
        .also { stdout ->
            exec {
                commandLine("git", "rev-list", "--count", "HEAD")
                standardOutput = stdout
            }
        }.toString()
        .trim()
        .toIntOrNull() ?: 0
}

val gitDescribe: String by lazy {
    ByteArrayOutputStream()
        .also { stdout ->
            exec {
                commandLine("git", "describe", "--tags", "--always")
                standardOutput = stdout
            }
        }.toString()
        .trim()
        .removePrefix("v")
}

val versionMajor: Int by lazy {
    gitDescribe.removePrefix("v").substringBefore(".").toIntOrNull() ?: 0
}

android {
    namespace = "eu.hxreborn.phdp"
    compileSdk = 36

    defaultConfig {
        applicationId = namespace
        minSdk = 31
        targetSdk = 36
        versionCode = project.findProperty("version.code")?.toString()?.toInt()
            ?: (versionMajor * 10000 + gitCommitCount)
        versionName = project.findProperty("version.name")?.toString()
            ?: gitDescribe
    }

    androidResources {
        localeFilters += "en"
    }

    signingConfigs {
        create("release") {
            fun secret(name: String): String? =
                providers
                    .gradleProperty(name)
                    .orElse(providers.environmentVariable(name))
                    .orNull

            val storeFilePath = secret("RELEASE_STORE_FILE")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
                storeType = secret("RELEASE_STORE_TYPE") ?: "PKCS12"
            } else {
                logger.warn("RELEASE_STORE_FILE not found. Release signing is disabled.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                signingConfigs
                    .getByName("release")
                    .takeIf { it.storeFile != null }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            pickFirsts += "META-INF/xposed/*"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable.addAll(listOf("PrivateApi", "DiscouragedPrivateApi"))
        ignoreTestSources = true
    }
}

android.applicationVariants.all {
    outputs.forEach { output ->
        if (output is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
            output.outputFileName = "phdp-v$versionName-$name.apk"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    version.set("1.8.0")
    android.set(true)
    ignoreFailures.set(false)
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.libsu.core)
    implementation(libs.lifecycle.runtime)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.compose.preference)
}

tasks.named("preBuild").configure {
    dependsOn("ktlintCheck")
}
