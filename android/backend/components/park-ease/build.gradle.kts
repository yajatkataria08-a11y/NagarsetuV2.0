plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.backend.parkease" }

dependencies {
    implementation(project(":backend:components:core"))
    implementation(project(":frontend:common-ui"))
    implementation(libs.bundles.base)
    implementation(libs.bundles.retrofit)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    implementation(libs.supabase.postgrest)
    ksp(libs.hilt.compiler)
}
