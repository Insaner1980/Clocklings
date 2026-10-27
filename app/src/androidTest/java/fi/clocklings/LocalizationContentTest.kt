package fi.clocklings

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Hint
import fi.clocklings.domain.Hints
import fi.clocklings.domain.Text
import fi.clocklings.ui.digitalLabel
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.locale.AppLocaleController
import fi.clocklings.ui.locale.AppLocalePlatform
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.resolve
import fi.clocklings.ui.spokenTime
import fi.clocklings.ui.text
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import androidx.compose.material3.Text as UiText

/** Resolves the actual packaged resources, independently of the emulator's device language. */
@RunWith(AndroidJUnit4::class)
class LocalizationContentTest {
    @get:Rule val compose = createComposeRule()
    private val answers =
        mapOf(
            AppLanguage.ENGLISH to
                listOf(
                    "The short pink hand",
                    "One hour",
                    "Nine o'clock",
                    "Half past five",
                    "Quarter to seven",
                    "Ten past four",
                    "Twenty to nine",
                    "Seven minutes past three",
                    "Seven in the evening: 19:00",
                ),
            AppLanguage.FINNISH to
                listOf(
                    "Lyhyt pinkki viisari",
                    "Yksi tunti",
                    "Tasan yhdeksän",
                    "Puoli kuusi",
                    "Varttia vaille seitsemän",
                    "Kymmentä yli neljä",
                    "Kahtakymmentä vaille yhdeksän",
                    "Seitsemän minuuttia yli kolme",
                    "Seitsemän illalla: 19.00",
                ),
            AppLanguage.SWEDISH to
                listOf(
                    "Den korta rosa visaren",
                    "En timme",
                    "Nio",
                    "Halv sex",
                    "Kvart i sju",
                    "Tio över fyra",
                    "Tjugo i nio",
                    "Sju minuter över tre",
                    "Sju på kvällen: 19.00",
                ),
        )

    @Test
    fun allNineLessonQuizzesMatchIndependentNumericAndLanguageFixtures() {
        val ids = listOf("viisarit", "kierros", "tasan", "puoli", "vartti", "ylivaille", "viisi", "minuutit", "vrk")
        val targets = listOf(240, 180, 540, 330, 405, 250, 520, 187, 1140)
        for (language in AppLanguage.entries) {
            val resources = resources(language.tag)
            val lessons = LessonCatalog.all(language)
            assertEquals(ids, lessons.map { it.id })
            lessons.forEachIndexed { index, lesson ->
                assertEquals("${language.tag}/${lesson.id} time", targets[index], lesson.quiz.time)
                val correct = lesson.quiz.choices.filter { it.correct }
                assertEquals("${language.tag}/${lesson.id} correct count", 1, correct.size)
                assertEquals(answers.getValue(language)[index], correct.single().text.resolve(resources))
                val labels = lesson.quiz.choices.map { it.text.resolve(resources) }
                assertEquals("${language.tag}/${lesson.id} distinct choices", labels.size, labels.toSet().size)
                assertTrue(
                    lesson.quiz.question
                        .resolve(resources)
                        .isNotBlank(),
                )
                assertTrue(
                    lesson.quiz.hint
                        .resolve(resources)
                        .isNotBlank(),
                )
            }
        }
    }

    @Test
    fun resourceAdaptersKeepTimeDigitsNarrationAndContextInTheSameLanguage() {
        val fixtures =
            listOf(
                AdapterFixture(
                    "en",
                    "Twenty-five past three",
                    "Twenty-five to four",
                    "3:07",
                    "15:30",
                    "It is midnight. On a 24-hour clock, zero zero zero.",
                    "It is noon. On a 24-hour clock, twelve zero zero.",
                    "midnight",
                    "noon",
                ),
                AdapterFixture(
                    "fi",
                    "Viittä vaille puoli neljä",
                    "Viittä yli puoli neljä",
                    "3.07",
                    "15.30",
                    "On keskiyö. Numeroina nolla nolla nolla.",
                    "On keskipäivä. Numeroina kaksitoista nolla nolla.",
                    "keskiyö",
                    "keskipäivä",
                ),
                AdapterFixture(
                    "sv",
                    "Fem i halv fyra",
                    "Fem över halv fyra",
                    "3.07",
                    "15.30",
                    "Det är midnatt. Med siffror noll noll noll.",
                    "Klockan är tolv på dagen. Med siffror tolv noll noll.",
                    "midnatt",
                    "middag",
                ),
            )
        for (fixture in fixtures) {
            val resources = resources(fixture.tag)
            assertEquals(fixture.past, Text.Time(205).resolve(resources))
            assertEquals(fixture.to, Text.Time(215).resolve(resources))
            assertEquals(fixture.digital12, digitalLabel(resources, 187, false))
            assertEquals(fixture.digital24, digitalLabel(resources, 930, true))
            assertEquals(fixture.midnight, spokenTime(resources, 0, true))
            assertEquals(fixture.noon, spokenTime(resources, 720, true))
            assertTrue(Hint.HandsRight(0).text(resources).contains(fixture.midnightLabel))
            assertTrue(Hint.HandsRight(720).text(resources).contains(fixture.noonLabel))
            assertEquals("Clocklings", Text.Res(R.string.app_name).resolve(resources))
            assertTrue(Text.Plural(R.plurals.minutes_count, 2).resolve(resources).contains("2"))
        }
    }

