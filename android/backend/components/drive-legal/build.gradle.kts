// backend/components/drive-legal/build.gradle.kts
// Pure JVM/Android library — no Compose dependency needed.
plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nagarsetu.backend.drivelegal"

    defaultConfig {
        // Asset folder for drive_legal_fines.json is provided by :frontend:app
    }
}

dependencies {
    // Core data helpers (AssetDataRepository, CivicDataHub)
    implementation(project(":frontend:common-ui"))

    // Base: Kotlin coroutines, lifecycle
    implementation(libs.bundles.base)

    // JSON parsing for asset data
    implementation(libs.gson)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
