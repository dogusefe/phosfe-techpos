plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun propertyOr(key: String, fallback: String): String =
    (project.findProperty(key) as? String)?.takeIf(String::isNotBlank) ?: fallback

android {
    namespace = "com.phosfe.bkmtechpos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.phosfe.bkmtechpos"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "HOST_PRIMARY", "\"${propertyOr("HOST_PRIMARY", "techpos-host.bkmtest.com.tr")}\"")
        buildConfigField("int", "HOST_PORT", propertyOr("HOST_PORT", "12500"))
        buildConfigField("String", "VENDOR_ID", "\"${propertyOr("VENDOR_ID", "00")}\"")
        buildConfigField("String", "PRODUCER_CODE", "\"${propertyOr("PRODUCER_CODE", "PHS")}\"")
        buildConfigField("String", "DEVICE_TYPE", "\"${propertyOr("DEVICE_TYPE", "POS")}\"")
    }

    flavorDimensions += listOf("mode", "vendor")
    productFlavors {
        create("standalone") {
            dimension = "mode"
            buildConfigField("boolean", "EXTERNAL_CONTROL", "false")
        }
        create("ecr") {
            dimension = "mode"
            buildConfigField("boolean", "EXTERNAL_CONTROL", "true")
        }
        create("datecs") {
            dimension = "vendor"
            buildConfigField("String", "HARDWARE_VENDOR", "\"DATECS\"")
        }
        create("newland") {
            dimension = "vendor"
            buildConfigField("String", "HARDWARE_VENDOR", "\"NEWLAND\"")
        }
        create("simulator") {
            dimension = "vendor"
            buildConfigField("String", "HARDWARE_VENDOR", "\"SIMULATOR\"")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

