plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.a3ro.betterola"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.a3ro.betterola"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "2.0.0"

        vectorDrawables.useSupportLibrary = true

        // GitHub repo that holds the public Releases used for update checks.
        buildConfigField("String", "GIT_OWNER", "\"a3ro-dev\"")
        buildConfigField("String", "GIT_REPO", "\"ola-s1-air-mod\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt"
            )
        }
    }
}

// The dash HUD is built first and bundled into this app's assets so setup can install it offline.
val hudApk = file("build/generated/hudAssets")
val copyHud = tasks.register<Copy>("copyHudApk") {
    dependsOn(":hud:assembleDebug")
    from(project(":hud").layout.buildDirectory.file("outputs/apk/debug/hud-debug.apk"))
    rename { "betterola-hud.apk" }
    into(hudApk)
}
android.sourceSets.getByName("main").assets.srcDir(hudApk)
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach { dependsOn(copyHud) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Embedded Pure Kotlin ADB Client
    implementation(libs.dadb)

    // Root gRPC Client for Scooter Port 50005
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.stub)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
