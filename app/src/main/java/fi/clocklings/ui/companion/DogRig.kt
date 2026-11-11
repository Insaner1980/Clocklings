// Literal coordinates were measured on the reference sheet in the 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.ovalPath
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
 * The dog as a shaded vinyl toy, traced from the front view of the owner's reference sheet: the
 * outlines of the head, the long hanging ears, the patch over the right eye, the sitting body, the
 * tail, the nose with the mouth and the tongue are [DogTrace] paths. Inside the body's outline the
 * haunches, the front legs and the four paws are forms of their own, placed on creases measured on the
 * sheet and clipped to the traced outline.
 *
 * The body breathes around the feet, the head tilts around the neck, and partDeg wags the tail and
 * flops the right ear outward. The ears hang in front of the head, so they are drawn after it.
 */
internal object DogRig : AnimalRig(
    // The crops keep every item of the slot whole while leaving out the tail and the mouth and tongue.
    headCrop = Rect(20f, 0f, 220f, 174f),
    neckCrop = Rect(66f, 158f, 174f, 238f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "dog-extra-ball" to Rect(60f, 180f, 180f, 300f),
            "dog-extra-frisbee" to Rect(60f, 84f, 180f, 204f),
            "dog-extra-boots" to Rect(57f, 174f, 183f, 300f),
            "dog-extra-backpack" to Rect(14f, 116f, 185f, 287f),
            "dog-extra-raincoat" to Rect(40f, 128f, 200f, 288f),
        ),
) {
    /** Fur brushes for every companion color, built once and indexed by the color's ordinal. */
    private val furs = CompanionColor.entries.map { DogFur(it) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val fur = furs[color.ordinal]
        posedAnimal(pose, outfit, Ground) {
            unitOval(TailRoot, fur.dot)
            rotate(pose.partDeg, TailPivot) { drawPath(TailPath, fur.tail) }
            fur.body.draw(this, includeFrontPaws = true)
            specular(BodyShine)
            specular(FrontPawLeftBounds)
            specular(FrontPawRightBounds)
            drawPath(Toes, fur.crease, alpha = TOE_ALPHA, style = ToeStroke)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.headTiltDeg, Neck) { head(fur, outfit, pose) }
        }
    }

    private fun DrawScope.head(
        fur: DogFur,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        drawPath(HeadPath, fur.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        clipPath(HeadPath) {
            drawPath(PatchPath, fur.patch)
            contactShadow(EarShadowLeft)
            contactShadow(EarShadowRight)
        }
        drawPath(EarLeft, fur.earLeft)
        specular(EarLeftShine, EAR_SHINE_DEG)
        rotate(-pose.partDeg, EarRightPivot) {
            drawPath(EarRight, fur.earRight)
            specular(EarRightShine, -EAR_SHINE_DEG)
        }
        vinylShape(Muzzle)
        cheeks(CheekLeft, CheekRight)
        drawPath(Brows, fur.brow, style = BrowStroke)
        rimmedEye(EyeLeft, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
        rimmedEye(EyeRight, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
        vinylShape(Tongue)
        drawPath(TongueLine, ArtTones.Pink.shade, style = TongueStroke)
        drawPath(Nose, PersonalizationColors.Cocoa)
        drawOval(ArtTones.NoseShine, NoseGlint.topLeft, NoseGlint.size)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }
}

/** One fur color's brushes: the fur itself and the darker ears and eye patch. */
private class DogFur(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.DOG, color))
    private val marking = vinylTones(PersonalizationColors.dogMarking(color))

    /** The toe lines: a deep tone of the fur. */
    val crease: Color = lerp(tones.shade, Color.Black, CREASE_DEPTH)

    /** The brows: the marking color, deepened so the right brow still shows on the eye patch. */
    val brow: Color = lerp(marking.shade, Color.Black, BROW_DEPTH)
    val dot: Brush = unitBrush(tones)
    val tail = vinylBrush(TailPath, tones)
    val body = SeatedBody.Paint(tones, dot)
    val head = vinylBrush(HeadPath, tones)
    val patch = vinylBrush(PatchPath, marking)
    val earLeft = vinylBrush(EarLeft, marking)
    val earRight = vinylBrush(EarRight, marking)
}

