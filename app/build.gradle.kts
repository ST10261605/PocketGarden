import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.gms.google-services") version "4.4.0"
}


android {
    namespace = "com.example.pocketgarden"
    compileSdk = 36

    defaultConfig {

        buildConfigField("String", "PLANT_ID_API_KEY", "\"mRnpO239bpQY3EcOGlxTgQ9GfXl2Krg6Xqqg4WhDkzzXEwSvlX\"")

        applicationId = "com.example.pocketgarden"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load API key from local.properties
        val localProperties = Properties().apply {
            load(FileInputStream(rootProject.file("local.properties")))
        }
        val apiKey: String = localProperties.getProperty("PLANT_ID_API_KEY") ?: ""
        buildConfigField("String", "PLANT_ID_API_KEY", "\"$apiKey\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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
}

dependencies {
    val camerax_version = "1.3.4"

    //CameraX dependencies
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    //Biometric dependencies
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    //Google Sign in dependencies
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    //RoomDb dependencies
    implementation(libs.androidx.material3)
    implementation(libs.play.services.base)

    val room_version = "2.6.1"

    implementation ("androidx.room:room-runtime:$room_version")
    kapt ("androidx.room:room-compiler:$room_version")

    implementation("androidx.room:room-ktx:${room_version}")

    //adding coroutines, gson, http and retrofit for plant.id API
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("androidx.work:work-runtime-ktx:2.8.1") // for background sync
    implementation ("io.coil-kt:coil:2.3.0") // image loading

    implementation("androidx.fragment:fragment-ktx:1.8.2") //for viewModels

    //for my garden page
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    kapt ("com.github.bumptech.glide:compiler:4.16.0")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    //network status monitoring
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}