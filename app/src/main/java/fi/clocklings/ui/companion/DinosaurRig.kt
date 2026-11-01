// Literal coordinates place the dinosaur in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The dinosaur as a shaded vinyl toy, after the reference: standing upright and turned toward the
 * viewer's right, with a big round head and a long snout, rounded spikes down the back of the head,
 * the back and the tail, darker spots on the back, a cream belly, short arms and big feet. The body
 * breathes around the feet, the head tilts around the neck and partDeg swings the tail up around its
 * root. Held items ([ArtPart.HAND]) are drawn before the arms, which hold them; the backpack is drawn
 * there too, so its shoulder strap runs under the back arm.
 */
internal object DinosaurRig : AnimalRig(
    // The crops keep every item of the slot whole: the head crop holds the hats and the head spikes and ends
    // under the jaw; the neck crop starts below the mouth.
    headCrop = Rect(34f, 0f, 226f, 146f),
    neckCrop = Rect(66f, 138f, 192f, 230f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "dinosaur-extra-backpack" to Rect(12f, 129f, 135f, 252f),
            "dinosaur-extra-bucket" to Rect(120f, 177f, 240f, 297f),
            "dinosaur-extra-balloon" to Rect(24f, 0f, 240f, 216f),
            "dinosaur-extra-umbrella" to Rect(2f, 0f, 240f, 238f),
            "dinosaur-extra-kite" to Rect(0f, 0f, 218f, 218f),
            "dinosaur-extra-skateboard" to Rect(10f, 80f, 230f, 300f),
        ),
) {
    /** Hide brushes for every companion color, built once and indexed by the color's ordinal. */
    private val hides = CompanionColor.entries.map { DinosaurHide(it) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val hide = hides[color.ordinal]
        groundShadow(Ground)
        posedBody(pose) {
            items(outfit, ArtPart.BODY, ItemLayer.BEHIND)
            rotate(pose.partDeg, TailPivot) { tail(hide) }
            spikes(BackSpikes, hide.backSpikes)
            drawPath(BodyPath, hide.body)
            clipPath(BodyPath) {
                drawPath(BodySpots, hide.spots)
                vinylShape(Belly)
            }
            specular(BodyShine)
            hide.limbs.draw(this, outfit)
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(hide, outfit, pose.blink) }
        }
    }

    private fun DrawScope.tail(hide: DinosaurHide) {
        spikes(TailSpikes, hide.tailSpikes)
        drawPath(TailPath, hide.tail)
        clipPath(TailPath) { drawPath(TailSpots, hide.spots) }
    }

    /** Rounded spikes: the fill, and a round-joined outline in the same brush that softens the tips. */
    private fun DrawScope.spikes(
        path: Path,
        brush: Brush,
    ) {
        drawPath(path, brush)
        drawPath(path, brush, style = SpikeStroke)
    }

    private fun DrawScope.head(
        hide: DinosaurHide,
        outfit: Outfit,
        blink: Float,
    ) {
        spikes(HeadSpikes, hide.headSpikes)
        drawPath(HeadPath, hide.head)
        clipPath(HeadPath) { drawPath(HeadSpots, hide.spots) }
        specular(HeadShine, HEAD_SHINE_DEG)
        specular(SnoutShine)
        cheeks(CheekLeft, CheekRight, ArtTones.CoolCheekColor)
        glossyEyes(EyeLeft, EyeRight, EYE_RX, EYE_RY, blink)
        drawPath(Nostrils, hide.shade)
        drawPath(Mouth, PersonalizationColors.EyeInk, style = MouthStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }
}

/** One hide color's brushes: the body color, and the deeper marking color for the spikes and spots. */
private class DinosaurHide(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.DINOSAUR, color))
    private val marking = vinylTones(PersonalizationColors.dinosaurMarking(color))
    val shade: Color = tones.shade
    val spots: Color = marking.base
    val tail: Brush = vinylBrush(TailPath.getBounds(), tones)
    val body: Brush = vinylBrush(BodyPath.getBounds(), tones)
    val limbs = ClawedLimbs(ArmLeft to ArmRight, ArmStroke, FootLeft to FootRight, Claws, tones)
    val head: Brush = vinylBrush(HeadPath.getBounds(), tones)
    val headSpikes: Brush = vinylBrush(HeadSpikes.getBounds(), marking)
    val backSpikes: Brush = vinylBrush(BackSpikes.getBounds(), marking)
    val tailSpikes: Brush = vinylBrush(TailSpikes.getBounds(), marking)
}

/**
 * Rounded spikes whose bases sit at the given points just inside the outline: each spike is
 * (x, y, direction in degrees, size), and it points [size] x 1.35 outward from its base.
 */
