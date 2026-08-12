package com.kalima.quran.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageManagerTest {
    @Test
    fun portuguesePhoneDefaultsToPortuguese() {
        assertEquals(
            AppLanguage.Portuguese,
            LanguageManager.defaultLanguageFor("pt-BR"),
        )
        assertEquals(
            AppLanguage.Portuguese,
            LanguageManager.defaultLanguageFor("pt_PT"),
        )
    }

    @Test
    fun everyOtherPhoneLanguageDefaultsToEnglish() {
        assertEquals(AppLanguage.English, LanguageManager.defaultLanguageFor("en-US"))
        assertEquals(AppLanguage.English, LanguageManager.defaultLanguageFor("es-BR"))
        assertEquals(AppLanguage.English, LanguageManager.defaultLanguageFor(null))
    }
}
