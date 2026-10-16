plugins {
    id("nagarsetu.android.library.compose")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nagarsetu.core"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.base)
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.appcompat)
    implementation(libs.osmdroid.android)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.android.maps.utils)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.security.crypto)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlinx.coroutines.play.services)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)

    // Ktor
    implementation(libs.ktor.android)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.android.testing)
}
