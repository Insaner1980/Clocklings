// Shading constants are the vinyl toy recipe; shapes are drawn in the 240 x 300 artwork grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.art

import android.graphics.Matrix
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import fi.clocklings.ui.theme.PersonalizationColors
import fi.clocklings.ui.theme.SkColors
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/** The three tones of one vinyl pigment: the highlight side, the base and the shaded side. */
@Immutable
data class VinylTones(
    val light: Color,
    val base: Color,
    val shade: Color,
)

private const val LIGHT_MIX = 0.36f
private const val SHADE_STRONG = 0.89f
private const val SHADE_WEAK = 0.74f
private const val PALE_START = 0.8f
private const val PALE_SPAN = 0.15f
private const val PALE_SHADE_FACTOR = 0.88f
private const val PALE_TINT_MIX = 0.5f
private val PaleShadeTint = Color(0xFFD6C6E4)
private const val LIGHT_CENTER_X = 0.36f
private const val LIGHT_CENTER_Y = 0.30f
private const val GRADIENT_RADIUS = 0.78f
private const val BASE_STOP = 0.5f
private const val SHADE_STOP = 0.9f
private const val CORE_STOP = 0.97f
private const val CORE_FACTOR = 0.94f
private const val BOUNCE_MIX = 0.5f
private const val HOTSPOT_ALPHA = 0.4f
private const val HOTSPOT_SCALE = 0.42f
private const val SPECULAR_ALPHA = 0.42f
private const val SPECULAR_WIDTH = 0.34f
private const val SPECULAR_HEIGHT = 0.2f
private const val CONTACT_SHADOW_ALPHA = 0.34f
private const val GROUND_SHADOW_ALPHA = 0.22f
private const val CLOSED_EYE_BLINK = 0.7f
private const val CLOSED_EYE_WIDTH = 3f
private const val CLOSED_EYE_START_DEG = 200f
private const val CLOSED_EYE_SWEEP_DEG = 140f
private const val CLOSED_EYE_RISE = 0.25f
private const val CLOSED_EYE_HEIGHT = 0.55f
private val ClosedEyeStroke = Stroke(CLOSED_EYE_WIDTH, cap = StrokeCap.Round)
private const val UNIT_CENTER = 0.5f
private const val EYE_BIG_HIGHLIGHT_RADIUS = 0.38f
private const val EYE_BIG_HIGHLIGHT_X = -0.3f
private const val EYE_BIG_HIGHLIGHT_Y = -0.4f
private const val EYE_SMALL_HIGHLIGHT_RADIUS = 0.17f
private const val EYE_SMALL_HIGHLIGHT_X = 0.3f
private const val EYE_SMALL_HIGHLIGHT_Y = 0.45f
private const val IRIS_WIDTH = 0.8f
private const val IRIS_TOP = -0.3f
private const val IRIS_HEIGHT = 1.22f
private const val IRIS_ALPHA = 0.85f

fun vinylTones(base: Color): VinylTones =
    VinylTones(
        light = lerp(base, Color.White, LIGHT_MIX),
        base = base,
        shade = shadeOf(base),
    )

/**
 * The shaded tone of [base]. A saturated pigment deepens and gains saturation, as the shadows of the
 * reference renders do: its strongest channel keeps [SHADE_STRONG] of itself, its weakest [SHADE_WEAK]
 * and the third something between, so orange shades to a deep orange and not to brown; mixing toward
 * ink would grey it. A near-white pigment would only turn grey that way, so the whiter the base is (its
 * weakest channel past [PALE_START]), the more its shade becomes a light, soft lilac instead.
 */
private fun shadeOf(base: Color): Color {
    val whiteness = minOf(base.red, base.green, base.blue)
    val pale = ((whiteness - PALE_START) / PALE_SPAN).coerceIn(0f, 1f)
    val span = (maxOf(base.red, base.green, base.blue) - whiteness).coerceAtLeast(MIN_EXTENT)

    fun kept(channel: Float) = channel * (SHADE_WEAK + (SHADE_STRONG - SHADE_WEAK) * (channel - whiteness) / span)
    val saturated = Color(kept(base.red), kept(base.green), kept(base.blue), base.alpha)
    val paleShade = lerp(deepen(base, PALE_SHADE_FACTOR), PaleShadeTint, PALE_TINT_MIX)
    return lerp(saturated, paleShade, pale)
}

