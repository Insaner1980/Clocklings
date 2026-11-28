package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RenderedBunnyDeviceTest : RenderedWardrobeDeviceTest(CompanionKind.BUNNY) {
    @Test
    fun bunnyColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors()
        compose.clickByLabel("Magic hat bunny")
        selectColor("PURPLE")
        selectItem("Carrot")
        verifyOutfitAfterRecreation("PURPLE", "Carrot", "purple_carrot")
    }
}
