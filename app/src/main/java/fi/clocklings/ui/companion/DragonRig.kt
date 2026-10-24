// Literal coordinates place the dragon in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
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
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The dragon as a shaded vinyl toy, after the reference: membrane wings behind the shoulders, a tail
 * with a golden diamond tip, a round body with a cream plated belly, small arms resting on it, feet with
 * cream claws, and a round head with cream horns, frilled ears, a muzzle with nostrils, pink cheeks and
 * glossy eyes. The body breathes around the feet, the head tilts around the neck and the wings flap
 * with partDeg around the shoulders. A [ArtPart.WINGS] item replaces the dragon's own wings while worn
 * and flaps the same way: it is drawn once per wing, clipped to that wing's half of the grid. A helmet
 * ([ItemLayer.OVER_EARS]) covers the top of the head, and the horns are then drawn after it so that they
 * come out of it. Held items ([ArtPart.HAND]) are drawn before the arms, which hold them.
 */
internal object DragonRig : AnimalRig(
    // The crops keep every item of the slot whole. The head crop shows the whole face, above the wings;
    // the neck crop starts below the mouth and is wide enough to show the royal cape beside the body.
    headCrop = Rect(24f, 0f, 216f, 151f),
    neckCrop = Rect(60f, 156f, 180f, 240f),
) {
    /** Hide brushes for every companion color, built once and indexed by the color's ordinal. */
    private val hides = CompanionColor.entries.map { DragonHide(it) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val hide = hides[color.ordinal]
        groundShadow(Ground)
        posedBody(pose) {
            wings(hide, outfit, pose.partDeg)
            items(outfit, ArtPart.BODY, ItemLayer.BEHIND)
            drawPath(TailPath, hide.tail)
            vinylShape(TailTip)
            drawPath(TailTipFacet, ArtTones.Gold.shade, alpha = FACET_ALPHA, style = FacetStroke)
            specular(TailTip.bounds)
            vinylOval(Body, hide.body)
            vinylOval(NeckBase, hide.body)
            vinylShape(Belly)
            clipPath(Belly.path) { drawPath(Plates, ArtTones.Cream.shade, alpha = PLATE_ALPHA, style = PlateStroke) }
            specular(BodyShine)
            hide.limbs.draw(this, outfit)
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(hide, outfit, pose.blink) }
        }
    }

    /** The dragon's own wings, or a worn wing item in their place; both flap up around the shoulders. */
    private fun DrawScope.wings(
        hide: DragonHide,
        outfit: Outfit,
        partDeg: Float,
    ) {
        val wingItem = outfit[Slot.EXTRA]?.let { itemArt(it) }?.part == ArtPart.WINGS
        rotate(partDeg, ShoulderLeft) {
            if (wingItem) {
                clipRect(-width, -height, MID_X, 2 * height) { items(outfit, ArtPart.WINGS, ItemLayer.BEHIND) }
            } else {
                wing(WingLeft, hide.wingLeft, hide)
            }
        }
        rotate(-partDeg, ShoulderRight) {
            if (wingItem) {
                clipRect(MID_X, -height, 2 * width, 2 * height) { items(outfit, ArtPart.WINGS, ItemLayer.BEHIND) }
            } else {
                wing(WingRight, hide.wingRight, hide)
            }
        }
    }

    private fun DrawScope.wing(
        wing: DragonWing,
        membrane: Brush,
        hide: DragonHide,
    ) {
        drawPath(wing.membrane, membrane)
        drawPath(wing.fingers, hide.wingShade, alpha = FINGER_ALPHA, style = FingerStroke)
        drawPath(wing.arm, membrane, style = WingArmStroke)
    }

    private fun DrawScope.head(
        hide: DragonHide,
        outfit: Outfit,
        blink: Float,
    ) {
        val headItem = outfit[Slot.HEAD]
        val helmet = headItem?.let { itemArt(it) }?.takeIf { it.layer == ItemLayer.OVER_EARS }
        if (helmet == null) horns()
        ears(hide)
        vinylOval(Head, hide.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        items(outfit, ArtPart.HEAD, ItemLayer.OVER_EARS)
        val hornClip = helmet?.earClip
        if (hornClip != null) {
            clipPath(hornClip) { horns() }
        } else if (helmet != null) {
            horns()
        }
        drawPath(MuzzlePath, hide.muzzle)
        cheeks(CheekLeft, CheekRight)
        glossyEyes(EyeLeft, EyeRight, EYE_RX, EYE_RY, blink)
        drawPath(Nostrils, hide.shade)
        drawPath(Mouth, PersonalizationColors.EyeInk, style = MouthStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.horns() {
        vinylShape(HornLeft)
        vinylShape(HornRight)
        drawPath(HornRidges, ArtTones.Cream.shade, alpha = RIDGE_ALPHA, style = RidgeStroke)
        specular(HornLeft.bounds, HORN_SHINE_DEG)
        specular(HornRight.bounds, -HORN_SHINE_DEG)
    }

    private fun DrawScope.ears(hide: DragonHide) {
        drawPath(EarLeft.frill, hide.earLeft)
        drawPath(EarRight.frill, hide.earRight)
        drawPath(EarLeft.spines, hide.wingShade, alpha = FINGER_ALPHA, style = FingerStroke)
        drawPath(EarRight.spines, hide.wingShade, alpha = FINGER_ALPHA, style = FingerStroke)
    }
}

/** One hide color's brushes: the body color, and the deeper wing color for the wings and the ear frills' spines. */
private class DragonHide(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.DRAGON, color))
    private val wingTones = vinylTones(PersonalizationColors.dragonWing(color))
    val shade: Color = tones.shade
    val wingShade: Color = wingTones.shade
    val tail: Brush = vinylBrush(TailPath.getBounds(), tones)
    val body: Brush = vinylBrush(Body, tones)
    val limbs = ClawedLimbs(ArmLeft to ArmRight, ArmStroke, FootLeft to FootRight, Claws, tones)
    val head: Brush = vinylBrush(Head, tones)
    val muzzle: Brush = vinylBrush(MuzzlePath.getBounds(), tones)
    val wingLeft: Brush = vinylBrush(WingLeft.membrane.getBounds(), wingTones)
    val wingRight: Brush = vinylBrush(WingRight.membrane.getBounds(), wingTones)
    val earLeft: Brush = vinylBrush(EarLeft.frill.getBounds(), tones)
    val earRight: Brush = vinylBrush(EarRight.frill.getBounds(), tones)
}

