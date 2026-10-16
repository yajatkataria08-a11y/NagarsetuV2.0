// backend/components/dashboard/build.gradle.kts
plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.backend.dashboard" }

dependencies {
    implementation(project(":frontend:common-ui"))
    implementation(project(":backend:components:core"))
    implementation(project(":backend:components:park-ease"))
    implementation(project(":backend:components:charge-up"))
    implementation(libs.bundles.base)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(project(":backend:components:firebase"))   // HybridAlertBridge
    ksp(libs.hilt.compiler)
}
