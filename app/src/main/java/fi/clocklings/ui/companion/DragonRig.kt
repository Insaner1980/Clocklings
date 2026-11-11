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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VioletIris
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.rimmedEye
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The dragon as a shaded vinyl toy, traced from the front view of the owner's reference sheet: the
 * outlines of the standing body with its arms and feet, the membrane wings, the ear frills, the horns,
 * the tail and its leaf-shaped tip are [DragonTrace] paths. The head is the ellipse measured on the
 * sheet; the belly, the arms, the feet and the claws are forms placed on measured creases.
 *
 * The body breathes around the feet, the head tilts around the neck and the wings flap with partDeg
 * around the shoulders. A [ArtPart.WINGS] item replaces the dragon's own wings while worn and flaps the
 * same way: it is drawn once per wing, clipped to that wing's half of the grid. A helmet
 * ([ItemLayer.OVER_EARS]) covers the top of the head, and the horns are then drawn after it so that they
 * come out of it. Held items ([ArtPart.HAND]) are drawn before the arms, which hold them.
 */
internal object DragonRig : AnimalRig(
    // The crops keep every item of the slot whole. The head crop shows the whole face, above the wings;
    // the neck crop starts below the mouth and is wide enough to show the royal cape beside the body.
    headCrop = Rect(24f, 0f, 216f, 151f),
    neckCrop = Rect(60f, 156f, 180f, 240f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "dragon-extra-coins" to Rect(64f, 174f, 190f, 300f),
            "dragon-extra-book" to Rect(60f, 162f, 180f, 282f),
            "dragon-extra-shield" to Rect(0f, 164f, 120f, 284f),
            "dragon-extra-star-cloud" to Rect(4f, 67f, 237f, 300f),
            "dragon-extra-wings" to Rect(0f, 60f, 240f, 300f),
        ),
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
            drawPath(TailTipVein, ArtTones.Gold.shade, alpha = VEIN_ALPHA, style = VeinStroke)
            specular(TailTip.bounds)
            vinylOval(Crotch, hide.body)
            drawPath(BodyPath, hide.body)
            clipPath(BodyPath) {
                vinylShape(Belly)
                clipPath(Belly.path) { drawPath(Plates, CreamShade, alpha = PLATE_ALPHA, style = PlateStroke) }
                contactShadow(ChinShadow)
            }
            specular(BodyShine)
            hide.limbs.draw(this, outfit)
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
                drawPath(WingLeft, hide.wingLeft)
                drawPath(WingRibsLeft, hide.wingShade, alpha = RIB_ALPHA, style = RibStroke)
            }
        }
        rotate(-partDeg, ShoulderRight) {
            if (wingItem) {
                clipRect(MID_X, -height, 2 * width, 2 * height) { items(outfit, ArtPart.WINGS, ItemLayer.BEHIND) }
            } else {
                drawPath(WingRight, hide.wingRight)
                drawPath(WingRibsRight, hide.wingShade, alpha = RIB_ALPHA, style = RibStroke)
            }
        }
    }

    private fun DrawScope.head(
        hide: DragonHide,
        outfit: Outfit,
        blink: Float,
    ) {
        val helmet = outfit[Slot.HEAD]?.let { itemArt(it) }?.takeIf { it.layer == ItemLayer.OVER_EARS }
        if (helmet == null) horns()
        drawPath(FrillLeft, hide.frillLeft)
        drawPath(FrillRight, hide.frillRight)
        vinylOval(Head, hide.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        items(outfit, ArtPart.HEAD, ItemLayer.OVER_EARS)
        val hornClip = helmet?.earClip
        if (hornClip != null) {
            clipPath(hornClip) { horns() }
        } else if (helmet != null) {
            horns()
        }
        cheeks(CheekLeft, CheekRight)
        drawPath(Brows, hide.crease, style = BrowStroke)
        rimmedEye(EyeLeft, EYE_RX, EYE_RY, blink, ArtTones.EyeInk, VioletIris)
        rimmedEye(EyeRight, EYE_RX, EYE_RY, blink, ArtTones.EyeInk, VioletIris)
        drawPath(Nostrils, hide.crease)
        drawPath(Mouth, PersonalizationColors.EyeInk, style = MouthStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.horns() {
        vinylShape(HornLeft)
        vinylShape(HornRight)
        clipPath(HornLeft.path) { drawPath(HornRidgesLeft, CreamShade, alpha = RIDGE_ALPHA, style = RidgeStroke) }
        clipPath(HornRight.path) { drawPath(HornRidgesRight, CreamShade, alpha = RIDGE_ALPHA, style = RidgeStroke) }
    }
}

/** One hide color's brushes: the body color, and the deeper wing color for the wings and the ear frills. */
private class DragonHide(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.DRAGON, color))
    private val wingTones = vinylTones(PersonalizationColors.dragonWing(color))

    /** The brows and the nostrils: a deep tone of the hide. */
    val crease: Color = lerp(tones.shade, Color.Black, CREASE_DEPTH)
    val wingShade: Color = wingTones.shade
    val tail = vinylBrush(TailPath, tones)
    val body = vinylBrush(BodyPath, tones)
    val limbs = ClawedLimbs(ArmLeft to ArmRight, ArmStroke, FootLeft to FootRight, Claws, tones)
    val head: Brush = vinylBrush(Head, tones)
    val wingLeft = vinylBrush(WingLeft, wingTones)
    val wingRight = vinylBrush(WingRight, wingTones)
    val frillLeft = vinylBrush(FrillLeft, wingTones)
    val frillRight = vinylBrush(FrillRight, wingTones)
}

