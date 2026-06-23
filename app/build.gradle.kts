plugins {
    alias(libs.plugins.android.application)
    id("androidx.navigation.safeargs.kotlin")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.iie.group8_prog7313_poe_pt_2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iie.group8_prog7313_poe_pt_2"
        minSdk = 24
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures{
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.13.0")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))

    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    // https://firebase.google.com/docs/android/setup#available-libraries

    // Declare the dependency for the Cloud Firestore library
    implementation("com.google.firebase:firebase-firestore")

    // Add the dependency for the Firebase Authentication library
    implementation("com.google.firebase:firebase-auth")

    //NAVIGATION COMPONENTS/ FRAGMENTS
    val navVersion = "2.9.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    //COROUTINES
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0") // Core library for coroutines (launch, async, Flow, etc.)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // Android-specific library for Main thread support and Dispatchers.Main
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0") // Enables .await() on Firebase Tasks
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0") // View model scope (automatically cancels coroutines when the ViewModel is cleared)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // lifecycle scope (automatically cancels coroutines when the Activity or Fragment is destroyed)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0") //UNIT TESTING FOR COROUTINES

    //COIL
    implementation("io.coil-kt:coil:2.7.0") // Coil 2 — View-based image loading (imageView.load {})
    implementation("io.coil-kt.coil3:coil-compose:3.4.0") // Coil 3 core for Jetpack Compose
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0") // Required: Network artifact to load images from URLs

    //VIEWMODEL
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    //LIVEDATA
    implementation("androidx.lifecycle:lifecycle-livedata:2.10.0")
}