package fi.clocklings.ui.art

import androidx.compose.ui.graphics.Color
import fi.clocklings.ui.theme.PersonalizationColors as C

/**
 * Vinyl tones of the shared art pigments and the shared flat face tones, built once so every rig and
 * item reuses the same values.
 */
object ArtTones {
    val Cream = vinylTones(C.Cream)
    val EyeInk = vinylTones(C.EyeInk)
    val Pink = vinylTones(C.Cheek)
    val Salmon = vinylTones(C.Salmon)
    val Scarf = vinylTones(C.Scarf)
    val Knit = vinylTones(C.Knit)
    val Teal = vinylTones(C.Teal)
    val Gold = vinylTones(C.Gold)
    val Ruby = vinylTones(C.Ruby)
    val Pearl = vinylTones(C.Pearl)
    val Denim = vinylTones(C.Denim)
    val Leaf = vinylTones(C.Leaf)
    val Sky = vinylTones(C.Sky)
    val Sun = vinylTones(C.Sun)
    val Tennis = vinylTones(C.Tennis)
    val Straw = vinylTones(C.Straw)
    val Olive = vinylTones(C.Olive)
    val Metal = vinylTones(C.Metal)

    /** The robot's face screen: dark glass whose lit side only rises to the metal tone. */
    val Screen = vinylTones(C.Screen).copy(light = C.Metal)
    val Steel = vinylTones(C.Steel)

    /** The translucent pink blush on the cheeks. */
    val CheekColor = C.Cheek.copy(alpha = 0.35f)

    /** The dinosaur's stronger blush: the light blush mixes to grey on its green hide. */
    val CoolCheekColor = C.Cheek.copy(alpha = 0.75f)

    /** The small white glint on a glossy nose. */
    val NoseShine = Color.White.copy(alpha = 0.6f)
}
