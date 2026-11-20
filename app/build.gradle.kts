import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // TODO: enable Firebase
    // alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun getLocalProp(keyName: String): String {
    val direct = localProperties.getProperty(keyName)
    if (!direct.isNullOrBlank()) return direct.trim()
    val entry = localProperties.entries.firstOrNull { it.key.toString().trim() == keyName }
    return entry?.value?.toString()?.trim().orEmpty()
}

val mapsApiKey: String = getLocalProp("MAPS_API_KEY")
val routeApiKey: String = getLocalProp("ROUTE_API_KEY").ifBlank { mapsApiKey }
val geocodingApiKey: String = getLocalProp("GEOCODING_API_KEY").ifBlank { mapsApiKey }
val placesApiKey: String = getLocalProp("PLACES_API_KEY").ifBlank { mapsApiKey }

android {
    namespace = "com.example.mos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mos"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "ROUTE_API_KEY", "\"$routeApiKey\"")
        buildConfigField("String", "GEOCODING_API_KEY", "\"$geocodingApiKey\"")
        buildConfigField("String", "PLACES_API_KEY", "\"$placesApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.maps.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.material.icons.extended)

    // TODO: enable Firebase
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
