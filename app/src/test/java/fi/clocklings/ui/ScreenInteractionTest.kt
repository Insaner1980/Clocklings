package fi.clocklings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fi.clocklings.Screen
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.home.HomeDestination
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allHomeDestinationsRemainReachableAtNarrowWidthAndLargeFont() {
        val viewport = mutableStateOf(411 to 1f)
        val companion = mutableStateOf(CompanionKind.HUMAN)
        var opened: HomeDestination? = null
        compose.setContent {
            val base = LocalDensity.current
            val personalization = Personalization(companion = companion.value)
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, viewport.value.second),
                LocalPersonalization provides personalization,
            ) {
                ClocklingsTheme {
                    Box(Modifier.requiredSize(viewport.value.first.dp, 800.dp)) {
                        HomeScreen(Progress(), Settings(personalization = personalization), { opened = it })
                    }
                }
            }
        }
        val destinations =
            mapOf(
                "Learn" to HomeDestination.KOULU,
                "Explore" to HomeDestination.VAPAA,
                "Play" to HomeDestination.PELAA,
                "Play Together" to HomeDestination.KYSY,
                "My Companion" to HomeDestination.NUKKE,
                "Settings" to HomeDestination.SETTINGS,
            )
        for (cell in listOf(411 to 1f, 320 to 1f, 411 to 2f, 320 to 2f)) {
            for (kind in listOf(CompanionKind.HUMAN, CompanionKind.CAT, CompanionKind.NONE)) {
                compose.runOnIdle {
                    viewport.value = cell
                    companion.value = kind
                }
                for ((label, expected) in destinations) {
                    compose
                        .onAllNodes(
                            (hasText(label) or hasContentDescription(label, substring = true)) and hasClickAction(),
                        )[0]
                        .scrollIfNeeded()
                        .assertIsDisplayed()
                        .performClick()
                    assertEquals("$cell / $kind / $label", expected, opened)
                }
            }
        }
    }

    @Test
    fun eachLevelAndItsLessonDispatchTheCorrectIndex() {
        val progress = mutableStateOf(Progress())
        var played = -1
        var lesson = -1
        var challenge = false
        compose.setContent {
            ClocklingsTheme { LevelsScreen(progress.value, { played = it }, { lesson = it }, { challenge = true }, {}) }
        }
        val titles = listOf("Whole hours", "Half past", "Quarter hours", "Five-minute steps", "Exact minutes")
        for (stars in listOf(listOf(0, 0, 0, 0, 0), listOf(3, 2, 1, 0, 0), listOf(3, 3, 3, 3, 3))) {
            compose.runOnIdle { progress.value = Progress(stars = stars, challengeBest = 7) }
            for ((index, title) in titles.withIndex()) {
                compose
                    .onNode(
                        hasContentDescription("$title.", substring = true) and hasClickAction(),
                    ).scrollIfNeeded()
                    .performClick()
                assertEquals(index, played)
                compose.onNodeWithContentDescription("Watch the lesson: $title").scrollIfNeeded().performClick()
                assertEquals(index, lesson)
            }
        }
        compose
            .onNode(
                hasContentDescription("Minute Challenge.", substring = true) and hasClickAction(),
            ).scrollIfNeeded()
            .performClick()
        assertEquals(true, challenge)
    }

    @Test
    fun resultStarsAndActionsMatchTheCompletedLevelAndCompanion() {
        val kind = mutableStateOf(CompanionKind.HUMAN)
        val result = mutableStateOf(Screen.Result(0, 1, 0, 0))
        var action = ""
        compose.setContent {
            CompositionLocalProvider(LocalPersonalization provides Personalization(companion = kind.value)) {
                ClocklingsTheme {
                    ResultScreen(
                        result.value,
                        Outfit.default(kind.value),
                        { action = "replay" },
                        { action = "next" },
                        { action = "outfits" },
                        { action = "levels" },
                    )
                }
            }
        }
        for (companion in CompanionKind.entries) {
            for (stars in 1..3) {
                compose.runOnIdle {
                    kind.value = companion
                    result.value = Screen.Result(0, stars, 0, 0)
                }
                val description = if (stars == 1) "1\u00A0star out of three" else "$stars\u00A0stars out of three"
                compose.onNodeWithContentDescription(description).assertExists()
                compose.onNodeWithText("Play again").scrollIfNeeded().performClick()
                assertEquals("replay", action)
                compose.onNodeWithText("Next level").scrollIfNeeded().performClick()
                assertEquals("next", action)
                compose.onNodeWithText("Levels").scrollIfNeeded().performClick()
                assertEquals("levels", action)
            }
        }
        compose.runOnIdle { result.value = Screen.Result(4, 3, 3, 15) }
        compose.onNodeWithText("Next level").assertDoesNotExist()
    }

    private fun SemanticsNodeInteraction.scrollIfNeeded(): SemanticsNodeInteraction {
        if (generateSequence(
                fetchSemanticsNode().parent,
            ) { it.parent }.any { SemanticsActions.ScrollBy in it.config }
        ) {
            performScrollTo()
        }
        return this
    }
}
