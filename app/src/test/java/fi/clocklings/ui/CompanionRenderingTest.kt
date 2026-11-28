package fi.clocklings.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.colorOptions
import fi.clocklings.domain.isTransformation
import fi.clocklings.ui.companion.CompanionRig
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.captureRenderedImage
import fi.clocklings.ui.companion.rememberCompanionRig
import fi.clocklings.ui.companion.rememberTransformationRig
import fi.clocklings.ui.theme.ClocklingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class CompanionRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun everyCompanionAndWearableProducesVisibleDistinctArtwork() {
        val kind = mutableStateOf(CompanionKind.CAT)
        val outfit = mutableStateOf(Outfit.default(CompanionKind.CAT))
        compose.setContent {
            ClocklingsTheme {
                CompanionView(kind.value, outfit.value, Modifier.size(180.dp, 240.dp), animated = false)
            }
        }
        for (companion in CompanionKind.entries.filter { it != CompanionKind.NONE }) {
            compose.runOnIdle {
                kind.value = companion
                outfit.value = Outfit.default(companion)
            }
            val baseline = pixels(companion)
            assertTrue("Empty companion: $companion", baseline.any { it != 0 })
            for (item in Wardrobe.itemsFor(companion)) {
                compose.runOnIdle { outfit.value = Outfit.default(companion).with(item.slot, item.id) }
                val dressed = pixels(companion)
                if (Outfit.default(companion).isOn(item)) {
                    assertTrue("Default item changed: ${item.id}", baseline.contentEquals(dressed))
                } else {
                    assertTrue("Invisible item: ${item.id}", !baseline.contentEquals(dressed))
                }
            }
        }
    }

    @Test
    fun accessoryThumbnailsKeepTheirCloseupsAndTransformationsShowTheWholeModel() {
        val kind = mutableStateOf(CompanionKind.CAT)
        val outfit = mutableStateOf(Outfit())
        var selectedRig: CompanionRig? = null
        compose.setContent {
            selectedRig = rememberTransformationRig(kind.value, outfit.value, CompanionColor.ORIGINAL)
                ?: rememberCompanionRig(kind.value, CompanionColor.ORIGINAL)
        }
        val accessories = Wardrobe.items.filter { it.slot == Slot.EXTRA }
        assertTrue("No accessories found", accessories.isNotEmpty())
        val unzoomed = mutableListOf<String>()
        val outside = mutableListOf<String>()
        val small = mutableListOf<String>()
        // Preserve the fitted thumbnail padding around artwork that reaches the original grid edges.
        val extendedCrops =
            mapOf(
                "bunny-extra-bubbles" to Rect(110f, 163f, 247f, 293f),
                "dinosaur-extra-balloon" to Rect(24f, -3f, 256f, 216f),
                "unicorn-extra-balloon" to Rect(-3f, 112f, 120f, 232f),
                "unicorn-extra-cloud" to Rect(44f, 142f, 204f, 300f),
            )
        for (item in accessories) {
            compose.runOnIdle {
                kind.value = item.companion
                outfit.value = Outfit.default(item.companion).previewFor(item)
            }
            val rig = compose.runOnIdle { checkNotNull(selectedRig) }
            val crop = rig.crop(item)
            val full = rig.full(outfit.value)
            if (item.isTransformation) {
                assertFullModelCrop(item.id, crop, full)
                continue
            }
            if (crop == full) unzoomed += item.id
            val outsideHorizontally = crop.left < full.left || crop.right > full.right
            val outsideVertically = crop.top < full.top || crop.bottom > full.bottom
            if (item.id in extendedCrops) {
                assertEquals(item.id, extendedCrops.getValue(item.id), crop)
            } else if (outsideHorizontally || outsideVertically) {
                outside += item.id
            }
            if (crop.width < MIN_CROP || crop.height < MIN_CROP) small += item.id
        }
        assertTrue("Accessory crops equal to the whole grid: $unzoomed", unzoomed.isEmpty())
        assertTrue("Accessory crops outside the grid: $outside", outside.isEmpty())
        assertTrue("Accessory crops under $MIN_CROP units: $small", small.isEmpty())
    }

    private fun assertFullModelCrop(
        itemId: String,
        crop: Rect,
        full: Rect,
    ) {
        assertEquals("Square thumbnail for $itemId", crop.width, crop.height, 0.01f)
        assertTrue(
            "Full-body reward was cropped: $itemId",
            crop.left <= full.left + 0.01f && crop.top <= full.top + 0.01f &&
                crop.right >= full.right - 0.01f && crop.bottom >= full.bottom - 0.01f,
        )
    }

    @Test
    fun eachCompanionColorChangesTheRenderedBody() {
        val kind = mutableStateOf(CompanionKind.CAT)
        val color = mutableStateOf(CompanionColor.ORIGINAL)
        compose.setContent {
            ClocklingsTheme {
                CompanionView(
                    kind.value,
                    Outfit.default(kind.value),
                    Modifier.size(180.dp, 240.dp),
                    color = color.value,
                    animated = false,
                )
            }
        }
        for (companion in CompanionKind.entries.filter { it != CompanionKind.NONE && it != CompanionKind.HUMAN }) {
            compose.runOnIdle {
                kind.value = companion
                color.value = CompanionColor.ORIGINAL
            }
            val baseline = pixels(companion)
            for (shade in companion.colorOptions().filter { it != CompanionColor.ORIGINAL }) {
                compose.runOnIdle { color.value = shade }
                assertTrue("Unchanged color: $companion / $shade", !baseline.contentEquals(pixels(companion)))
            }
        }
    }

    @Test
    fun robotShellChangesColorWhileTheCuckooAndItsWoodenClockRetainTheirPigments() {
        val kind = CompanionKind.ROBOT
        val color = mutableStateOf(CompanionColor.ORIGINAL)
        val outfit = Wardrobe.toggle(Outfit(), Wardrobe.item("robot-model-cuckoo"))
        compose.setContent {
            ClocklingsTheme {
                CompanionView(kind, outfit, Modifier.size(240.dp), color = color.value, animated = false)
            }
        }
        val original = pixels(kind)
        compose.runOnIdle { color.value = CompanionColor.PURPLE }
        val purple = pixels(kind)
        var turquoise = 0
        var retinted = 0
        var golden = 0
        var unchanged = 0
        for (i in original.indices) {
            val r = original[i] ushr 16 and 255
            val g = original[i] ushr 8 and 255
            val b = original[i] and 255
            val difference =
                maxOf(
                    abs(r - (purple[i] ushr 16 and 255)),
                    abs(g - (purple[i] ushr 8 and 255)),
                    abs(b - (purple[i] and 255)),
                )
            if (g > r + 40 && b > r + 30 && g > 100) {
                turquoise++
                if (difference > 24) retinted++
            }
            val goldenHue = g > r * 0.48 && g < r * 0.88
            if (r > 140 && goldenHue && b < g * 0.70) {
                golden++
                if (difference <= 4) unchanged++
            }
        }
        assertTrue("Too little identifiable turquoise shell or golden equipment", turquoise > 100 && golden > 100)
        assertTrue("Only $retinted of $turquoise shell pixels changed", retinted > turquoise * 0.80)
        assertTrue("Only $unchanged of $golden prop pixels kept their color", unchanged > golden * 0.96)
    }

    private companion object {
        /** Smallest thumbnail crop, so the companion stays recognisable next to its item. */
        const val MIN_CROP = 120f
    }

    private fun pixels(kind: CompanionKind): IntArray {
        val image = compose.captureRenderedImage("companion_${kind.name}")
        val pixels = image.toPixelMap()
        return IntArray(image.width * image.height) { index ->
            pixels[index % image.width, index / image.width].toArgb()
        }
    }
}
