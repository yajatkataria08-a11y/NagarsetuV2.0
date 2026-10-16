buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.hilt.android.gradle.plugin)
        // Firebase — must come after AGP so google-services.json is processed
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
    }
}

plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("com.squareup:javapoet:1.13.0")
        }
    }
}
