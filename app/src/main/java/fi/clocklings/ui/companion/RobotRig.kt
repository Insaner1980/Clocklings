// Literal coordinates were measured on the reference sheet in the 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.unitBrush
import fi.clocklings.ui.art.unitOval
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The robot as a shaded vinyl toy, traced from the front view of the owner's reference sheet: the
 * outlines of the head, the ear discs, the face screen, the body, both arms with their mitten hands,
 * both legs with their domed feet and the chest panel are [RobotTrace] paths. One shell color covers
 * it all; the joints (ear discs, neck, shoulder caps, wrist cuffs and ankle bands) are a deeper tone of
 * the same color, placed where they were measured on the sheet.
 *
 * The body breathes around the feet, the head tilts around the neck and the left arm waves with armDeg.
 * Antenna items ([ArtPart.ANTENNA]) bounce with partDeg around the top of the head. The glowing eyes
 * never close: blinking dims them. A screen item ([ItemLayer.SCREEN]) replaces the eyes while worn and
 * dims the same way. The wrench ([ArtPart.HAND]) is drawn before the right hand, which holds it.
 */
internal object RobotRig : AnimalRig(
    // The crops keep every item of the slot whole; the neck crop starts below the face screen and leaves
    // out the lower arms and hands.
    headCrop = Rect(24f, 0f, 216f, 172f),
    neckCrop = Rect(67f, 148f, 173f, 238f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "robot-extra-wrench" to Rect(120f, 143f, 240f, 263f),
            "robot-extra-heart-eyes" to Rect(60f, 40f, 180f, 160f),
            "robot-extra-star-eyes" to Rect(60f, 40f, 180f, 160f),
            "robot-extra-pet" to Rect(120f, 180f, 240f, 300f),
            "robot-extra-jetpack" to Rect(4f, 68f, 236f, 300f),
            "robot-extra-bubble-pack" to Rect(0f, 66f, 160f, 226f),
            "robot-extra-wind-up-key" to Rect(120f, 140f, 240f, 260f),
        ),
) {
    /** Shell brushes for every companion color, built once and indexed by the color's ordinal. */
    private val shells = CompanionColor.entries.map { RobotShell(it) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val shell = shells[color.ordinal]
        groundShadow(Ground)
        posedBody(pose, Feet) {
            items(outfit, ArtPart.BODY, ItemLayer.BEHIND)
            leg(LowerLeft, FootLeft, shell.lowerLeft, shell)
            leg(LowerRight, FootRight, shell.lowerRight, shell)
            drawPath(AnkleLeft, shell.jointColor)
            drawPath(AnkleRight, shell.jointColor)
            drawPath(NeckJoint, shell.jointColor)
            // The traced body ends flat where the arms covered it; an oval rounds its sides under them.
            unitOval(BodySides, shell.dot)
            drawPath(BodyPath, shell.body)
            specular(BodyShine)
            vinylShape(Panel)
            contactShadow(ButtonShadowLeft)
            contactShadow(ButtonShadowRight)
            vinylShape(ButtonLeft)
            vinylShape(ButtonRight)
            specular(ButtonLeft.bounds)
            specular(ButtonRight.bounds)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            drawCircle(shell.jointColor, SHOULDER_R, ShoulderLeft)
            drawCircle(shell.jointColor, SHOULDER_R, ShoulderRight)
            rotate(pose.armDeg, ShoulderLeft) { arm(LeftArm, shell.armLeft, shell) }
            arm(RightArm, shell.armRight, shell) {
                // The held item was drawn for a lower hand; lift it to the traced one.
                translate(HELD_DX, HELD_DY) { items(outfit, ArtPart.HAND, ItemLayer.FRONT) }
            }
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(shell, outfit, pose) }
        }
    }

    /** A leg with its domed foot: the traced outline, and the foot as a round form inside it. */
    private fun DrawScope.leg(
        outline: Path,
        foot: Rect,
        brush: Brush,
        shell: RobotShell,
    ) {
        drawPath(outline, brush)
        clipPath(outline) { unitOval(foot, shell.dot) }
        specular(foot)
    }

    private inline fun DrawScope.arm(
        arm: RobotArm,
        brush: Brush,
        shell: RobotShell,
        held: DrawScope.() -> Unit = {},
    ) {
        drawPath(arm.outline, brush)
        held()
        clipPath(arm.outline) {
            unitOval(arm.hand, shell.dot)
            drawPath(arm.cuff, shell.jointColor, style = CuffStroke)
        }
        specular(arm.hand)
    }

    private fun DrawScope.head(
        shell: RobotShell,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        rotate(pose.partDeg, AntennaBase) { items(outfit, ArtPart.ANTENNA, ItemLayer.BEHIND) }
        drawPath(EarLeft, shell.jointColor)
        drawPath(EarRight, shell.jointColor)
        drawPath(HeadPath, shell.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        // The bezel: a rim of the shell's deep tone that sinks the screen into the head.
        drawPath(Screen.path, shell.jointColor, style = BezelStroke)
        vinylShape(Screen)
        val screenEyes = outfit[Slot.EXTRA]?.let { itemArt(it) }?.layer == ItemLayer.SCREEN
        if (!screenEyes) eyes()
        items(outfit, ArtPart.HEAD, ItemLayer.SCREEN)
        // Blinking dims the eyes: the screen is painted over them again, partly transparent.
        if (pose.blink > 0f) drawPath(Screen.path, Screen.brush, alpha = pose.blink * BLINK_DIM)
        drawPath(Cheeks, PersonalizationColors.Cheek, alpha = CHEEK_ALPHA)
        drawPath(Smile, PersonalizationColors.ScreenMint, style = SmileStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.eyes() {
        drawPath(Brows, PersonalizationColors.ScreenMint, alpha = BROW_ALPHA, style = BrowStroke)
        for (i in Eyes.indices) {
            val eye = Eyes[i]
            drawOval(eye.haloBrush, eye.halo.topLeft, eye.halo.size)
            drawOval(eye.eyeBrush, eye.eye.topLeft, eye.eye.size)
            drawOval(ArtTones.NoseShine, eye.glint.topLeft, eye.glint.size)
        }
    }
}

/** One shell color's brushes: every part shares the companion color and the joints its deep tone. */
private class RobotShell(
    color: CompanionColor,
) {
    // The shell shades toward its own deeper tone, so the color stays clean on every side.
    private val base = PersonalizationColors.companion(CompanionKind.ROBOT, color)
    private val deep = PersonalizationColors.robotJoint(color)
    private val tones = VinylTones(lerp(base, Color.White, SHELL_LIGHT_MIX), base, lerp(base, deep, SHELL_SHADE_MIX))

    private fun brush(path: Path): Brush = vinylBrush(path.getBounds(), tones)

    val jointColor: Color = deep
    val dot: Brush = unitBrush(tones)
    val head = brush(HeadPath)
    val body = brush(BodyPath)
    val lowerLeft = brush(LowerLeft)
    val lowerRight = brush(LowerRight)
    val armLeft = brush(LeftArm.outline)
    val armRight = brush(RightArm.outline)
}

/** One arm in its rest pose: [side] 0 is the viewer's left, 1 the right; the measured parts are mirrored. */
private class RobotArm(
    private val side: Int,
) {
    private fun x(v: Float) = if (side == 0) v else MIRROR - v

    val outline = svgPath(if (side == 0) RobotTrace.ARM_LEFT else RobotTrace.ARM_RIGHT)

    val cuff = svgPath("M${x(44f)} 205.5 L${x(70f)} 200")
    val hand = ovalRect(x(50.4f), 221f, 19.5f, 19.5f)

    private companion object {
        const val MIRROR = 240f
    }
}

private class RobotEye(
    cx: Float,
    cy: Float,
) {
    val eye = ovalRect(cx, cy, 8.1f, 12f)
    val eyeBrush =
        Brush.radialGradient(
            0f to lerp(PersonalizationColors.ScreenMint, Color.White, EYE_CORE_MIX),
            1f to PersonalizationColors.ScreenMint,
            center = Offset(cx - 1f, cy - 3f),
            radius = 12f,
        )
    val halo = ovalRect(cx, cy, 16f, 19f)
    val haloBrush =
        Brush.radialGradient(
            0.5f to PersonalizationColors.ScreenMint.copy(alpha = HALO_ALPHA),
            1f to PersonalizationColors.ScreenMint.copy(alpha = 0f),
            center = Offset(cx, cy),
            radius = 19f,
        )
    val glint = ovalRect(cx - 1.5f, cy - 6f, 2.6f, 3.8f)
}

private const val HEAD_SHINE_DEG = -10f
private const val BLINK_DIM = 0.8f
private const val HALO_ALPHA = 0.2f
private const val EYE_CORE_MIX = 0.45f
private const val BROW_ALPHA = 0.4f
private const val CHEEK_ALPHA = 0.3f
private const val SHELL_LIGHT_MIX = 0.4f
private const val SHELL_SHADE_MIX = 0.55f
private const val SHOULDER_R = 10f
private const val HELD_DX = 1f
private const val HELD_DY = -13f

private val Feet = Offset(120f, 281f)
private val Neck = Offset(120f, 152f)
private val AntennaBase = Offset(120f, 46f)

private val Ground = ovalRect(120f, 283f, 92f, 11f)
private val HeadShadow = ovalRect(120f, 156f, 44f, 8f)

private val LowerLeft = svgPath(RobotTrace.LOWER_LEFT)
private val LowerRight = svgPath(RobotTrace.LOWER_RIGHT)
private val FootLeft = ovalRect(90.5f, 270f, 30f, 23f)
private val FootRight = ovalRect(150f, 270f, 30f, 23f)
private val AnkleLeft = roundRectPath(80.7f, 242f, 110.3f, 251f, 4.5f)
private val AnkleRight = roundRectPath(129.5f, 242f, 159.2f, 251f, 4.5f)

private val NeckJoint = roundRectPath(100.6f, 147f, 139.5f, 158f, 4f)

private val BodyPath = svgPath(RobotTrace.BODY)
private val BodyShine = specularRect(92f, 163f, 14f, 4f)

/** The cream chest panel: the traced bounds with evenly rounded corners. */
private val Panel = VinylShape(roundRectPath(85.9f, 168.8f, 154.5f, 207.4f, 13f), ArtTones.Cream)
private val ButtonLeft = VinylShape(ovalPath(105.4f, 187.8f, 9.9f), ArtTones.Knit)
private val ButtonRight = VinylShape(ovalPath(135.7f, 187.8f, 9.9f), ArtTones.Teal)
private val ButtonShadowLeft = ovalRect(106.5f, 191f, 12f, 11f)
private val ButtonShadowRight = ovalRect(136.8f, 191f, 12f, 11f)

private val ShoulderLeft = Offset(75f, 171f)
private val ShoulderRight = Offset(165f, 171f)
private val BodySides = ovalRect(120f, 192f, 51f, 41f)
private val LeftArm = RobotArm(0)
private val RightArm = RobotArm(1)
private val CuffStroke = Stroke(9.5f, cap = StrokeCap.Butt)

private val EarLeft = svgPath(RobotTrace.EAR_LEFT)
private val EarRight = svgPath(RobotTrace.EAR_RIGHT)

private val HeadPath = svgPath(RobotTrace.HEAD)
private val HeadShine = specularRect(84f, 53f, 24f, 5f)

private val Screen = VinylShape(svgPath(RobotTrace.SCREEN), ArtTones.Screen)
private val BezelStroke = Stroke(3.5f)
private val Eyes = listOf(RobotEye(94.6f, 97.4f), RobotEye(146.2f, 97.4f))
private val Brows = svgPath("M81 78.5 Q89 71.5 98 76 M141.5 76 Q150.5 71.5 158.5 78.5")
private val BrowStroke = itemStroke(2.5f)
private val Cheeks =
    Path().apply {
        addPath(ovalPath(77.1f, 116.4f, 9.5f, 6.5f))
        addPath(ovalPath(163.1f, 116.4f, 9.5f, 6.5f))
    }
private val Smile = svgPath("M107.5 115 Q121 126 134.5 115")
private val SmileStroke = itemStroke(3.5f)
