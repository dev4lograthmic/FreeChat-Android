plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.freechat.android.core.protocol"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.bsr.freechat)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
