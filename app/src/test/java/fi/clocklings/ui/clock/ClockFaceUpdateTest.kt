package fi.clocklings.ui.clock

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
class ClockFaceUpdateTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `immediate mini clock updates its hands without a second draw`() {
        val command = mutableStateOf(ClockCommand(0))
        var draws = 0
        compose.setContent {
            ClocklingsTheme {
                ClockFace(
                    command.value,
                    Modifier.size(200.dp).testTag("clock").drawWithContent {
                        draws++
                        drawContent()
                    },
                    mini = true,
                )
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            draws = 0
            command.value = ClockCommand(15)
        }
        compose.waitForIdle()
        val description =
            compose.onNodeWithTag("clock").fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
        assertTrue(description.single().contains(timeLanguage(AppLanguage.ENGLISH).fullWords(15, h24 = false)))
        assertEquals(1, draws)
        val pixels = compose.onNodeWithTag("clock").captureToImage().toPixelMap()
        val scale = pixels.width / ClockSpec.MINI_VIEW_SIZE
        // At minute 15 the blue shaft points right, clear of the centre pin and numeral 3.
        val x = ((ClockSpec.CENTER + 60f - ClockSpec.MINI_VIEW_ORIGIN) * scale).roundToInt()
        val y = ((ClockSpec.CENTER - ClockSpec.MINI_VIEW_ORIGIN) * scale).roundToInt()
        var handPixels = 0
        for (px in x - 1..x + 1) {
            for (py in y - 1..y + 1) {
                val color = pixels[px, py]
                if (abs(color.red - SkColors.Minute.red) < 0.16f &&
                    abs(color.green - SkColors.Minute.green) < 0.16f &&
                    abs(color.blue - SkColors.Minute.blue) < 0.16f
                ) {
                    handPixels++
                }
            }
        }
        assertTrue("The minute hand was not drawn pointing right after the update", handPixels > 0)
    }
}
