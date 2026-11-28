package fi.clocklings.ui.intro

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.EnglishTime
import fi.clocklings.domain.Greeting
import fi.clocklings.ui.LocalClockTime
import fi.clocklings.ui.greetingText
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntroGreetingVoiceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun narratorUsesTheTimeShownAtTheGreetingBoundary() {
        val time =
            when (EnglishTime.greeting(ClockTime.now())) {
                Greeting.MORNING, Greeting.DAY -> 4 * 60 + 59
                Greeting.EVENING, Greeting.NIGHT -> 17 * 60 + 59
            }
        val greeting = EnglishTime.greeting(time)
        val text = ApplicationProvider.getApplicationContext<Context>().getString(greetingText(greeting))
        var spoken: Greeting? = null
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalClockTime provides { time }) {
                ClocklingsTheme { IntroOverlay(onTick = {}, onDone = {}) }
                IntroGreetingVoiceEffect(active = true) { spoken = it }
            }
        }
        compose.onNodeWithContentDescription(text, substring = true).assertExists()
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(greeting, spoken) }
    }

    @Test
    fun skippingJustBeforeTheLastFrameCompletesTheIntroOnce() {
        var completions = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ClocklingsTheme { IntroOverlay(onTick = {}, onDone = { completions++ }) }
        }
        compose.mainClock.advanceTimeBy(IntroTimeline.END_MS - 48)
        compose.onNodeWithContentDescription("Clocklings", substring = true).performClick()
        compose.runOnIdle { assertEquals(1, completions) }
        compose.mainClock.advanceTimeBy(96)
        compose.runOnIdle { assertEquals(1, completions) }
    }

    @Test
    fun leavingHomeCancelsTheGreetingPermanentlyAcrossReturnAndRestoration() {
        val active = mutableStateOf(true)
        var greetings = 0
        val restoration = StateRestorationTester(compose)
        compose.mainClock.autoAdvance = false
        restoration.setContent { IntroGreetingVoiceEffect(active.value) { greetings++ } }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { assertEquals(0, greetings) }
        compose.runOnIdle { active.value = false }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(0, greetings) }
        compose.runOnIdle { active.value = true }
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(0, greetings) }
    }

    @Test
    fun remainingOnHomeGreetsOnceWithoutReplayAfterRestoration() {
        var greetings = 0
        val restoration = StateRestorationTester(compose)
        compose.mainClock.autoAdvance = false
        restoration.setContent { IntroGreetingVoiceEffect(active = true) { greetings++ } }
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(1, greetings) }
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(IntroTimeline.GREETING_VOICE_MS + 100)
        compose.runOnIdle { assertEquals(1, greetings) }
    }
}
