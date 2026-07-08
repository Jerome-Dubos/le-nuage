plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.duboswebservices.monptitnuage"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.duboswebservices.monptitnuage"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Le cœur de rendu partagé (web/) est bundlé tel quel comme asset, à la racine du repo.
    sourceSets["main"].assets.srcDirs("src/main/assets", "../../web")
}

dependencies {
    // Jalon 1 : volontairement sans dépendance externe (Activity + WebView du framework).
}
