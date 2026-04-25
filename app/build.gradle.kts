plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.aboutlibraries)
}

val gitCommitCount: Provider<String> =
    providers
        .exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText
        .map { it.trim() }

val gitDescribe: Provider<String> =
    providers
        .exec {
            commandLine("git", "describe", "--tags", "--always")
        }.standardOutput.asText
        .map { it.trim().removePrefix("v") }

val versionMajor: Provider<Int> =
    gitDescribe.map { it.substringBefore(".").toIntOrNull() ?: 0 }

android {
    namespace = "eu.hxreborn.phdp"
    compileSdk = 36

    defaultConfig {
        applicationId = namespace
        minSdk = 28
        targetSdk = 36
        versionCode = project.findProperty("version.code")?.toString()?.toInt()
            ?: (versionMajor.get() * 10000 + gitCommitCount.get().toInt())
        versionName = project.findProperty("version.name")?.toString()
            ?: gitDescribe.get()
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
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
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
            merges += "META-INF/xposed/*"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable.addAll(listOf("PrivateApi", "DiscouragedPrivateApi"))
        ignoreTestSources = true
    }
}

base.archivesName = "phdp-v${
    project.findProperty("version.name")?.toString() ?: gitDescribe.get()
}"

kotlin {
    jvmToolchain(21)
}

val ktlintClasspath by configurations.creating

dependencies {
    ktlintClasspath("com.pinterest.ktlint:ktlint-cli:1.8.0")
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.libsu.core)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.serialization.json)
    implementation(libs.compose.preference)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose)
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin code style"
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = ktlintClasspath
    args("src/**/*.kt")
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Format Kotlin code style"
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = ktlintClasspath
    args("-F", "src/**/*.kt")
}

val copyAboutLibraries by tasks.registering(Copy::class) {
    dependsOn("exportLibraryDefinitions")
    from("build/generated/aboutLibraries/aboutlibraries.json")
    into("build/generated/aboutLibrariesRes/raw")
}

android.sourceSets["main"]
    .res.directories
    .add("build/generated/aboutLibrariesRes")

tasks.named("preBuild").configure {
    dependsOn("ktlintCheck", copyAboutLibraries)
}
