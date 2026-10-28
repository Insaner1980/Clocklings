// Literal coordinates follow the approved sample in the 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.contactShadow
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
 * The cat as a shaded vinyl toy, after the approved sample: tail, body with a cream chest, paws, ears,
 * head with forehead stripes, muzzle, cheeks, glossy eyes, nose, mouth and whiskers. The body breathes
 * around the feet, the head tilts around the neck, the tail swings with partDeg and the eyes blink.
 * A hat ([ItemLayer.OVER_EARS]) covers the forehead stripes, and the ears are then drawn after it so
 * that they come through the hat. The wand ([ArtPart.HAND]) is drawn before the paws, which hold it.
 */
internal object CatRig : AnimalRig(
    // The crops keep every item of the slot whole while leaving out the tail and the mouth and whiskers.
    headCrop = Rect(46f, 10f, 194f, 151f),
    neckCrop = Rect(55f, 150f, 185f, 236f),
) {
    /** Fur brushes for every companion color, built once and indexed by the color's ordinal. */
    private val furs = CompanionColor.entries.map { CatFur(PersonalizationColors.companion(CompanionKind.CAT, it)) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val fur = furs[color.ordinal]
        posedAnimal(pose, outfit, Ground) {
            tail(fur, pose.partDeg)
            contactShadow(TailShadow)
            vinylOval(Body, fur.body)
            vinylShape(Chest)
            specular(BodyShine)
            items(outfit, ArtPart.HAND, ItemLayer.FRONT)
            contactShadow(PawShadowLeft)
            contactShadow(PawShadowRight)
            vinylOval(PawLeft, fur.pawLeft)
            vinylOval(PawRight, fur.pawRight)
            specular(PawLeft)
            specular(PawRight)
            drawPath(Toes, fur.stripe, alpha = TOE_ALPHA, style = ToeStroke)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(fur, outfit, pose.blink) }
        }
    }

    private fun DrawScope.tail(
        fur: CatFur,
        angle: Float,
    ) {
        rotate(angle, TailPivot) {
            drawPath(TailPath, fur.tail, style = TailStroke)
            drawPath(TailTipPath, TailTipBrush, style = TailStroke)
        }
    }

    private fun DrawScope.head(
        fur: CatFur,
        outfit: Outfit,
        blink: Float,
    ) {
        val headItem = outfit[Slot.HEAD]
        val hat = headItem?.let { itemArt(it) }?.takeIf { it.layer == ItemLayer.OVER_EARS }
        if (hat == null) ears(fur)
        vinylOval(Head, fur.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        if (hat == null) drawPath(Stripes, fur.stripe, style = StripeStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.OVER_EARS)
        val earClip = hat?.earClip
        if (earClip != null) {
            clipPath(earClip) { ears(fur) }
        } else if (hat != null) {
            ears(fur)
        }
        vinylShape(Muzzle)
        cheeks(CheekLeft, CheekRight)
        glossyEyes(EyeLeft, EyeRight, EYE_RX, EYE_RY, blink)
        vinylShape(Nose)
        drawOval(ArtTones.NoseShine, NoseGlint.topLeft, NoseGlint.size)
        drawPath(Mouth, PersonalizationColors.EyeInk, style = MouthStroke)
        drawPath(Whiskers, WhiskerColor, style = WhiskerStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.ears(fur: CatFur) {
        drawPath(EarLeftPath, fur.earLeft)
        drawPath(EarRightPath, fur.earRight)
        vinylShape(InnerEarLeft)
        vinylShape(InnerEarRight)
        contactShadow(EarShadowLeft)
        contactShadow(EarShadowRight)
    }
}

/** One fur color's brushes. */
private class CatFur(
    base: Color,
) {
    private val tones = vinylTones(base)
    val stripe: Color = tones.shade
    val tail: Brush = vinylBrush(TailBounds, tones)
    val body: Brush = vinylBrush(Body, tones)
    val pawLeft: Brush = vinylBrush(PawLeft, tones)
    val pawRight: Brush = vinylBrush(PawRight, tones)
    val earLeft: Brush = vinylBrush(EarLeftPath.getBounds(), tones)
    val earRight: Brush = vinylBrush(EarRightPath.getBounds(), tones)
    val head: Brush = vinylBrush(Head, tones)
}

private const val TOE_ALPHA = 0.7f
private const val HEAD_SHINE_DEG = -18f
private const val EYE_RX = 10.5f
private const val EYE_RY = 13.5f
private const val TAIL_WIDTH = 24f

private val TailPivot = Offset(168f, 250f)
private val Neck = Offset(120f, 160f)

private val Ground = ovalRect(120f, 284f, 86f, 11f)
private val HeadShadow = ovalRect(120f, 160f, 54f, 15f)

private val TailPath = svgPath("M166 252 C214 262 224 218 206 198 C192 182 196 166 212 164")
private val TailBounds = TailPath.getBounds().inflate(TAIL_WIDTH / 2)
private val TailTipPath = svgPath("M206 198 C192 182 196 166 212 164")
private val TailTipBrush = vinylBrush(TailTipPath.getBounds().inflate(TAIL_WIDTH / 2), ArtTones.Cream)
private val TailStroke = itemStroke(TAIL_WIDTH)

private val Body = ovalRect(120f, 214f, 60f, 62f)
private val Chest = VinylShape(ovalPath(120f, 218f, 33f, 45f), ArtTones.Cream)
private val BodyShine = specularRect(98f, 186f, 11f, 6f)
private val PawLeft = ovalRect(94f, 270f, 24f, 15f)
private val PawRight = ovalRect(146f, 270f, 24f, 15f)
private val Toes = svgPath("M88 276 L88 268 M100 276 L100 268 M140 276 L140 268 M152 276 L152 268")
private val ToeStroke = itemStroke(2.5f)

private val EarLeftPath = svgPath("M62 104 L56 40 Q58 28 70 34 L108 72 Z")
private val EarRightPath = svgPath("M178 104 L184 40 Q182 28 170 34 L132 72 Z")
private val InnerEarLeft = VinylShape(svgPath("M70 88 L67 49 L97 76 Z"), ArtTones.Pink)
private val InnerEarRight = VinylShape(svgPath("M170 88 L173 49 L143 76 Z"), ArtTones.Pink)

private val Head = ovalRect(120f, 110f, 76f, 58f)
private val HeadShine = specularRect(90f, 76f, 24f, 10f)
private val Stripes = svgPath("M104 58 L108 76 M120 55 L120 74 M136 58 L132 76")
private val StripeStroke = itemStroke(7f)

private val Muzzle = VinylShape(ovalPath(120f, 140f, 37f, 23f), ArtTones.Cream)
private val CheekLeft = ovalRect(76f, 132f, 12f, 7f)
private val CheekRight = ovalRect(164f, 132f, 12f, 7f)
private val EyeLeft = Offset(92f, 112f)
private val EyeRight = Offset(148f, 112f)
private val Nose = VinylShape(svgPath("M111 128 Q120 122 129 128 Q125 135 120 136 Q115 135 111 128 Z"), ArtTones.EyeInk)
private val NoseGlint = ovalRect(117f, 127f, 3f, 1.6f)
private val Mouth = svgPath("M120 136 L120 141 M108 140 Q114 147 120 141 Q126 147 132 140")
private val MouthStroke = itemStroke(2.5f)
private val Whiskers = svgPath("M86 137 L50 130 M86 144 L50 147 M154 137 L190 130 M154 144 L190 147")
private val WhiskerColor = PersonalizationColors.EyeInk.copy(alpha = 0.8f)
private val WhiskerStroke = itemStroke(1.6f)

// Contact shadows where parts meet: above the paws, at the tail root and at the ear roots.
private val PawShadowLeft = ovalRect(94f, 258f, 22f, 7f)
private val PawShadowRight = ovalRect(146f, 258f, 22f, 7f)
private val TailShadow = ovalRect(172f, 252f, 13f, 10f)
private val EarShadowLeft = ovalRect(82f, 74f, 16f, 7f)
private val EarShadowRight = ovalRect(158f, 74f, 16f, 7f)
