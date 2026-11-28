package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClockTaskScaffoldTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun narrowWindowDoesNotUseTallClockMinimumEvenAtTallHeight() {
        val width = mutableStateOf(320.dp)
        compose.setContent {
            ClocklingsTheme {
                Box(Modifier.requiredSize(width.value, 800.dp)) {
                    ClockTaskScaffold(
                        section = Section.LEARN,
                        topBar = {},
                        visual = { ClockBox(it) {} },
                        clockFit = ClockFit.FIT,
                        tallFitMin = SkDimens.FiveMinuteDemoClockMin,
                    ) {
                        Box(Modifier.height(800.dp))
                    }
                }
            }
        }
        val visual = compose.onNodeWithTag("task_clock", useUnmergedTree = true)
        with(compose.density) {
            assertEquals(SkDimens.FitClockMin.roundToPx(), visual.fetchSemanticsNode().size.width)
            compose.runOnIdle { width.value = 360.dp }
            assertEquals(SkDimens.FiveMinuteDemoClockMin.roundToPx(), visual.fetchSemanticsNode().size.width)
        }
    }

    @Test
    fun actionFocusAndPortraitScrollSurviveMovingIntoAndOutOfTheCompactPane() {
        val window = mutableStateOf(320 to 800)
        lateinit var portrait: androidx.compose.foundation.ScrollState
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            ClocklingsTheme {
                portrait = rememberScrollState()
                Box(Modifier.requiredSize(window.value.first.dp, window.value.second.dp)) {
                    ClockTaskScaffold(
                        section = Section.LEARN,
                        topBar = { TopBar("Lesson", {}) },
                        visual = { ClockBox(it) {} },
                        scrollState = portrait,
                        actions = { focus, _ ->
                            Row {
                                SkButton("Previous", {}, Modifier.taskActionFocus(focus))
                                SkButton("Next", {}, Modifier.testTag("next").taskActionFocus(focus))
                            }
                        },
                    ) { _ ->
                        Column {
                            Box(Modifier.height(1200.dp))
                        }
                    }
                }
            }
        }
        compose.runOnIdle { runBlocking { portrait.scrollTo(200) } }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val action = compose.onNodeWithTag("next")
        action.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        action.assertIsFocused()
        val id = action.fetchSemanticsNode().id
        compose.runOnIdle { window.value = 1000 to 360 }
        assertEquals(id, action.fetchSemanticsNode().id)
        compose.runOnIdle { assertEquals(InputMode.Keyboard, inputMode.inputMode) }
        action.assertIsFocused()
        compose.runOnIdle { window.value = 320 to 800 }
        assertEquals(id, action.fetchSemanticsNode().id)
        compose.runOnIdle { assertEquals(InputMode.Keyboard, inputMode.inputMode) }
        action.assertIsFocused()
        compose.runOnIdle { assertEquals(200, portrait.value) }
    }
}
