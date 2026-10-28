// Literal coordinates place the bunny in its 240 x 300 design grid.
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
 * The bunny as a shaded vinyl toy, after the reference: a sitting body with a pearl belly, small front
 * paws, big feet showing pink pads, long floppy ears with pink inner sides hanging beside a large
 * round head, small brows, glossy eyes, a pearl muzzle with a pink nose and a "w" mouth, and big
 * cheeks. The body breathes around the feet, the head tilts around the neck and partDeg swings the
 * ears. The ears hang behind the head, so hats ([ItemLayer.FRONT] on [ArtPart.HEAD]) cover their roots.
 * Held items ([ArtPart.HAND]) are drawn before the front paws, which hold them.
 */
internal object BunnyRig : AnimalRig(
    // The crops keep every item of the slot whole: the head crop takes in the hanging ears, the neck
    // crop the locket and the cape clasp.
    headCrop = Rect(20f, 0f, 220f, 176f),
    neckCrop = Rect(60f, 160f, 180f, 240f),
) {
    /** Fur brushes for every companion color, built once and indexed by the color's ordinal. */
    private val furs = CompanionColor.entries.map { BunnyFur(it) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val fur = furs[color.ordinal]
        posedAnimal(pose, outfit, Ground) {
            vinylOval(Body, fur.body)
            vinylShape(Belly)
            specular(BodyShine)
            contactShadow(FootShadowLeft)
            contactShadow(FootShadowRight)
            vinylOval(FootLeft, fur.footLeft)
            vinylOval(FootRight, fur.footRight)
            specular(FootLeft)
            specular(FootRight)
            vinylShape(Pads)
            items(outfit, ArtPart.HAND, ItemLayer.FRONT)
            contactShadow(PawShadowLeft)
            contactShadow(PawShadowRight)
            vinylShape(PawLeft)
            vinylShape(PawRight)
            drawPath(Fingers, fur.shade, alpha = FINGER_ALPHA, style = FingerStroke)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(fur, outfit, pose) }
        }
    }

    private fun DrawScope.head(
        fur: BunnyFur,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        rotate(pose.partDeg, EarLeftPivot) {
            drawPath(EarLeftPath, fur.earLeft)
            vinylShape(InnerEarLeft)
            specular(EarLeftShine, EAR_SHINE_DEG)
        }
        rotate(-pose.partDeg, EarRightPivot) {
            drawPath(EarRightPath, fur.earRight)
            vinylShape(InnerEarRight)
            specular(EarRightShine, -EAR_SHINE_DEG)
        }
        contactShadow(EarShadowLeft)
        contactShadow(EarShadowRight)
        drawPath(Tuft, fur.head)
        vinylOval(Head, fur.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        vinylShape(Muzzle)
        cheeks(CheekLeft, CheekRight, ArtTones.CoolCheekColor)
        glossyEyes(EyeLeft, EyeRight, EYE_RX, EYE_RY, pose.blink)
        vinylShape(Nose)
        drawOval(ArtTones.NoseShine, NoseGlint.topLeft, NoseGlint.size)
        drawPath(Philtrum, PersonalizationColors.EyeInk, style = MouthStroke)
        vinylShape(Mouth)
        vinylShape(Tongue)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }
}

/** One fur color's brushes. */
private class BunnyFur(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.BUNNY, color))
    val shade: Color = tones.shade
    val body: Brush = vinylBrush(Body, tones)
    val footLeft: Brush = vinylBrush(FootLeft, tones)
    val footRight: Brush = vinylBrush(FootRight, tones)
    val head: Brush = vinylBrush(Head, tones)
    val earLeft: Brush = vinylBrush(EarLeftPath.getBounds(), tones)
    val earRight: Brush = vinylBrush(EarRightPath.getBounds(), tones)
}

private const val FINGER_ALPHA = 0.55f
private const val HEAD_SHINE_DEG = -18f
private const val EAR_SHINE_DEG = 16f
private const val EYE_RX = 12.5f
private const val EYE_RY = 14.5f

private val Neck = Offset(120f, 172f)
private val EarLeftPivot = Offset(82f, 62f)
private val EarRightPivot = Offset(158f, 62f)

private val Ground = ovalRect(120f, 284f, 96f, 11f)
private val HeadShadow = ovalRect(120f, 176f, 48f, 12f)

