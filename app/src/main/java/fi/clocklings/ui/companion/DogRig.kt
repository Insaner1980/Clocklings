// Literal coordinates place the dog in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
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
 * The dog as a shaded vinyl toy, after the reference: a wagging tail, a sitting body with haunches and
 * a cream belly, paws, a round head with a dark patch over the right eye, floppy dark ears, a cream
 * muzzle with a large glossy nose, a smile and a pink tongue. The body breathes around the feet, the
 * head tilts around the neck, and partDeg wags the tail and flops the right ear outward. The ears
 * hang in front of the head, so hats ([ItemLayer.FRONT] on [ArtPart.HEAD]) cover their roots.
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
            rotate(pose.partDeg, TailPivot) { drawPath(TailPath, fur.tail, style = TailStroke) }
            vinylOval(Body, fur.body)
            vinylOval(HaunchLeft, fur.haunchLeft)
            vinylOval(HaunchRight, fur.haunchRight)
            vinylShape(Belly)
            specular(BodyShine)
            contactShadow(PawShadowLeft)
            contactShadow(PawShadowRight)
            vinylOval(PawLeft, fur.pawLeft)
            vinylOval(PawRight, fur.pawRight)
            specular(PawLeft)
            specular(PawRight)
            drawPath(Toes, fur.shade, alpha = TOE_ALPHA, style = ToeStroke)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(fur, outfit, pose) }
        }
    }

    private fun DrawScope.head(
        fur: DogFur,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        vinylOval(Head, fur.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        drawPath(PatchPath, fur.patch)
        contactShadow(EarShadowLeft)
        contactShadow(EarShadowRight)
        drawPath(EarLeftPath, fur.earLeft)
        specular(EarLeftShine, EAR_SHINE_DEG)
        rotate(-pose.partDeg, EarRightPivot) {
            drawPath(EarRightPath, fur.earRight)
            specular(EarRightShine, -EAR_SHINE_DEG)
        }
        vinylShape(Muzzle)
        cheeks(CheekLeft, CheekRight)
        glossyEyes(EyeLeft, EyeRight, EYE_RX, EYE_RY, pose.blink)
        vinylShape(Tongue)
        drawPath(TongueLine, ArtTones.Pink.shade, style = TongueStroke)
        drawPath(Mouth, PersonalizationColors.EyeInk, style = MouthStroke)
        vinylShape(Nose)
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
    val shade: Color = tones.shade
    val tail: Brush = vinylBrush(TailBounds, tones)
    val body: Brush = vinylBrush(Body, tones)
    val haunchLeft: Brush = vinylBrush(HaunchLeft, tones)
    val haunchRight: Brush = vinylBrush(HaunchRight, tones)
    val pawLeft: Brush = vinylBrush(PawLeft, tones)
    val pawRight: Brush = vinylBrush(PawRight, tones)
    val head: Brush = vinylBrush(Head, tones)
    val patch: Brush = vinylBrush(PatchPath.getBounds(), marking)
    val earLeft: Brush = vinylBrush(EarLeftPath.getBounds(), marking)
    val earRight: Brush = vinylBrush(EarRightPath.getBounds(), marking)
}

private const val TOE_ALPHA = 0.7f
private const val HEAD_SHINE_DEG = -18f
private const val EAR_SHINE_DEG = 20f
private const val EYE_RX = 10f
private const val EYE_RY = 12.5f
private const val TAIL_WIDTH = 22f

private val TailPivot = Offset(164f, 252f)
private val Neck = Offset(120f, 160f)
private val EarRightPivot = Offset(156f, 56f)

private val Ground = ovalRect(120f, 284f, 92f, 11f)
private val HeadShadow = ovalRect(120f, 162f, 54f, 14f)

private val TailPath = svgPath("M160 254 C196 262 214 238 206 206")
private val TailBounds = TailPath.getBounds().inflate(TAIL_WIDTH / 2)
private val TailStroke = itemStroke(TAIL_WIDTH)

private val Body = ovalRect(120f, 212f, 60f, 60f)
private val HaunchLeft = ovalRect(74f, 250f, 24f, 28f)
private val HaunchRight = ovalRect(166f, 250f, 24f, 28f)
private val Belly = VinylShape(ovalPath(120f, 220f, 32f, 44f), ArtTones.Cream)
private val BodyShine = specularRect(98f, 184f, 11f, 6f)
private val PawLeft = ovalRect(94f, 272f, 24f, 14f)
private val PawRight = ovalRect(146f, 272f, 24f, 14f)
private val Toes = svgPath("M88 278 L88 270 M100 278 L100 270 M140 278 L140 270 M152 278 L152 270")
private val ToeStroke = itemStroke(2.5f)

/** The body, haunches and paws as one outline, for items that show only where they come out from behind the dog. */
internal val DogBodySilhouette: Path =
    Path().apply {
        addOval(Body)
        addOval(HaunchLeft)
        addOval(HaunchRight)
        addOval(PawLeft)
        addOval(PawRight)
    }

private val Head = ovalRect(120f, 106f, 76f, 62f)
private val HeadShine = specularRect(88f, 72f, 22f, 9f)
private val PatchPath =
    svgPath("M126 88 C124 70 146 62 164 68 C182 76 186 98 178 112 C170 124 150 126 138 118 C128 112 126 100 126 88 Z")

private val EarLeftPath =
    svgPath(
        "M92 54 C76 44 54 48 42 62 C26 80 20 116 26 146 C30 166 52 174 62 158 " +
            "C70 144 68 118 72 100 C76 82 84 66 92 54 Z",
    )
private val EarRightPath =
    svgPath(
        "M148 54 C164 44 186 48 198 62 C214 80 220 116 214 146 C210 166 188 174 178 158 " +
            "C170 144 172 118 168 100 C164 82 156 66 148 54 Z",
    )
private val EarLeftShine = specularRect(46f, 84f, 6f, 12f)
private val EarRightShine = specularRect(194f, 84f, 6f, 12f)

private val Muzzle = VinylShape(ovalPath(120f, 134f, 40f, 26f), ArtTones.Cream)
private val CheekLeft = ovalRect(80f, 134f, 11f, 7f)
private val CheekRight = ovalRect(160f, 134f, 11f, 7f)
private val EyeLeft = Offset(92f, 100f)
private val EyeRight = Offset(148f, 100f)
private val Nose = VinylShape(svgPath("M104 118 Q120 108 136 118 Q134 130 120 134 Q106 130 104 118 Z"), ArtTones.EyeInk)
private val NoseGlint = ovalRect(113f, 116f, 5f, 2.4f)
private val Mouth = svgPath("M120 133 L120 139 M106 138 Q113 146 120 139 Q127 146 134 138")
private val MouthStroke = itemStroke(2.5f)
private val Tongue = VinylShape(svgPath("M112 141 Q111 157 120 157 Q129 157 128 141 Z"), ArtTones.Pink)
private val TongueLine = svgPath("M120 144 L120 152")
private val TongueStroke = itemStroke(1.6f)

// Contact shadows where parts meet: above the paws and under the floppy ears.
private val PawShadowLeft = ovalRect(94f, 262f, 22f, 7f)
private val PawShadowRight = ovalRect(146f, 262f, 22f, 7f)
private val EarShadowLeft = ovalRect(80f, 100f, 14f, 36f)
private val EarShadowRight = ovalRect(160f, 100f, 14f, 36f)
