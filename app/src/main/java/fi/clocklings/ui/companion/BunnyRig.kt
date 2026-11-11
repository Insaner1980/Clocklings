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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import fi.clocklings.ui.art.softRim
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
 * The bunny as a shaded vinyl toy, traced from the front view of the owner's reference sheet: the
 * outlines of the head with its tuft, the long lop ears and their pink insides, the sitting body with
 * its feet and round tail, and the nose are [BunnyTrace] paths. Inside the body's outline the tail,
 * the torso, the feet, the arms and the hands are forms of their own, placed on creases measured on
 * the sheet and clipped to the traced outline.
 *
 * The body breathes around the feet, the head tilts around the neck, partDeg sways the ears outward
 * and the eyes blink. The ears hang in front of the head, so they are drawn after it. Items held in
 * the hands ([ArtPart.HAND]) are drawn before the hands.
 */
internal object BunnyRig : AnimalRig(
    headCrop = Rect(20f, 0f, 220f, 176f),
    neckCrop = Rect(60f, 160f, 180f, 240f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "bunny-extra-carrot" to Rect(59f, 170f, 179f, 290f),
            "bunny-extra-basket" to Rect(120f, 180f, 240f, 300f),
            "bunny-extra-watering-can" to Rect(81f, 167f, 201f, 287f),
            "bunny-extra-bubbles" to Rect(110f, 163f, 240f, 293f),
            "bunny-extra-bouquet" to Rect(60f, 162f, 180f, 282f),
            "bunny-extra-wings" to Rect(0f, 50f, 240f, 290f),
        ),
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
            drawPath(BodyPath, fur.body)
            clipPath(BodyPath) {
                drawOval(fur.tail, Tail.topLeft, Tail.size)
                contactShadow(TailShadow)
                drawPath(HipRight, fur.body)
                unitOval(LapShadow, fur.lap)
                drawPath(Belly, fur.belly)
                contactShadow(ChinShadow)
                contactShadow(ArmShadowLeft)
                contactShadow(ArmShadowRight)
                contactShadow(FootShadowLeft)
                contactShadow(FootShadowRight)
            }
            // The feet are whole measured ovals: the traced outline would cut their soles flat.
            unitOval(FootLeft, fur.foot)
            unitOval(FootRight, fur.foot)
            drawPath(Pads, PadBrush)
            arms(fur)
            items(outfit, ArtPart.HAND, ItemLayer.FRONT)
            // The hands again, so they close over what they hold.
            clipRect(top = HANDS_TOP) { arms(fur) }
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.headTiltDeg, Neck) { head(fur, outfit, pose) }
        }
    }

    /** The arms, each ending in a paler hand, and the finger lines. */
    private fun DrawScope.arms(fur: BunnyFur) =
        clipPath(BodyPath) {
            arm(ArmLeft, fur.armLeft, HandLeft, fur)
            arm(ArmRight, fur.armRight, HandRight, fur)
            drawPath(Fingers, fur.crease, alpha = FINGER_ALPHA, style = FingerStroke)
        }

    /**
     * One arm: the soft shade it casts around itself, its own form with a shaded
     * rim that rounds it, and the pale tint of the hand.
     */
    private fun DrawScope.arm(
        path: Path,
        brush: Brush,
        hand: Rect,
        fur: BunnyFur,
    ) {
        for (i in ArmShadeStrokes.indices) drawPath(path, fur.armShade, style = ArmShadeStrokes[i])
        drawPath(path, brush)
        softRim(path, fur.rim)
        clipPath(path) { unitOval(hand, fur.hand) }
    }

    private fun DrawScope.head(
        fur: BunnyFur,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        drawPath(HeadPath, fur.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        rotate(pose.partDeg, EarLeftPivot) {
            drawPath(EarLeft, fur.earLeft)
            vinylShape(InnerEarLeft)
            specular(EarLeftShine, EAR_SHINE_DEG)
        }
        rotate(-pose.partDeg, EarRightPivot) {
            drawPath(EarRight, fur.earRight)
            vinylShape(InnerEarRight)
            specular(EarRightShine, -EAR_SHINE_DEG)
        }
        drawPath(Muzzle, fur.muzzle)
        cheeks(CheekLeft, CheekRight, ArtTones.CoolCheekColor)
        drawPath(Brows, fur.crease, style = BrowStroke)
        rimmedEye(EyeLeft, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
        rimmedEye(EyeRight, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
        vinylShape(Nose)
        drawOval(ArtTones.NoseShine, NoseGlint.topLeft, NoseGlint.size)
        drawPath(Philtrum, PersonalizationColors.Cocoa, style = MouthStroke)
        drawPath(Mouth, PersonalizationColors.Cocoa)
        clipPath(Mouth) { vinylShape(Tongue) }
        vinylShape(Teeth)
        drawPath(ToothGap, ArtTones.Pearl.shade, style = ToothStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }
}

/** One fur color's brushes. */
private class BunnyFur(
    color: CompanionColor,
) {
    private val base = PersonalizationColors.companion(CompanionKind.BUNNY, color)
    private val tones = vinylTones(base)

    /** The belly, the muzzle and the hands: the fur's own pale tint. */
    private val pale = lerp(base, Color.White, PALE_MIX)
    private val paleTones = vinylTones(pale)

    private fun brush(path: Path): Brush = vinylBrush(path.getBounds(), tones)

    /** The brows and the finger lines: a deep tone of the fur. */
    val crease: Color = lerp(tones.shade, Color.Black, CREASE_DEPTH)

    /** One step of the shade an arm casts around itself, and one step of the rim that rounds it. */
    val armShade: Color = crease.copy(alpha = ARM_SHADE_ALPHA)
    val rim: Color = tones.shade.copy(alpha = RIM_ALPHA)
    val dot: Brush = unitBrush(tones)
    val tail = vinylBrush(TailLight, tones)
    val foot: Brush = unitBrush(vinylTones(lerp(base, Color.White, FOOT_MIX)))
    val belly = vinylBrush(Belly.getBounds(), paleTones)
    val muzzle = vinylBrush(Muzzle.getBounds(), paleTones)

    /** The lap between the feet: the fur's own warm shade, fading out at its rim. */
    val lap: Brush = fadeBrush(tones.shade.copy(alpha = LAP_ALPHA), 0f)

    /** The pale tint in the middle of the hand, fading into the arm at its rim. */
    val hand: Brush = fadeBrush(pale, HAND_CORE)

    /** [color] up to [core] of the unit box's radius, then fading to nothing at its rim. */
    private fun fadeBrush(
        color: Color,
        core: Float,
    ): Brush =
        Brush.radialGradient(
            0f to color,
            core to color,
            1f to color.copy(alpha = 0f),
            center = Offset(0.5f, 0.5f),
            radius = 0.5f,
        )

    val body = brush(BodyPath)
    val armLeft = brush(ArmLeft)
    val armRight = brush(ArmRight)
    val head = brush(HeadPath)
    val earLeft = brush(EarLeft)
    val earRight = brush(EarRight)
}

/** The smooth left hip, where the trace was cut against the hanging ear. */
private const val HIP_LEFT = "M77 202 C69 212 61.5 226 59.5 240 L62 256 L84 250 L90 215 Z"
private const val FINGER_ALPHA = 0.5f
private const val ARM_SHADE_ALPHA = 0.1f
private const val RIM_ALPHA = 0.08f
private const val PALE_MIX = 0.5f
private const val FOOT_MIX = 0.22f
private const val HAND_CORE = 0.6f
private const val LAP_ALPHA = 0.55f
private const val HANDS_TOP = 224f
private const val CREASE_DEPTH = 0.1f
private const val HEAD_SHINE_DEG = -18f
private const val EAR_SHINE_DEG = 16f
private const val EYE_RX = 15f
private const val EYE_RY = 15.6f

private val Neck = Offset(120f, 172f)
private val EarLeftPivot = Offset(84f, 58f)
private val EarRightPivot = Offset(156f, 58f)

private val Ground = ovalRect(120f, 284f, 96f, 11f)

/**
 * The traced outline with a smooth left hip, cut off below the tops of the feet: there only the lap
 * shows between them, and the feet are ovals of their own.
 */
private val BodyPath =
    Path.combine(
        PathOperation.Difference,
        Path.combine(PathOperation.Union, svgPath(BunnyTrace.BODY), svgPath(HIP_LEFT)),
        svgPath("M30 262 H100 V270 Q121 283 140 270 V262 H215 V295 H30 Z"),
    )
private val ChinShadow = ovalRect(121.5f, 174f, 44f, 11f)

// Every part below is a form of its own, in the place measured on the reference, and the traced
// outline cuts their outer sides.

// The round tail, and the right hip in front of it.
private val Tail = ovalRect(186.2f, 238.8f, 16.5f, 23f)

/** The box the tail's brush is fitted to: shifted right, so the light falls on the part the hip leaves in view. */
private val TailLight = Rect(178f, 214f, 214f, 264f)
private val TailShadow = ovalRect(179f, 238f, 9f, 22f)
private val HipRight = svgPath("M120 200 H150 C160 204 168 214 172 224 C176 232 179 241 180 252 V290 H120 Z")

/** The paler belly: round under the chin and narrowing to a point between the hands. */
private val Belly =
    svgPath(
        "M121.3 176.5 C134 176.5 143 184 142.8 192 C142.5 203 131 214 121.3 225 " +
            "C111.5 214 100 203 100 192 C100 184 109 176.5 121.3 176.5 Z",
    )

// Each chubby arm hangs from the shoulder, crosses the belly and ends in a hand; the hands meet in the
// middle. The right arm mirrors the left around x = 121.4.
private val ArmLeft =
    svgPath(
        "M92 170 C84 180 76 192 75 206 C74.5 218 81 229 90 237 C96 243 103 247.5 110 247.5 " +
            "C116 247.5 121.5 244.5 122.2 238 C122.4 232 122 228 121.5 225.5 C114 218 104 204 99.5 190 " +
            "C98 184 96 177 92 170 Z",
    )
private val ArmRight =
    svgPath(
        "M150.8 170 C158.8 180 166.8 192 167.8 206 C168.3 218 161.8 229 152.8 237 " +
            "C146.8 243 139.8 247.5 132.8 247.5 C126.8 247.5 121.3 244.5 120.6 238 C120.4 232 120.8 228 121.3 225.5 " +
            "C128.8 218 138.8 204 143.3 190 C144.8 184 146.8 177 150.8 170 Z",
    )
private val ArmShadeStrokes = listOf(8f, 5.5f, 3.5f, 2f).map { itemStroke(it) }
private val HandLeft = ovalRect(106f, 235.5f, 21f, 17f)
private val HandRight = ovalRect(136.8f, 235.5f, 21f, 17f)
private val Fingers =
    svgPath(
        "M103.9 234 Q104.2 239 105.6 243.5 M114.5 232 Q115 237 116.6 241.5 " +
            "M131.5 232 Q131 237 129.4 241.5 M142.1 234 Q141.8 239 140.4 243.5",
    )
private val FingerStroke = itemStroke(1.1f)

// The shades the arms cast on the hips, the hands on the lap and the feet on the body.
private val ArmShadowLeft = ovalRect(76f, 232f, 13f, 15f)
private val ArmShadowRight = ovalRect(166.8f, 232f, 13f, 15f)
private val LapShadow = ovalRect(121.4f, 258f, 30f, 24f)
private val FootShadowLeft = ovalRect(79f, 259f, 27f, 25f)
private val FootShadowRight = ovalRect(162.5f, 259f, 27f, 25f)

// The feet point at the viewer and show their pads.
private val FootLeft = ovalRect(76.8f, 259.8f, 26.3f, 23.3f)
private val FootRight = ovalRect(164.5f, 259.5f, 27.3f, 23.5f)
private val Pads =
    Path().apply {
        addPath(svgPath("M69.9 265.4 A12 9.6 15 1 0 93.1 271.6 A12 9.6 15 1 0 69.9 265.4 Z"))
        addOval(ovalRect(60.1f, 258.4f, 4.6f))
        addOval(ovalRect(68.4f, 248.8f, 5f))
        addOval(ovalRect(82.1f, 248.8f, 4.7f))
        addPath(svgPath("M151.4 271.1 A12 9.6 -15 1 0 174.6 264.9 A12 9.6 -15 1 0 151.4 271.1 Z"))
        addOval(ovalRect(162.5f, 248.8f, 4.7f))
        addOval(ovalRect(176f, 249.7f, 5f))
        addOval(ovalRect(183.6f, 258.2f, 4.6f))
    }

/** The pads are flat on the soles: lighter toward the toes, with no gloss of their own. */
private val PadBrush =
    Brush.verticalGradient(listOf(ArtTones.Salmon.light, ArtTones.Salmon.base), startY = 236f, endY = 272f)

private val HeadPath = svgPath(BunnyTrace.HEAD)
private val HeadShine = specularRect(92f, 70f, 22f, 9f)
private val EarLeft = svgPath(BunnyTrace.EAR_LEFT)
private val EarRight = svgPath(BunnyTrace.EAR_RIGHT)
private val InnerEarLeft = VinylShape(svgPath(BunnyTrace.INNER_LEFT), ArtTones.Salmon)
private val InnerEarRight = VinylShape(svgPath(BunnyTrace.INNER_RIGHT), ArtTones.Salmon)
private val EarLeftShine = specularRect(22f, 90f, 6f, 14f)
private val EarRightShine = specularRect(212f, 86f, 6f, 14f)

/** The lighter muzzle around the nose and the mouth. */
private val Muzzle = ovalPath(121.8f, 142.6f, 26.4f, 20.6f)
private val CheekLeft = ovalRect(73.6f, 135.8f, 12f, 8f)
private val CheekRight = ovalRect(167.5f, 134.8f, 12f, 8f)
private val Brows = svgPath("M78.5 84.5 Q84 76.5 95.5 76 M145 76 Q156.5 76.5 161 84.5")
private val BrowStroke = itemStroke(3f)
private val EyeLeft = Offset(87.2f, 113f)
private val EyeRight = Offset(153.9f, 112.5f)
private val Nose = VinylShape(svgPath(BunnyTrace.NOSE), ArtTones.Pink)
private val NoseGlint = ovalRect(119.5f, 128.5f, 2.6f, 1.3f)

/** A happy open smile under the nose, with two front teeth and a pink tongue. */
private val Philtrum = svgPath("M121.8 135 L121.7 141")
private val Mouth = svgPath("M105.5 139.5 Q121.5 146 137.5 139.5 Q136 154.5 121.5 154.5 Q107 154.5 105.5 139.5 Z")
private val Tongue = VinylShape(ovalPath(121.5f, 152.5f, 8f, 4f), ArtTones.Pink)
private val Teeth =
    VinylShape(
        svgPath("M114.7 142.2 H128.8 V146.5 Q128.8 148.2 127 148.2 H116.5 Q114.7 148.2 114.7 146.5 Z"),
        ArtTones.Pearl,
    )
private val ToothGap = svgPath("M121.75 142.6 L121.75 147.8")
private val ToothStroke = itemStroke(0.9f)
private val MouthStroke = itemStroke(2f)
