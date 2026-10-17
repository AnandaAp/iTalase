import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.crashlytics)
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
}

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
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
}

koinCompiler {
    userLogs = true
}