/** [color] with every channel scaled by [factor]. */
private fun deepen(
    color: Color,
    factor: Float,
): Color = Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)

/**
 * Radial gradient lit from the upper left of [bounds] and stretched to its ellipse, so wide and tall
 * parts shade as round forms: the lit side, the base color, the shaded side, a darker core shadow and,
 * at the far rim, a little bounced light. Build it once per color and part, never per frame.
 */
fun vinylBrush(
    bounds: Rect,
    tones: VinylTones,
): Brush = VinylBrush(bounds, tones)

internal fun vinylBrush(
    path: Path,
    tones: VinylTones,
): Brush = vinylBrush(path.getBounds(), tones)

/** The gradient of [vinylBrush], laid out in the unit box and mapped onto [bounds] by the shader matrix. */
private class VinylBrush(
    private val bounds: Rect,
    tones: VinylTones,
) : ShaderBrush() {
    private val colors =
        listOf(
            tones.light,
            tones.base,
            tones.shade,
            deepen(tones.shade, CORE_FACTOR),
            lerp(tones.shade, tones.base, BOUNCE_MIX),
        )

    override fun createShader(size: Size): Shader =
        RadialGradientShader(Offset(LIGHT_CENTER_X, LIGHT_CENTER_Y), GRADIENT_RADIUS, colors, VinylStops).apply {
            setLocalMatrix(
                Matrix().apply {
                    setScale(max(bounds.width, MIN_EXTENT), max(bounds.height, MIN_EXTENT))
                    postTranslate(bounds.left, bounds.top)
                },
            )
        }
}

private val VinylStops = listOf(0f, BASE_STOP, SHADE_STOP, CORE_STOP, 1f)
private const val MIN_EXTENT = 0.001f

fun DrawScope.vinylOval(
    bounds: Rect,
    brush: Brush,
) = drawOval(brush, bounds.topLeft, bounds.size)

/**
 * The bounds for which [specular] draws its highlight as the ellipse centered at ([cx], [cy]) with
 * radii [rx] x [ry].
 */
fun specularRect(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
): Rect {
    val width = 2 * rx / SPECULAR_WIDTH
    val height = 2 * ry / SPECULAR_HEIGHT
    val left = cx - width * LIGHT_CENTER_X
    val top = cy - height * LIGHT_CENTER_Y
    return Rect(left, top, left + width, top + height)
}

/**
 * White highlight in the upper left of [bounds], optionally tilted around its center: a broad soft
 * sheen with a small bright hotspot in its middle, as on glossy vinyl.
 */
fun DrawScope.specular(
    bounds: Rect,
    rotationDeg: Float = 0f,
) {
    val halfWidth = bounds.width * SPECULAR_WIDTH / 2
    val halfHeight = bounds.height * SPECULAR_HEIGHT / 2
    val cx = bounds.left + bounds.width * LIGHT_CENTER_X
    val cy = bounds.top + bounds.height * LIGHT_CENTER_Y
    unitFade(SpecularBrush, cx - halfWidth, cy - halfHeight, halfWidth * 2, halfHeight * 2, rotationDeg)
    val hotWidth = halfWidth * HOTSPOT_SCALE
    val hotHeight = halfHeight * HOTSPOT_SCALE
    unitFade(HotspotBrush, cx - hotWidth, cy - hotHeight, hotWidth * 2, hotHeight * 2, rotationDeg)
}

/** Warm dark pool under a part: fades from the contact shadow pigment to transparent. */
fun DrawScope.contactShadow(bounds: Rect) =
    unitFade(ContactShadowBrush, bounds.left, bounds.top, bounds.width, bounds.height)

/** Wide ground shadow: fades from ink to transparent. */
fun DrawScope.groundShadow(bounds: Rect) =
    unitFade(GroundShadowBrush, bounds.left, bounds.top, bounds.width, bounds.height)

/** Strokes of the soft rim, widest first: stacked inside an outline they fade from the edge inward. */
private val SoftRimStrokes = listOf(22f, 19f, 16f, 13f, 10f, 7f, 4f).map { Stroke(it, cap = StrokeCap.Round) }

/**
 * A soft shade just inside the outline of [path], so a light shape stays readable on a light background:
 * low-alpha strokes clipped to the path stack up toward its edge. [color] carries the alpha of one step.
 */
