// Small shared builders, one per kind of item artwork.
@file:Suppress("TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Small builders shared by the companions' item artwork. They run once when an item map is built.

/** A vinyl shape from SVG path data. */
internal fun itemShape(
    data: String,
    tones: VinylTones,
) = VinylShape(svgPath(data), tones)

/** A vinyl circle centered at [cx], [cy]. */
internal fun itemCircle(
    cx: Float,
    cy: Float,
    r: Float,
    tones: VinylTones,
) = VinylShape(ovalPath(cx, cy, r), tones)

/** One path made of all [parts], for paired pieces that share one vinyl brush. */
internal fun itemPath(vararg parts: Path) = Path().apply { for (part in parts) addPath(part) }

/** A round-capped stroke [width] wide. */
internal fun itemStroke(width: Float) = Stroke(width, cap = StrokeCap.Round)

/** A point on the quadratic curve from [x0], [y0] through control [cx], [cy] to [x1], [y1] at [t]. */
@Suppress("LongParameterList")
internal fun itemQuad(
    x0: Float,
    y0: Float,
    cx: Float,
    cy: Float,
    x1: Float,
    y1: Float,
    t: Float,
): Offset {
    val u = 1 - t
    return Offset(u * u * x0 + 2 * t * u * cx + t * t * x1, u * u * y0 + 2 * t * u * cy + t * t * y1)
}

/** Four-pointed twinkle centered at [x], [y] with arm length [r]. */
internal fun Path.addItemTwinkle(
    x: Float,
    y: Float,
    r: Float,
) {
    moveTo(x, y - r)
    quadraticTo(x, y, x + r, y)
    quadraticTo(x, y, x, y + r)
    quadraticTo(x, y, x - r, y)
    quadraticTo(x, y, x, y - r)
    close()
}

/** Five round petals of radius [petalR] on a ring of radius [ring] around [cx], [cy], the first at [startDeg]. */
internal fun itemPetals(
    cx: Float,
    cy: Float,
    ring: Float,
    petalR: Float,
    startDeg: Float,
) = Path().apply {
    for (k in 0 until FLOWER_PETALS) {
        val a = (k * PETAL_STEP_DEG + startDeg) * (PI.toFloat() / HALF_TURN_DEG)
        addPath(ovalPath(cx + ring * cos(a), cy + ring * sin(a), petalR))
    }
}

private const val FLOWER_PETALS = 5
private const val PETAL_STEP_DEG = 72f
private const val HALF_TURN_DEG = 180f

/**
 * A polka-dot bow tie centered at [center]: two wings reaching [reach] to each side and [spread] up and
 * down, two [dotColor] dots of radius [dotRadius] on each wing, and a knot with a highlight.
 */
@Suppress("LongParameterList")
internal fun itemPolkaDotBowTie(
    center: Offset,
    reach: Float,
    spread: Float,
    tones: VinylTones,
    dotColor: Color,
    dotRadius: Float,
): ItemArt {
    val (cx, cy) = center

    fun wing(side: Float) =
        itemShape(
            "M${cx + BOW_KNOT_HALF * side} $cy L${cx + reach * side} ${cy - spread} " +
                "Q${cx + (reach + BOW_BULGE) * side} $cy ${cx + reach * side} ${cy + spread} Z",
            tones,
        )
    val left = wing(-1f)
    val right = wing(1f)
    val knot = VinylShape(ovalPath(cx, cy, BOW_KNOT_RX, BOW_KNOT_RY), tones)
    val dots =
        Path().apply {
            for (side in floatArrayOf(-1f, 1f)) {
                addPath(ovalPath(cx + (reach - BOW_TOP_DOT_IN) * side, cy - (spread - BOW_TOP_DOT_UP), dotRadius))
                addPath(ovalPath(cx + (reach - BOW_LOW_DOT_IN) * side, cy + (spread - BOW_LOW_DOT_UP), dotRadius))
            }
        }
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(left)
        vinylShape(right)
        drawPath(dots, dotColor)
        vinylShape(knot)
        specular(knot.bounds)
    }
}

/**
 * A flower lei along the neckline curve [at] (0 to 1): [flowers] evenly spaced flowers of five petals
 * ([petalRing], [petalR]) cycling pink, sun, pearl and knit with gold centers, each turned [stepDeg]
 * more than the last, on leaves hanging [leafDrop] below the curve between them.
 */