private const val BEYOND_CHEEK_RIGHT =
    "M186 110 C187 125 185 137 180 143.5 C175 149.5 166 156.5 157 162.5 C153 165 147 167.5 140 169.5 " +
        "V180 H215 V110 Z"
private const val TOE_ALPHA = 0.8f
private const val CREASE_DEPTH = 0.12f
private const val BROW_DEPTH = 0.25f
private const val HEAD_SHINE_DEG = -18f
private const val EAR_SHINE_DEG = 20f
private const val EYE_RX = 14.5f
private const val EYE_RY = 16.5f

private val TailPivot = Offset(189f, 251f)

/** The tail's root behind the haunch: it stays put and fills the joint when the tail wags. */
private val TailRoot = ovalRect(189f, 236f, 11f, 22f)
private val Neck = Offset(120f, 160f)
private val EarRightPivot = Offset(162f, 52f)

private val Ground = ovalRect(120f, 285f, 92f, 11f)

private val TailPath = svgPath(DogTrace.TAIL)

private val BodyPath = svgPath(DogTrace.BODY)
private val BodyShine = specularRect(90f, 196f, 8f, 7f)
private val ChinShadow = ovalRect(121f, 169f, 46f, 12f)

/** The body with its haunches and paws as one outline, for items that show only where they come out from behind it. */
internal val DogBodySilhouette: Path = BodyPath

// The front legs from the shoulders to the paws. Their inner edges run inside the chest patch, which is
// drawn over them, so the patch's own outline is the visible edge.
private val LegLeft =
    svgPath(
        "M84 164 C76 180 70 198 69 212 C70 228 73 244 75 262 L117 262 C115 250 113 242 111 234.7 " +
            "C108 224 103 213 103 203 C103 195 108 187 113 182 L100 166 Z",
    )
private val LegRight =
    svgPath(
        "M158.4 164 C166.4 180 172.4 198 173.4 212 C172.4 228 169.4 244 167.4 262 L125.4 262 " +
            "C127.4 250 129.4 242 131.4 234.7 C134.4 224 139.4 213 139.4 203 " +
            "C139.4 195 134.4 187 129.4 182 L142.4 166 Z",
    )

/** The cream chest patch: round at the top and tapering to a point between the front paws. */
private val Chest =
    VinylShape(
        svgPath(
            "M121.2 179 C133 179 142.5 190 142.4 203 C142.3 214 138 225 134.2 234.7 " +
                "C131.5 243 129.5 250 128 257.4 C126 263 123.5 268 121.2 271 " +
                "C119 268 116.5 263 114.4 257.4 C113 250 111 243 108.7 234.7 " +
                "C105 225 100.2 214 100 203 C100 190 109.5 179 121.2 179 Z",
        ),
        ArtTones.Cream,
    )

private val BackPawLeft = pawPath(46.9f, 252.9f, 74.6f, 279f)
private val BackPawRight = pawPath(166.7f, 252.9f, 193.3f, 279f)
private val FrontPawLeft = pawPath(73f, 257.4f, 116.2f, 284.4f)
private val FrontPawRight = pawPath(125.8f, 257.4f, 169f, 284.4f)
private val FrontPawLeftBounds = FrontPawLeft.getBounds()
private val FrontPawRightBounds = FrontPawRight.getBounds()
private val Toes =
    svgPath(
        "M56 260.8 L56 272.2 M68.2 260.8 L68.2 272.2 M84.8 265.4 L84.8 278 M101.4 265.4 L101.4 278 " +
            "M138.7 265.4 L138.7 278 M155.3 265.4 L155.3 278 M174.2 260.8 L174.2 272.2 M185.6 260.8 L185.6 272.2",
    )
