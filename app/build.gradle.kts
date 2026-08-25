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
        versionCode = 78
        versionName = "0.30.5"
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

val lockScreenContractFiles = mapOf(
    "manifest" to file("src/main/AndroidManifest.xml"),
    "activity" to file("src/main/java/com/kalima/quran/lockscreen/LockScreenStudyActivity.kt"),
    "service" to file("src/main/java/com/kalima/quran/lockscreen/LockScreenStudyService.kt"),
    "safety" to file("src/main/java/com/kalima/quran/lockscreen/LockScreenSystemSafety.kt"),
    "wakePolicy" to file("src/main/java/com/kalima/quran/data/LockScreenWakePolicy.kt"),
)

val startupContractFiles = mapOf(
    "progress" to file("src/main/java/com/kalima/quran/data/ProgressStore.kt"),
    "words" to file("src/main/java/com/kalima/quran/data/WordRepository.kt"),
    "reader" to file("src/main/java/com/kalima/quran/ui/QuranReaderScreen.kt"),
)

val verifyLockScreenRegression by tasks.registering {
    group = "verification"
    description = "Verifies that cards open over the keyguard without waking or holding the display."
    inputs.files(lockScreenContractFiles.values)

    doLast {
        val sources = lockScreenContractFiles.mapValues { (_, source) -> source.readText() }
        fun requireContract(fileKey: String, snippet: String, explanation: String) {
            check(sources.getValue(fileKey).contains(snippet)) {
                "Lock-screen regression: $explanation (${lockScreenContractFiles.getValue(fileKey)})."
            }
        }

        requireContract(
            "manifest",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "background activity launch permission is missing",
        )
        requireContract(
            "manifest",
            "android:showWhenLocked=\"true\"",
            "LockScreenStudyActivity is not declared to show over the keyguard",
        )
        requireContract(
            "manifest",
            "android:turnScreenOn=\"false\"",
            "the activity must not wake the display by itself",
        )
        requireContract(
            "activity",
            "setShowWhenLocked(true)",
            "the runtime Samsung-compatible showWhenLocked safeguard is missing",
        )
        requireContract(
            "activity",
            "setTurnScreenOn(false)",
            "the activity must preserve user-controlled display wakes",
        )
        requireContract(
            "activity",
            "WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON",
            "the activity no longer explicitly clears the keep-screen-on flag",
        )
        requireContract(
            "activity",
            "inactivityHandler.postDelayed(finishAfterInactivity, CARD_INACTIVITY_TIMEOUT_MS)",
            "an untouched lock-screen card can remain visible indefinitely",
        )
        requireContract(
            "activity",
            "LockScreenSystemSafety.blockReason(this, allowLockedDevice = true)",
            "the activity incorrectly rejects the still-locked keyguard",
        )
        requireContract(
            "service",
            "Intent.ACTION_SCREEN_ON",
            "the foreground service no longer observes display wakes",
        )
        requireContract(
            "service",
            "LockScreenWakeEvent.DisplayWoke",
            "display wakes no longer request the lock-screen card",
        )
        requireContract(
            "service",
            "allowLockedDevice = true",
            "the service incorrectly rejects launches over the keyguard",
        )
        requireContract(
            "safety",
            "allowLockedDevice: Boolean = false",
            "locked-device safety is no longer an explicit opt-in",
        )
        requireContract(
            "wakePolicy",
            "event == LockScreenWakeEvent.DisplayWoke",
            "the wake policy waits until after authentication",
        )
        check(!sources.getValue("service").contains("Intent.ACTION_USER_PRESENT")) {
            "Lock-screen regression: the service must not wait for ACTION_USER_PRESENT."
        }
        check(
            !sources.getValue("activity").contains(
                "addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON",
            ),
        ) {
            "Lock-screen regression: the activity must never keep the display awake."
        }
    }
}

val verifyStartupRegression by tasks.registering {
    group = "verification"
    description = "Verifies that optional Quran reader data stays off the app startup path."
    inputs.files(startupContractFiles.values)

    doLast {
        val sources = startupContractFiles.mapValues { (_, source) -> source.readText() }
        check(!sources.getValue("progress").contains("QuranReaderRepository.initialize")) {
            "Startup regression: ProgressStore must not load all Quran pages before first render."
        }
        check(sources.getValue("words").contains("readerIndex = null")) {
            "Startup regression: the Quran reader word index must remain deferred."
        }
        check(sources.getValue("words").contains("fun prepareReaderIndex()")) {
            "Startup regression: the deferred Quran reader word index entry point is missing."
        }
        check(sources.getValue("reader").contains("initializeQuranReader(context)")) {
            "Startup regression: the Quran tab no longer loads its offline pages on demand."
        }
        check(sources.getValue("reader").contains("WordRepository.prepareReaderIndex()")) {
            "Startup regression: the Quran tab no longer prepares word lookups off the startup path."
        }
    }
}

tasks.configureEach {
    if (name == "packageRelease") dependsOn(verifyUptodownSigning)
    if (name == "preBuild" || name == "check") {
        dependsOn(verifyLockScreenRegression, verifyStartupRegression)
    }
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
