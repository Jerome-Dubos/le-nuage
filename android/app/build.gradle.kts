plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Assets natifs partagés avec l'iOS (nuages SVG + banques de vannes). Copiés depuis la
// racine du repo → source unique, pas de duplication versionnée. Bundlés sous assets/.
val partageDir = layout.buildDirectory.dir("generated/partageAssets")
val copiePartage = tasks.register<Copy>("copiePartageAssets") {
    val repo = rootProject.projectDir.parentFile
    from("$repo/svg") { into("svg") }
    from("$repo/svg-doux") { into("svg-doux") }
    from("$repo/svg-accessoires") { into("svg-accessoires") }
    from(repo) { include("vannes-taquines.json", "messages-doux.json") }
    into(partageDir)
}
tasks.withType<com.android.build.gradle.tasks.MergeSourceSetFolders>().configureEach {
    dependsOn(copiePartage)
}

android {
    namespace = "fr.duboswebservices.monptitnuage"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.duboswebservices.monptitnuage"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "0.20"
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

    // Cœur de rendu partagé (web/) + assets natifs partagés (générés ci-dessus).
    sourceSets["main"].assets.srcDirs("src/main/assets", "../../web")
    sourceSets["main"].assets.srcDir(partageDir)
}

dependencies {
    // ViewPager2 pour le balayage entre pages météo (tire recyclerview + core).
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    // WorkManager pour le réveil quotidien + l'alerte pluie en arrière-plan.
    implementation("androidx.work:work-runtime:2.10.0")
}