private val Body = ovalRect(120f, 224f, 48f, 50f)
private val Belly = VinylShape(ovalPath(120f, 232f, 27f, 34f), ArtTones.Pearl)
private val BodyShine = specularRect(102f, 196f, 9f, 5f)
private val FootLeft = ovalRect(72f, 260f, 28f, 24f)
private val FootRight = ovalRect(168f, 260f, 28f, 24f)
private val Pads =
    VinylShape(
        Path().apply {
            for (x in floatArrayOf(72f, 168f)) {
                addOval(ovalRect(x, 266f, 11f, 9f))
                addOval(ovalRect(x - 12f, 250f, 4.5f))
                addOval(ovalRect(x, 246f, 4.5f))
                addOval(ovalRect(x + 12f, 250f, 4.5f))
            }
        },
        ArtTones.Pink,
    )
private val PawLeft = VinylShape(ovalPath(105f, 258f, 14f, 16f), ArtTones.Pearl)
private val PawRight = VinylShape(ovalPath(135f, 258f, 14f, 16f), ArtTones.Pearl)
private val Fingers = svgPath("M100 268 L100 262 M110 268 L110 262 M130 268 L130 262 M140 268 L140 262")
private val FingerStroke = itemStroke(2f)

private val Head = ovalRect(120f, 112f, 72f, 66f)
private val HeadShine = specularRect(88f, 72f, 24f, 10f)

/** A little curl of fur on top of the head, drawn in the head's own brush before the head. */
private val Tuft = svgPath("M106 54 Q104 36 118 38 Q114 46 122 50 Q126 36 138 42 Q130 46 134 56 Z")

private val EarLeftPath =
    svgPath("M86 60 C66 30 30 34 18 72 C6 110 12 168 30 192 C44 208 66 196 62 172 C58 146 56 116 70 88 Z")
private val EarRightPath =
    svgPath("M154 60 C174 30 210 34 222 72 C234 110 228 168 210 192 C196 208 174 196 178 172 C182 146 184 116 170 88 Z")
private val InnerEarLeft =
    VinylShape(
        svgPath("M76 70 C62 50 40 54 30 82 C22 112 26 158 38 178 C46 190 56 182 54 166 C52 140 52 112 64 90 Z"),
        ArtTones.Pink,
    )
private val InnerEarRight =
    VinylShape(
        svgPath(
            "M164 70 C178 50 200 54 210 82 C218 112 214 158 202 178 C194 190 184 182 186 166 C188 140 188 112 176 90 Z",
        ),
        ArtTones.Pink,
    )
private val EarLeftShine = specularRect(28f, 82f, 6f, 12f)
private val EarRightShine = specularRect(212f, 82f, 6f, 12f)

private val Muzzle = VinylShape(ovalPath(120f, 148f, 24f, 17f), ArtTones.Pearl)
private val CheekLeft = ovalRect(80f, 140f, 14f, 9f)
private val CheekRight = ovalRect(160f, 140f, 14f, 9f)
private val EyeLeft = Offset(92f, 119f)
private val EyeRight = Offset(148f, 119f)
private val Nose = VinylShape(svgPath("M113 139 Q120 134 127 139 Q125 145 120 146 Q115 145 113 139 Z"), ArtTones.Pink)
private val NoseGlint = ovalRect(118f, 138.5f, 2.6f, 1.4f)

/** A happy open smile under the nose, with a pink tongue. */
private val Philtrum = svgPath("M120 146 L120 149")
private val Mouth =
    VinylShape(svgPath("M110 149 Q120 153 130 149 Q128 162 120 162 Q112 162 110 149 Z"), ArtTones.EyeInk)
private val Tongue = VinylShape(svgPath("M114 157 Q120 153 126 157 Q124 161 120 161 Q116 161 114 157 Z"), ArtTones.Pink)
private val MouthStroke = itemStroke(2.5f)

// Contact shadows where parts meet: above the feet and paws and at the ear roots.
private val FootShadowLeft = ovalRect(74f, 244f, 24f, 8f)
private val FootShadowRight = ovalRect(166f, 244f, 24f, 8f)
private val PawShadowLeft = ovalRect(105f, 248f, 13f, 6f)
private val PawShadowRight = ovalRect(135f, 248f, 13f, 6f)
private val EarShadowLeft = ovalRect(86f, 62f, 16f, 8f)
private val EarShadowRight = ovalRect(154f, 62f, 16f, 8f)
