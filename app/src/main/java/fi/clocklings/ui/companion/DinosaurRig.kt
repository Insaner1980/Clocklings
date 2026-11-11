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
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.groundShadow
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
 * The dinosaur as a shaded vinyl toy, traced from the large three-quarter view of the owner's
 * reference sheet: standing upright and turned toward the viewer's right. The outlines of the head
 * with its long snout, the body with its arms and feet, the tail, the leaf-shaped spikes down the back
 * and the cream belly are [DinosaurTrace] paths; the arms, the feet, the claws and the darker spots are
 * forms placed where they were measured on the sheet.
 *
 * The body breathes around the feet, the head tilts around the neck and partDeg swings the tail up
 * around its root. Held items ([ArtPart.HAND]) are drawn before the arms, which hold them; the backpack
 * is drawn there too, so its shoulder strap runs under the back arm.
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
            "dinosaur-extra-swim-ring" to Rect(50f, 136f, 212f, 298f),
            "dinosaur-extra-ice-cream" to Rect(110f, 100f, 230f, 220f),
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
            rotate(pose.partDeg, TailPivot) {
                drawPath(TailSpikes, hide.tailSpikes)
                drawPath(TailPath, hide.tail)
                drawPath(TailSpots, hide.spots)
            }
            val iceCream = outfit[Slot.EXTRA] == ICE_CREAM
            drawPath(BackSpikes, hide.backSpikes)
            drawPath(BodyPath, hide.body)
            clipPath(BodyPath) {
                drawPath(BodySpots, hide.spots)
                vinylShape(Belly)
                // The belly is traced around the far hand of the plain pose; that hand now holds the cone,
                // so the belly is whole there and the cone lies against it.
                if (iceCream) drawOval(Belly.brush, BellyUnderHand.topLeft, BellyUnderHand.size)
                contactShadow(ChinShadow)
            }
            (if (iceCream) hide.holdingLimbs else hide.limbs).draw(this, outfit)
            // Only the fist shows, right of the cone: the arm stays hidden behind the cone and the body.
            if (iceCream) fist(hide)
            rotate(pose.headTiltDeg, Neck) { head(hide, outfit, pose.blink) }
            // The scoops stand in front of the snout: the ice cream is drawn again above the hand.
            if (iceCream) {
                clipRect(IceCreamScoops.left, IceCreamScoops.top, IceCreamScoops.right, IceCreamScoops.bottom) {
                    items(outfit, ArtPart.HAND, ItemLayer.FRONT)
                }
            }
        }
    }

    /** The fist around the cone, as measured on the sheet: the fingers' lobe below, the knuckles' lobe over it. */
    private fun DrawScope.fist(hide: DinosaurHide) {
        contactShadow(FistShadow)
        rotate(FIST_DEG, FistLower.center) { unitOval(FistLower, hide.dot) }
        rotate(FIST_DEG, FistUpper.center) { unitOval(FistUpper, hide.dot) }
        specular(FistUpper)
    }

    private fun DrawScope.head(
        hide: DinosaurHide,
        outfit: Outfit,
        blink: Float,
    ) {
        drawPath(HeadPath, hide.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        specular(SnoutShine)
        drawOval(ArtTones.CoolCheekColor, Cheek.topLeft, Cheek.size)
        drawPath(Brows, hide.crease, style = BrowStroke)
        rimmedEye(EyeNear, EYE_RX, EYE_RY, blink, ArtTones.EyeInk)
        rimmedEye(EyeFar, FAR_EYE_RX, FAR_EYE_RY, blink, ArtTones.EyeInk)
        drawPath(Nostrils, hide.crease)
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

    /** The brows and the nostrils: a deep tone of the hide. */
    val crease: Color = lerp(tones.shade, Color.Black, CREASE_DEPTH)
    val spots: Color = tones.shade
    val tail = vinylBrush(TailPath, tones)
    val body = vinylBrush(BodyPath, tones)
    val limbs = ClawedLimbs(ArmLeft to ArmRight, ArmStroke, FootLeft to FootRight, Claws, tones, armShadow = false)

    /** The limbs while the far hand holds the ice cream out beside the jaw. */
    val holdingLimbs =
        ClawedLimbs(ArmLeft to ArmTucked, ArmStroke, FootLeft to FootRight, Claws, tones, armShadow = false)
    val dot: Brush = unitBrush(tones)
    val head = vinylBrush(HeadPath, tones)
    val backSpikes = vinylBrush(BackSpikes, marking)
    val tailSpikes = vinylBrush(TailSpikes, marking)
}

private const val HEAD_SHINE_DEG = -20f
private const val EYE_RX = 12.5f
private const val EYE_RY = 13.8f
private const val FAR_EYE_RX = 8f
private const val FAR_EYE_RY = 12f
private const val ARM_WIDTH = 21f
private const val FIST_DEG = -24f
private const val CREASE_DEPTH = 0.15f

private val Neck = Offset(130f, 148f)
private val TailPivot = Offset(82f, 250f)

private val Ground = ovalRect(116f, 284f, 100f, 11f)

private val TailPath = svgPath(DinosaurTrace.TAIL)
private val TailSpikes =
    Path().apply {
        addPath(svgPath(DinosaurTrace.SPIKE_TAIL_ROOT))
        addPath(svgPath(DinosaurTrace.SPIKE_TAIL_TIP))
    }
private val TailSpots =
    Path().apply {
        addPath(ovalPath(64.8f, 240.7f, 6.5f, 5.5f))
        addPath(ovalPath(48.2f, 246.8f, 5f, 4.2f))
    }

/** The spikes behind the head and down the back: they stay with the body when the head tilts. */
private val BackSpikes =
    Path().apply {
        addPath(svgPath(DinosaurTrace.SPIKES_UPPER))
        addPath(svgPath(DinosaurTrace.SPIKE_BACK))
    }

private val BodyPath = svgPath(DinosaurTrace.BODY)
private val BodySpots =
    Path().apply {
        addPath(ovalPath(88.6f, 188.4f, 6f, 9f))
        addPath(ovalPath(87.2f, 214.2f, 7f, 11f))
    }

/** The cream belly: the traced part, continued down between the feet where the shading hid it from the trace. */
private val Belly =
    VinylShape(
        Path().apply { op(svgPath(DinosaurTrace.BELLY), ovalPath(147.5f, 237f, 28.5f, 28f), PathOperation.Union) },
        ArtTones.Cream,
    )
private val ChinShadow = ovalRect(134f, 150f, 36f, 9f)
private val BellyUnderHand = ovalRect(165f, 191f, 10f, 18f)

// The near arm lies across the belly; the far hand shows beside it.
private val ArmLeft = svgPath("M108 171 Q110 190 132 193")
private val ArmRight = svgPath("M172 176 Q178 182 176 190")

/** While the far hand holds the ice cream, the limbs' own far arm is a stub under the fist. */
private val ArmTucked = svgPath("M196 197.5 L196.5 198.5")

// The fist's two lobes, measured on the ice cream sheet.
private val FistUpper = ovalRect(197.4f, 193.8f, 10.4f, 8f)
private val FistLower = ovalRect(192.8f, 202f, 9.4f, 7.4f)
private val FistShadow = ovalRect(193f, 199f, 13f, 12f)
private const val ICE_CREAM = "dinosaur-extra-ice-cream"
private val ArmStroke = itemStroke(ARM_WIDTH)

private val FootLeft = ovalRect(101.6f, 268f, 26.7f, 14.5f)
private val FootRight = ovalRect(159.6f, 265.6f, 27.6f, 13.2f)

/** The body and feet as one outline, for items that show only where they come out from behind the dinosaur. */
internal val DinosaurBodySilhouette: Path = BodyPath

private val Claws =
    VinylShape(
        Path().apply {
            addPath(ovalPath(96.2f, 277f, 6f, 5.6f))
            addPath(ovalPath(110.3f, 276.6f, 6f, 5.6f))
            addPath(ovalPath(122f, 276f, 5.4f, 5.2f))
            addPath(ovalPath(167.5f, 274.4f, 5.5f, 5.2f))
            addPath(ovalPath(178.8f, 273f, 5f, 4.8f))
            addPath(ovalPath(186f, 272.1f, 3f, 4f))
        },
        ArtTones.Cream,
    )

private val HeadPath = svgPath(DinosaurTrace.HEAD)
private val HeadShine = specularRect(112f, 62f, 20f, 9f)
private val SnoutShine = specularRect(186f, 88f, 12f, 5f)

private val Cheek = ovalRect(115.5f, 116.6f, 11.5f, 7.5f)
private val Brows = svgPath("M101.5 76 Q104 66.5 116 65.5 M151.5 57 Q157 53 163 59")
private val BrowStroke = itemStroke(3.5f)
private val EyeNear = Offset(120f, 93f)
private val EyeFar = Offset(164.6f, 78.5f)
private val Nostrils =
    Path().apply {
        addPath(ovalPath(192.2f, 95.3f, 3.5f, 4f))
        addPath(ovalPath(207.5f, 93f, 2.2f, 3.6f))
    }
private val Mouth = svgPath("M144.5 119.5 Q168.5 139.5 202.5 114.5")
private val MouthStroke = itemStroke(2.5f)
