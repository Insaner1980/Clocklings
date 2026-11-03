package fi.clocklings.audio

import fi.clocklings.domain.CompanionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundTest {
    @Test
    fun `intro feedback sounds and companion voices are recorded clips`() {
        val recorded = Sound.entries - Sound.POP
        for (sound in recorded) assertNotNull("$sound", sound.clip)
        assertNull(Sound.POP.clip)
    }

    @Test
    fun `every companion has its own voice`() {
        val companions = CompanionKind.entries - CompanionKind.NONE
        val voices = companions.map { companionVoice(it) }
        for (voice in voices) assertTrue("$voice", voice?.voice == true)
        assertEquals(companions.size, voices.toSet().size)
        assertNull(companionVoice(CompanionKind.NONE))
    }
}