fun DrawScope.softRim(
    path: Path,
    color: Color,
) = clipPath(path) { for (i in SoftRimStrokes.indices) drawPath(path, color, style = SoftRimStrokes[i]) }

/** Radial fade from [color] at the center to transparent at the edge of the 1 x 1 unit box. */
private fun unitFadeBrush(color: Color): Brush =
    Brush.radialGradient(
        0f to color,
        1f to color.copy(alpha = 0f),
        center = Offset(UNIT_CENTER, UNIT_CENTER),
        radius = UNIT_CENTER,
    )

private val SpecularBrush = unitFadeBrush(Color.White.copy(alpha = SPECULAR_ALPHA))
private val HotspotBrush = unitFadeBrush(Color.White.copy(alpha = HOTSPOT_ALPHA))
private val ContactShadowBrush =
    unitFadeBrush(PersonalizationColors.ContactShadow.copy(alpha = CONTACT_SHADOW_ALPHA))
private val GroundShadowBrush = unitFadeBrush(SkColors.Ink.copy(alpha = GROUND_SHADOW_ALPHA))

/** The iris glow of a [glossyEye] in [color]. Build it once, never per frame. */
fun irisBrush(color: Color): Brush = unitFadeBrush(color.copy(alpha = IRIS_ALPHA))

/** The default brown iris and the violet iris of the unicorn and the dragon. */
val WarmIris: Brush = irisBrush(PersonalizationColors.EyeIris)
val VioletIris: Brush = irisBrush(PersonalizationColors.EyeIrisViolet)

/**
 * Draws a unit-box [brush] stretched to the given rectangle, so the shared brush fades to zero on the
 * whole edge.
 */
private fun DrawScope.unitFade(
    brush: Brush,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    rotationDeg: Float = 0f,
) {
    if (width <= 0f || height <= 0f) return
    withTransform({
        translate(left, top)
        rotate(rotationDeg, Offset(width / 2, height / 2))
        scale(width, height, Offset.Zero)
    }) {
        drawOval(brush, Offset.Zero, UnitSize)
    }
}

/** The 1 x 1 unit box that [unitBrush] brushes are built for. */
private val UnitBox = Rect(0f, 0f, 1f, 1f)
private val UnitSize = Size(1f, 1f)

/**
 * A vinyl brush for the 1 x 1 unit box. [unitOval] stretches it to any ellipse, so one brush serves
 * every oval of a pigment. Build it once, never per frame.
 */
fun unitBrush(tones: VinylTones): Brush = vinylBrush(UnitBox, tones)

/** The ellipse centered at ([cx], [cy]) with radii [rx] x [ry], filled with a [unitBrush] stretched to it. */
fun DrawScope.unitOval(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    brush: Brush,
) {
    withTransform({
        translate(cx - rx, cy - ry)
        scale(2 * rx, 2 * ry, Offset.Zero)
    }) {
        drawOval(brush, Offset.Zero, UnitSize)
    }
}

/** The ellipse inscribed in [bounds], filled with a [unitBrush] stretched to it. */
fun DrawScope.unitOval(
    bounds: Rect,
    brush: Brush,
) = unitOval(bounds.center.x, bounds.center.y, bounds.width / 2, bounds.height / 2, brush)

/** The circle of radius [r] centered at ([cx], [cy]), filled with a [unitBrush] stretched to it. */
fun DrawScope.unitCircle(
    cx: Float,
    cy: Float,
    r: Float,
    brush: Brush,
) = unitOval(cx, cy, r, r, brush)

/** Unit-box vinyl brushes by tones, so a glossy eye reuses its brush on every frame. */
private val EyeBrushes = ConcurrentHashMap<VinylTones, Brush>()

/**
 * Glossy toy eye. At [blink] of 0.7 or more it is a closed arch (in `tones.base`), otherwise a gradient
 * oval with a large and a small highlight. Callers pass EyeInk tones.
 */
