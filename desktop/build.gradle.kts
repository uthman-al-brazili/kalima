import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

group = "com.kalima.quran"
version = "0.14.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    testImplementation(kotlin("test"))
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin", "../app/src/main/java")
        kotlin.include(
            "com/kalima/quran/desktop/**",
            "com/kalima/quran/data/QuranWord.kt",
            "com/kalima/quran/data/StudyProgress.kt",
            "com/kalima/quran/data/LearningWordLimiter.kt",
            "com/kalima/quran/data/ReviewHistory.kt",
            "com/kalima/quran/data/SpacedRepetition.kt",
            "com/kalima/quran/data/StreakCalculator.kt",
            "com/kalima/quran/data/GeneratedQuranSurahs.kt",
            "com/kalima/quran/data/GeneratedQuranVocabulary.kt",
            "com/kalima/quran/data/VocabularyAssetLoader.kt",
            "com/kalima/quran/data/WordRepository.kt",
            "com/kalima/quran/localization/AppLanguage.kt",
            "com/kalima/quran/ui/theme/Theme.kt",
            "com/kalima/quran/quiz/QuizQuestion.kt",
            "com/kalima/quran/quiz/QuizEngine.kt",
            "com/kalima/quran/quiz/QuizMastery.kt",
            "com/kalima/quran/quiz/VerseExcerptBuilder.kt",
        )
        resources.srcDirs("../app/src/main/assets", "../app/src/main")
        resources.include("quran_vocabulary.tsv.gz", "ic_launcher-playstore.png")
    }
}

compose.desktop {
    application {
        mainClass = "com.kalima.quran.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "Kalima"
            packageVersion = project.version.toString()
            description = "Aprenda vocabulario do arabe coranico no Windows"
            vendor = "Kalima"
            modules("java.desktop", "java.logging", "java.prefs")

            windows {
                perUserInstall = true
                dirChooser = true
                menuGroup = "Kalima"
                upgradeUuid = "4d68a59b-2a9c-4a63-9497-a30dc1a2575f"
            }
        }
    }
}
