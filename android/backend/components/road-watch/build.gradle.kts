// backend/components/road-watch/build.gradle.kts
plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nagarsetu.backend.roadwatch"
}

dependencies {
    // Core data helpers (AssetDataRepository, CivicDataHub, MapMarker)
    implementation(project(":frontend:common-ui"))

    // Base: Kotlin coroutines + lifecycle
    implementation(libs.bundles.base)

    // JSON
    implementation(libs.gson)

    // TFLite for pothole detection model
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
