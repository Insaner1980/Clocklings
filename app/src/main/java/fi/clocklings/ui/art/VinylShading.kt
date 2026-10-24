// Shading constants are the vinyl toy recipe; shapes are drawn in the 240 x 300 artwork grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.art

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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

private const val LIGHT_MIX = 0.45f
private const val SHADE_MIX = 0.30f
private const val LIGHT_CENTER_X = 0.36f
private const val LIGHT_CENTER_Y = 0.30f
private const val GRADIENT_RADIUS = 0.78f
private const val BASE_STOP = 0.55f
private const val SPECULAR_ALPHA = 0.55f
private const val SPECULAR_WIDTH = 0.34f
private const val SPECULAR_HEIGHT = 0.2f
private const val CONTACT_SHADOW_ALPHA = 0.28f
private const val GROUND_SHADOW_ALPHA = 0.22f
private const val CLOSED_EYE_BLINK = 0.7f
private const val CLOSED_EYE_WIDTH = 3f
private const val UNIT_CENTER = 0.5f
private const val EYE_BIG_HIGHLIGHT_RADIUS = 0.38f
private const val EYE_BIG_HIGHLIGHT_X = 0.33f
private const val EYE_BIG_HIGHLIGHT_Y = -0.4f
private const val EYE_SMALL_HIGHLIGHT_RADIUS = 0.17f
private const val EYE_SMALL_HIGHLIGHT_X = -0.28f
private const val EYE_SMALL_HIGHLIGHT_Y = 0.45f

fun vinylTones(base: Color): VinylTones =
    VinylTones(
        light = lerp(base, Color.White, LIGHT_MIX),
        base = base,
        shade = lerp(base, SkColors.Ink, SHADE_MIX),
    )

/** Radial gradient lit from the upper left of [bounds]. Build it once per color and part, never per frame. */
fun vinylBrush(
    bounds: Rect,
    tones: VinylTones,
): Brush =
    Brush.radialGradient(
        0f to tones.light,
        BASE_STOP to tones.base,
        1f to tones.shade,
        center = Offset(bounds.left + bounds.width * LIGHT_CENTER_X, bounds.top + bounds.height * LIGHT_CENTER_Y),
        radius = GRADIENT_RADIUS * max(bounds.width, bounds.height),
    )

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

/** Soft white highlight in the upper left of [bounds], optionally tilted around its center. */
fun DrawScope.specular(
    bounds: Rect,
    rotationDeg: Float = 0f,
) {
    val halfWidth = bounds.width * SPECULAR_WIDTH / 2
    val halfHeight = bounds.height * SPECULAR_HEIGHT / 2
    val left = bounds.left + bounds.width * LIGHT_CENTER_X - halfWidth
    val top = bounds.top + bounds.height * LIGHT_CENTER_Y - halfHeight
    unitFade(SpecularBrush, left, top, halfWidth * 2, halfHeight * 2, rotationDeg)
}

/** Warm dark pool under a part: fades from the contact shadow pigment to transparent. */
fun DrawScope.contactShadow(bounds: Rect) =
    unitFade(ContactShadowBrush, bounds.left, bounds.top, bounds.width, bounds.height)

/** Wide ground shadow: fades from ink to transparent. */
fun DrawScope.groundShadow(bounds: Rect) =
    unitFade(GroundShadowBrush, bounds.left, bounds.top, bounds.width, bounds.height)

/** Radial fade from [color] at the center to transparent at the edge of the 1 x 1 unit box. */
private fun unitFadeBrush(color: Color): Brush =
    Brush.radialGradient(
        0f to color,
        1f to color.copy(alpha = 0f),
        center = Offset(UNIT_CENTER, UNIT_CENTER),
        radius = UNIT_CENTER,
    )

private val SpecularBrush = unitFadeBrush(Color.White.copy(alpha = SPECULAR_ALPHA))
private val ContactShadowBrush =
    unitFadeBrush(PersonalizationColors.ContactShadow.copy(alpha = CONTACT_SHADOW_ALPHA))
private val GroundShadowBrush = unitFadeBrush(SkColors.Ink.copy(alpha = GROUND_SHADOW_ALPHA))

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
 * Glossy toy eye. At [blink] of 0.7 or more it is a closed line (in `tones.base`), otherwise a gradient
 * oval with a large and a small highlight. Callers pass EyeInk tones.
 */
fun DrawScope.glossyEye(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    blink: Float,
    tones: VinylTones,
) {
    if (blink >= CLOSED_EYE_BLINK) {
        drawLine(
            tones.base,
            Offset(center.x - radiusX, center.y),
            Offset(center.x + radiusX, center.y),
            CLOSED_EYE_WIDTH,
            StrokeCap.Round,
        )
        return
    }
    unitOval(center.x, center.y, radiusX, radiusY, EyeBrushes.getOrPut(tones) { unitBrush(tones) })
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
