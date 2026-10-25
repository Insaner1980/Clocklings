// Keep the private drawing layers next to the clock canvas that owns their order.
@file:Suppress("TooManyFunctions")

package fi.clocklings.ui.clock

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.DayPart
import fi.clocklings.domain.Hand
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.Mark
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.appearance.clockThemeName
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.Fredoka
import fi.clocklings.ui.theme.Nunito
import fi.clocklings.ui.theme.SkColors
import kotlin.math.hypot
import fi.clocklings.ui.clock.ClockSpec as S

/**
 * Kellotaulu. Piirretään yhdellä Canvasilla web-version SVG:n järjestyksessä.
 * Tila (viisarien kulmat ja animaatio) on tämän sisällä; kutsuja ohjaa sitä [command]-käskyillä.
 *
 * @param period 720 (12 h) tai 1440 (24 h: aurinko tai kuu ja vuorokaudenaika näkyvät)
 * @param step raahauksen pyöristys minuutteina
 * @param help yli- ja vaille-puoliskot, minuuttiluvut ja 24 h -tilassa sisäkehän luvut
 * @param mini pieni koristekello ilman apua ja 24 h -osia
 * @param onChange kutsutaan, kun käyttäjä muuttaa aikaa (raahaus tai saavutettavuustoiminto)
 * @param paintRingFrom aloitusnäkymä: sateenkaarirengas maalautuu minuuttiviisarin perässä tästä ajasta alkaen
 * @param onSettle kutsutaan, kun animoitu siirto on kulkenut loppuun
 */
@Composable
fun ClockFace(
    command: ClockCommand,
    modifier: Modifier = Modifier,
    period: Int = ClockTime.HALF_DAY,
    step: Int = 5,
    interactive: Boolean = false,
    help: Boolean = false,
    mini: Boolean = false,
    describeTime: Boolean = true,
    highlight: Hand? = null,
    under: List<Mark> = emptyList(),
    over: List<Mark> = emptyList(),
    onChange: ((Int) -> Unit)? = null,
    paintRingFrom: Int? = null,
    onSettle: (() -> Unit)? = null,
    appearance: ClockAppearance = LocalPersonalization.current.clock,
) {
    val scope = rememberCoroutineScope()
    val state = remember { ClockState(command.time, period, scope) }
    val reduceMotion = rememberReduceMotion()
    SideEffect {
        state.step = step
        state.reduceMotion = reduceMotion
        state.onChange = onChange
        state.onSettle = onSettle
    }
    LaunchedEffect(period) { state.updatePeriod(period) }
    LaunchedEffect(command) { state.apply(command) }

    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
    val density = LocalDensity.current
    val plainNumbers = appearance.numerals == NumeralStyle.PLAIN
    val styles = remember(density, plainNumbers) { ClockTextStyles(density, plainNumbers) }
    val is24 = period == ClockTime.DAY && !mini

    val pulse = highlightPulse(highlight, reduceMotion)

    val time = state.value
    val dayPart = timeLanguage(LocalAppLanguage.current).dayPart(time)
    val themeLabel = stringResource(clockThemeName(appearance.theme))
    val description = clockDescription(time, period, describeTime)
    val zoneLabels = clockZoneLabels()
    val addLabel = pluralStringResource(R.plurals.clock_add, step, step)
    val subtractLabel = pluralStringResource(R.plurals.clock_subtract, step, step)
    val currentInteractive by rememberUpdatedState(interactive)
    val origin = if (mini) S.MINI_VIEW_ORIGIN else S.VIEW_ORIGIN
    val viewSize = if (mini) S.MINI_VIEW_SIZE else S.VIEW_SIZE

    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics {
                contentDescription = description
                stateDescription = themeLabel
                if (interactive) {
                    customActions = clockActions(state, addLabel, subtractLabel)
                }
            }.clockDrag(state, origin, viewSize) { currentInteractive },
    ) {
        val scale = size.width / viewSize
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
            translate(-origin, -origin)
        }) {
            val reveal = paintRingFrom?.let { S.ringReveal(state.raw, it, state.period) } ?: ClockTime.FULL_TURN
            if (appearance.theme == ClockTheme.RAINBOW) drawRing(reveal) else drawThemedRim(appearance, reveal)
            drawCircle(SkColors.Face, S.FACE_RADIUS, Offset(S.CENTER, S.CENTER))
            if (help && !mini) drawHelp(textMeasurer, styles, is24, zoneLabels)
            under.forEach { drawMark(it, textMeasurer, styles) }
            drawTicks()
            drawNumbers(textMeasurer, styles)
            if (is24) drawDayPart(dayPart, textMeasurer, styles)
            drawHands(state.raw, highlight, pulse, interactive && !mini, appearance.hands)
            drawCircle(SkColors.Ink, S.PIN_RADIUS, Offset(S.CENTER, S.CENTER))
            drawCircle(SkColors.Face, S.PIN_DOT, Offset(S.CENTER, S.CENTER))
            over.forEach { drawMark(it, textMeasurer, styles) }
        }
    }
}

