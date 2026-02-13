// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    // KAPT is inherent to the Kotlin plugin, so we don't need a global declaration here,
    // but we must apply it in the app module.
}

buildscript {
    dependencies {
        // Strict classpath forcing to prevent version conflicts
        classpath(libs.hilt.android.gradle.plugin)
    }
}