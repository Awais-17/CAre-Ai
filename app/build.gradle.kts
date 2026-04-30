import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

fun propOrDefault(name: String, fallback: String): String {
    val value = project.findProperty(name)?.toString()?.trim()
        ?: localProperties.getProperty(name)?.trim()
    return if (value.isNullOrEmpty()) fallback else value
}

fun quoted(value: String): String = "\"${value.replace("\"", "\\\"")}\""

android {
    namespace = "com.example.careai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.careai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "ACTIVE_REASONING_PROVIDER", quoted(propOrDefault("ACTIVE_REASONING_PROVIDER", "GROK_PRIMARY")))
        buildConfigField("String", "GROK_BASE_URL", quoted(propOrDefault("GROK_BASE_URL", "https://api.x.ai/v1/")))
        buildConfigField("String", "GROQ_API_KEY", quoted(propOrDefault("GROQ_API_KEY", "")))
        buildConfigField("String", "GROQ_MODEL", quoted(propOrDefault("GROQ_MODEL", "llama-3.3-70b-versatile")))
        buildConfigField("String", "GEMMA_BASE_URL", quoted(propOrDefault("GEMMA_BASE_URL", "https://openrouter.ai/api/v1/")))
        buildConfigField("String", "GEMMA_API_KEY", quoted(propOrDefault("GEMMA_API_KEY", "")))
        buildConfigField("String", "GEMMA_MODEL", quoted(propOrDefault("GEMMA_MODEL", "google/gemma-2-9b-it")))
        buildConfigField("String", "SARVAM_BASE_URL", quoted(propOrDefault("SARVAM_BASE_URL", "https://api.example-sarvam-gateway.com/")))
        buildConfigField("String", "SARVAM_API_KEY", quoted(propOrDefault("SARVAM_API_KEY", "")))
        buildConfigField("String", "ABDM_BASE_URL", quoted(propOrDefault("ABDM_BASE_URL", "https://api.example-abdm-gateway.com/")))
        buildConfigField("String", "ABDM_API_KEY", quoted(propOrDefault("ABDM_API_KEY", "")))
        buildConfigField("String", "GEOAPIFY_API_KEY", quoted(propOrDefault("GEOAPIFY_API_KEY", "8548daa9cafe4a09b165f4ceb5815f3b")))
        buildConfigField("String", "GROQ_BASE_URL", quoted(propOrDefault("GROQ_BASE_URL", "https://api.groq.com/openai/v1/")))
        buildConfigField("String", "GROQ_API_KEY", quoted(propOrDefault("GROQ_API_KEY", "")))
        buildConfigField("String", "GEMINI_BASE_URL", quoted(propOrDefault("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta/openai/")))
        buildConfigField("String", "GEMINI_API_KEY", quoted(propOrDefault("GEMINI_API_KEY", "")))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}