/** Finnish and Swedish can refer to the half hour on either side of the face. */
@Composable
private fun clockZoneLabels(): Pair<String, String>? =
    if (LocalAppLanguage.current == AppLanguage.ENGLISH) {
        stringResource(R.string.clock_zone_yli) to stringResource(R.string.clock_zone_vaille)
    } else {
        null
    }

@Composable
private fun clockDescription(
    time: Int,
    period: Int,
    describeTime: Boolean,
): String =
    if (describeTime) {
        stringResource(
            R.string.clock_description,
            timeLanguage(LocalAppLanguage.current).fullWords(
                time,
                h24 =
                    period == ClockTime.DAY,
            ),
        )
    } else {
        stringResource(R.string.clock_question_description)
    }

/** Fonttikoot kellon koordinaatistossa: px-koko = SVG-yksikkö, koska Canvas skaalataan. */
internal class ClockTextStyles(
    private val density: Density,
    plainNumbers: Boolean = false,
) {
    private fun style(
        family: FontFamily,
        weight: FontWeight,
        size: Float,
        color: Color,
    ) = TextStyle(fontFamily = family, fontWeight = weight, fontSize = with(density) { size.toSp() }, color = color)

    val number = style(if (plainNumbers) Nunito else Fredoka, FontWeight.SemiBold, S.NUMBER_FONT, SkColors.Ink)
    val minute = style(Nunito, FontWeight.ExtraBold, S.MINUTE_FONT, SkColors.InkSoft)
    val zone = style(Fredoka, FontWeight.SemiBold, S.ZONE_FONT, SkColors.InkSoft)
    val inner24 = style(Nunito, FontWeight.ExtraBold, S.INNER_24_FONT, SkColors.Hour24)
    val day = style(Fredoka, FontWeight.SemiBold, S.DAY_FONT, SkColors.Ink)
    val bubble = style(Nunito, FontWeight.ExtraBold, S.BUBBLE_FONT, SkColors.Minute)
}

private val MoonPath: Path = PathParser().parsePathString("M203 220A12 12 0 0 0 203 244A7 12 0 0 1 203 220Z").toPath()

/** Teksti keskitettynä pisteeseen (SVG: text-anchor middle, dominant-baseline central). */
private fun DrawScope.drawCentered(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    at: Offset,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

/** Teksti peruslinjalle [baselineY] (SVG:n oletus). */
private fun DrawScope.drawOnBaseline(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    baselineY: Float,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(x - layout.size.width / 2f, baselineY - layout.firstBaseline))
}

