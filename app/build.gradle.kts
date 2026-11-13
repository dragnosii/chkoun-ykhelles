import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Clé d'importation : lue depuis un fichier HORS du projet (jamais dans le dépôt).
val cleFichier = file(System.getProperty("user.home") + "/.android-keys/chkoun/signature.properties")
val cle = Properties().apply { if (cleFichier.exists()) cleFichier.inputStream().use { load(it) } }

android {
    namespace = "ma.nexphasegroup.chkoun"
    compileSdk = 36

    defaultConfig {
        applicationId = "ma.nexphasegroup.chkoun"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"
    }

    signingConfigs {
        if (cleFichier.exists()) {
            create("publication") {
                storeFile = file(cle.getProperty("storeFile"))
                storePassword = cle.getProperty("storePassword")
                keyAlias = cle.getProperty("keyAlias")
                keyPassword = cle.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (cleFichier.exists()) signingConfig = signingConfigs.getByName("publication")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
