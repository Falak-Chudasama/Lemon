import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val localPropFile = rootProject.file("local.properties")
    if (localPropFile.exists()) {
        localPropFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.coconut"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coconut"
        minSdk = 29
        targetSdk = 36
        versionCode = 3
        versionName = "2.0.0"
        manifestPlaceholders["appLabel"] = "Lemon"
    }

    signingConfigs {
        create("release") {
            val configuredPath = System.getenv("COCONUT_KEYSTORE_PATH")
                ?: (project.findProperty("COCONUT_KEYSTORE_PATH") as? String)
                ?: localProperties.getProperty("COCONUT_KEYSTORE_PATH")
                ?: (System.getProperty("user.home") + "/.coconut/keystore/coconut-release.jks")

            var keystoreFile = file(configuredPath)
            if (!keystoreFile.exists()) {
                val fallbackRepoKeystore = rootProject.file("coconut-release.jks")
                if (fallbackRepoKeystore.exists()) {
                    keystoreFile = fallbackRepoKeystore
                }
            }

            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("COCONUT_KEYSTORE_PASSWORD")
                    ?: (project.findProperty("COCONUT_KEYSTORE_PASSWORD") as? String)
                    ?: localProperties.getProperty("COCONUT_KEYSTORE_PASSWORD")
                    ?: throw GradleException("COCONUT_KEYSTORE_PASSWORD is not configured.")

                keyAlias = System.getenv("COCONUT_KEY_ALIAS")
                    ?: (project.findProperty("COCONUT_KEY_ALIAS") as? String)
                    ?: localProperties.getProperty("COCONUT_KEY_ALIAS")
                    ?: "coconut"

                keyPassword = System.getenv("COCONUT_KEY_PASSWORD")
                    ?: (project.findProperty("COCONUT_KEY_PASSWORD") as? String)
                    ?: localProperties.getProperty("COCONUT_KEY_PASSWORD")
                    ?: throw GradleException("COCONUT_KEY_PASSWORD is not configured.")

                enableV1Signing = true
                enableV2Signing = true
            } else {
                throw GradleException("Release keystore not found at: ${keystoreFile.absolutePath}")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["appLabel"] = "Lemon"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "Lemon (Debug)"
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

tasks.matching { it.name.startsWith("kapt") && it.name.contains("UnitTest") }.configureEach {
    enabled = false
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    testImplementation("junit:junit:4.13.2")
}
