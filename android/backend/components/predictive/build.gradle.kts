// backend/components/predictive/build.gradle.kts
plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.backend.predictive" }

dependencies {
    implementation(project(":frontend:common-ui"))
    implementation(project(":backend:components:core"))
    implementation(libs.bundles.base)
    implementation(libs.bundles.retrofit)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
