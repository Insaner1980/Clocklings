package fi.clocklings.ui.companion

import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Wardrobe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderedAccessoryLoadingTest {
    @Test
    fun defaultOutfitsDoNotLoadUnusedAccessoryAtlases() {
        for (kind in CompanionKind.entries) {
            assertFalse("Unnecessary accessory atlas for $kind", needsRenderedAccessories(kind, Outfit.default(kind)))
        }
    }

    @Test
    fun everyModeledAccessoryLoadsItsAtlas() {
        for (kind in CompanionKind.entries.filter { it != CompanionKind.NONE }) {
            for (item in Wardrobe.itemsFor(kind)) {
                val needsAtlas = kind != CompanionKind.HUMAN || item.id in DollAccessoryRegions
                val outfit = Outfit.default(kind).with(item.slot, item.id)
                if (needsAtlas) {
                    assertTrue("Missing atlas for ${item.id}", needsRenderedAccessories(kind, outfit))
                    assertTrue(
                        "Incoming outfit needs its atlas before animation: ${item.id}",
                        needsRenderedAccessories(kind, Outfit.default(kind), outfit),
                    )
                } else {
                    assertFalse("Base atlas already contains ${item.id}", needsRenderedAccessories(kind, outfit))
                }
            }
        }
    }

    @Test
    fun removingAnOptionalAccessoryReleasesTheAtlasRequirement() {
        val item = Wardrobe.itemsFor(CompanionKind.ROBOT).first()
        val dressed = Outfit().with(item.slot, item.id)
        assertTrue(needsRenderedAccessories(CompanionKind.ROBOT, dressed))
        assertFalse(needsRenderedAccessories(CompanionKind.ROBOT, dressed.with(item.slot, null)))
    }
}
