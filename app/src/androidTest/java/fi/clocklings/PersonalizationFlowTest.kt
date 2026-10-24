package fi.clocklings

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Settings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class PersonalizationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSyntheticData() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val app = context.applicationContext as ClocklingsApplication
            app.container.repository.resetProgress()
            app.container.repository.updateSettings { Settings(sound = false) }
        }

    @Test
    fun everyCompanionIsAvailableAndAppearsOnHome() {
        for (kind in CompanionKind.entries) {
            click("My Companion")
            choose("choose_companion_${kind.name}")
            if (kind != CompanionKind.NONE) {
                compose.onNodeWithTag("companion_preview").performScrollTo()
                save("companion_${kind.name}")
            }
            click("← Back")
            if (kind == CompanionKind.NONE) {
                compose.onAllNodes(companionArt(), useUnmergedTree = true).assertCountEquals(0)
            } else {
                compose.onNodeWithTag("companion_${kind.name}", useUnmergedTree = true).assertExists()
            }
        }
        compose.activityRule.scenario.recreate()
        waitFor("RIGHT NOW")
        compose.onAllNodes(companionArt(), useUnmergedTree = true).assertCountEquals(0)
        click("Explore")
        compose
            .onNode(hasContentDescription("Clock face:", substring = true))
            .performCustomAccessibilityActionWithLabel("Add 5 minutes")
        compose.onNodeWithText("3:05").assertExists()
        save("no_companion_explore")
    }

    @Test
    fun companionStyleSurvivesSwitchingAndRecreation() {
        click("My Companion")
        choose("choose_companion_CAT")
        click("Choose an outfit")
        choose("color_BLUE")
        click("Striped scarf")
        waitSelected("Striped scarf")
        click("← Back")
        choose("choose_companion_DOG")
        click("Choose an outfit")
        choose("color_PURPLE")
        click("← Back")
        choose("choose_companion_CAT")
        click("Choose an outfit")
        waitTagSelected("color_BLUE")
        waitSelected("Striped scarf")
        compose.activityRule.scenario.recreate()
        waitFor("Outfits")
        waitTagSelected("color_BLUE")
        waitSelected("Striped scarf")
    }

    @Test
    fun everyThemeReachesExploreAndCustomOptionsPersist() {
        val themes =
            listOf(
                "CLASSIC" to "Classic",
                "RAINBOW" to "Rainbow",
                "SPACE" to "Space",
                "FOREST" to "Forest",
                "OCEAN" to "Ocean",
                "GARDEN" to "Garden",
                "WORKSHOP" to "Workshop",
                "CUSTOM" to "My own clock",
            )
        for ((id, name) in themes) {
            click("Clock styles")
            choose("choose_clock_$id")
            compose.onNodeWithTag("selected_clock").performScrollTo()
            save("clock_$id")
            click("← Back")
            clockWithTheme(name).assertExists()
            click("Explore")
            clockWithTheme(name).performCustomAccessibilityActionWithLabel("Add 5 minutes")
            compose.onNodeWithText("3:05").assertExists()
            click("← Back")
        }
        click("Clock styles")
        click("Coral")
        click("Blocks")
        click("Plain")
        click("Pointed ends")
        compose.activityRule.scenario.recreate()
        waitFor("Make it yours")
        for (label in listOf("Coral", "Blocks", "Plain", "Pointed ends")) {
            waitSelected(label)
            compose.onNodeWithText(label).performScrollTo()
            save("control_$label")
            assertReadable(label)
        }
        save("custom_controls")
        compose.onNodeWithTag("selected_clock").performScrollTo()
        save("custom_clock")
        click("← Back")
        click("Play Together")
        clockWithTheme("My own clock").assertExists()
        click("← Back")
        click("Learn")
        // The first lesson uses the same global clock appearance.
        click("Two hands")
        clockWithTheme("My own clock").assertExists()
    }

    private fun waitFor(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(text: String) {
        waitFor(text)
        compose.onAllNodes(hasText(text) and hasClickAction())[0].performScrollTo().performClick()
    }

    private fun choose(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        waitTagSelected(tag)
    }

    private fun waitTagSelected(tag: String) {
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithTag(tag)
                .fetchSemanticsNodes()
                .any { it.config.getOrElse(SemanticsProperties.Selected) { false } }
        }
        compose.onNodeWithTag(tag).assertIsSelected()
    }

    private fun waitSelected(text: String) {
        waitFor(text)
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(text) and hasClickAction()).fetchSemanticsNodes().any {
                it.config.getOrElse(SemanticsProperties.Selected) { false }
            }
        }
    }

    private fun clockWithTheme(name: String) =
        compose.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, name),
        )

    private fun companionArt() =
        SemanticsMatcher("Companion artwork") {
            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("companion_")
        }

    private fun assertReadable(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        for (layout in layouts) {
            assertTrue("Clipped text: $text", !layout.didOverflowHeight)
            for (line in 0 until layout.lineCount) {
                // Centered wrap-content Text retains paragraph coordinates from its wider
                // measurement. Compare the rendered line width, not its paragraph offset.
                assertTrue(
                    "Clipped line: $text",
                    layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width + 1f,
                )
                val end = layout.getLineEnd(line)
                if (line < layout.lineCount - 1 && end in 1 until text.length) {
                    assertTrue("Split word: $text", text[end - 1].isWhitespace() || text[end].isWhitespace())
                }
            }
        }
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory =
            File(
                instrumentation.targetContext.getExternalFilesDir(null),
                "personalization",
            ).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
