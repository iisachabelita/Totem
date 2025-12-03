plugins {
    alias(libs.plugins.android.application)
}

android {
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

    namespace = "com.projeto.totem"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.projeto.totem"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/*.so")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.rendering)
    implementation(files("libs/clisitef-android.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(files("libs/EasyLayer-SK210-v2.1.7-release.aar"))
    implementation("org.mozilla.geckoview:geckoview:140.0.20250707120347")
}