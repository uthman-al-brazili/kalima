plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigningValues = mapOf(
    "storeFile" to providers.environmentVariable("KALIMA_KEYSTORE_FILE").orNull,
    "storePassword" to providers.environmentVariable("KALIMA_KEYSTORE_PASSWORD").orNull,
    "keyAlias" to providers.environmentVariable("KALIMA_KEY_ALIAS").orNull,
    "keyPassword" to providers.environmentVariable("KALIMA_KEY_PASSWORD").orNull,
)
val hasReleaseSigning = releaseSigningValues.values.all { !it.isNullOrBlank() }

android {
    namespace = "com.kalima.quran"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kalima.quran"
        minSdk = 26
        targetSdk = 36
        versionCode = 58
        versionName = "0.26.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("uptodown") {
                storeFile = file(requireNotNull(releaseSigningValues["storeFile"]))
                storePassword = requireNotNull(releaseSigningValues["storePassword"])
                keyAlias = requireNotNull(releaseSigningValues["keyAlias"])
                keyPassword = requireNotNull(releaseSigningValues["keyPassword"])
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("uptodown")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = false
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

val verifyUptodownSigning by tasks.registering {
    doLast {
        check(hasReleaseSigning) {
            "A public release requires KALIMA_KEYSTORE_FILE, " +
                "KALIMA_KEYSTORE_PASSWORD, KALIMA_KEY_ALIAS, and KALIMA_KEY_PASSWORD."
        }
    }
}

tasks.configureEach {
    if (name == "packageRelease") dependsOn(verifyUptodownSigning)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
