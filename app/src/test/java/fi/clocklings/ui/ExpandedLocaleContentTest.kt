package fi.clocklings.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.ui.locale.resourceLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExpandedLocaleContentTest {
    @Test
    fun germanAndFrenchResourcesWrapTheirOwnClockGrammar() {
        val german = localizedContext(AppLanguage.GERMAN).resources
        assertEquals("Lernen", german.getString(R.string.menu_koulu))
        assertEquals("Es ist ein Uhr", spokenTime(german, 60, false))
        assertEquals("Es ist halb eins", spokenTime(german, 750, false))
        assertEquals(
            "Es ist halb vier nachmittags. Im 24-Stunden-Format: fünfzehn Uhr dreißig",
            spokenTime(german, 930, true),
        )

        val french = localizedContext(AppLanguage.FRENCH).resources
        assertEquals("Apprendre", french.getString(R.string.menu_koulu))
        assertEquals("Il est une heure", spokenTime(french, 60, false))
        assertEquals("Il est trois heures et demie", spokenTime(french, 210, false))
        assertEquals(
            "Il est midi et demi. Au format 24 heures, douze heures trente",
            spokenTime(french, 750, true),
        )
        assertEquals(
            "Il est minuit et demi. Au format 24 heures, zéro heure trente",
            spokenTime(french, 30, true),
        )
    }

    @Test
    fun spanishNarrationAgreesWithNamedHoursAndActualNumericHours() {
        val spanish = localizedContext(AppLanguage.SPANISH).resources
        assertEquals("Aprender", spanish.getString(R.string.menu_koulu))
        assertEquals("Es la una", spokenTime(spanish, 60, false))
        assertEquals("Son las dos", spokenTime(spanish, 120, false))
        assertEquals("Es la una menos cinco", spokenTime(spanish, 775, false))
        assertEquals(
            "Son las nueve y veintiuno de la noche. En formato de 24 horas: las veintiuna horas y veintiún minutos",
            spokenTime(spanish, 1281, true),
        )
        assertEquals(
            "Son las diez y veintidós de la noche. En formato de 24 horas: las veintidós horas y veintidós minutos",
            spokenTime(spanish, 1342, true),
        )
    }

    @Test
    fun brazilianNarrationAgreesWithHoursMinutesAndRemainingTime() {
        val portuguese = localizedContext(AppLanguage.PORTUGUESE_BRAZIL).resources
        assertEquals("Que horas são?", portuguese.getString(R.string.question_read))
        assertEquals("É uma hora e um minuto", spokenTime(portuguese, 61, false))
        assertEquals("São duas horas e dois minutos", spokenTime(portuguese, 122, false))
        assertEquals("Faltam vinte e cinco minutos para as quatro", spokenTime(portuguese, 215, false))
        assertEquals(
            "São nove horas e vinte e um minutos da noite. " +
                "No formato de 24 horas: vinte e uma horas e vinte e um minutos",
            spokenTime(portuguese, 1281, true),
        )
        assertEquals(
            "São dez horas e vinte e dois minutos da noite. " +
                "No formato de 24 horas: vinte e duas horas e vinte e dois minutos",
            spokenTime(portuguese, 1342, true),
        )
    }

    @Test
    fun midnightAndNoonTemplatesKeepTheCorrectNumericUnits() {
        val expected =
            mapOf(
                AppLanguage.GERMAN to
                    Pair(
                        "Es ist Mitternacht. Im 24-Stunden-Format: null Uhr.",
                        "Es ist Mittag. Im 24-Stunden-Format: zwölf Uhr.",
                    ),
                AppLanguage.FRENCH to
                    Pair(
                        "Il est minuit. Au format 24 heures, zéro heure.",
                        "Il est midi. Au format 24 heures, douze heures.",
                    ),
                AppLanguage.SPANISH to
                    Pair(
                        "Es medianoche. En formato de 24 horas: las cero horas.",
                        "Es mediodía. En formato de 24 horas: las doce horas.",
                    ),
                AppLanguage.PORTUGUESE_BRAZIL to
                    Pair(
                        "É meia-noite. No formato de 24 horas: zero hora.",
                        "É meio-dia. No formato de 24 horas: doze horas.",
                    ),
            )
        for ((language, sentences) in expected) {
            val resources = localizedContext(language).resources
            assertEquals(language.tag, sentences.first, spokenTime(resources, 0, true))
            assertEquals(language.tag, sentences.second, spokenTime(resources, 720, true))
        }
    }

    @Test
    fun portugueseDeviceLocalesResolveToBrazilianResourcesAndNarration() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        for (tag in listOf("pt", "pt-PT", "pt-BR")) {
            val deviceContext =
                app.createConfigurationContext(
                    Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) },
                )
            val resolved = resourceLanguage(deviceContext.resources)
            assertEquals(tag, AppLanguage.PORTUGUESE_BRAZIL, resolved)

            // AppLocaleProvider applies the resolved language tag to its resource context.
            val resources = localizedContext(resolved).resources
            assertEquals(tag, "pt-BR", resources.configuration.locales[0].toLanguageTag())
            assertEquals(tag, "Que horas são?", resources.getString(R.string.question_read))
            assertEquals(tag, "É uma hora e um minuto", spokenTime(resources, 61, false))
        }
    }
}
