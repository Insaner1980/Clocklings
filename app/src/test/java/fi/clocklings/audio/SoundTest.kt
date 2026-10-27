package fi.clocklings.audio

import fi.clocklings.domain.CompanionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SoundTest {
    @Test
    fun `the win sound and companion voices are recorded clips`() {
        assertNotNull(Sound.WIN.clip)
        assertNotNull(Sound.CAT.clip)
        assertNotNull(Sound.DOG.clip)
        assertNull(Sound.OK.clip)
    }

    @Test
    fun `companions without a recorded voice stay silent`() {
        assertEquals(Sound.CAT, companionVoice(CompanionKind.CAT))
        assertEquals(Sound.DOG, companionVoice(CompanionKind.DOG))
        assertNull(companionVoice(CompanionKind.ROBOT))
        assertNull(companionVoice(CompanionKind.NONE))
    }
}