private val ToeStroke = itemStroke(2.2f)

/**
 * The skull alone. The traced head outline runs around the hanging ears too, so it is cut to the
 * skull's oval, the wider jaw under the ears and the crown between them: no fur-colored ear shape is
 * left behind the ears. The right cheek ends on the round curve measured on the reference, from under
 * the ear down to the neck: the trace ran straight there, into the corner where the ear met the cheek.
 */
private val HeadPath =
    listOf(
        ovalPath(120f, 108f, 66f, 66f),
        ovalPath(120f, 132f, 69f, 44f),
        Path().apply { addRect(Rect(84f, 30f, 158f, 100f)) },
    ).reduce { skull, part -> Path.combine(PathOperation.Union, skull, part) }
        .let { Path.combine(PathOperation.Intersect, svgPath(DogTrace.HEAD), it) }
        .let { Path.combine(PathOperation.Difference, it, svgPath(BEYOND_CHEEK_RIGHT)) }
private val HeadShine = specularRect(100f, 62f, 22f, 8f)
private val PatchPath = svgPath(DogTrace.PATCH)
private val EarLeft = svgPath(DogTrace.EAR_LEFT)
private val EarRight = svgPath(DogTrace.EAR_RIGHT)
private val EarLeftShine = specularRect(38f, 84f, 6f, 14f)
private val EarRightShine = specularRect(200f, 84f, 6f, 14f)

/** The cream muzzle: its width and height are the measured ones, its edge is drawn smooth under the eyes. */
private val Muzzle =
    VinylShape(
        svgPath(
            "M79.5 138 C79.5 128 88 121.5 98 121 C106 120.5 110 110 121.5 110 C133 110 137 120.5 145 121 " +
                "C155 121.5 164.5 128 164.5 138 C164.5 155 146 166.5 122 166.5 C98 166.5 79.5 155 79.5 138 Z",
        ),
        ArtTones.Cream,
    )
private val CheekLeft = ovalRect(73.5f, 129f, 9f, 6.5f)
private val CheekRight = ovalRect(168.3f, 129.6f, 9f, 6.5f)
private val Brows = svgPath("M81 74.5 Q87 66 96.5 68.5 M146 67.5 Q155 65 162.5 73.5")
private val BrowStroke = itemStroke(3.5f)
private val EyeLeft = Offset(90.6f, 101f)
private val EyeRight = Offset(152.6f, 101f)
private val Nose = svgPath(DogTrace.NOSE)
private val NoseGlint = ovalRect(116f, 121f, 5f, 2.2f)
private val Tongue = VinylShape(svgPath(DogTrace.TONGUE), ArtTones.Pink)
private val TongueLine = svgPath("M121.8 150 L121.8 156")
private val TongueStroke = itemStroke(1.4f)

// Contact shadows under the hanging ears.
private val EarShadowLeft = ovalRect(70f, 110f, 12f, 44f)
private val EarShadowRight = ovalRect(174f, 110f, 12f, 44f)

private val SeatedBody =
    SeatedAnimalBody(
        BodyPath,
        SeatedAnimalBody.Leg(
            haunch = ovalRect(58f, 238f, 19f, 23f),
            thigh = LegLeft,
            backPaw = BackPawLeft,
            frontPaw = FrontPawLeft,
            backShadow = ovalRect(60.7f, 253.5f, 13f, 5f),
            frontShadow = ovalRect(94.6f, 258f, 19f, 5.5f),
        ),
        SeatedAnimalBody.Leg(
            haunch = ovalRect(184f, 238f, 19f, 23f),
            thigh = LegRight,
            backPaw = BackPawRight,
            frontPaw = FrontPawRight,
            backShadow = ovalRect(180f, 253.5f, 13f, 5f),
            frontShadow = ovalRect(147.4f, 258f, 19f, 5.5f),
        ),
        Chest,
        ChinShadow,
    )
