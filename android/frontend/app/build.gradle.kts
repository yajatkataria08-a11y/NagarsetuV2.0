import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")        // Processes google-services.json
    id("com.google.firebase.crashlytics")       // Uploads mapping files for crash deobfuscation
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

// Load secrets from local.properties and .env
val localProperties = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
    val env = rootProject.file(".env")
    if (env.exists()) {
        env.readLines().forEach { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) props[parts[0].trim()] = parts[1].trim()
        }
    }
}

android {
    namespace = "com.nagarsetu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nagarsetu"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // ── Twilio Verify secrets ──
        buildConfigField("String", "TWILIO_ACCOUNT_SID",
            "\"${localProperties["TWILIO_ACCOUNT_SID"] ?: ""}\"")
        buildConfigField("String", "TWILIO_AUTH_TOKEN",
            "\"${localProperties["TWILIO_AUTH_TOKEN"] ?: ""}\"")
        buildConfigField("String", "TWILIO_API_KEY_SID",
            "\"${localProperties["TWILIO_API_KEY_SID"] ?: ""}\"")
        buildConfigField("String", "TWILIO_VERIFY_SID",
            "\"${localProperties["TWILIO_VERIFY_SID"] ?: ""}\"")
        buildConfigField("String", "SENDGRID_API_KEY",
            "\"${localProperties["SENDGRID_API_KEY"] ?: ""}\"")
        buildConfigField("String", "TWILIO_PHONE_NUMBER",
            "\"${localProperties["TWILIO_PHONE_NUMBER"] ?: ""}\"")

        // ── LLM backends (AssistantModule / RAG) ──
        buildConfigField("String", "GROQ_API_KEY",
            "\"${localProperties["GROQ_API_KEY"] ?: ""}\"")
        buildConfigField("String", "GEMINI_API_KEY",
            "\"${localProperties["GEMINI_API_KEY"] ?: ""}\"")

        // ── Supabase secrets ──
        buildConfigField("String", "SUPABASE_URL",
            "\"${localProperties["SUPABASE_URL"] ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",
            "\"${localProperties["SUPABASE_ANON_KEY"] ?: ""}\"")

        // ── External APIs ──
        buildConfigField("String", "OPENWEATHER_API_KEY",
            "\"${localProperties["OPENWEATHER_API_KEY"] ?: ""}\"")
        buildConfigField("String", "DATA_GOV_IN_KEY",
            "\"${localProperties["DATA_GOV_IN_KEY"] ?: ""}\"")
        buildConfigField("String", "OPEN_CHARGE_MAP_KEY",
            "\"${localProperties["OPEN_CHARGE_MAP_KEY"] ?: ""}\"")
        buildConfigField("String", "GNEWS_API_KEY",
            "\"${localProperties["GNEWS_API_KEY"] ?: ""}\"")
        buildConfigField("String", "WAQI_TOKEN",
            "\"${localProperties["WAQI_TOKEN"] ?: ""}\"")

        // ── Admin Dashboard Integration ──
        buildConfigField("String", "ADMIN_BASE_URL",
            "\"${localProperties["NAGARSETU_ADMIN_URL"] ?: "http://10.0.2.2:3000"}\"")
        
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] =
            localProperties["GOOGLE_MAPS_API_KEY"] ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    aaptOptions {
        noCompress("tflite")  // prevents TFLite file compression which breaks loading
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":backend:components:auth"))
    implementation(project(":backend:components:core"))
    implementation(project(":frontend:common-ui"))
    implementation(project(":frontend:components:auth"))
    implementation(project(":frontend:components:assistant"))
    implementation(project(":frontend:components:charge-up"))
    implementation(project(":frontend:components:dashboard"))
    implementation(project(":frontend:components:drive-legal"))
    implementation(project(":frontend:components:emergency-ai"))
    implementation(project(":frontend:components:green-route"))
    implementation(project(":frontend:components:health-watch"))
    implementation(project(":frontend:components:park-ease"))
    implementation(project(":frontend:components:predictive"))
    implementation(project(":backend:components:raksha"))
    implementation(project(":frontend:components:raksha"))
    implementation(project(":frontend:components:road-watch"))
    implementation(project(":frontend:components:report-it"))

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.base)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.preference)
    implementation(libs.osmdroid.android)
    implementation(libs.security.crypto)
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.wearable)
    implementation(libs.android.maps.utils)

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

    // DataStore
    implementation(libs.datastore.preferences)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // ── Firebase (hybrid model — real-time, push, analytics) ──
    implementation(project(":backend:components:firebase"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.android.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