    @Test
    fun fiveMinuteLessonExplainsBothSidesOfHalfInEachLanguage() {
        val expected =
            mapOf(
                AppLanguage.ENGLISH to
                    "At number 5, say twenty-five past. At number 7, say twenty-five to the next hour.",
                AppLanguage.FINNISH to
                    (
                        "Numeron 5 kohdalla sanomme viittä vaille puoli. " +
                            "Numeron 7 kohdalla sanomme viittä yli puoli. Nimeä seuraava tunti."
                    ),
                AppLanguage.SWEDISH to "Vid 5 säger vi fem i halv. Vid 7 säger vi fem över halv. Nämn nästa timme.",
            )
        for (language in AppLanguage.entries) {
            val page = LessonCatalog.all(language).single { it.id == "viisi" }.pages[2]
            assertTrue(page.text.map { it.resolve(resources(language.tag)) }.contains(expected.getValue(language)))
        }
    }

    @Test
    fun nearHalfHintCoversTwentyOneThroughThirtyNineWithoutClaimingExactHalf() {
        val expected =
            mapOf(
                "en" to "Near half past, it is near the halfway point between this hour and the next.",
                "fi" to "Puolen tunnin lähellä viisari on lähellä tämän tunnin ja seuraavan tunnin puoliväliä.",
                "sv" to "Nära halv är visaren nära mitten mellan den här timmen och nästa.",
            )
        for ((tag, tip) in expected) {
            for (minute in 20..40) {
                val hint = Hints.forRead(180 + minute, 240 + minute) as Hint.ReadHour
                assertEquals(minute in 21..39, hint.nearHalf)
                val resolved = hint.text(resources(tag))
                if (minute in 21..39) assertTrue(resolved.endsWith(tip)) else assertFalse(resolved.contains(tip))
            }
        }
    }

    @Test
    fun sharedSwedishRegionalResourcesResolveConsistently() {
        val swedish = resources("sv")
        for (tag in listOf("sv-SE", "sv-FI")) {
            val regional = resources(tag)
            assertEquals("Inställningar", regional.getString(R.string.settings_title))
            for (lesson in LessonCatalog.all(AppLanguage.SWEDISH)) {
                assertEquals(lesson.title.resolve(swedish), lesson.title.resolve(regional))
            }
            assertEquals("Fem i halv fyra", Text.Time(205).resolve(regional))
        }
        assertEquals("Settings", resources("de-DE").getString(R.string.settings_title))
        assertEquals("Twenty-five past three", Text.Time(205).resolve(resources("de-DE")))
    }

    @Test
    fun appResourceBoundaryResolvesSupportedSecondaryBeforeLoadingUiAndClockWords() {
        val fixtures =
            mapOf(
                "de-DE,sv-FI" to "sv|Inställningar|Fem i halv fyra",
                "de-DE,fi-FI" to "fi|Asetukset|Viittä vaille puoli neljä",
                "de-DE" to "en|Settings|Twenty-five past three",
            )
        val contexts = fixtures.keys.associateWith(::localizedContext)
        val tags = mutableStateOf(fixtures.keys.first())
        val repository = inMemoryRepository()
        lateinit var controller: AppLocaleController
        compose.runOnIdle { controller = AppLocaleController(repository, DeviceDefaultPlatform) }
        compose.setContent {
            val upstream = contexts.getValue(tags.value)
            CompositionLocalProvider(
                LocalContext provides upstream,
                LocalConfiguration provides upstream.resources.configuration,
                LocalResources provides upstream.resources,
            ) {
                AppLocaleProvider(repository, controller = controller) {
                    val settings = stringResource(R.string.settings_title)
                    val words = Text.Time(205).resolve()
                    UiText("${LocalAppLanguage.current.tag}|$settings|$words")
                }
            }
        }
        for ((source, expected) in fixtures) {
            compose.runOnIdle { tags.value = source }
            compose.onNodeWithText(expected).assertExists()
        }
    }

    private fun inMemoryRepository(): ProgressRepository =
        ProgressRepository(
            object : DataStore<Preferences> {
                override val data = MutableStateFlow<Preferences>(emptyPreferences())

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    transform(data.value).also { data.value = it }
            },
        )

    private object DeviceDefaultPlatform : AppLocalePlatform {
        override val supported = false

        override fun read(): AppLanguage? = null

        override fun write(language: AppLanguage?) = error("This read-only fixture must not write a platform locale")
    }

    private fun resources(tags: String): Resources = localizedContext(tags).resources

    private fun localizedContext(tags: String): android.content.Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(*tags.split(',').map(Locale::forLanguageTag).toTypedArray()))
        return context.createConfigurationContext(configuration)
    }

    private data class AdapterFixture(
        val tag: String,
        val past: String,
        val to: String,
        val digital12: String,
        val digital24: String,
        val midnight: String,
        val noon: String,
        val midnightLabel: String,
        val noonLabel: String,
    )
}
