plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nagarsetu.firebase"
}

dependencies {
    implementation(project(":backend:components:core"))
    implementation(libs.bundles.base)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase BOM — single version controls all Firebase libraries
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)        // Realtime Database (live GPS tracking)
    implementation(libs.firebase.messaging.ktx)       // FCM push notifications
    implementation(libs.firebase.analytics.ktx)       // Analytics
    implementation(libs.firebase.crashlytics.ktx)     // Crash reporting
    implementation(libs.firebase.firestore.ktx)       // Firestore (ephemeral live events)

    // Coroutines support for Firebase Tasks
    implementation(libs.kotlinx.coroutines.play.services)
}
