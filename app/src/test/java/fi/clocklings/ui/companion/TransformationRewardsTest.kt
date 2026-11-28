package fi.clocklings.ui.companion

import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind.BUNNY
import fi.clocklings.domain.CompanionKind.CAR
import fi.clocklings.domain.CompanionKind.CAT
import fi.clocklings.domain.CompanionKind.DINOSAUR
import fi.clocklings.domain.CompanionKind.DOG
import fi.clocklings.domain.CompanionKind.DRAGON
import fi.clocklings.domain.CompanionKind.HUMAN
import fi.clocklings.domain.CompanionKind.ROBOT
import fi.clocklings.domain.CompanionKind.UNICORN
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.colorOptions
import fi.clocklings.domain.isTransformation
import fi.clocklings.domain.transformationId
import fi.clocklings.domain.transformationIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformationRewardsTest {
    @Test
    fun eachSpeciesUsesItsOwnAtlasesForEverySelectableBodyColor() {
        val used = mutableSetOf<Int>()
        for ((kind, items) in transformationIds) {
            val source = items.map { transformationArt.getValue(it).atlas }.distinct().single()
            val dyes = transformationDyes[source].orEmpty()
            val colors = if (kind == HUMAN) emptySet() else kind.colorOptions().toSet() - CompanionColor.ORIGINAL
            assertEquals(kind.name, colors, dyes.keys)
            for (atlas in listOf(source) + dyes.values) assertTrue(kind.name, used.add(atlas))
        }
    }

    @Test
    fun dragonPropsDoNotResizeOrRecenterTheCharacter() {
        val frames = transformationIds.getValue(DRAGON).map(transformationArt::getValue)
        val eyes = frames.first().eyes.map { it.destination to it.size }
        assertEquals(2, eyes.size)
        for (frame in frames) {
            assertEquals(eyes, frame.eyes.map { it.destination to it.size })
        }
    }

    @Test
    fun theTransparentBubbleAndHelmetStayOutsideTheRobotsBlinkLayers() {
        for (id in listOf("robot-extra-bubble-pack", "robot-model-rover")) {
            val frame = transformationArt.getValue(id)
            assertEquals("$id must blink only its two eyes", 2, frame.eyes.size)
            for (eye in frame.eyes) {
                assertTrue(id, eye.size.width < frame.body.size.width / 5)
                assertTrue(id, eye.size.height < frame.body.size.height / 5)
            }
        }
    }

    @Test
    fun everyDesignHasItsOwnFrameAndUnlocksForItsCompanionAtTheExpectedStar() {
        val rewards = ExpectedRewards
        assertEquals(rewards.mapValues { it.value.keys }, transformationIds.mapValues { it.value.toSet() })
        assertEquals(rewards.values.flatMap { it.keys }.toSet(), transformationArt.keys)
        assertEquals(36, transformationArt.values.distinctBy { it.atlas to it.body.source }.size)
        val full = AccessPolicy(fullAccess = true)
        val free = AccessPolicy(fullAccess = false)
        for ((kind, choices) in rewards) {
            for ((id, star) in choices) {
                val item = Wardrobe.item(id)
                assertEquals(id, kind, item.companion)
                assertTrue(id, item in Wardrobe.newlyUnlocked(kind, star - 1, star))
                assertFalse(id, full.item(item, star - 1))
                assertTrue(id, full.item(item, star))
                assertFalse(id, free.item(item, star - 1))
                assertEquals(id, id == "cat-extra-yarn", free.item(item, star))
                val ordinary =
                    Wardrobe
                        .itemsFor(kind)
                        .filter { !it.isTransformation && full.item(it, star) }
                        .associate { it.slot to it.id }
                val original = Outfit(Outfit.default(kind).items + ordinary)
                assertEquals(id, original.previewFor(item).transformationId())
                val equipped = Wardrobe.toggle(original, item)
                assertEquals(id, equipped.transformationId())
                assertEquals(id, equipped, full.displayedOutfit(kind, equipped, star))
                assertEquals(id, original, Wardrobe.toggle(equipped, item))
            }
        }
    }
}

private val ExpectedRewards =
    mapOf(
        CAR to
            mapOf(
                "car-model-fire-engine" to 10,
                "car-model-ice-cream" to 5,
                "car-model-monster" to 8,
                "car-model-recycling" to 12,
                "car-model-rocket" to 15,
            ),
        CAT to
            mapOf(
                "cat-head-sailor-hat" to 7,
                "cat-extra-yarn" to 2,
                "cat-model-detective" to 9,
                "cat-model-boots" to 13,
            ),
        DOG to
            mapOf(
                "dog-model-branch" to 4,
                "dog-model-rescue" to 7,
                "dog-model-post" to 11,
                "dog-extra-raincoat" to 14,
            ),
        ROBOT to
            mapOf(
                "robot-model-cuckoo" to 4,
                "robot-model-rover" to 10,
                "robot-extra-bubble-pack" to 7,
                "robot-model-driver" to 14,
            ),
        DRAGON to
            mapOf(
                "dragon-model-marshmallow" to 4,
                "dragon-model-ice" to 8,
                "dragon-model-ruby" to 11,
                "dragon-model-woodland" to 14,
            ),
        DINOSAUR to
            mapOf(
                "dinosaur-model-egg" to 3,
                "dinosaur-model-stegosaurus" to 6,
                "dinosaur-model-fossil" to 10,
                "dinosaur-extra-skateboard" to 15,
            ),
        BUNNY to
            mapOf(
                "bunny-head-top-hat" to 8,
                "bunny-model-carrot-car" to 5,
                "bunny-model-moon" to 11,
                "bunny-model-strawberry" to 14,
            ),
        UNICORN to
            mapOf(
                "unicorn-model-sea" to 5,
                "unicorn-model-carousel" to 8,
                "unicorn-model-shooting-star" to 14,
                "unicorn-head-butterfly" to 10,
            ),
        HUMAN to
            mapOf(
                "model-veterinarian" to 6,
                "shoes-roller-skates" to 13,
                "model-dragon-costume" to 14,
            ),
    )
