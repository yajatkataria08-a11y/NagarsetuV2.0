plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.5.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.21")
}

configurations.all {
    resolutionStrategy {
        force("com.squareup:javapoet:1.13.0")
    }
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "nagarsetu.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "nagarsetu.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
    }
}