private fun spikePath(vararg spikes: FloatArray) =
    Path().apply {
        for (spike in spikes) {
            val x = spike[0]
            val y = spike[1]
            val deg = spike[2]
            val size = spike[3]
            val a = deg * (PI.toFloat() / HALF_TURN_DEG)
            val dx = cos(a)
            val dy = sin(a)
            val half = size * SPIKE_HALF_WIDTH
            val length = size * SPIKE_LENGTH
            val bulge = SPIKE_BULGE
            moveTo(x - dy * half, y + dx * half)
            quadraticTo(
                x + dx * length * bulge - dy * half * bulge,
                y + dy * length * bulge + dx * half * bulge,
                x + dx * length,
                y + dy * length,
            )
            quadraticTo(
                x + dx * length * bulge + dy * half * bulge,
                y + dy * length * bulge - dx * half * bulge,
                x + dy * half,
                y - dx * half,
            )
            close()
        }
    }

private fun spots(vararg spots: Float) =
    Path().apply {
        for (i in spots.indices step 3) addPath(ovalPath(spots[i], spots[i + 1], spots[i + 2]))
    }

private const val HALF_TURN_DEG = 180f
private const val SPIKE_HALF_WIDTH = 0.62f
private const val SPIKE_LENGTH = 1.35f
private const val SPIKE_BULGE = 0.55f
private const val HEAD_SHINE_DEG = -20f
private const val EYE_RX = 10f
private const val EYE_RY = 12.5f
private const val ARM_WIDTH = 15f

private val Neck = Offset(120f, 150f)
private val TailPivot = Offset(92f, 246f)

private val Ground = ovalRect(116f, 284f, 100f, 11f)
private val HeadShadow = ovalRect(124f, 152f, 54f, 12f)

private val SpikeStroke = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private val TailPath =
    svgPath("M88 220 C64 236 36 246 14 254 C5 257 5 267 16 267 C46 271 80 270 106 262 Z")
private val TailSpikes =
    spikePath(
        floatArrayOf(62f, 238f, -110f, 10f),
        floatArrayOf(42f, 246f, -105f, 8f),
        floatArrayOf(25f, 252f, -100f, 6f),
    )
private val TailSpots = spots(52f, 254f, 4.5f, 34f, 259f, 3.5f)

private val BodyPath =
    svgPath("M98 130 C134 124 168 150 172 200 C178 244 162 276 120 278 C84 280 64 262 62 224 C60 188 70 142 98 130 Z")
private val BackSpikes =
    spikePath(
        floatArrayOf(80f, 144f, 172f, 17f),
        floatArrayOf(70f, 170f, 172f, 17f),
        floatArrayOf(64f, 196f, 168f, 16f),
        floatArrayOf(64f, 222f, 160f, 14f),
    )
private val BodySpots = spots(80f, 184f, 6f, 72f, 206f, 5f, 90f, 214f, 4f, 82f, 234f, 6.5f, 98f, 194f, 3.5f)
private val Belly = VinylShape(ovalPath(136f, 218f, 30f, 50f), ArtTones.Cream)
private val BodyShine = specularRect(150f, 176f, 8f, 5f)

private val ArmLeft = svgPath("M108 190 Q112 202 120 208")
private val ArmRight = svgPath("M158 186 Q166 198 176 204")
private val ArmStroke = itemStroke(ARM_WIDTH)

private val FootLeft = ovalRect(92f, 272f, 28f, 13f)
private val FootRight = ovalRect(150f, 273f, 30f, 13f)

/** The body and feet as one outline, for items that show only where they come out from behind the dinosaur. */
internal val DinosaurBodySilhouette: Path =
    Path().apply {
        addPath(BodyPath)
        addOval(FootLeft)
        addOval(FootRight)
    }

private val Claws =
    VinylShape(
        Path().apply {
            for (x in floatArrayOf(98f, 106f, 113f, 158f, 166f, 173f)) addPath(ovalPath(x, 278f, 3f, 2.6f))
        },
        ArtTones.Cream,
    )

private val HeadPath =
    svgPath(
        "M112 42 C148 40 168 56 172 78 C196 76 222 92 222 114 C222 136 198 146 170 146 " +
            "C150 146 130 144 116 142 C80 142 62 120 62 94 C62 64 82 42 112 42 Z",
    )
private val HeadSpikes =
    spikePath(
        floatArrayOf(95f, 49f, -105f, 11f),
        floatArrayOf(79f, 59f, -135f, 14f),
        floatArrayOf(68f, 78f, -160f, 16f),
        floatArrayOf(65f, 102f, 180f, 17f),
        floatArrayOf(71f, 126f, 165f, 16f),
    )
private val HeadSpots = spots(76f, 82f, 4.5f, 84f, 68f, 3.5f)
private val HeadShine = specularRect(96f, 60f, 20f, 9f)
private val SnoutShine = specularRect(194f, 96f, 12f, 5f)

private val CheekLeft = ovalRect(94f, 118f, 12f, 7f)
private val CheekRight = ovalRect(146f, 116f, 9f, 6f)
private val EyeLeft = Offset(102f, 92f)
private val EyeRight = Offset(152f, 84f)
private val Nostrils =
    Path().apply {
        addPath(ovalPath(200f, 100f, 3f, 2.2f))
        addPath(ovalPath(212f, 105f, 3f, 2.2f))
    }
private val Mouth = svgPath("M146 128 Q176 144 206 124")
private val MouthStroke = itemStroke(2.5f)
