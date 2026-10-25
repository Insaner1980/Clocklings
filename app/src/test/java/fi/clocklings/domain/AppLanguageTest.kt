package fi.clocklings.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun resolvesFirstSupportedLanguageRatherThanOnlyFirstLocale() {
        assertEquals(AppLanguage.SWEDISH, resolveLanguage(listOf("de-DE", "sv-FI", "fi-FI")))
        assertEquals(AppLanguage.FINNISH, resolveLanguage(listOf("fr", "fi-FI", "en-US")))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(listOf("en-GB", "fi")))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(listOf("es", "fr")))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(emptyList()))
    }

    @Test
    fun explicitTagsResolveSupportedPrimaryLanguagesAndRejectUnsupportedOnes() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromTag("FI-fi"))
        assertEquals(AppLanguage.SWEDISH, AppLanguage.fromTag("sv-SE"))
        assertEquals(AppLanguage.SWEDISH, AppLanguage.fromTag("sv-FI"))
        assertEquals(AppLanguage.SWEDISH, AppLanguage.fromTag("sv-Latn-SE"))
        assertNull(AppLanguage.fromTag(null))
        assertNull(AppLanguage.fromTag(""))
        assertNull(AppLanguage.fromTag("device"))
        assertNull(AppLanguage.fromTag("finnish"))
        assertNull(AppLanguage.fromTag("es-ES"))
    }
}
