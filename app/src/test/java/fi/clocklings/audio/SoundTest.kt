package fi.clocklings.audio

import fi.clocklings.domain.CompanionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundTest {
    @Test
    fun `feedback sounds and companion voices are recorded clips`() {
        val recorded = Sound.entries - Sound.POP - Sound.TICK
        for (sound in recorded) assertNotNull("$sound", sound.clip)
        assertNull(Sound.POP.clip)
        assertNull(Sound.TICK.clip)
    }

    @Test
    fun `every companion with a recording has its own voice`() {
        val silent = setOf(CompanionKind.NONE, CompanionKind.BUNNY, CompanionKind.UNICORN, CompanionKind.CAR)
        val companions = CompanionKind.entries - silent
        val voices = companions.map { companionVoice(it) }
        for (voice in voices) assertTrue("$voice", voice?.voice == true)
        assertEquals(companions.size, voices.toSet().size)
        for (kind in silent) assertNull("$kind", companionVoice(kind))
    }
}
