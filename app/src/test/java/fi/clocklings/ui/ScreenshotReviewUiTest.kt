package fi.clocklings.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.ClocklingsApplication
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.slots
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.lesson.LessonCatalog
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.lesson.LessonScreen
import fi.clocklings.ui.lesson.LessonUiState
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ScreenshotReviewUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lockedItemDescriptionsUseTheThresholdQuantityInEveryLanguageAndDisplaySize() {
        val app = ApplicationProvider.getApplicationContext<ClocklingsApplication>()
        val model = AppViewModel(FakeServices())
        val language = mutableStateOf("en")
        val viewport = mutableStateOf(411 to 1f)
        compose.setContent {
            val context =
                app.createConfigurationContext(
                    Configuration(app.resources.configuration).apply {
                        setLocale(Locale.forLanguageTag(language.value))
                    },
                )
            CompositionLocalProvider(
                LocalAppViewModel provides model,
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(2f, viewport.value.second),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                        WardrobeScreen(CompanionKind.CAT, Progress(), CompanionColor.ORIGINAL, {})
                    }
                }
            }
        }
        val descriptions =
            mapOf(
                "en" to
                    listOf("Locked. Opens at 1\u00a0star. You have 0.", "Locked. Opens at 2\u00a0stars. You have 0."),
                "fi" to
                    listOf(
                        "Lukittu. Avautuu, kun tähtiä on 1. Sinulla on 0.",
                        "Lukittu. Avautuu, kun tähtiä on 2. Sinulla on 0.",
                    ),
                "sv" to
                    listOf(
                        "Låst. Låses upp vid 1\u00a0stjärna. Du har 0.",
                        "Låst. Låses upp vid 2\u00a0stjärnor. Du har 0.",
                    ),
            )
        for ((tag, expected) in descriptions) {
            for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
                compose.runOnIdle {
                    language.value = tag
                    viewport.value = cell
                }
                for (description in expected) {
                    compose.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun wardrobeNamesKeepWholeWordsAtOneSizePerGrid() {
        val context = finnishContext()
        val model = AppViewModel(FakeServices())
        val kind = mutableStateOf(CompanionKind.DOG)
        compose.setContent {
            CompositionLocalProvider(
                LocalAppViewModel provides model,
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(2.625f, 1f),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize((1080f / 2.625f).dp, 800.dp)) {
                        WardrobeScreen(kind.value, Progress(stars = listOf(3, 0, 0, 0, 0)), CompanionColor.ORIGINAL, {})
                    }
                }
            }
        }
        for (companion in listOf(CompanionKind.DOG, CompanionKind.ROBOT, CompanionKind.CAT)) {
            compose.runOnIdle { kind.value = companion }
            for (items in Wardrobe.itemsFor(companion).groupBy { it.slot }.values) {
                val sizes =
                    items.map { item ->
                        val text = context.getString(itemName(item.id))
                        assertWholeWords(text).layoutInput.style.fontSize
                    }
                assertEquals("Different name sizes in ${items.first().slot}", 1, sizes.distinct().size)
            }
        }
    }

    @Test
    fun wardrobeSectionTitlesKeepWholeWordsAtNarrowWidthAndDoubleText() {
        val context = finnishContext()
        val model = AppViewModel(FakeServices())
        val kind = mutableStateOf(CompanionKind.CAR)
        compose.setContent {
            CompositionLocalProvider(
                LocalAppViewModel provides model,
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalDensity provides Density(3.375f, 2f),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        WardrobeScreen(kind.value, Progress(), CompanionColor.ORIGINAL, {})
                    }
                }
            }
        }
        for (companion in CompanionKind.entries.filter { it.slots.isNotEmpty() }) {
            compose.runOnIdle { kind.value = companion }
            for (slot in companion.slots) assertWholeWords(context.getString(slotName(slot)))
        }
    }

    @Test
    fun longAnswerWordsStayWholeAtNarrowWidthAndDoubleText() {
        val words = "Twenty-one minutes to four in the afternoon"
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3.375f, 2f)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(256.dp, 640.dp)) {
                        OptionButton(words, {}, digital = "15:39")
                    }
                }
            }
        }
        assertWholeWords(words)
    }

    private fun assertWholeWords(
        text: String,
        index: Int = 0,
    ): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onAllNodesWithText(text, useUnmergedTree = true)[index]
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        for (line in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(line)
            assertTrue(
                "Word split in $text",
                end !in 1 until text.length || text[end - 1].isWhitespace() || text[end - 1] == '-' ||
                    text[end].isWhitespace(),
            )
        }
        return layout
    }

    @Test
    fun eachLessonChapterStartsItsOwnPracticeLevel() {
        var opened = -1
        val context = finnishContext()
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalResources provides context.resources,
                LocalAppLanguage provides AppLanguage.FINNISH,
                LocalDensity provides Density(3.375f, 2f),
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        LessonListScreen(true, Progress(), { opened = it }, {}, {})
                    }
                }
            }
        }
        val names = listOf("Tasatunnit", "Puoli tuntia", "Vartit", "Viiden minuutin välein", "Tarkat minuutit")
        names.forEachIndexed { level, name ->
            assertWholeWords("Aloita pelaaminen!", level)
            compose
                .onNode(
                    hasContentDescription("$name. Harjoittele. Aloita pelaaminen!", substring = true) and
                        hasClickAction(),
                ).performScrollTo()
                .assertIsDisplayed()
                .performClick()
            assertEquals(level, opened)
        }
    }

    private fun finnishContext() =
        ApplicationProvider.getApplicationContext<ClocklingsApplication>().let { app ->
            app.createConfigurationContext(
                Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag("fi")) },
            )
        }

    @Test
    fun lessonNavigationRemainsVisibleWithoutScrollingAtNarrowWidthAndLargeText() {
        var next = 0
        val viewport = mutableStateOf(320 to 1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, viewport.value.second)) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 640.dp)) {
                        LessonScreen(
                            LessonUiState(LessonCatalog.lesson("minuutit")),
                            false,
                            true,
                            {},
                            { next++ },
                            {},
                            {},
                            {},
                            {},
                        )
                    }
                }
            }
        }
        for (cell in listOf(320 to 1f, 411 to 2f, 320 to 2f)) {
            compose.runOnIdle { viewport.value = cell }
            compose.onNodeWithText("Previous").assertIsDisplayed()
            compose.onNodeWithText("Next").assertIsDisplayed().performClick()
        }
        assertEquals(3, next)
    }

    @Test
    fun answerTimeChipsKeepTheSameEndEdgeWithDifferentWordLengths() {
        compose.setContent {
            ClocklingsTheme {
                Column(Modifier.requiredSize(320.dp, 600.dp)) {
                    OptionButton("One", {}, digital = "1:00")
                    OptionButton("Twenty-five minutes past eleven", {}, digital = "11:25")
                }
            }
        }
        val first = compose.onNodeWithText("1:00", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("11:25", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(first.right, second.right, 1f)
    }
}
