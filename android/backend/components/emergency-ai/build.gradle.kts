// backend/components/emergency-ai/build.gradle.kts
plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.backend.emergencyai" }

dependencies {
    implementation(project(":backend:components:core"))
    implementation(project(":frontend:common-ui"))
    implementation(libs.bundles.base)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.auth)
    ksp(libs.hilt.compiler)
}
