package fi.clocklings.ui.clock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.MarkColor
import fi.clocklings.ui.theme.SkColors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Kellotaulun mitat web-version SVG:n 400×400-koordinaatistossa (keskipiste 200, 200).
 * Kaikki säteet, viivanleveydet ja fonttikoot ovat tämän koordinaatiston yksiköitä.
 */
internal object ClockSpec {
    const val CENTER = 200f

    /** Iso kello: viewBox -34 -34 468 468 (tilaa minuuttiluvuille). Pieni kello: 12 12 376 376. */
    const val VIEW_ORIGIN = -34f
    const val VIEW_SIZE = 468f
    const val MINI_VIEW_ORIGIN = 12f
    const val MINI_VIEW_SIZE = 376f

    const val RING_RADIUS = 178f
    const val RING_WIDTH = 17f
    const val FACE_RADIUS = 170f
    const val HALF_RADIUS = 168f
    const val MINUTE_LABEL_RADIUS = 207f
    const val INNER_24_RADIUS = 82f
    const val TICK_OUTER = 162f
    const val TICK_BIG_INNER = 145f
    const val TICK_SMALL_INNER = 153f
    const val TICK_BIG_WIDTH = 4f
    const val TICK_SMALL_WIDTH = 2f
    const val NUMBER_RADIUS = 110f

    const val HOUR_TAIL = 16f
    const val HOUR_LENGTH = 86f
    const val HOUR_WIDTH = 16f
    const val HOUR_WIDTH_PULSE = 25f
    const val HOUR_KNOB = 12f
    const val MINUTE_TAIL = 22f

    /** Both the round tip and pointed arrowhead reach the minute ticks beyond the numeral ring. */
    const val MINUTE_LENGTH = 150f
    const val MINUTE_WIDTH = 10f
    const val MINUTE_WIDTH_PULSE = 17f
    const val MINUTE_KNOB = 10f
    const val KNOB_STROKE = 4f
    const val HAND_HALO = 3f

    /** Arrowhead of a pointed hand, as multiples of the hand width. */
    const val POINTER_HEAD_LENGTH = 1.7f
    const val POINTER_HEAD_WIDTH = 2.4f

    /**
     * How far the shaft reaches into the arrowhead: far enough to close the seam, short enough that
     * its round cap stays inside.
     */
    const val POINTER_SHAFT_OVERLAP = 0.7f
    const val POINTER_CORNER = 3f
    const val DIMMED_ALPHA = 0.22f
    const val PULSE_HALF_MS = 550

    const val PIN_RADIUS = 14f
    const val PIN_DOT = 5f

    const val SUN_CY = 232f
    const val SUN_RADIUS = 9f
    const val SUN_RAY_FROM = 13f
    const val SUN_RAY_TO = 18f
    const val SUN_RAY_WIDTH = 3f
    const val DAY_TEXT_Y = 266f

    /** "past" sits at 5 o'clock and "to" at 7 o'clock, below the pin, where hands cover them less often. */
    const val ZONE_LABEL_ANGLE = 148f
    const val ZONE_RADIUS = 70f

    /** A label is hidden when a hand is within [ZONE_HIDE_ANGLE] and fully shown beyond [ZONE_FADE_ANGLE]. */
    const val ZONE_HIDE_ANGLE = 10f
    const val ZONE_FADE_ANGLE = 24f

    // Opetuksen merkinnät
    const val GLOW_NUMBER_RADIUS = 27f
    const val GLOW_TICK_AT = 157f
    const val GLOW_TICK_RADIUS = 7f
    const val BUBBLE_RADIUS = 18f
    const val BUBBLE_STROKE = 3f

    // Fonttikoot
    const val NUMBER_FONT = 39f
    const val MINUTE_FONT = 17f
    const val ZONE_FONT = 19f
    const val INNER_24_FONT = 12f
    const val DAY_FONT = 17f
    const val BUBBLE_FONT = 16f

    /** Dragging: kosketuksen täytyy olla vähintään tämän verran keskeltä liikkeen aikana. */
    const val DRAG_MIN_RADIUS = 8f

    /** Renkaan yhden värin kaari: 30° tunnin kohdalla keskitettynä. */
    const val RING_PART_DEG = 30f

    /**
     * Kuinka paljon renkaasta on maalattu, kun minuuttiviisari on kulkenut ajasta [from] aikaan [raw]:
     * 0° alussa ja koko kierros tunnin jälkeen. Kulku lasketaan jakson [period] ympäri.
     */
    fun ringReveal(
        raw: Float,
        from: Int,
        period: Int,
    ): Float {
        val minutes = ClockTime.mod(raw - from, period.toFloat())
        return (minutes / ClockTime.MINUTES_PER_HOUR).coerceIn(0f, 1f) * ClockTime.FULL_TURN
    }

    /**
     * Renkaan väri [index] leikattuna maalattuun osaan, joka alkaa kahdestatoista ja jatkuu
     * myötäpäivään [revealDeg] astetta. Palauttaa näkyvät palat (alkukulma, pituus) asteina
     * kahdestatoista; kahdentoista kohdan väri voi jakautua kahteen palaan.
     */
    fun ringPieces(
        index: Int,
        revealDeg: Float,
    ): List<Pair<Float, Float>> {
        val start = index * RING_PART_DEG - RING_PART_DEG / 2
        return listOf(start, start + ClockTime.FULL_TURN).mapNotNull { a ->
            val from = max(a, 0f)
            val to = min(a + RING_PART_DEG, revealDeg)
            if (to > from) from to (to - from) else null
        }
    }

    /** Piste kellotaululla: säde ja kulma (0 = ylös, myötäpäivään). */
    fun point(
        radius: Float,
        degrees: Float,
    ): Offset {
        val a = Math.toRadians(degrees.toDouble())
        return Offset(CENTER + radius * sin(a).toFloat(), CENTER - radius * cos(a).toFloat())
    }

    fun markColor(color: MarkColor): Color =
        when (color) {
            MarkColor.YLI -> SkColors.Yli
            MarkColor.VAILLE -> SkColors.Vaille
            MarkColor.HALF -> SkColors.MarkHalf
            MarkColor.RED -> SkColors.MarkRed
            MarkColor.GREEN -> SkColors.MarkGreen
            MarkColor.BLUE -> SkColors.MarkBlue
        }
}
