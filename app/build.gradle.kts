plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.pasindu.woundlab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pasindu.woundlab"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Export Schema for Room verification
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
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

    // --- PACKAGING FIX START ---
    packaging {
        resources {
            // Resolves license collisions from Open Health Stack, Room, and Kotlin Coroutines
            excludes += "META-INF/ASL-2.0.txt"
            excludes += "META-INF/LGPL2.1"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL-3.0.txt"
        }
    }
    // --- PACKAGING FIX END ---

    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // ADDED: Lifecycle Compose for collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- ARCHITECTURE COMPONENTS ---

    // Dependency Injection (Hilt) - KAPT MODE
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Database (Room) - KAPT MODE
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Open Health Stack (FHIR)
    implementation(libs.fhir.engine)
    implementation(libs.fhir.data.capture)

    // Visualization (Vico)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Logging
    implementation(libs.timber)

    // Permissions (Accompanist)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.hilt.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}