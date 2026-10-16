plugins {
    id("nagarsetu.android.library.compose")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.predictive" }

dependencies {
    implementation(project(":backend:components:predictive"))
    implementation(project(":backend:components:raksha"))
    implementation(project(":frontend:common-ui"))
    implementation(project(":frontend:components:raksha"))   // RiskApiRepository + RiskCell
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.base)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp)
    implementation(libs.bundles.compose.ui)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.osmdroid.android)
    ksp(libs.hilt.compiler)
}
