import java.util.Properties

plugins {
    id("nagarsetu.android.library")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
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
    namespace = "com.nagarsetu.backend.reportit" 
    
    defaultConfig {
        buildConfigField("String", "ADMIN_BASE_URL",
            "\"${localProperties["NAGARSETU_ADMIN_URL"] ?: "http://10.0.2.2:3000"}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":backend:components:core"))
    implementation(project(":frontend:common-ui"))
    implementation(libs.bundles.base)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.hilt.android)
    implementation(libs.supabase.postgrest)
    ksp(libs.hilt.compiler)
}
