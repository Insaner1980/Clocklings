package fi.clocklings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.intro.IntroOverlay
import fi.clocklings.ui.intro.IntroTimeline
import fi.clocklings.ui.intro.coveredByIntro
import fi.clocklings.ui.isReduceMotion
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntroOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tickPlaysOnceWhenTheHandsStopAndTheIntroFinishes() {
        assumeFalse(isReduceMotion(InstrumentationRegistry.getInstrumentation().targetContext))
        var ticks = 0
        var ticksAtDone = -1
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ClocklingsTheme {
                IntroOverlay(Outfit.Default, onTick = { ticks++ }, onDone = { ticksAtDone = ticks })
            }
        }
        // Kesken pyörähdyksen tik ei ole vielä soinut.
        compose.mainClock.advanceTimeBy(IntroTimeline.SPIN_START_MS + IntroTimeline.SPIN_MS / 2)
        compose.runOnIdle { assertEquals(0, ticks) }
        // Pyörähdyksen jälkeen tik on soinut kerran.
        compose.mainClock.advanceTimeBy(IntroTimeline.SPIN_MS.toLong())
        compose.runOnIdle { assertEquals(1, ticks) }
        compose.mainClock.advanceTimeBy(IntroTimeline.END_MS)
        compose.runOnIdle { assertEquals(1, ticksAtDone) }
    }

    @Test
    fun tapSkipsTheIntro() {
        var done = 0
        // Kello pysäytetään ennen koostamista, jotta intro ei ehdi päättyä itsestään.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ClocklingsTheme { IntroOverlay(Outfit.Default, onTick = {}, onDone = { done++ }) }
        }
        compose.onNodeWithContentDescription("Clocklings", substring = true).performClick()
        compose.runOnIdle { assertEquals(1, done) }
    }

    @Test
    fun contentUnderTheIntroIsHiddenFromAccessibilityUntilItEnds() {
        var covered by mutableStateOf(true)
        compose.setContent {
            Box(Modifier.coveredByIntro(covered)) {
                Button(onClick = {}) { Text("Play") }
            }
        }
        compose.onNodeWithText("Play").assertDoesNotExist()
        covered = false
        compose.onNodeWithText("Play").assertExists()
    }
}
