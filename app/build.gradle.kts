import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val customerId = (project.findProperty("CUSTOMER") as? String)?.takeIf(String::isNotBlank)
    ?: System.getenv("TECHPOS_CUSTOMER")?.takeIf(String::isNotBlank)
    ?: "phosfe"
require(customerId.matches(Regex("[a-z][a-z0-9-]{1,31}"))) { "Invalid CUSTOMER profile: $customerId" }

val customerDirectory = rootProject.file("customers/$customerId")
val customerProfileFile = customerDirectory.resolve("profile.properties")
require(customerProfileFile.isFile) { "Customer profile not found: $customerProfileFile" }
val customerProperties = Properties().apply { customerProfileFile.inputStream().use(::load) }

val customerEnvironmentFile = customerDirectory.resolve("bkm-environment.properties")
val legacyEnvironmentFile = rootProject.file("bkm-environment.properties")
val bkmEnvironmentProperties = Properties().apply {
    if (legacyEnvironmentFile.exists()) legacyEnvironmentFile.inputStream().use(::load)
    if (customerEnvironmentFile.exists()) customerEnvironmentFile.inputStream().use(::load)
}

fun propertyOr(key: String, fallback: String): String =
    (project.findProperty(key) as? String)?.takeIf(String::isNotBlank)
        ?: System.getenv("TECHPOS_$key")?.takeIf(String::isNotBlank)
        ?: bkmEnvironmentProperties.getProperty(key)?.takeIf(String::isNotBlank)
        ?: customerProperties.getProperty(key)?.takeIf(String::isNotBlank)
        ?: fallback

fun quotedProperty(key: String, fallback: String = ""): String =
    "\"${propertyOr(key, fallback).replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun environmentComplete(suffix: String): Boolean = listOf(
    "VENDOR_ID_$suffix",
    "IMMK_$suffix",
    "RSA_MODULUS_$suffix",
    "PRODUCER_CODE",
    "DEVICE_TYPE",
    "SERIAL_HEADER"
).all { propertyOr(it, "").isNotBlank() }

val customerSigningFile = customerDirectory.resolve("keystore.properties")
val signingFile = customerSigningFile.takeIf { it.exists() } ?: rootProject.file("keystore.properties")
val signingProperties = Properties().apply {
    if (signingFile.exists()) signingFile.inputStream().use(::load)
}
val hasReleaseSigning = signingProperties.getProperty("storeFile")
    ?.let(rootProject::file)
    ?.exists() == true

android {
    namespace = "com.phosfe.bkmtechpos"
    compileSdk = 35

    defaultConfig {
        applicationId = propertyOr("APPLICATION_ID", "com.phosfe.bkmtechpos")
        minSdk = 24
        targetSdk = 35
        versionCode = propertyOr("VERSION_CODE", "1").toInt()
        versionName = propertyOr("VERSION_NAME", "1.0.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = propertyOr("APP_NAME", "TechPOS")

        buildConfigField("String", "CUSTOMER_ID", quotedProperty("CUSTOMER_ID", customerId))
        buildConfigField("String", "APP_NAME", quotedProperty("APP_NAME", "TechPOS"))
        buildConfigField("String", "RSA_EXPONENT", "\"010001\"")
        buildConfigField("boolean", "PHASE2_SUPPORT", "true")
        buildConfigField("boolean", "BIN8_SUPPORT", "true")
        buildConfigField("boolean", "QR_SUPPORT", "true")
        buildConfigField("boolean", "FQDN_SUPPORT", "true")
        buildConfigField("boolean", "TR31_SUPPORT", "true")
        buildConfigField("String", "PRODUCER_CODE", quotedProperty("PRODUCER_CODE"))
        buildConfigField("String", "DEVICE_TYPE", quotedProperty("DEVICE_TYPE"))
        buildConfigField("String", "SERIAL_HEADER", quotedProperty("SERIAL_HEADER"))
        buildConfigField("boolean", "IS_DAKT", propertyOr("DAKT", "false"))
        buildConfigField("boolean", "PRINTER_PRESENT", propertyOr("PRINTER_PRESENT", "true"))
        buildConfigField("boolean", "DB_ENCRYPTED", "false")
        buildConfigField("boolean", "HOST_WIRE_LOG", "false")
        buildConfigField("boolean", "DB_TOOLS", "false")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
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
            buildConfigField("boolean", "HOST_WIRE_LOG", "true")
            buildConfigField("boolean", "DB_TOOLS", "true")
            buildConfigField("String", "BKM_ENVIRONMENT", "\"TEST\"")
            buildConfigField("boolean", "BKM_CONFIGURATION_COMPLETE", environmentComplete("TEST").toString())
            buildConfigField("String", "VENDOR_ID", quotedProperty("VENDOR_ID_TEST"))
            buildConfigField("String", "IMMK", quotedProperty("IMMK_TEST"))
            buildConfigField("String", "HOST_RSA_MODULUS", quotedProperty("RSA_MODULUS_TEST"))
            buildConfigField("String", "HOST_PRIMARY", quotedProperty("HOST_PRIMARY_TEST", "techpos-host.bkmtest.com.tr"))
            buildConfigField("int", "PORT_PRIMARY", propertyOr("PORT_PRIMARY_TEST", "12500"))
            buildConfigField("String", "HOST_SECONDARY", quotedProperty("HOST_SECONDARY_TEST", "techpos-host.bkmtest.com.tr"))
            buildConfigField("int", "PORT_SECONDARY", propertyOr("PORT_SECONDARY_TEST", "12500"))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("boolean", "DB_ENCRYPTED", "true")
            buildConfigField("boolean", "HOST_WIRE_LOG", "false")
            buildConfigField("boolean", "DB_TOOLS", "false")
            buildConfigField("String", "BKM_ENVIRONMENT", "\"PRODUCTION\"")
            buildConfigField("boolean", "BKM_CONFIGURATION_COMPLETE", environmentComplete("PROD").toString())
            buildConfigField("String", "VENDOR_ID", quotedProperty("VENDOR_ID_PROD"))
            buildConfigField("String", "IMMK", quotedProperty("IMMK_PROD"))
            buildConfigField("String", "HOST_RSA_MODULUS", quotedProperty("RSA_MODULUS_PROD"))
            buildConfigField("String", "HOST_PRIMARY", quotedProperty("HOST_PRIMARY_PROD", "techpos-host.bkm.com.tr"))
            buildConfigField("int", "PORT_PRIMARY", propertyOr("PORT_PRIMARY_PROD", "12500"))
            buildConfigField("String", "HOST_SECONDARY", quotedProperty("HOST_SECONDARY_PROD", "techpos-host.bkm.com.tr"))
            buildConfigField("int", "PORT_SECONDARY", propertyOr("PORT_SECONDARY_PROD", "12500"))
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        create("demo") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            matchingFallbacks += listOf("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    applicationVariants.all {
        val modeCode = when (productFlavors.firstOrNull { it.dimension == "mode" }?.name) {
            "standalone" -> "STD"
            "ecr" -> "ECR"
            else -> "UNK"
        }
        val vendorCode = when (productFlavors.firstOrNull { it.dimension == "vendor" }?.name) {
            "datecs" -> "DTS"
            "newland" -> "NWL"
            "simulator" -> "SIM"
            else -> "UNK"
        }
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "${propertyOr("ARTIFACT_NAME", "TechPOS")}-$modeCode-$vendorCode-${buildType.name}-v$versionName.apk"
        }
    }

    sourceSets.getByName("main").res.srcDir(customerDirectory.resolve("res"))

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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.sqlcipher.android)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