/** One membrane wing in its rest pose: [side] 0 is the viewer's left, 1 the right. Mirrored from the left wing. */
private class DragonWing(
    private val side: Int,
) {
    private fun x(v: Float) = if (side == 0) v else MID_X * 2 - v

    val membrane =
        Path().apply {
            moveTo(x(100f), 194f)
            quadraticTo(x(62f), 154f, x(12f), 154f)
            quadraticTo(x(28f), 170f, x(20f), 188f)
            quadraticTo(x(40f), 186f, x(46f), 204f)
            quadraticTo(x(64f), 202f, x(74f), 220f)
            quadraticTo(x(88f), 214f, x(100f), 230f)
            close()
        }
    val arm =
        Path().apply {
            moveTo(x(100f), 194f)
            quadraticTo(x(62f), 154f, x(12f), 154f)
        }
    val fingers =
        Path().apply {
            for ((fx, fy) in listOf(20f to 188f, 46f to 204f, 74f to 220f)) {
                moveTo(x(58f), 164f)
                lineTo(x(fx), fy)
            }
        }
}

/** One frilled ear: three spines fanning out from the upper side of the head. Built mirrored like the wings. */
private class DragonEar(
    private val side: Int,
) {
    private fun x(v: Float) = if (side == 0) v else MID_X * 2 - v

    val frill =
        Path().apply {
            moveTo(x(66f), 62f)
            quadraticTo(x(54f), 52f, x(38f), 48f)
            quadraticTo(x(44f), 57f, x(42f), 62f)
            quadraticTo(x(32f), 64f, x(25f), 70f)
            quadraticTo(x(34f), 74f, x(38f), 77f)
            quadraticTo(x(32f), 83f, x(32f), 90f)
            quadraticTo(x(44f), 91f, x(54f), 96f)
            close()
        }
    val spines =
        Path().apply {
            for ((sx, sy) in listOf(39f to 50f, 27f to 70f, 33f to 88f)) {
                moveTo(x(60f), 76f)
                lineTo(x(sx), sy)
            }
        }
}

