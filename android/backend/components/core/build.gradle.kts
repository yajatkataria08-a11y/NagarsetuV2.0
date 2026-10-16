plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.backend.core" }

dependencies {
    implementation(project(":frontend:common-ui"))
    implementation(libs.bundles.base)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.socket.io.client)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