@Suppress("LongParameterList")
internal fun itemFlowerLei(
    at: (Float) -> Offset,
    flowers: Int,
    leafDrop: Float,
    petalRing: Float,
    petalR: Float,
    stepDeg: Float,
): ItemArt {
    val gaps = flowers - 1
    val petalTones = listOf(ArtTones.Pink, ArtTones.Sun, ArtTones.Pearl, ArtTones.Knit)
    val leaves =
        VinylShape(
            Path().apply {
                for (i in 0 until gaps) {
                    at((2 * i + 1) / (2f * gaps)).let { addPath(ovalPath(it.x, it.y + leafDrop, LEAF_RX, LEAF_RY)) }
                }
            },
            ArtTones.Leaf,
        )
    val blooms =
        (0..gaps).map { i ->
            val c = at(i / gaps.toFloat())
            val tones = petalTones[i % petalTones.size]
            val petals = VinylShape(itemPetals(c.x, c.y, petalRing, petalR, i * stepDeg), tones)
            petals to itemCircle(c.x, c.y, FLOWER_CENTER_R, ArtTones.Gold)
        }
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(leaves)
        for (i in blooms.indices) {
            vinylShape(blooms[i].first)
            vinylShape(blooms[i].second)
        }
    }
}

/** Stripe geometry for the neck wrap and the hanging end of a scarf. */
internal class ScarfStripes(
    val xs: FloatArray,
    val top: Float,
    val bottom: Float,
    val end: Path,
)

/**
 * A striped knit scarf: a [wrap] around the neck and an [end] hanging down, both decorated with
 * [stripes] in [stripeTones], and four fringe threads [fringeLength] long hanging from [fringeStart]. The shapes are
 * SVG path data in the companion's design grid; stripes are clipped to each shape.
 */
@Suppress("LongParameterList")
internal fun itemStripedScarf(
    wrap: String,
    end: String,
    stripes: ScarfStripes,
    fringeStart: Offset,
    fringeLength: Float,
    tones: VinylTones,
    stripeTones: VinylTones,
): ItemArt {
    val wrapShape = itemShape(wrap, tones)
    val endShape = itemShape(end, tones)
    val wrapStripes =
        Path().apply {
            for (x in stripes.xs) {
                addRect(Rect(x - SCARF_STRIPE_HALF, stripes.top, x + SCARF_STRIPE_HALF, stripes.bottom))
            }
        }
    val fringe =
        Path().apply {
            for (u in ScarfFringeAt) {
                moveTo(fringeStart.x + SCARF_FRINGE_RUN_X * u, fringeStart.y + SCARF_FRINGE_RUN_Y * u)
                lineTo(
                    fringeStart.x - 1f + SCARF_FRINGE_RUN_X * u,
                    fringeStart.y + fringeLength + SCARF_FRINGE_RUN_Y * u,
                )
            }
        }
    val wrapStripeBrush = vinylBrush(wrapShape.bounds, stripeTones)
    val endStripeBrush = vinylBrush(endShape.bounds, stripeTones)
    val fringeStroke = itemStroke(SCARF_FRINGE_WIDTH)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(fringe, tones.base, style = fringeStroke)
        vinylShape(endShape)
        clipPath(endShape.path) { drawPath(stripes.end, endStripeBrush) }
        vinylShape(wrapShape)
        clipPath(wrapShape.path) { drawPath(wrapStripes, wrapStripeBrush) }
        specular(wrapShape.bounds)
    }
}

/**
 * Bands across a scarf end, each [SCARF_BAND_HEIGHT] tall and sloping [SCARF_BAND_HEIGHT] down from
 * [left] to [right], with top-left corners at [ys].
 */
internal fun itemScarfBands(
    left: Float,
    right: Float,
    vararg ys: Float,
) = Path().apply {
    for (y in ys) {
        moveTo(left, y)
        lineTo(right, y + SCARF_BAND_HEIGHT)
        lineTo(right, y + 2 * SCARF_BAND_HEIGHT)
        lineTo(left, y + SCARF_BAND_HEIGHT)
        close()
    }
}

private const val SCARF_STRIPE_HALF = 5f
private const val SCARF_BAND_HEIGHT = 6f
private const val SCARF_FRINGE_RUN_X = 22f
private const val SCARF_FRINGE_RUN_Y = 6f
private const val SCARF_FRINGE_WIDTH = 3.5f
private val ScarfFringeAt = floatArrayOf(0.12f, 0.37f, 0.62f, 0.87f)

private const val BOW_KNOT_HALF = 3f
private const val BOW_BULGE = 7f
private const val BOW_KNOT_RX = 7f
private const val BOW_KNOT_RY = 8f
private const val BOW_TOP_DOT_IN = 5f
private const val BOW_TOP_DOT_UP = 8f
private const val BOW_LOW_DOT_IN = 6f
private const val BOW_LOW_DOT_UP = 7f
private const val LEAF_RX = 6f
private const val LEAF_RY = 4.5f
private const val FLOWER_CENTER_R = 3f
