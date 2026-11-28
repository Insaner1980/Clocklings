package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RenderedDogDeviceTest : RenderedWardrobeDeviceTest(CompanionKind.DOG) {
    @Test
    fun dogColorsAndOutfitSurviveRecreation() {
        openWardrobe()
        captureColors()
        compose.clickByLabel("Party hat")
        selectColor("PURPLE")
        for (label in listOf("Backpack", "Puddle puppy", "Frisbee")) selectItem(label)
        verifyOutfitAfterRecreation("PURPLE", "Frisbee", "purple_frisbee")
    }
}
