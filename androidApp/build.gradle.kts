import org.gradle.kotlin.dsl.java
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.stability.analyzer)
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        optIn.addAll(listOf(
            "kotlin.ExperimentalStdlibApi"
        ))
        freeCompilerArgs.addAll(listOf(
            "-Xexplicit-context-arguments",
            "-Xcollection-literals",
            "-Xintrinsic-const-evaluation",
            "-Xallow-returns-result-of",
            "-Xklib-ir-inliner=full"
        ))
    }
}
dependencies {
    implementation(project(":shared"))
    val bom = project.dependencies.platform(libs.compose.bom)

    implementation(bom)
    implementation(libs.bundles.android.compose)
    debugImplementation(libs.android.compose.ui.tooling.preview)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.foundation)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.annotations)

    debugImplementation(libs.compose.uiTooling)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlin.test)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}
val releaseStorePath = localProps.getProperty("RELEASE_STORE_FILE")
val releaseStorePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")

android {
    namespace = libs.versions.app.id.get()
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = libs.versions.app.id.get()
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        version = libs.versions.app.version.get()
        versionCode = libs.versions.app.versioncode.get().toInt()
        versionName = version.toString()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            if (!releaseStorePath.isNullOrEmpty() && file(releaseStorePath).exists()) {
                storeFile = file(releaseStorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".pd"
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
                )
            )
            signingConfig = if (!releaseStorePath.isNullOrEmpty() && file(releaseStorePath).exists() && !releaseKeyAlias.isNullOrEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

koinCompiler {
    userLogs = true
}
