import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.stability.analyzer)
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    id("kotlin-parcelize")
}



kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // CocoaPods for iOS dependencies (SQLCipher)
    cocoapods {
        summary = "Shared framework for iTalase"
        homepage = "https://github.com/anlian/iTalase"
        version = "1.1.2"
        ios.deploymentTarget = "16.0"
        pod("SQLCipher", "~> 4.6.0")
    }
    
    android {
        namespace = libs.versions.app.shared.get()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        version = libs.versions.app.version.get()
    
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.compose.foundation)
            val bom = project.dependencies.platform(libs.compose.bom)

            implementation(bom)
            implementation(libs.bundles.android.compose)
            api(libs.bundles.ktor.shared.android)
            api(libs.bundles.android.essentials)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.animation.core)
            implementation(libs.compose.animation.graphic)
            implementation(libs.compose.ui)
            // BackHandler untuk konfirmasi keluar di semua platform (androidMain
            // hanya punya activity-compose).
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.compose.navigation.multiplatform)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            api(libs.napier)
            api(libs.bundles.ktor.core)
            api(libs.kotlinx.collections.immutable)
        }
        commonMain.configure {
            this@configure.kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    compilerOptions {
        optIn.addAll(listOf(
            "kotlin.RequiresOptIn",
            "kotlin.ExperimentalStdlibApi",
            "kotlin.ExperimentalUnsignedTypes"
        ))
        freeCompilerArgs.addAll(listOf(
            "-Xexpect-actual-classes"
        ))
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    androidRuntimeClasspath(libs.koin.android)
    androidRuntimeClasspath(libs.koin.androidx.compose)

    val bom = project.dependencies.platform(libs.compose.bom)

    androidRuntimeClasspath(bom)
    androidRuntimeClasspath(libs.bundles.android.compose)

    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

koinCompiler {
    userLogs = true
    skipDefaultValues = true
    compileSafety = true
}

