plugins {
    id("nagarsetu.android.library.compose")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android { namespace = "com.nagarsetu.auth" }

dependencies {
    implementation(project(":backend:components:auth"))
    implementation(project(":backend:components:firebase"))
    implementation(project(":frontend:common-ui"))

    // Firebase (Hybrid linking)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.base)
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
