// backend/components/raksha/build.gradle.kts
plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.backend.raksha" }

dependencies {
    implementation(project(":frontend:common-ui"))
    implementation(project(":backend:components:core"))
    implementation(project(":backend:components:emergency-ai"))
    implementation(libs.play.services.wearable)
    implementation(libs.play.services.location)
    implementation(libs.bundles.base)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    implementation(project(":backend:components:firebase"))   // GpsTrackingRepository for LiveTrack
    ksp(libs.hilt.compiler)
}