private const val MID_X = 120f
private const val HEAD_SHINE_DEG = -18f
private const val HORN_SHINE_DEG = -30f
private const val EYE_RX = 10.5f
private const val EYE_RY = 13.5f
private const val ARM_WIDTH = 17f
private const val FINGER_ALPHA = 0.6f
private const val PLATE_ALPHA = 0.7f
private const val RIDGE_ALPHA = 0.55f
private const val FACET_ALPHA = 0.45f

private val Neck = Offset(120f, 162f)
private val ShoulderLeft = Offset(98f, 210f)
private val ShoulderRight = Offset(142f, 210f)

private val Ground = ovalRect(120f, 284f, 92f, 11f)
private val HeadShadow = ovalRect(120f, 162f, 54f, 14f)

private val WingLeft = DragonWing(0)
private val WingRight = DragonWing(1)
private val FingerStroke = itemStroke(2.4f)
private val WingArmStroke = itemStroke(5f)

private val TailPath = svgPath("M150 240 C178 256 202 250 207 214 L211 206 L215 214 C220 262 186 286 146 278 Z")
private val TailTip = VinylShape(svgPath("M218 182.5 L224.6 200.8 L210 213.5 L203.4 195.2 Z"), ArtTones.Gold)
private val TailTipFacet = svgPath("M218 182.5 L210 213.5")
private val FacetStroke = itemStroke(1.6f)

private val Body = ovalRect(120f, 214f, 62f, 62f)
private val NeckBase = ovalRect(120f, 168f, 56f, 20f)
private val Belly = VinylShape(ovalPath(120f, 222f, 36f, 46f), ArtTones.Cream)
private val Plates =
    Path().apply {
        for (y in floatArrayOf(192f, 208f, 224f, 240f, 256f)) {
            moveTo(82f, y)
            quadraticTo(120f, y + 9f, 158f, y)
        }
    }
private val PlateStroke = itemStroke(2.2f)
private val BodyShine = specularRect(96f, 184f, 11f, 6f)

private val ArmLeft = svgPath("M84 196 L96 214")
private val ArmRight = svgPath("M156 196 L144 214")
private val ArmStroke = itemStroke(ARM_WIDTH)

private val FootLeft = ovalRect(92f, 270f, 25f, 15f)
private val FootRight = ovalRect(148f, 270f, 25f, 15f)
private val Claws =
    VinylShape(
        Path().apply {
            for (x in floatArrayOf(80f, 92f, 104f, 136f, 148f, 160f)) addPath(ovalPath(x, 280f, 3.4f, 3f))
        },
        ArtTones.Cream,
    )

private val Head = ovalRect(120f, 108f, 76f, 62f)
private val HeadShine = specularRect(90f, 74f, 24f, 10f)

private val HornLeft = VinylShape(svgPath("M84 62 C74 44 66 30 72 16 C82 22 98 36 112 52 Z"), ArtTones.Cream)
private val HornRight = VinylShape(svgPath("M156 62 C166 44 174 30 168 16 C158 22 142 36 128 52 Z"), ArtTones.Cream)
private val HornRidges =
    svgPath("M78 42 Q88 38 96 42 M74 30 Q80 27 86 30 M162 42 Q152 38 144 42 M166 30 Q160 27 154 30")
private val RidgeStroke = itemStroke(2f)

private val EarLeft = DragonEar(0)
private val EarRight = DragonEar(1)

private val MuzzlePath = ovalPath(120f, 136f, 32f, 20f)
private val CheekLeft = ovalRect(74f, 130f, 12f, 7f)
private val CheekRight = ovalRect(166f, 130f, 12f, 7f)
private val EyeLeft = Offset(92f, 106f)
private val EyeRight = Offset(148f, 106f)
private val Nostrils =
    Path().apply {
        addPath(ovalPath(111f, 130f, 3f, 2.2f))
        addPath(ovalPath(129f, 130f, 3f, 2.2f))
    }
private val Mouth = svgPath("M106 141 Q120 153 134 141")
private val MouthStroke = itemStroke(2.5f)
