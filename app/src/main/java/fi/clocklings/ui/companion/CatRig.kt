// Literal coordinates were measured on the reference sheet in the 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.rimmedEye
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.unitBrush
import fi.clocklings.ui.art.unitOval
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The cat as a shaded vinyl toy, traced from the front view of the owner's reference sheet: the
 * outlines of the head with its ears, the sitting body, the tail, the muzzle, the forehead stripes,
 * the brows and the nose with the mouth are [CatTrace] paths. Inside the body's outline the haunches,
 * the front legs and the four paws are forms of their own, placed on creases measured on the sheet and
 * clipped to the traced outline, so every limb shades as a round part while the silhouette stays exact.
 *
 * The body breathes around the feet, the head tilts around the neck, the tail swings with partDeg and
 * the eyes blink. A hat ([ItemLayer.OVER_EARS]) covers the forehead stripes, and the ears are then
 * drawn again after it so that they come through the hat. The wand ([ArtPart.HAND]) is drawn before the
 * front paws, which hold it.
 */
internal object CatRig : AnimalRig(
    // The crops keep every item of the slot whole while leaving out the tail and the mouth and whiskers.
    headCrop = Rect(42f, 8f, 198f, 164f),
    neckCrop = Rect(55f, 150f, 185f, 236f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "cat-extra-yarn" to Rect(87f, 167f, 220f, 300f),
            "cat-extra-glasses" to Rect(36f, 28f, 204f, 196f),
            "cat-extra-fish-backpack" to Rect(0f, 134f, 144f, 278f),
            "cat-extra-wand" to Rect(0f, 143f, 139f, 282f),
            "cat-extra-wings" to Rect(0f, 40f, 240f, 280f),
        ),
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
            fur.body.draw(this, includeFrontPaws = false)
            specular(BodyShine)
            items(outfit, ArtPart.HAND, ItemLayer.FRONT)
            fur.body.drawFrontPaws(this)
            specular(FrontPawLeftBounds)
            specular(FrontPawRightBounds)
            drawPath(Toes, fur.stripe, alpha = TOE_ALPHA, style = ToeStroke)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.headTiltDeg, Neck) { head(fur, outfit, pose.blink) }
        }
    }

    private fun DrawScope.tail(
        fur: CatFur,
        angle: Float,
    ) {
        unitOval(TailRoot, fur.dot)
        rotate(angle, TailPivot) {
            drawPath(TailPath, fur.tail)
            clipPath(TailPath) { drawPath(TailTipArea, TailTipBrush) }
            specular(TailTipBounds)
        }
    }

    private fun DrawScope.head(
        fur: CatFur,
        outfit: Outfit,
        blink: Float,
    ) {
        val hat = outfit[Slot.HEAD]?.let { itemArt(it) }?.takeIf { it.layer == ItemLayer.OVER_EARS }
        drawPath(HeadPath, fur.head)
        ears(fur)
        specular(HeadShine, HEAD_SHINE_DEG)
        if (hat == null) drawPath(Stripes, fur.stripe)
        drawPath(Brows, fur.stripe)
        items(outfit, ArtPart.HEAD, ItemLayer.OVER_EARS)
        val earClip = hat?.earClip
        if (earClip != null) {
            clipPath(earClip) { ears(fur) }
        } else if (hat != null) {
            ears(fur)
        }
        vinylShape(Muzzle)
        cheeks(CheekLeft, CheekRight)
        rimmedEye(EyeLeft, EYE_RX, EYE_RY, blink, ArtTones.EyeInk)
        rimmedEye(EyeRight, EYE_RX, EYE_RY, blink, ArtTones.EyeInk)
        drawPath(Nose, PersonalizationColors.Cocoa)
        drawOval(ArtTones.NoseShine, NoseGlint.topLeft, NoseGlint.size)
        drawPath(Whiskers, PersonalizationColors.Cocoa, style = WhiskerStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    /** The ears lie behind the head's dome: each is shaded as its own part and darkens toward the dome. */
    private fun DrawScope.ears(fur: CatFur) {
        drawPath(EarLeft, fur.earLeft)
        drawPath(EarRight, fur.earRight)
        vinylShape(InnerEarLeft)
        vinylShape(InnerEarRight)
        clipPath(EarLeft) { contactShadow(EarShadowLeft) }
        clipPath(EarRight) { contactShadow(EarShadowRight) }
    }
}

/** One fur color's brushes. */
private class CatFur(
    base: Color,
) {
    private val tones = vinylTones(base)

    /** The forehead stripes, the brows and the toe lines: a deep tone of the fur. */
    val stripe: Color = lerp(tones.shade, Color.Black, STRIPE_DEPTH)
    val dot: Brush = unitBrush(tones)
    val tail = vinylBrush(TailPath, tones)
    val body = SeatedBody.Paint(tones, dot)
    val head = vinylBrush(HeadPath, tones)
    val earLeft = vinylBrush(EarLeft, tones)
    val earRight = vinylBrush(EarRight, tones)
}

private const val TOE_ALPHA = 0.8f
private const val STRIPE_DEPTH = 0.12f
private const val HEAD_SHINE_DEG = -18f
private const val EYE_RX = 14f
private const val EYE_RY = 14.6f

private val TailPivot = Offset(187f, 264f)

/** The tail's root behind the haunch: it stays put and fills the joint when the tail swings. */
private val TailRoot = ovalRect(186f, 248f, 14f, 24f)
private val Neck = Offset(120f, 160f)

private val Ground = ovalRect(120f, 284f, 86f, 11f)

private val TailPath = svgPath(CatTrace.TAIL)

/** The cream tip: the part of the tail above a smooth line measured on the sheet, clipped to the tail. */
private val TailTipArea =
    svgPath("M170 140 L240 140 L240 192 L207 195.4 C201 198.5 195 199.5 191 199 C186 198 181 195 178 190 Z")
private val TailTipBounds = Rect(178f, 155f, 228f, 199f)
private val TailTipBrush = vinylBrush(TailTipBounds, ArtTones.Cream)

private val BodyPath = svgPath(CatTrace.BODY)
private val BodyShine = specularRect(92f, 196f, 8f, 7f)
private val ChinShadow = ovalRect(120.5f, 169f, 48f, 12f)

// The front legs from the shoulders to the paws. Their inner edges run inside the chest patch, which is
// drawn over them, so the patch's own outline is the visible edge.
private val LegLeft =
    svgPath(
        "M78 164 C72 184 70.5 206 74 222 C77 236 81 250 83 262 L122 262 C120 254 119 247 118 240 " +
            "C116 233 113 227 111 222 C107 214 104 207 104 200 C104 193 109 187 113 183 L104 166 Z",
    )
private val LegRight =
    svgPath(
        "M169 164 C175 184 176.5 206 173 222 C170 236 166 250 164 262 L125.5 262 " +
            "C127.5 254 128.5 247 129.5 240 C131.5 233 134.5 227 136.5 222 " +
            "C140.5 214 143.5 207 143.5 200 C143.5 193 138.5 187 134.5 183 L143.5 166 Z",
    )

/** The cream chest patch: round at the top and tapering to a point between the front paws. */
private val Chest =
    VinylShape(
        svgPath(
            "M123.7 177.8 C136 178 146 187 145.8 199 C145.5 209 142 218 139.3 225.4 " +
                "C136.5 232 134 238 132.2 243 C130.5 248 129 252 127.8 255.4 " +
                "C126.5 260 125 265 123.7 267.8 C122.5 265 121.5 260 120.7 255.4 " +
                "C119.5 252 118 248 116.3 243 C114 238 111.5 232 109.3 225.4 " +
                "C106 218 101.5 209 101.7 199 C102 187 112 178 123.7 177.8 Z",
        ),
        ArtTones.Cream,
    )

private val BackPawLeft = pawPath(49.2f, 251.6f, 83.7f, 277.5f)
private val BackPawRight = pawPath(160.4f, 251.6f, 193.1f, 277.5f)
private val FrontPawLeft = pawPath(82.8f, 255.8f, 122f, 283.5f)
private val FrontPawRight = pawPath(124.3f, 255.8f, 163.4f, 283.5f)
private val FrontPawLeftBounds = FrontPawLeft.getBounds()
private val FrontPawRightBounds = FrontPawRight.getBounds()
private val Toes =
    svgPath(
        "M58.4 261.6 L58.4 272.2 M70.8 261.6 L70.8 272.2 M95.1 266 L95.1 276.6 M108.4 266 L108.4 276.6 " +
            "M139.8 266 L139.8 276.6 M152.9 266 L152.9 276.6 M173.7 261.6 L173.7 272.2 M185.5 261.6 L185.5 272.2",
    )
private val ToeStroke = itemStroke(2.2f)

private val HeadPath = svgPath(CatTrace.HEAD)
private val HeadShine = specularRect(92f, 66f, 24f, 9f)
private val EarLeft = svgPath(CatTrace.EAR_LEFT)
private val EarRight = svgPath(CatTrace.EAR_RIGHT)
private val InnerEarLeft = VinylShape(svgPath(CatTrace.INNER_EAR_LEFT), ArtTones.Pink)
private val InnerEarRight = VinylShape(svgPath(CatTrace.INNER_EAR_RIGHT), ArtTones.Pink)
private val Stripes =
    Path().apply {
        addPath(svgPath(CatTrace.STRIPE_LEFT))
        addPath(svgPath(CatTrace.STRIPE_CENTER))
        addPath(svgPath(CatTrace.STRIPE_RIGHT))
    }
private val Brows =
    Path().apply {
        addPath(svgPath(CatTrace.BROW_LEFT))
        addPath(svgPath(CatTrace.BROW_RIGHT))
    }

/** The cream muzzle: its width and height are the traced ones, its edge is drawn smooth under the eyes. */
private val Muzzle =
    VinylShape(
        svgPath(
            "M70.5 139 C70.5 129 78 124 88 123.5 C98 123 106 116.5 117.5 116.5 C129 116.5 137 123 147 123.5 " +
                "C157 124 164.5 129 164.5 139 C164.5 151 146 158.5 117.5 158.5 C89 158.5 70.5 151 70.5 139 Z",
        ),
        ArtTones.Cream,
    )
private val CheekLeft = ovalRect(66f, 135.5f, 11f, 8f)
private val CheekRight = ovalRect(175f, 134.5f, 11f, 8f)
private val EyeLeft = Offset(89.5f, 111f)
private val EyeRight = Offset(152f, 110.5f)
private val Nose = svgPath(CatTrace.NOSE)
private val NoseGlint = ovalRect(119.5f, 128.2f, 2.4f, 1.1f)
private val Whiskers =
    svgPath(
        "M40 133 Q60 131.5 78 136.5 M44 148 Q62 143.5 78 142.5 " +
            "M164 136.5 Q184 131.5 202 132 M164 142.5 Q182 143.5 198 148",
    )
private val WhiskerStroke = itemStroke(1.8f)

// Contact shadows where the head's dome passes in front of the ears.
private val EarShadowLeft = ovalRect(76f, 76f, 20f, 11f)
private val EarShadowRight = ovalRect(168f, 74f, 20f, 11f)

private val SeatedBody =
    SeatedAnimalBody(
        BodyPath,
        SeatedAnimalBody.Leg(
            haunch = ovalRect(64f, 241f, 21f, 22f),
            thigh = LegLeft,
            backPaw = BackPawLeft,
            frontPaw = FrontPawLeft,
            backShadow = ovalRect(66.5f, 252f, 15f, 5f),
            frontShadow = ovalRect(102.4f, 256.5f, 18f, 5.5f),
        ),
        SeatedAnimalBody.Leg(
            haunch = ovalRect(171f, 241f, 21f, 22f),
            thigh = LegRight,
            backPaw = BackPawRight,
            frontPaw = FrontPawRight,
            backShadow = ovalRect(176.7f, 252f, 15f, 5f),
            frontShadow = ovalRect(143.8f, 256.5f, 18f, 5.5f),
        ),
        Chest,
        ChinShadow,
    )
