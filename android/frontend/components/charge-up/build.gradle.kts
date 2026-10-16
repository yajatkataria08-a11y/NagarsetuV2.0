plugins {
    id("nagarsetu.android.library.compose")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.chargeup" }

dependencies {
    implementation(project(":backend:components:charge-up"))
    implementation(project(":frontend:common-ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.base)
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.accompanist.permissions)
    ksp(libs.hilt.compiler)
}
