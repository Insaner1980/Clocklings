package fi.clocklings

import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.companion.BunnyAccessoryRegions
import fi.clocklings.ui.companion.CarAccessoryRegions
import fi.clocklings.ui.companion.CatAccessoryRegions
import fi.clocklings.ui.companion.CompanionRig
import fi.clocklings.ui.companion.DinosaurAccessoryRegions
import fi.clocklings.ui.companion.DogAccessoryRegions
import fi.clocklings.ui.companion.DollAccessoryRegions
import fi.clocklings.ui.companion.DragonAccessoryRegions
import fi.clocklings.ui.companion.RenderedAccessoryRegion
import fi.clocklings.ui.companion.RenderedBunnyRig
import fi.clocklings.ui.companion.RenderedCarRig
import fi.clocklings.ui.companion.RenderedCatRig
import fi.clocklings.ui.companion.RenderedDinosaurRig
import fi.clocklings.ui.companion.RenderedDogRig
import fi.clocklings.ui.companion.RenderedDragonRig
import fi.clocklings.ui.companion.RenderedRobotRig
import fi.clocklings.ui.companion.RenderedUnicornRig
import fi.clocklings.ui.companion.RobotAccessoryRegions
import fi.clocklings.ui.companion.UnicornAccessoryRegions
import fi.clocklings.ui.companion.rememberCompanionRig
import fi.clocklings.ui.doll.RenderedDollRig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the production 3D dispatch and every accessory sprite, including the doll. */
@RunWith(AndroidJUnit4::class)
class CompanionArtCoverageTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun everyCatItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.CAT,
            CatAccessoryRegions,
            R.drawable.cat_accessories_rendered,
            RenderedCatRig::class.java,
        )

    @Test
    fun everyDogItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.DOG,
            DogAccessoryRegions,
            R.drawable.dog_accessories_rendered,
            RenderedDogRig::class.java,
        )

    @Test
    fun everyRobotItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.ROBOT,
            RobotAccessoryRegions,
            R.drawable.robot_accessories_rendered,
            RenderedRobotRig::class.java,
        )

    @Test
    fun everyDragonItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.DRAGON,
            DragonAccessoryRegions,
            R.drawable.dragon_accessories_rendered,
            RenderedDragonRig::class.java,
        )

    @Test
    fun everyDinosaurItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.DINOSAUR,
            DinosaurAccessoryRegions,
            R.drawable.dinosaur_accessories_rendered,
            RenderedDinosaurRig::class.java,
        )

    @Test
    fun everyBunnyItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.BUNNY,
            BunnyAccessoryRegions,
            R.drawable.bunny_accessories_rendered,
            RenderedBunnyRig::class.java,
        )

    @Test
    fun everyUnicornItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.UNICORN,
            UnicornAccessoryRegions,
            R.drawable.unicorn_accessories_rendered,
            RenderedUnicornRig::class.java,
        )

    @Test
    fun everyCarItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.CAR,
            CarAccessoryRegions,
            R.drawable.car_accessories_rendered,
            RenderedCarRig::class.java,
        )

    @Test
    fun everyDollItemHasRenderedArtwork() =
        checkArtwork(
            CompanionKind.HUMAN,
            DollAccessoryRegions,
            R.drawable.doll_accessories_rendered,
            RenderedDollRig::class.java,
            DollBodyItems,
        )

    @Test
    fun noCompanionHasNoRenderer() {
        var rig: CompanionRig? = null
        compose.setContent { rig = rememberCompanionRig(CompanionKind.NONE, CompanionColor.ORIGINAL) }
        compose.runOnIdle { assertNull(rig) }
    }

    private fun checkArtwork(
        kind: CompanionKind,
        regions: Map<String, List<RenderedAccessoryRegion>>,
        resource: Int,
        expectedRig: Class<out CompanionRig>,
        bodyItems: Set<String> = emptySet(),
    ) {
        val ids = Wardrobe.itemsFor(kind).map { it.id }.toSet()
        assertEquals("Unmapped wardrobe choices: $kind", ids, regions.keys + bodyItems)
        var selectedRig: CompanionRig? = null
        compose.setContent { selectedRig = rememberCompanionRig(kind, CompanionColor.ORIGINAL) }
        compose.runOnIdle { assertEquals(expectedRig, checkNotNull(selectedRig)::class.java) }
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val bitmap = BitmapFactory.decodeResource(resources, resource)
        try {
            for ((id, pieces) in regions) {
                assertTrue("No sprite for $id", pieces.isNotEmpty())
                val crop = checkNotNull(selectedRig).crop(Wardrobe.item(id))
                for (piece in pieces) {
                    val left = piece.source.x
                    val top = piece.source.y
                    val width = piece.sourceSize.width
                    val height = piece.sourceSize.height
                    assertTrue("Invalid sprite size: $id", width > 0 && height > 0)
                    assertTrue(
                        "Sprite outside atlas: $id",
                        left >= 0 && top >= 0 && left + width <= bitmap.width && top + height <= bitmap.height,
                    )
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, left, top, width, height)
                    assertTrue("Empty sprite: $id", pixels.any { it ushr 24 != 0 })
                    assertTrue(
                        "Thumbnail clips sprite: $id",
                        crop.left <= piece.destination.x && crop.top <= piece.destination.y &&
                            crop.right >= piece.destination.x + piece.size.width &&
                            crop.bottom >= piece.destination.y + piece.size.height,
                    )
                }
            }
        } finally {
            bitmap.recycle()
        }
    }
}

/** These choices are layers in the doll's body atlas; the pixel rendering suite checks each one. */
private val DollBodyItems =
    setOf(
        "hair-letit",
        "hair-pitkat",
        "hair-kiharat",
        "hair-sateenkaari",
        "dress-perus",
        "dress-pilkku",
        "dress-tahti",
        "dress-overalls",
        "dress-sateenkaari",
        "shoes-tossut",
        "shoes-punaiset",
        "shoes-saappaat",
    )
