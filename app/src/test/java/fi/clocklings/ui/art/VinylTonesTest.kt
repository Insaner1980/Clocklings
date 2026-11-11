package fi.clocklings.ui.art

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VinylTonesTest {
    private val orange = Color(0xFFEE8C3E)

    private fun saturation(color: Color): Float {
        val max = maxOf(color.red, color.green, color.blue)
        return (max - minOf(color.red, color.green, color.blue)) / max
    }

    @Test
    fun `light is brighter and shade darker than base`() {
        val t = vinylTones(orange)
        assertTrue(t.light.luminance() > t.base.luminance())
        assertTrue(t.shade.luminance() < t.base.luminance())
        assertEquals(orange, t.base)
    }

    @Test
    fun `light mixes 36 percent toward white`() {
        assertEquals(lerp(Color.Black, Color.White, 0.36f), vinylTones(Color.Black).light)
    }

    @Test
    fun `shade of a saturated base keeps its hue order and gains saturation`() {
        val shade = vinylTones(orange).shade
        assertTrue(shade.red > shade.green && shade.green > shade.blue)
        assertTrue(saturation(shade) > saturation(orange))
        assertEquals(orange.red * 0.89f, shade.red, 0.002f)
        assertEquals(orange.blue * 0.74f, shade.blue, 0.002f)
    }

    @Test
    fun `shade of white is a light lilac, not grey`() {
        val shade = vinylTones(Color.White).shade
        assertTrue(shade.luminance() > 0.6f)
        assertTrue(shade.blue > shade.green)
    }
}