fun DrawScope.glossyEye(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    blink: Float,
    tones: VinylTones,
    iris: Brush = WarmIris,
) {
    if (blink >= CLOSED_EYE_BLINK) {
        // A closed eye is a happy arch, as in the references: the top arc of a flat ellipse.
        drawArc(
            tones.base,
            CLOSED_EYE_START_DEG,
            CLOSED_EYE_SWEEP_DEG,
            useCenter = false,
            topLeft = Offset(center.x - radiusX, center.y - CLOSED_EYE_RISE * radiusY),
            size = Size(2 * radiusX, 2 * CLOSED_EYE_HEIGHT * radiusY),
            style = ClosedEyeStroke,
        )
        return
    }
    unitOval(center.x, center.y, radiusX, radiusY, EyeBrushes.getOrPut(tones) { unitBrush(tones) })
    // The iris: a warm glow in the lower half of the eye, as light passing through it.
    unitFade(
        iris,
        center.x - IRIS_WIDTH * radiusX,
        center.y + IRIS_TOP * radiusY,
        2 * IRIS_WIDTH * radiusX,
        IRIS_HEIGHT * radiusY,
    )
    drawCircle(
        Color.White,
        EYE_BIG_HIGHLIGHT_RADIUS * radiusX,
        Offset(center.x + EYE_BIG_HIGHLIGHT_X * radiusX, center.y + EYE_BIG_HIGHLIGHT_Y * radiusY),
    )
    drawCircle(
        Color.White,
        EYE_SMALL_HIGHLIGHT_RADIUS * radiusX,
        Offset(center.x + EYE_SMALL_HIGHLIGHT_X * radiusX, center.y + EYE_SMALL_HIGHLIGHT_Y * radiusY),
    )
}

/** The bounds of the ellipse centered at ([cx], [cy]) with radii [rx] x [ry], as in SVG. */
fun ovalRect(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float = rx,
): Rect = Rect(cx - rx, cy - ry, cx + rx, cy + ry)

/** A path from SVG path data, for artwork written in its design grid. */
fun svgPath(data: String): Path = PathParser().parsePathString(data).toPath()

/** An oval (or circle) as a path, for shapes that are combined or clipped. */
fun ovalPath(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float = rx,
): Path = Path().apply { addOval(ovalRect(cx, cy, rx, ry)) }

/** A rounded rectangle as a path. */
fun roundRectPath(rect: RoundRect): Path = Path().apply { addRoundRect(rect) }

/** A rectangle with one corner [radius] as a path. */
fun roundRectPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
): Path = roundRectPath(RoundRect(left, top, right, bottom, CornerRadius(radius)))

/** A path and its vinyl brush, built once from the path bounds. */
class VinylShape(
    val path: Path,
    tones: VinylTones,
) {
    val bounds: Rect = path.getBounds()
    val brush: Brush = vinylBrush(bounds, tones)
}

fun DrawScope.vinylShape(shape: VinylShape) = drawPath(shape.path, shape.brush)

private const val SCLERA_SCALE_X = 0.9f
private const val SCLERA_SCALE_Y = 0.92f
private const val SCLERA_SHIFT_X = -0.035f
private const val IRIS_SCALE_X = 0.86f
private const val IRIS_SCALE_Y = 0.93f
private const val IRIS_SHIFT_X = 0.085f
private const val IRIS_SHIFT_Y = 0.04f

/**
 * A [glossyEye] set in a dark-rimmed white, as in the references: the rim fills the given radii, the
 * white sits inside it and the iris covers most of the white, leaving a crescent on the lit side and a
 * thicker rim on top as the upper lid. A closed eye is the same arch as the plain glossy eye's.
 */
fun DrawScope.rimmedEye(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    blink: Float,
    tones: VinylTones,
    iris: Brush = WarmIris,
) {
    if (blink >= CLOSED_EYE_BLINK) {
        glossyEye(center, radiusX, radiusY, blink, tones, iris)
        return
    }
    drawOval(tones.base, Offset(center.x - radiusX, center.y - radiusY), Size(2 * radiusX, 2 * radiusY))
    val whiteX = radiusX * SCLERA_SCALE_X
    val whiteY = radiusY * SCLERA_SCALE_Y
    drawOval(
        Color.White,
        Offset(center.x + SCLERA_SHIFT_X * radiusX - whiteX, center.y - whiteY),
        Size(2 * whiteX, 2 * whiteY),
    )
    glossyEye(
        Offset(center.x + IRIS_SHIFT_X * radiusX, center.y + IRIS_SHIFT_Y * radiusY),
        radiusX * IRIS_SCALE_X,
        radiusY * IRIS_SCALE_Y,
        blink,
        tones,
        iris,
    )
}
