import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
    id("jacoco")
}

// Apply JaCoCo configuration
// apply(from = "../jacoco-config.gradle") // Temporairement dÃƒÆ’Ã‚Â©sactivÃƒÆ’Ã‚Â©

android {
    namespace = "com.example.n8nmonitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.n8nmonitor.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Inclure les symboles de dÃ©bogage pour l'analyse des plantages
            ndk {
                debugSymbolLevel = "FULL"
            }
            // GÃ©nÃ©rer les symboles de dÃ©bogage natifs
            packaging {
                jniLibs {
                    keepDebugSymbols += "**/*.so"
                }
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            // Inclure les symboles de dÃ©bogage pour le dÃ©veloppement
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Configuration pour les symboles de dÃ©bogage
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.compose)
    implementation(libs.activity.compose)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.moshi.adapters)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation("androidx.room:room-paging:2.6.1")
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher)

    // WorkManager
    implementation(libs.workmanager)
    implementation(libs.hilt.work)

    // DataStore
    implementation(libs.datastore)

    // Biometric
    implementation(libs.biometric)

    // UI Enhancements
    implementation(libs.coil.compose)
    implementation(libs.accompanist.placeholder)
    implementation(libs.accompanist.swiperefresh)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.work.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}




