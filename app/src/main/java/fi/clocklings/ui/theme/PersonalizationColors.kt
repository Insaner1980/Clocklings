package fi.clocklings.ui.theme

import androidx.compose.ui.graphics.Color
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind

/** Artwork pigments; learning text and hand roles continue to use SkColors. */
object PersonalizationColors {
    val Cream = Color(0xFFFFEBD0)
    val Cat = Color(0xFFE9A05D)
    val Dog = Color(0xFFC58A62)
    val DogEar = Color(0xFF83543F)
    val Robot = Color(0xFF83BFCB)
    val Dragon = Color(0xFFAE94DC)
    val Dinosaur = Color(0xFF86B98C)
    val Bunny = Color(0xFFEBCBA6)
    val Unicorn = Color(0xFFF8F1F3)
    val UnicornPatch = Color(0xFFF6CFDA)
    val UnicornHoof = Color(0xFFBBA0E0)
    val ManePink = Color(0xFFF4A3C4)
    val ManeBlue = Color(0xFFA6CBF2)
    val Car = Color(0xFFEE7F55)
    val CarTire = Color(0xFF7D6488)
    val CarGlass = Color(0xFFAFC0E6)
    val Blue = Color(0xFF86BDED)
    val Green = Color(0xFF94C9A1)
    val Purple = Color(0xFFC5A1DD)
    val Orange = Color(0xFFF4B26A)

    // Per-kind color options where a generic option would look like the kind's own Original color.
    val RobotBlue = Color(0xFF5B95DA)
    val Cheek = Color(0xFFF3A1AB)
    val Scarf = Color(0xFFD4586C)
    val Metal = Color(0xFF385B71)
    val Screen = Color(0xFF203D4F)
    val Glow = Color(0xFFBEF4E3)
    val Space = Color(0xFF444777)
    val SpaceLight = Color(0xFFA6A6DE)
    val Forest = Color(0xFF547B57)
    val ForestLight = Color(0xFFB8D6A0)
    val Ocean = Color(0xFF267E9A)
    val OceanLight = Color(0xFF98DADB)
    val Garden = Color(0xFFAC5681)
    val GardenLight = Color(0xFFF4B9D4)
    val Workshop = Color(0xFFAA7243)
    val WorkshopLight = Color(0xFFE8BB77)
    val Classic = Color(0xFF455260)
    val Coral = Color(0xFFB95760)
    val CustomBlue = Color(0xFF386EAA)
    val CustomPurple = Color(0xFF79599D)

    // Vinyl toy pigments: one base color each, the light and shade tones come from vinylTones.
    val ContactShadow = Color(0xFF5A3A2A)
    val EyeInk = Color(0xFF2B2230)
    val Knit = Color(0xFFE2583E)
    val Teal = Color(0xFF2FA39A)
    val Gold = Color(0xFFF2B83C)
    val Ruby = Color(0xFFC8324B)
    val Pearl = Color(0xFFFBF3E6)
    val Denim = Color(0xFF3F6FB5)
    val Leaf = Color(0xFF5DAE5B)
    val Sky = Color(0xFF7CC4F0)
    val Sun = Color(0xFFFFD84A)
    val Steel = Color(0xFF6F8798)
    val Tennis = Color(0xFFD5E05B)
    val Straw = Color(0xFFF1D58E)
    val Olive = Color(0xFF7F8C4C)

    /** The dog's dark ears and eye patch: [DogEar] on the original fur, a deeper tone of any other fur color. */
    fun dogMarking(color: CompanionColor): Color =
        if (color == CompanionColor.ORIGINAL) DogEar else darken(companion(CompanionKind.DOG, color), DOG_MARKING)

    private fun darken(
        color: Color,
        factor: Float,
    ): Color = Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)

    private const val DOG_MARKING = 0.66f

    /** The dragon's wings and the spines of its ear frills: a deeper tone of its body color, as in the reference. */
    fun dragonWing(color: CompanionColor): Color = darken(companion(CompanionKind.DRAGON, color), DRAGON_WING)

    private const val DRAGON_WING = 0.84f

    /** The dinosaur's back spikes and spots: a deeper tone of its body color, as in the reference. */
    fun dinosaurMarking(color: CompanionColor): Color =
        darken(companion(CompanionKind.DINOSAUR, color), DINOSAUR_MARKING)

    private const val DINOSAUR_MARKING = 0.8f

    fun companion(
        kind: CompanionKind,
        color: CompanionColor,
    ): Color =
        when (color) {
            CompanionColor.BLUE -> {
                if (kind == CompanionKind.ROBOT) RobotBlue else Blue
            }

            CompanionColor.GREEN -> {
                Green
            }

            CompanionColor.PURPLE -> {
                Purple
            }

            CompanionColor.ORANGE -> {
                Orange
            }

            CompanionColor.ORIGINAL -> {
                originalCompanion(kind)
            }
        }

    private fun originalCompanion(kind: CompanionKind): Color =
        when (kind) {
            CompanionKind.CAT -> Cat
            CompanionKind.DOG -> Dog
            CompanionKind.ROBOT -> Robot
            CompanionKind.DRAGON -> Dragon
            CompanionKind.BUNNY -> Bunny
            CompanionKind.UNICORN -> Unicorn
            CompanionKind.CAR -> Car
            else -> Dinosaur
        }

    fun clock(palette: ClockPalette): Color =
        when (palette) {
            ClockPalette.BLUE -> CustomBlue
            ClockPalette.GREEN -> Forest
            ClockPalette.PURPLE -> CustomPurple
            ClockPalette.CORAL -> Coral
        }
}