/** Sateenkaarirengas; [revealDeg] alle täyden kierroksen piirtää maalaamattoman osan vaaleana. */
private fun DrawScope.drawRing(revealDeg: Float) {
    val r = S.RING_RADIUS
    val arc = { color: Color, from: Float, sweep: Float ->
        drawArc(
            color = color,
            startAngle = from - ClockTime.FULL_TURN / 4,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(S.CENTER - r, S.CENTER - r),
            size = Size(2 * r, 2 * r),
            style = Stroke(S.RING_WIDTH),
        )
    }
    if (revealDeg >= ClockTime.FULL_TURN) {
        for (i in 0 until ClockTime.HOURS_PER_HALF_DAY) {
            arc(SkColors.Rainbow[i], i * S.RING_PART_DEG - S.RING_PART_DEG / 2, S.RING_PART_DEG)
        }
        return
    }
    arc(SkColors.Line, 0f, ClockTime.FULL_TURN)
    for (i in 0 until ClockTime.HOURS_PER_HALF_DAY) {
        for ((from, sweep) in S.ringPieces(i, revealDeg)) arc(SkColors.Rainbow[i], from, sweep)
    }
}

private fun DrawScope.drawHelp(
    measurer: TextMeasurer,
    styles: ClockTextStyles,
    is24: Boolean,
    zoneLabels: Pair<String, String>?,
) {
    val r = S.HALF_RADIUS
    val topLeft = Offset(S.CENTER - r, S.CENTER - r)
    val size = Size(2 * r, 2 * r)
    drawArc(SkColors.Yli, -QUARTER_TURN, ClockTime.HALF_TURN, useCenter = true, topLeft = topLeft, size = size)
    drawArc(SkColors.Vaille, QUARTER_TURN, ClockTime.HALF_TURN, useCenter = true, topLeft = topLeft, size = size)
    zoneLabels?.let {
        drawOnBaseline(measurer, it.first, styles.zone, S.ZONE_YLI_X, S.ZONE_Y)
        drawOnBaseline(measurer, it.second, styles.zone, S.ZONE_VAILLE_X, S.ZONE_Y)
    }
    for (i in 0 until ClockTime.HOURS_PER_HALF_DAY) {
        drawCentered(
            measurer,
            (i * ClockTime.MINUTES_PER_NUMBER).toString(),
            styles.minute,
            S.point(
                S.MINUTE_LABEL_RADIUS,
                i * DEGREES_PER_HOUR,
            ),
        )
    }
    if (is24) {
        for (n in 1..ClockTime.HOURS_PER_HALF_DAY) {
            drawCentered(
                measurer,
                ((n + ClockTime.HOURS_PER_HALF_DAY) % ClockTime.HOURS_PER_DAY).toString(),
                styles.inner24,
                S.point(
                    S.INNER_24_RADIUS,
                    n * DEGREES_PER_HOUR,
                ),
            )
        }
    }
}

