import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Properties

fun loadPropertiesFromFile(filePath: String): Properties {
    val properties = Properties()
    properties.load(FileInputStream(filePath))
    return properties
}


plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleAndroidLibrariesMapsplatformSecretsGradlePlugin)
}

android {
    namespace = "com.example.med6grp5supercykelstig"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.med6grp5supercykelstig"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.play.services.maps)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.google.maps.services)
    implementation(libs.android.maps.utils)
    implementation (libs.places)
//    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("mysql:mysql-connector-java:5.1.49")
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation ("org.mindrot:jbcrypt:0.4")


}