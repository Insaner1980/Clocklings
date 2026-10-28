package fi.clocklings

import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.companion.BunnyItemArt
import fi.clocklings.ui.companion.BunnyRig
import fi.clocklings.ui.companion.CarItemArt
import fi.clocklings.ui.companion.CarRig
import fi.clocklings.ui.companion.CatItemArt
import fi.clocklings.ui.companion.CatRig
import fi.clocklings.ui.companion.DinosaurItemArt
import fi.clocklings.ui.companion.DinosaurRig
import fi.clocklings.ui.companion.DogItemArt
import fi.clocklings.ui.companion.DogRig
import fi.clocklings.ui.companion.DragonItemArt
import fi.clocklings.ui.companion.DragonRig
import fi.clocklings.ui.companion.RobotItemArt
import fi.clocklings.ui.companion.RobotRig
import fi.clocklings.ui.companion.UnicornItemArt
import fi.clocklings.ui.companion.UnicornRig
import fi.clocklings.ui.companion.itemArt
import fi.clocklings.ui.companion.rigFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/** Every companion item has artwork, and the rig and the shared item registry actually reach it. */
@RunWith(AndroidJUnit4::class)
class CompanionArtCoverageTest {
    private fun ids(kind: CompanionKind): Set<String> = Wardrobe.itemsFor(kind).map { it.id }.toSet()

    @Test
    fun everyCatItemHasArtwork() = assertEquals(ids(CompanionKind.CAT), CatItemArt.keys)

    @Test
    fun catItemsResolveThroughTheRegistry() {
        for (id in ids(CompanionKind.CAT)) {
            val art = CatItemArt[id]
            assertNotNull(id, art)
            assertSame(id, art, itemArt(id))
        }
    }

    @Test
    fun catUsesItsOwnRig() = assertSame(CatRig, rigFor(CompanionKind.CAT))

    @Test
    fun everyDogItemHasArtwork() {
        assertEquals(ids(CompanionKind.DOG), DogItemArt.keys)
        for (id in ids(CompanionKind.DOG)) assertSame(id, DogItemArt[id], itemArt(id))
        assertSame(DogRig, rigFor(CompanionKind.DOG))
    }

    @Test
    fun everyRobotItemHasArtwork() {
        assertEquals(ids(CompanionKind.ROBOT), RobotItemArt.keys)
        for (id in ids(CompanionKind.ROBOT)) assertSame(id, RobotItemArt[id], itemArt(id))
        assertSame(RobotRig, rigFor(CompanionKind.ROBOT))
    }

    @Test
    fun everyDragonItemHasArtwork() {
        assertEquals(ids(CompanionKind.DRAGON), DragonItemArt.keys)
        for (id in ids(CompanionKind.DRAGON)) assertSame(id, DragonItemArt[id], itemArt(id))
        assertSame(DragonRig, rigFor(CompanionKind.DRAGON))
    }

    @Test
    fun everyDinosaurItemHasArtwork() {
        assertEquals(ids(CompanionKind.DINOSAUR), DinosaurItemArt.keys)
        for (id in ids(CompanionKind.DINOSAUR)) assertSame(id, DinosaurItemArt[id], itemArt(id))
        assertSame(DinosaurRig, rigFor(CompanionKind.DINOSAUR))
    }

    @Test
    fun everyBunnyItemHasArtwork() {
        assertEquals(ids(CompanionKind.BUNNY), BunnyItemArt.keys)
        for (id in ids(CompanionKind.BUNNY)) assertSame(id, BunnyItemArt[id], itemArt(id))
        assertSame(BunnyRig, rigFor(CompanionKind.BUNNY))
    }

    @Test
    fun everyUnicornItemHasArtwork() {
        assertEquals(ids(CompanionKind.UNICORN), UnicornItemArt.keys)
        for (id in ids(CompanionKind.UNICORN)) assertSame(id, UnicornItemArt[id], itemArt(id))
        assertSame(UnicornRig, rigFor(CompanionKind.UNICORN))
    }

    @Test
    fun everyCarItemHasArtwork() {
        assertEquals(ids(CompanionKind.CAR), CarItemArt.keys)
        for (id in ids(CompanionKind.CAR)) assertSame(id, CarItemArt[id], itemArt(id))
        assertSame(CarRig, rigFor(CompanionKind.CAR))
    }
}
