// Literal coordinates place the robot in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The robot as a shaded vinyl toy, after the reference: a wide rounded head with a dark glossy face
 * screen, glowing eyes and smile, side ear discs, a neck joint, a body with a cream chest panel and two
 * buttons, jointed dark arms with round hands and short legs with rounded feet. The body breathes
 * around the feet, the head tilts around the neck and the left arm waves with armDeg. Antenna items
 * ([ArtPart.ANTENNA]) bounce with partDeg around the top of the head. The glowing eyes never close:
 * blinking dims them. A screen item ([ItemLayer.SCREEN]) replaces the eyes while worn and dims the same
 * way. The wrench ([ArtPart.HAND]) is drawn before the right hand, which holds it.
 */
internal object RobotRig : AnimalRig(
    // The crops keep every item of the slot whole; the neck crop starts below the face screen and leaves
    // out the lower arms and hands.
    headCrop = Rect(24f, 0f, 216f, 172f),
    neckCrop = Rect(67f, 148f, 173f, 238f),
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
            vinylShape(LegLeft)
            vinylShape(LegRight)
            vinylShape(Knees)
            drawPath(FootLeft, shell.footLeft)
            drawPath(FootRight, shell.footRight)
            specular(FootLeftBounds)
            specular(FootRightBounds)
            vinylShape(NeckJoint)
            drawPath(NeckRing, ArtTones.Metal.shade, style = RingStroke)
            drawPath(BodyPath, shell.body)
            specular(BodyShine)
            vinylShape(Panel)
            specular(Panel.bounds)
            vinylShape(ButtonLeft)
            vinylShape(ButtonRight)
            specular(ButtonLeft.bounds)
            specular(ButtonRight.bounds)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            rotate(pose.armDeg, ShoulderLeft) { arm(LeftArm, shell.handLeft) }
            arm(RightArm, shell.handRight) { items(outfit, ArtPart.HAND, ItemLayer.FRONT) }
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(shell, outfit, pose) }
        }
    }

    private inline fun DrawScope.arm(
        arm: RobotArm,
        hand: Brush,
        held: DrawScope.() -> Unit = {},
    ) {
        drawPath(arm.upper, ArmBrushes[arm.side], style = ArmStroke)
        drawPath(arm.lower, ArmBrushes[arm.side], style = ArmStroke)
        vinylShape(arm.elbow)
        vinylShape(arm.shoulder)
        held()
        drawOval(hand, arm.hand.topLeft, arm.hand.size)
        specular(arm.hand)
    }

    private fun DrawScope.head(
        shell: RobotShell,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        rotate(pose.partDeg, AntennaBase) { items(outfit, ArtPart.ANTENNA, ItemLayer.BEHIND) }
        vinylShape(EarLeft)
        vinylShape(EarRight)
        specular(EarLeft.bounds)
        specular(EarRight.bounds)
        drawPath(HeadPath, shell.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        vinylShape(Screen)
        val screenEyes = outfit[Slot.EXTRA]?.let { itemArt(it) }?.layer == ItemLayer.SCREEN
        if (!screenEyes) eyes()
        items(outfit, ArtPart.HEAD, ItemLayer.SCREEN)
        // Blinking dims the eyes: the screen is painted over them again, partly transparent.
        if (pose.blink > 0f) drawPath(Screen.path, Screen.brush, alpha = pose.blink * BLINK_DIM)
        drawPath(Smile, PersonalizationColors.Glow, style = SmileStroke)
        specular(ScreenShine, SCREEN_SHINE_DEG)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }

    private fun DrawScope.eyes() {
        for (i in Eyes.indices) {
            val eye = Eyes[i]
            drawOval(eye.haloBrush, eye.halo.topLeft, eye.halo.size)
            drawRoundRect(PersonalizationColors.Glow, eye.eye.topLeft, eye.eye.size, EyeCorner)
            drawOval(ArtTones.NoseShine, eye.glint.topLeft, eye.glint.size)
        }
    }
}

/** One shell color's brushes: the head, body, hands and feet share the companion color. */
private class RobotShell(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.ROBOT, color))
    val head: Brush = vinylBrush(HeadPath.getBounds(), tones)
    val body: Brush = vinylBrush(BodyPath.getBounds(), tones)
    val footLeft: Brush = vinylBrush(FootLeftBounds, tones)
    val footRight: Brush = vinylBrush(FootRightBounds, tones)
    val handLeft: Brush = vinylBrush(LeftArm.hand, tones)
    val handRight: Brush = vinylBrush(RightArm.hand, tones)
}

