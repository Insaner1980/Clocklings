package fi.clocklings.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.ui.parent.ParentScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParentScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cancellationNeverOpensProductInformationOrStartsBilling() {
        var back = 0
        compose.setContent {
            ClocklingsTheme {
                ParentScreen(PurchaseUiState(), { error("purchase") }, { error("restore") }, {}, { back++ })
            }
        }
        compose.onNodeWithText("Clocklings Full Unlock").assertDoesNotExist()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals(1, back)
    }

    @Test
    fun keyboardGateAndRestorationRequireAnIntentionalAdultAction() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ClocklingsTheme { ParentScreen(PurchaseUiState(), {}, {}, {}, {}) }
        }
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("0000")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNodeWithText("Check the order and try again.").assertExists()
        compose.onNodeWithText("Clocklings Full Unlock").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        passGate()
        compose.onNodeWithText("Clocklings Full Unlock").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Clocklings Full Unlock").assertDoesNotExist()
    }

    @Test
    fun unavailablePriceDisablesPurchaseAndOffersRestoreAndRetry() {
        var restores = 0
        var retries = 0
        compose.setContent {
            ClocklingsTheme {
                ParentScreen(
                    PurchaseUiState(),
                    { error("purchase") },
                    { restores++ },
                    { retries++ },
                    {},
                )
            }
        }
        passGate()
        compose.onNodeWithText("Restore purchases").performScrollTo().performClick()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(1, restores)
        assertEquals(1, retries)
        compose
            .onNode(hasText("Purchases are unavailable. Please try again later.") and hasClickAction())
            .assertIsNotEnabled()
    }

    @Test
    fun localizedPlayPriceAndOwnershipUpdateWithoutRestart() {
        val state =
            mutableStateOf(
                PurchaseUiState(localizedPrice = "4,50 €", status = PurchaseStatus.READY, canPurchase = true),
            )
        var purchases = 0
        compose.setContent {
            ClocklingsTheme { ParentScreen(state.value, { purchases++ }, {}, {}, {}) }
        }
        passGate()
        compose
            .onNodeWithText("Buy once — 4,50 €")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(1, purchases)
        compose.runOnIdle { state.value = PurchaseUiState(fullAccess = true, status = PurchaseStatus.RESTORED) }
        compose.onNodeWithText("Buy once — 4,50 €").assertDoesNotExist()
        compose.onNodeWithText("Full version restored.").assertExists()
    }

    private fun passGate() {
        val instruction =
            compose
                .onNodeWithText("For adults:", substring = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString { it.text }
        val answer = instruction.filter(Char::isDigit).reversed()
        compose.onNode(hasSetTextAction()).performTextInput(answer)
        compose.onNode(hasSetTextAction()).performImeAction()
    }
}
