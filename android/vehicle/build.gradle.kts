plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.a3ro.betterola.vehicle"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.grpc.okhttp)
    api(libs.grpc.stub)
    testImplementation(libs.junit)
}
