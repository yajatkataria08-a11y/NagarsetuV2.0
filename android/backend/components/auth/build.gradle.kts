// backend/components/auth/build.gradle.kts
plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.backend.auth" }

dependencies {
    implementation(project(":frontend:common-ui"))
    implementation(project(":backend:components:firebase"))   // for SupabaseTokenUploader interface
    implementation(libs.bundles.base)
    implementation(libs.okhttp)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Supabase — needed by SupabaseTokenUploaderImpl
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
}
