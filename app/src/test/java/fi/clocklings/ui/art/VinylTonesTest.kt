package fi.clocklings.ui.art

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import fi.clocklings.ui.theme.SkColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VinylTonesTest {
    @Test
    fun `light is brighter and shade darker than base`() {
        val t = vinylTones(Color(0xFFEE8C3E))
        assertTrue(t.light.luminance() > t.base.luminance())
        assertTrue(t.shade.luminance() < t.base.luminance())
        assertEquals(Color(0xFFEE8C3E), t.base)
    }

    @Test
    fun `light mixes 50 percent toward white`() {
        assertEquals(lerp(Color.Black, Color.White, 0.5f), vinylTones(Color.Black).light)
    }

    @Test
    fun `shade mixes 36 percent toward ink`() {
        val base = Color(0xFFEE8C3E)
        assertEquals(lerp(base, SkColors.Ink, 0.36f), vinylTones(base).shade)
    }
}