/** One arm in its rest pose: [side] 0 is the viewer's left, 1 the right. Built mirrored from the left arm. */
private class RobotArm(
    val side: Int,
) {
    private fun x(v: Float) = if (side == 0) v else MIRROR - v

    val upper = svgPath("M${x(70f)} 184 L${x(59f)} 204")
    val lower = svgPath("M${x(56f)} 213 L${x(53f)} 224")
    val elbow = VinylShape(ovalPath(x(57.5f), 208.5f, 8f), ArtTones.Steel)
    val shoulder = VinylShape(ovalPath(x(71f), 182f, 9f), ArtTones.Metal)
    val hand = ovalRect(x(51f), 234f, 15f)

    /** Both segments, so the one arm brush shades the whole arm. */
    val bounds: Rect =
        Path()
            .apply {
                addPath(upper)
                addPath(lower)
            }.getBounds()
            .inflate(ARM_WIDTH / 2)

    private companion object {
        const val MIRROR = 240f
    }
}

private class RobotEye(
    cx: Float,
    cy: Float,
) {
    val eye = Rect(cx - 7f, cy - 10f, cx + 7f, cy + 10f)
    val halo = ovalRect(cx, cy, 17f, 21f)
    val haloBrush =
        Brush.radialGradient(
            0.4f to PersonalizationColors.Glow.copy(alpha = HALO_ALPHA),
            1f to PersonalizationColors.Glow.copy(alpha = 0f),
            center = Offset(cx, cy),
            radius = 21f,
        )
    val glint = ovalRect(cx - 2.5f, cy - 5f, 2.2f, 3f)
}

private const val HEAD_SHINE_DEG = -14f
private const val SCREEN_SHINE_DEG = -12f
private const val BLINK_DIM = 0.8f
private const val HALO_ALPHA = 0.45f
private const val ARM_WIDTH = 13f

private val Feet = Offset(120f, 282f)
private val Neck = Offset(120f, 156f)
private val AntennaBase = Offset(120f, 46f)

private val Ground = ovalRect(120f, 284f, 92f, 11f)
private val HeadShadow = ovalRect(120f, 162f, 48f, 10f)

private val LegLeft = VinylShape(roundRectPath(84f, 232f, 102f, 262f, 7f), ArtTones.Metal)
private val LegRight = VinylShape(roundRectPath(138f, 232f, 156f, 262f, 7f), ArtTones.Metal)
private val Knees =
    VinylShape(
        Path().apply {
            addPath(roundRectPath(82f, 243f, 104f, 251f, 4f))
            addPath(roundRectPath(136f, 243f, 158f, 251f, 4f))
        },
        ArtTones.Steel,
    )

private fun footPath(
    left: Float,
    right: Float,
) = roundRectPath(
    RoundRect(
        Rect(left, 254f, right, 282f),
        topLeft = CornerRadius(24f, 22f),
        topRight = CornerRadius(24f, 22f),
        bottomRight = CornerRadius(8f),
        bottomLeft = CornerRadius(8f),
    ),
)

private val FootLeft = footPath(67f, 119f)
private val FootRight = footPath(121f, 173f)
private val FootLeftBounds = FootLeft.getBounds()
private val FootRightBounds = FootRight.getBounds()

private val NeckJoint = VinylShape(roundRectPath(106f, 146f, 134f, 170f, 6f), ArtTones.Metal)
private val NeckRing = svgPath("M107 158 L133 158")
private val RingStroke = itemStroke(2.5f)

private val BodyPath = roundRectPath(70f, 162f, 170f, 240f, 24f)
private val BodyShine = specularRect(94f, 176f, 14f, 5f)
private val Panel = VinylShape(roundRectPath(88f, 178f, 152f, 220f, 12f), ArtTones.Cream)
private val ButtonLeft = VinylShape(ovalPath(106f, 199f, 9f), ArtTones.Knit)
private val ButtonRight = VinylShape(ovalPath(134f, 199f, 9f), ArtTones.Teal)

private val ShoulderLeft = Offset(71f, 182f)
private val LeftArm = RobotArm(0)
private val RightArm = RobotArm(1)
private val ArmBrushes = listOf(vinylBrush(LeftArm.bounds, ArtTones.Metal), vinylBrush(RightArm.bounds, ArtTones.Metal))
private val ArmStroke = itemStroke(ARM_WIDTH)

private val EarLeft = VinylShape(roundRectPath(32f, 78f, 50f, 122f, 9f), ArtTones.Metal)
private val EarRight = VinylShape(roundRectPath(190f, 78f, 208f, 122f, 9f), ArtTones.Metal)

private val HeadPath = roundRectPath(46f, 44f, 194f, 154f, 34f)
private val HeadShine = specularRect(84f, 54f, 22f, 6f)

private val Screen = VinylShape(roundRectPath(62f, 62f, 178f, 138f, 22f), ArtTones.Screen)
private val ScreenShine = specularRect(88f, 72f, 16f, 4f)
private val Eyes = listOf(RobotEye(95f, 94f), RobotEye(145f, 94f))
private val EyeCorner = CornerRadius(7f)
private val Smile = svgPath("M106 114 Q120 126 134 114")
private val SmileStroke = itemStroke(4f)
