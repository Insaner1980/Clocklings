// Literal SVG geometry and crop rectangles are source artwork data.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.doll

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.vector.PathParser
import fi.clocklings.domain.Item
import fi.clocklings.domain.Slot

/**
 * The doll's paths and thumbnail crops in its 200 x 340 design grid, after the reference: a large round
 * head under a side-swept fringe, pigtails beside the cheeks, an A-line dress with a scalloped collar
 * and domed shoes.
 */
internal object DollParts {
    const val WIDTH = 200f
    const val HEIGHT = 340f

    private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

    /** The A-line dress with its puff sleeves, traced from the reference. */
    val Dress = p(DollTrace.DRESS)
    val LongHair = p("M46 80C40 30 70 24 100 24C130 24 160 30 154 80L164 200Q100 216 36 200Z")

    /** The hair on the head, traced: a cap over the crown with a fringe swept from a side parting to both temples. */
    val Bangs = p(DollTrace.CAP)

    /** The pigtail bunches beside the cheeks and the arms with their mitten hands, traced from the reference. */
    val PigtailLeft = p(DollTrace.PIGTAIL_LEFT)
    val PigtailRight = p(DollTrace.PIGTAIL_RIGHT)
    val ArmLeft = arm { it }
    val ArmRight = arm { WIDTH - it }

    /**
     * One arm from the sleeve to a mitten hand, in the places measured on the reference: a limb that
     * narrows to the wrist, a round palm and a thumb on the body's side; [m] mirrors the x coordinates.
     */
    private fun arm(m: (Float) -> Float): Path {
        val limb =
            p("M${m(52f)} 162C${m(58f)} 157 ${m(68f)} 162 ${m(66f)} 171L${m(43f)} 207L${m(28f)} 199Z")
        val palm =
            p(
                "M${m(23.5f)} 205C${m(21.5f)} 196 ${m(31f)} 191.5 ${m(39.5f)} 196.5" +
                    "C${m(46.5f)} 201 ${m(46.5f)} 212 ${m(40.5f)} 217.5" +
                    "C${m(34.5f)} 222.5 ${m(25.5f)} 219.5 ${m(23.5f)} 205Z",
            )
        val thumb =
            p(
                "M${m(41f)} 204C${m(46.5f)} 203 ${m(50f)} 208.5 ${m(47f)} 214" +
                    "C${m(45f)} 217.5 ${m(40.5f)} 216 ${m(40.5f)} 212Z",
            )
        return Path().apply {
            op(limb, palm, PathOperation.Union)
            op(this, thumb, PathOperation.Union)
        }
    }

    /** A Peter Pan collar: two rounded lobes meeting under the chin, in the measured bounds. */
    val Collar = p("M77 140.5Q99.5 133 122 140.5Q127 149 119 154Q108 158 99.5 148.5Q91 158 80 154Q72 149 77 140.5Z")
    val Hem = p("M42.5 236.5Q100 257 157.5 236.5")
    val StarBelt = p("M65 178H135")

    val Brows = p("M69.5 76Q76 71 82 73.5M118 73.5Q124 71 130.5 76")
    val Lashes = p("M69 90.5L66 88.5M131 90.5L134 88.5")
    val Mouth = p("M89.5 113.5Q99.5 123 109.5 113.5")

    /** A domed shoe with a flat sole, centered at [cx], in the measured bounds. */
    private fun shoe(cx: Float) =
        p(
            "M${cx - 19.7f} 301C${cx - 19.7f} 290 ${cx - 11f} 283 $cx 283C${cx + 11f} 283 ${cx + 19.7f} 290 " +
                "${cx + 19.7f} 301Q${cx + 19.7f} 311 ${cx + 10f} 311L${cx - 10f} 311" +
                "Q${cx - 19.7f} 311 ${cx - 19.7f} 301Z",
        )

    val ShoeLeft = shoe(76.7f)
    val ShoeRight = shoe(120.9f)
    val Soles = p("M60 305Q76.7 308 93.5 305M104 305Q120.9 308 138 305")
    val ShoeStraps = p("M65 290Q76.7 294 88.5 290M109 290Q120.9 294 133 290")

    val BowLeft = p("M134 38L114 24V52Z")
    val BowRight = p("M134 38L154 24V52Z")
    val Crown = p("M76 40L78 12L90 26L100 6L110 26L122 12L124 40Z")
    val BerryStrap = p("M128 146Q112 184 82 210")
    val Berry = p(StrawberryTrace.BERRY)
    val BerryLeaves = p(StrawberryTrace.LEAVES)
    val BerrySeeds =
        Path().apply {
            for (seed in listOf(
                StrawberryTrace.SEED_1,
                StrawberryTrace.SEED_2,
                StrawberryTrace.SEED_3,
                StrawberryTrace.SEED_4,
                StrawberryTrace.SEED_5,
            )) {
                addPath(p(seed))
            }
        }

    val Overalls = p(OverallsTrace.ALL)
    val ShirtSleeveLeft = p(OverallsTrace.SLEEVE_LEFT)
    val ShirtNeck = p(OverallsTrace.NECK)
    val ShirtSleeveRight = p(OverallsTrace.SLEEVE_RIGHT)
    val OverallsButtons =
        Path().apply {
            addPath(p(OverallsTrace.BUTTON_LEFT))
            addPath(p(OverallsTrace.BUTTON_RIGHT))
        }

    /** The chest pocket and the turned-up cuffs, measured on the reference. */
    val OverallsSeams =
        p(
            "M81.5 192H129.5V208Q129.5 219 118 219H93Q81.5 219 81.5 208Z" +
                "M46 273Q72 277 97 273M103 273Q129 277 154 273",
        )

    val SkateBoot = p(SkateTrace.BOOT)
    val SkateStrap = p(SkateTrace.STRAP)
    val SkateBand = p(SkateTrace.BAND)
    val SkateWheelFront = p(SkateTrace.WHEEL_FRONT)
    val SkateWheelLeft = p(SkateTrace.WHEEL_LEFT)
    val SkateWheelRight = p(SkateTrace.WHEEL_RIGHT)

    /** The wardrobe thumbnail crops by slot. */
    fun crop(slot: Slot): Rect =
        when (slot) {
            Slot.HAIR -> Rect(8f, 8f, 192f, 192f)

            Slot.HEAD -> Rect(22f, 0f, 178f, 156f)

            Slot.DRESS -> Rect(24f, 124f, 176f, 276f)

            // Square, so the thumbnail window does not grow it downwards past the doll's feet into empty space.
            Slot.SHOES -> Rect(56f, 250f, 56f + 88f, 250f + 88f)

            Slot.EXTRA -> Rect(0f, 100f, 200f, 268f)

            // The doll has no neck or car slots; these crops only keep the mapping total.
            Slot.NECK -> Rect(60f, 110f, 60f + 80f, 110f + 60f)

            Slot.ROOF, Slot.FRONT, Slot.WHEELS, Slot.MODEL -> Full
        }

    /**
     * The accessory thumbnails: the item's drawing bounds plus 12 units on every side, squared to the
     * thumbnail window. The bag hangs from the right hand, so its crop takes in the shoulder and arm; the
     * wings span the doll's width.
     */
    fun crop(item: Item): Rect =
        when (item.id) {
            "extra-strawberry-bag" -> Rect(20f, 126f, 140f, 246f)
            "extra-siivet" -> Rect(0f, 84f, 200f, 284f)
            else -> crop(item.slot)
        }

    val Full = Rect(0f, 0f, WIDTH, HEIGHT)
}
