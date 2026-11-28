package fi.clocklings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.itemName
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Rule
import org.junit.Test

class TransformationRewardDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fireEngineSpinAndJumpFollowSystemMotionSetting() {
        RenderedMotionScenario(compose, CompanionKind.CAR, "outfit-review/motion")
            .verifyMotion(Slot.MODEL, "car-model-fire-engine")
    }

    @Test
    fun rewardScreenRevealsModelsAtTheirEarnedThresholds() {
        val selected = mutableStateOf("car-model-fire-engine")
        compose.setContent {
            val item = Wardrobe.item(selected.value)
            key(item.id) {
                ClocklingsTheme {
                    CompositionLocalProvider(
                        LocalPersonalization provides Personalization(companion = item.companion),
                    ) {
                        ResultScreen(
                            AccessPolicy(fullAccess = true),
                            Screen.Result(0, 3, 2, item.star - 1),
                            Outfit.default(item.companion),
                            {},
                            {},
                            {},
                            {},
                        )
                    }
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (id in listOf("car-model-fire-engine", "robot-model-driver", "model-veterinarian")) {
            compose.runOnIdle { selected.value = id }
            compose.mainClock.advanceTimeBy(4_000)
            compose
                .onNodeWithTag(
                    "companion_${Wardrobe.item(id).companion.name}",
                    useUnmergedTree = true,
                ).assertIsDisplayed()
            compose.onNodeWithText(context.getString(itemName(id))).scrollToIfScrollable().assertIsDisplayed()
            compose.onNodeWithTag("item_art_$id", useUnmergedTree = true).assertExists()
        }
    }
}
