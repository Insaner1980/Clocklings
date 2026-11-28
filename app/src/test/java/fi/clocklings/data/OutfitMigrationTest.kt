package fi.clocklings.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CompanionKind.CAR
import fi.clocklings.domain.CompanionKind.CAT
import fi.clocklings.domain.CompanionKind.DINOSAUR
import fi.clocklings.domain.CompanionKind.HUMAN
import fi.clocklings.domain.CompanionKind.ROBOT
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Slot.DRESS
import fi.clocklings.domain.Slot.EXTRA
import fi.clocklings.domain.Slot.HEAD
import fi.clocklings.domain.Slot.MODEL
import fi.clocklings.domain.Slot.NECK
import fi.clocklings.domain.Slot.SHOES
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.transformationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitMigrationTest {
    @Test
    fun dollCanSwitchCostumesAndRecoverHerSavedShoesAfterReopening() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(HUMAN, SHOES)] = "shoes-punaiset"
                it[key(HUMAN, DRESS)] = "dress-tahti"
                it[key(HUMAN, HEAD)] = "head-kukat"
            }
            val clothes = ProgressRepository(store).progress.first().outfitFor(HUMAN)
            val skates = Wardrobe.item("shoes-roller-skates")
            val vet = Wardrobe.item("model-veterinarian")
            for (item in listOf(skates, vet, skates)) {
                ProgressRepository(store).updateProgress {
                    it.withOutfit(HUMAN, Wardrobe.toggle(it.outfitFor(HUMAN), item))
                }
                val reopened = ProgressRepository(store).progress.first().outfitFor(HUMAN)
                assertEquals(item.id, reopened.transformationId())
                assertEquals("shoes-punaiset", reopened[SHOES])
                val access = AccessPolicy(fullAccess = true)
                assertEquals(item.id, access.displayedOutfit(HUMAN, reopened, item.star).transformationId())
                assertEquals(null, access.displayedOutfit(HUMAN, reopened, item.star - 1).transformationId())
            }
            ProgressRepository(store).updateProgress {
                it.withOutfit(HUMAN, Wardrobe.toggle(it.outfitFor(HUMAN), skates))
            }
            assertEquals(clothes, ProgressRepository(store).progress.first().outfitFor(HUMAN))
        }

    @Test
    fun oldCatSailorHatIsRecognizedAndSwitchingToYarnPreservesTheOtherSavedGear() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(CAT, HEAD)] = "cat-head-sailor-hat"
                it[key(CAT, NECK)] = "cat-neck-bell-collar"
                it[key(CAT, EXTRA)] = "cat-extra-fish-backpack"
            }
            val repository = ProgressRepository(store)
            assertEquals(
                "cat-head-sailor-hat",
                repository.progress
                    .first()
                    .outfitFor(CAT)
                    .transformationId(),
            )
            val yarn = Wardrobe.item("cat-extra-yarn")
            repository.updateProgress { it.withOutfit(CAT, Wardrobe.toggle(it.outfitFor(CAT), yarn)) }
            val reopened = ProgressRepository(store)
            val dressed = reopened.progress.first().outfitFor(CAT)
            assertEquals(yarn.id, dressed.transformationId())
            assertEquals(yarn.id, AccessPolicy(false).displayedOutfit(CAT, dressed, 2).transformationId())
            reopened.updateProgress { it.withOutfit(CAT, Wardrobe.toggle(it.outfitFor(CAT), yarn)) }
            assertEquals(
                Outfit(mapOf(NECK to "cat-neck-bell-collar", EXTRA to "cat-extra-fish-backpack")),
                ProgressRepository(store).progress.first().outfitFor(CAT),
            )
        }

    @Test
    fun choosingAnAlreadySavedScarfLeavesTheCostumeAndShowsTheScarf() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(CAT, HEAD)] = "cat-head-bow"
                it[key(CAT, NECK)] = "cat-neck-scarf"
            }
            val scarf = Wardrobe.item("cat-neck-scarf")
            val detective = Wardrobe.item("cat-model-detective")
            val repository = ProgressRepository(store)
            val everyday = repository.progress.first().outfitFor(CAT)
            repository.updateProgress { it.withOutfit(CAT, Wardrobe.toggle(it.outfitFor(CAT), detective)) }
            val inCostume = ProgressRepository(store).progress.first().outfitFor(CAT)
            assertTrue(inCostume.isOn(detective))
            assertFalse(inCostume.isOn(scarf))
            repository.updateProgress { it.withOutfit(CAT, Wardrobe.toggle(it.outfitFor(CAT), scarf)) }
            assertEquals(everyday, ProgressRepository(store).progress.first().outfitFor(CAT))
        }

    @Test
    fun switchingAndRemovingCarTransformationsRestoresAnExistingRaceModelAndItsRoof() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(CAR, MODEL)] = "car-model-race"
                it[key(CAR, Slot.ROOF)] = "car-roof-flag"
            }
            val before = ProgressRepository(store).progress.first().outfitFor(CAR)
            val race = Wardrobe.item("car-model-race")
            assertTrue(before.isOn(race))
            for (id in listOf("car-model-fire-engine", "car-model-monster")) {
                val item = Wardrobe.item(id)
                ProgressRepository(store).updateProgress {
                    it.withOutfit(CAR, Wardrobe.toggle(it.outfitFor(CAR), item))
                }
                val dressed = ProgressRepository(store).progress.first().outfitFor(CAR)
                assertTrue(id, dressed.isOn(item))
                assertFalse(id, dressed.isOn(race))
            }
            ProgressRepository(store).updateProgress {
                it.withOutfit(CAR, Wardrobe.toggle(it.outfitFor(CAR), Wardrobe.item("car-model-monster")))
            }
            assertEquals(before, ProgressRepository(store).progress.first().outfitFor(CAR))
        }

    @Test
    fun allReplacedItemsSurviveReadingSavingAndReopening() =
        runTest {
            val replacements =
                listOf(
                    Replacement(HUMAN, EXTRA, EXTRA, "extra-laukku", "extra-strawberry-bag"),
                    Replacement(HUMAN, DRESS, DRESS, "dress-prinsessa", "dress-overalls"),
                    Replacement(HUMAN, SHOES, SHOES, "shoes-kimalle", "shoes-roller-skates"),
                    Replacement(ROBOT, NECK, NECK, "robot-neck-tie", "robot-neck-cape"),
                    Replacement(ROBOT, NECK, EXTRA, "robot-neck-spring", "robot-extra-bubble-pack"),
                    Replacement(ROBOT, NECK, EXTRA, "robot-neck-lights", "robot-extra-wind-up-key"),
                    Replacement(DINOSAUR, NECK, EXTRA, "dinosaur-neck-lei", "dinosaur-extra-swim-ring"),
                    Replacement(DINOSAUR, HEAD, EXTRA, "dinosaur-head-explorer-hat", "dinosaur-extra-ice-cream"),
                )
            for (replacement in replacements) {
                val (kind, from, to) = replacement
                val oldId = replacement.oldId
                val newId = replacement.newId
                val store = InMemoryPreferencesStore()
                store.edit { it[key(kind, from)] = oldId }
                val repository = ProgressRepository(store)
                val migrated = repository.progress.first()
                assertEquals(oldId, newId, migrated.outfitFor(kind)[to])
                if (from != to) assertEquals(oldId, null, migrated.outfitFor(kind)[from])
                repository.updateProgress { it.withName(kind, "Mia") }
                assertEquals(newId, store.data.first()[key(kind, to)])
                assertEquals(migrated.withName(kind, "Mia"), ProgressRepository(store).progress.first())
                repository.updateProgress { it.withOutfit(kind, it.outfitFor(kind).with(EXTRA, null)) }
                assertEquals(null, repository.progress.first().outfitFor(kind)[EXTRA])
                repository.resetProgress()
                assertEquals(Progress(), repository.progress.first())
            }
        }

    @Test
    fun anExistingExtraWinsAndCompetingLegacyItemsUseHeadBeforeNeck() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(ROBOT, NECK)] = "robot-neck-spring"
                it[key(ROBOT, EXTRA)] = "robot-extra-jetpack"
                it[key(DINOSAUR, HEAD)] = "dinosaur-head-explorer-hat"
                it[key(DINOSAUR, NECK)] = "dinosaur-neck-lei"
            }
            val repository = ProgressRepository(store)
            val migrated = repository.progress.first()
            assertEquals(Outfit().with(EXTRA, "robot-extra-jetpack"), migrated.outfitFor(ROBOT))
            assertEquals(Outfit().with(EXTRA, "dinosaur-extra-ice-cream"), migrated.outfitFor(DINOSAUR))
            repository.updateProgress { it }
            assertEquals(migrated, ProgressRepository(store).progress.first())
        }

    @Test
    fun legacyIdsInTheWrongCompanionOrSourceSlotRemainInvalid() =
        runTest {
            val store = InMemoryPreferencesStore()
            store.edit {
                it[key(ROBOT, HEAD)] = "robot-neck-spring"
                it[key(DINOSAUR, NECK)] = "robot-neck-tie"
                it[key(HUMAN, HEAD)] = "dress-prinsessa"
            }
            assertEquals(Progress(), ProgressRepository(store).progress.first())
        }

    private data class Replacement(
        val kind: CompanionKind,
        val from: Slot,
        val to: Slot,
        val oldId: String,
        val newId: String,
    )

    private fun key(
        kind: CompanionKind,
        slot: Slot,
    ) = stringPreferencesKey(
        if (kind == HUMAN) {
            "outfit_${slot.name.lowercase()}"
        } else {
            "outfit_${kind.name.lowercase()}_${slot.name.lowercase()}"
        },
    )
}