private const val MID_X = 120f
private const val HEAD_SHINE_DEG = -18f
private const val EYE_RX = 12.5f
private const val EYE_RY = 14.5f
private const val ARM_WIDTH = 24f
private const val RIB_ALPHA = 0.5f
private const val PLATE_ALPHA = 0.7f
private const val RIDGE_ALPHA = 0.55f
private const val VEIN_ALPHA = 0.45f
private const val CREASE_DEPTH = 0.15f

private val Neck = Offset(120f, 158f)
private val ShoulderLeft = Offset(78f, 166f)
private val ShoulderRight = Offset(162f, 166f)

private val Ground = ovalRect(120f, 284f, 92f, 11f)

private val WingLeft = svgPath(DragonTrace.WING_LEFT)
private val WingRight = svgPath(DragonTrace.WING_RIGHT)
private val WingRibsLeft = svgPath("M76 162 L30.5 197 M76 162 L62.5 203")
private val WingRibsRight = svgPath("M164 162 L209.5 192.5 M164 162 L177.5 201")
private val RibStroke = itemStroke(2.5f)

private val TailPath = svgPath(DragonTrace.TAIL)
private val TailTip = VinylShape(svgPath(DragonTrace.TAIL_TIP), ArtTones.Gold)
private val TailTipVein = svgPath("M217 218.5 L233 187")
private val VeinStroke = itemStroke(1.8f)

private val BodyPath = svgPath(DragonTrace.BODY)
private val BodyShine = specularRect(78f, 226f, 8f, 8f)

/** The underside of the body between the feet, which the floor shadow hid from the trace. */
private val Crotch = ovalRect(120f, 262f, 30f, 17f)
private val ChinShadow = ovalRect(120f, 160f, 42f, 11f)

/** The cream plated belly, from under the chin to between the feet. */
private val Belly =
    VinylShape(
        svgPath(
            "M120 162.5 C138 162.5 150 180 153 204 C156 230 148 261 120 261 C92 261 84 230 87 204 " +
                "C90 180 102 162.5 120 162.5 Z",
        ),
        ArtTones.Cream,
    )
private val Plates =
    svgPath(
        "M84 177 Q120 186 156 177 M84 201 Q120 210 156 201 M84 222.5 Q120 231.5 156 222.5 " +
            "M84 243.5 Q120 252 156 243.5",
    )
private val CreamShade = ArtTones.Cream.shade
private val PlateStroke = itemStroke(2f)

// The short arms rest their hands on the belly.
private val ArmLeft = svgPath("M80 174 Q75 190 89 198")
private val ArmRight = svgPath("M160.5 174 Q165.5 190 152 198")
private val ArmStroke = itemStroke(ARM_WIDTH)

private val FootLeft = ovalRect(87f, 270f, 25.5f, 12.5f)
private val FootRight = ovalRect(153.6f, 270f, 25.5f, 12.5f)
private val Claws =
    VinylShape(
        Path().apply {
            for (x in floatArrayOf(71.4f, 81.9f, 93.4f, 146.2f, 157.4f, 168.1f)) addPath(ovalPath(x, 277.5f, 4f, 4.8f))
        },
        ArtTones.Cream,
    )

/** The head: the ellipse measured on the sheet. */
private val Head = ovalRect(120f, 102.3f, 66.5f, 56.7f)
private val HeadShine = specularRect(92f, 66f, 22f, 9f)
private val FrillLeft = svgPath(DragonTrace.FRILL_LEFT)
private val FrillRight = svgPath(DragonTrace.FRILL_RIGHT)
private val HornLeft = VinylShape(svgPath(DragonTrace.HORN_LEFT), ArtTones.Cream)
private val HornRight = VinylShape(svgPath(DragonTrace.HORN_RIGHT), ArtTones.Cream)
private val HornRidgesLeft = svgPath("M72 34 Q84 28 94 33 M76 45 Q90 38 104 44")
private val HornRidgesRight = svgPath("M168 34 Q156 28 146 33 M164 45 Q150 38 136 44")
private val RidgeStroke = itemStroke(1.8f)

private val CheekLeft = ovalRect(74.7f, 121.6f, 10.5f, 7f)
private val CheekRight = ovalRect(166.1f, 121.6f, 10.5f, 7f)
private val Brows = svgPath("M77.5 77.5 Q86 66 97.5 73 M144 73 Q155.5 66 163.5 77.5")
private val BrowStroke = itemStroke(3f)
private val EyeLeft = Offset(89.4f, 100.4f)
private val EyeRight = Offset(151.9f, 100.4f)
private val Nostrils =
    Path().apply {
        addPath(ovalPath(112f, 118.6f, 2.6f, 2.2f))
        addPath(ovalPath(129.5f, 118.6f, 2.6f, 2.2f))
    }
private val Mouth = svgPath("M102 129 Q120.8 146 139.5 129")
private val MouthStroke = itemStroke(2.4f)
