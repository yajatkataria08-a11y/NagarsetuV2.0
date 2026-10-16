plugins {
    id("nagarsetu.android.library.compose")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.roadwatch" }

dependencies {
    implementation(project(":backend:components:road-watch"))
    implementation(project(":frontend:common-ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.base)
    implementation(libs.bundles.compose.ui)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