private fun DrawScope.drawTicks() {
    for (i in 0 until ClockTime.MINUTES_PER_HOUR) {
        val big = i % ClockTime.MINUTES_PER_NUMBER == 0
        drawLine(
            color = if (big) SkColors.Ink else SkColors.Tick,
            start = S.point(if (big) S.TICK_BIG_INNER else S.TICK_SMALL_INNER, i * ClockTime.DEGREES_PER_MINUTE),
            end = S.point(S.TICK_OUTER, i * ClockTime.DEGREES_PER_MINUTE),
            strokeWidth = if (big) S.TICK_BIG_WIDTH else S.TICK_SMALL_WIDTH,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawNumbers(
    measurer: TextMeasurer,
    styles: ClockTextStyles,
) {
    for (n in 1..ClockTime.HOURS_PER_HALF_DAY) {
        drawCentered(
            measurer,
            n.toString(),
            styles.number,
            S.point(
                S.NUMBER_RADIUS,
                n * DEGREES_PER_HOUR,
            ),
        )
    }
}

private fun DrawScope.drawDayPart(
    part: DayPart,
    measurer: TextMeasurer,
    styles: ClockTextStyles,
) {
    if (part.isSun) {
        val c = Offset(S.CENTER, S.SUN_CY)
        drawCircle(SkColors.Sun, S.SUN_RADIUS, c)
        for (i in 0 until SUN_RAY_COUNT) {
            val from = S.point(S.SUN_RAY_FROM, i * SUN_RAY_ANGLE) - Offset(0f, S.CENTER - S.SUN_CY)
            val to = S.point(S.SUN_RAY_TO, i * SUN_RAY_ANGLE) - Offset(0f, S.CENTER - S.SUN_CY)
            drawLine(SkColors.SunRay, from, to, S.SUN_RAY_WIDTH, StrokeCap.Round)
        }
    } else {
        drawPath(MoonPath, SkColors.Moon)
    }
    drawOnBaseline(measurer, part.name, styles.day, S.CENTER, S.DAY_TEXT_Y)
}

private fun DrawScope.drawHands(
    raw: Float,
    highlight: Hand?,
    pulse: Float,
    knobs: Boolean,
    handStyle: HandStyle,
) {
    val hourAlpha = if (highlight == Hand.MINUTE) S.DIMMED_ALPHA else 1f
    val minuteAlpha = if (highlight == Hand.HOUR) S.DIMMED_ALPHA else 1f
    val hourWidth = if (highlight == Hand.HOUR) lerp(S.HOUR_WIDTH, S.HOUR_WIDTH_PULSE, pulse) else S.HOUR_WIDTH
    val minuteWidth =
        if (highlight ==
            Hand.MINUTE
        ) {
            lerp(S.MINUTE_WIDTH, S.MINUTE_WIDTH_PULSE, pulse)
        } else {
            S.MINUTE_WIDTH
        }
    drawHand(
        S.point(S.HOUR_TAIL, ClockTime.hourAngle(raw) + ClockTime.HALF_TURN),
        S.point(S.HOUR_LENGTH, ClockTime.hourAngle(raw)),
        hourWidth,
        if (knobs) S.HOUR_KNOB else null,
        SkColors.Hour,
        hourAlpha,
        handStyle,
    )
    drawHand(
        S.point(S.MINUTE_TAIL, ClockTime.minuteAngle(raw) + ClockTime.HALF_TURN),
        S.point(S.MINUTE_LENGTH, ClockTime.minuteAngle(raw)),
        minuteWidth,
        if (knobs) S.MINUTE_KNOB else null,
        SkColors.Minute,
        minuteAlpha,
        handStyle,
    )
}

private fun lerp(
    a: Float,
    b: Float,
    t: Float,
) = a + (b - a) * t

private fun DrawScope.drawHand(
    end: Offset,
    tip: Offset,
    width: Float,
    knob: Float?,
    color: Color,
    alpha: Float = 1f,
    handStyle: HandStyle = HandStyle.ROUND,
) {
    // Pehmeä varjo kahdella kerroksella (web: drop-shadow 0 3px 2px)
    val shadowOffset = Offset(0f, S.SHADOW_DY)
    val shadow = SkColors.Shadow.copy(alpha = S.SHADOW_ALPHA * alpha)
    drawLine(
        shadow.copy(alpha = shadow.alpha / 2),
        end + shadowOffset,
        tip + shadowOffset,
        width + HAND_SHADOW_EXTRA_WIDTH,
        StrokeCap.Round,
    )
    drawLine(shadow.copy(alpha = shadow.alpha / 2), end + shadowOffset, tip + shadowOffset, width, StrokeCap.Round)
    drawLine(color.copy(alpha = alpha), end, tip, width, StrokeCap.Round)
    if (handStyle == HandStyle.POINTER) drawPointer(end, tip, width, color.copy(alpha = alpha))
    if (knob != null) {
        drawCircle(color.copy(alpha = alpha), knob, tip)
        drawCircle(SkColors.Face.copy(alpha = alpha), knob, tip, style = Stroke(S.KNOB_STROKE))
    }
}

private fun DrawScope.drawPointer(
    end: Offset,
    tip: Offset,
    width: Float,
    color: Color,
) {
    val delta = tip - end
    val length = hypot(delta.x, delta.y)
    val direction = delta / length
    val normal = Offset(-direction.y, direction.x) * width
    val base = tip - direction * width * POINTER_LENGTH
    drawPath(
        Path().apply {
            moveTo(tip.x, tip.y)
            lineTo((base + normal).x, (base + normal).y)
            lineTo((base - normal).x, (base - normal).y)
            close()
        },
        color,
    )
}

private const val POINTER_LENGTH = 2.5f

private fun DrawScope.drawMark(
    mark: Mark,
    measurer: TextMeasurer,
    styles: ClockTextStyles,
) {
    when (mark) {
        is Mark.Sector -> {
            val color = S.markColor(mark.color)
            val r = S.HALF_RADIUS
            if (mark.toMin - mark.fromMin >= ClockTime.MINUTES_PER_HOUR) {
                drawCircle(color, r, Offset(S.CENTER, S.CENTER))
            } else {
                drawArc(
                    color = color,
                    startAngle = mark.fromMin * 6f - 90f,
                    sweepAngle = (mark.toMin - mark.fromMin) * 6f,
                    useCenter = true,
                    topLeft = Offset(S.CENTER - r, S.CENTER - r),
                    size = Size(2 * r, 2 * r),
                )
            }
        }

        is Mark.NumberGlow -> {
            drawCircle(SkColors.MarkNumber, S.GLOW_NUMBER_RADIUS, S.point(S.NUMBER_RADIUS, mark.n * DEGREES_PER_HOUR))
        }

        is Mark.TickGlow -> {
            drawCircle(
                SkColors.MarkTick,
                S.GLOW_TICK_RADIUS,
                S.point(
                    S.GLOW_TICK_AT,
                    mark.minute * ClockTime.DEGREES_PER_MINUTE,
                ),
            )
        }

        is Mark.Bubble -> {
            val at = S.point(S.MINUTE_LABEL_RADIUS, mark.minute * ClockTime.DEGREES_PER_MINUTE)
            drawCircle(SkColors.Face, S.BUBBLE_RADIUS, at)
            drawCircle(SkColors.Minute, S.BUBBLE_RADIUS, at, style = Stroke(S.BUBBLE_STROKE))
            drawCentered(measurer, mark.text, styles.bubble, at)
        }
    }
}

private const val QUARTER_TURN = 90f
private const val DEGREES_PER_HOUR = 30f
private const val SUN_RAY_COUNT = 8
private const val SUN_RAY_ANGLE = 45f
private const val HAND_SHADOW_EXTRA_WIDTH = 3f

private fun Modifier.clockDrag(
    state: ClockState,
    origin: Float,
    viewSize: Float,
    interactive: () -> Boolean,
): Modifier =
    this.pointerInput(state, origin, viewSize) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!interactive()) return@awaitEachGesture
            val scale = size.width / viewSize

            fun local(p: Offset) = Offset(p.x / scale + origin - S.CENTER, p.y / scale + origin - S.CENTER)
            val p = local(down.position)
            val angle = ClockTime.angleOf(p.x, p.y)
            val hand = ClockTime.pickHand(hypot(p.x, p.y), angle, state.raw) ?: return@awaitEachGesture
            down.consume()
            state.startDrag(hand, angle)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) break
                val q = local(change.position)
                if (hypot(q.x, q.y) >= S.DRAG_MIN_RADIUS) state.dragTo(ClockTime.angleOf(q.x, q.y))
                change.consume()
            }
            state.endDrag()
        }
    }

@Composable
private fun highlightPulse(
    highlight: Hand?,
    reduceMotion: Boolean,
): Float =
    if (highlight != null && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "korostus")
        transition
            .animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        tween(S.PULSE_HALF_MS, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                    ),
                label = "korostus",
            ).value
    } else {
        0f
    }

private fun clockActions(
    state: ClockState,
    addLabel: String,
    subtractLabel: String,
) = listOf(
    CustomAccessibilityAction(addLabel) {
        state.nudge(1)
        true
    },
    CustomAccessibilityAction(subtractLabel) {
        state.nudge(-1)
        true
    },
)
