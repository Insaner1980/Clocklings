package fi.clocklings.ui.game

import fi.clocklings.domain.CompanionKind.CAT
import fi.clocklings.domain.CompanionKind.DOG
import fi.clocklings.domain.CompanionKind.NONE
import fi.clocklings.domain.additionalModelItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleUnlocksTest {
    @Test
    fun everySharedStarMilestoneReturnsBothItsOriginalRewardAndTheNewModel() {
        assertEquals(26, additionalModelItems.size)
        for (model in additionalModelItems) {
            val rewards = visibleUnlocks(model.star - 1, model.star, model.companion)
            assertTrue(model.id, model in rewards)
            val original = rewards.single { it !in additionalModelItems }
            assertEquals(model.id, setOf(original, model), rewards.toSet())
            assertTrue(model.id, visibleUnlocks(model.star, model.star, model.companion).isEmpty())
        }
    }

    @Test
    fun `unlocks follow the companion shown`() {
        assertEquals(listOf("dog-head-cap"), visibleUnlocks(0, 1, DOG).map { it.id })
        assertEquals(listOf("cat-head-bow"), visibleUnlocks(0, 1, CAT).map { it.id })
        assertTrue(visibleUnlocks(0, 15, NONE).isEmpty())
    }
}
