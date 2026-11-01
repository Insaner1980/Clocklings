// Literal coordinates place the car in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import fi.clocklings.ui.art.unitBrush
import fi.clocklings.ui.art.unitOval
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The car as a shaded vinyl toy in a three-quarter view, after the reference: its side faces left with
 * a door handle, a side window and two wheels showing cream hubs, and its rounded front faces the
 * viewer with a cream face panel, glossy eyes in cream rings, a smile, cheeks and a cream bumper. A
 * cream cabin with a glass windshield sits on top, and round mirrors stand out like ears. The far front
 * wheel peeks out beside the bumper. The car breathes and sways like the animals, and partDeg rocks the
 * body gently on its wheels. Hubcap items ([ArtPart.WHEELS]) are drawn on the hubs after the wheels;
 * transformation parts that sit behind the car ([ItemLayer.BEHIND]) are drawn before the cabin.
 */
internal object CarRig : AnimalRig(
    // The crops keep every item of the slot whole: the roof items rise above the cabin, the front items
    // sit on the face and bumper, and the square wheel crop shows both side wheels inside the grid.
    crops =
        mapOf(
            Slot.ROOF to Rect(6f, 20f, 230f, 200f),
            Slot.FRONT to Rect(78f, 150f, 230f, 284f),
            Slot.WHEELS to Rect(10f, 200f, 110f, 300f),
        ),
) {
    /** Paint brushes for every companion color, built once and indexed by the color's ordinal. */
    private val paints = CompanionColor.entries.map { CarPaint(it) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val paint = paints[color.ordinal]
        groundShadow(Ground)
        posedBody(pose) {
            vinylShape(TireFarRight)
            rotate(pose.partDeg * ROCK, RockPivot) {
                items(outfit, ArtPart.BODY, ItemLayer.BEHIND)
                vinylShape(Cabin)
                specular(Cabin.bounds)
                vinylShape(SideWindow)
                vinylShape(Windshield)
                specular(WindshieldShine, WINDSHIELD_SHINE_DEG)
                drawPath(SidePath, paint.side)
                drawPath(Handle, paint.handle)
                drawPath(FrontPath, paint.front)
                specular(HoodShine, HOOD_SHINE_DEG)
                contactShadow(MirrorShadowLeft)
                contactShadow(MirrorShadowRight)
                vinylOval(CarMirrorLeft, paint.mirrorLeft)
                vinylOval(CarMirrorRight, paint.mirrorRight)
                specular(CarMirrorLeft)
                specular(CarMirrorRight)
                vinylShape(FacePanel)
                vinylShape(Bumper)
                specular(Bumper.bounds)
                cheeks(CheekLeft, CheekRight)
                glossyEyes(EyeLeft, EyeRight, EYE_RX, EYE_RY, pose.blink)
                drawPath(Smile, PersonalizationColors.EyeInk, style = SmileStroke)
            }
            contactShadow(ArchShadowRear)
            contactShadow(ArchShadowFront)
            wheel(TireRear, CarHubs[0])
            wheel(TireFront, CarHubs[1])
            items(outfit, ArtPart.WHEELS, ItemLayer.FRONT)
            rotate(pose.partDeg * ROCK, RockPivot) { items(outfit, ArtPart.BODY, ItemLayer.FRONT) }
        }
    }

    private fun DrawScope.wheel(
        tire: VinylShape,
        hub: CarHub,
    ) {
        vinylShape(tire)
        unitOval(hub.center.x, hub.center.y, hub.rx, hub.ry, HubBrush)
        specular(tire.bounds)
    }
}

/** One paint color's brushes: the side, the front, the door handle and the mirrors. */
private class CarPaint(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.CAR, color))
    val side: Brush = vinylBrush(SidePath.getBounds(), tones)
    val front: Brush = vinylBrush(FrontPath.getBounds(), tones)
    val handle: Brush = vinylBrush(Handle.getBounds(), vinylTones(tones.shade))
    val mirrorLeft: Brush = vinylBrush(CarMirrorLeft, tones)
    val mirrorRight: Brush = vinylBrush(CarMirrorRight, tones)
}

/** A visible wheel hub: an ellipse, since the side of the car is seen at an angle. */
internal class CarHub(
    val center: Offset,
    val rx: Float,
    val ry: Float,
)

/** The rear and front side wheel hubs, where hubcap items are drawn. */
internal val CarHubs = arrayOf(CarHub(Offset(34f, 251f), 10f, 14f), CarHub(Offset(72f, 256f), 11.5f, 16f))

/** The rear and front side tires as (center, radii), for items drawn on the whole tire. */
internal val CarTires = arrayOf(CarHub(Offset(38f, 251f), 17.6f, 25.6f), CarHub(Offset(77f, 256f), 20.4f, 28.4f))

/** How far partDeg rocks the body: a quarter of the part swing. */
private const val ROCK = 0.25f
private const val HOOD_SHINE_DEG = -8f
private const val WINDSHIELD_SHINE_DEG = -24f
private const val EYE_RX = 11f
private const val EYE_RY = 12.4f

internal val CarMirrorLeft = ovalRect(67.4f, 184.6f, 10f)
internal val CarMirrorRight = ovalRect(199.4f, 180f, 10f)

private val RockPivot = Offset(122f, 270f)
private val Ground = ovalRect(121f, 278f, 112f, 10f)

private val TireTones = vinylTones(PersonalizationColors.CarTire)
private val TireRear = VinylShape(ovalPath(38f, 251f, 17.6f, 25.6f), TireTones)
private val TireFront = VinylShape(ovalPath(77f, 256f, 20.4f, 28.4f), TireTones)
private val TireFarRight = VinylShape(ovalPath(204f, 254f, 20f, 25.6f), TireTones)
private val HubBrush = unitBrush(ArtTones.Cream)

private val Cabin =
    VinylShape(
        svgPath("M44 189 C42 165 45 135 61 123 C79 113 147 112 167 118 C181 123 190 141 193 184 Z"),
        ArtTones.Cream,
    )
private val GlassTones = vinylTones(PersonalizationColors.CarGlass)
private val SideWindow =
    VinylShape(svgPath("M51.4 180 C50.2 161 53 145.4 61.8 139 C67.4 135.4 77 135 81.4 136.2 L82.2 180 Z"), GlassTones)
private val Windshield =
    VinylShape(
        svgPath(
            "M90.6 180 L90.2 142.2 C90.2 135 95 132.6 103 132.6 L165 133.4 C174 133.8 179.4 139 181.4 147.4 L186 180 Z",
        ),
        GlassTones,
    )
private val WindshieldShine = specularRect(150f, 142f, 12f, 5f)

private val SidePath =
    svgPath(
        "M25 229 C23 211 27 195 40 188 C47 184.6 55 183 65 182.6 L99 179 L103 256 L33 256 C27 254 25.4 245 25 229 Z",
    )
private val Handle = roundRectPath(40.6f, 195.6f, 51.4f, 202.4f, 3.4f)
private val FrontPath =
    svgPath(
        "M60 203 C61 184 77 176 99 175 L181 174 C203 174 216 187 217.4 207 L218.2 242 " +
            "C218.2 252 212 256 203 256 L89 256 C75 256 61 250 60.2 235 Z",
    )
private val HoodShine = specularRect(110f, 182f, 26f, 5f)

private val FacePanel =
    VinylShape(
        Path().apply {
            addPath(roundRectPath(101f, 206f, 211f, 250f, 19f))
            addOval(ovalRect(116f, 216f, 19f))
            addOval(ovalRect(194f, 215f, 19f))
        },
        ArtTones.Cream,
    )
private val Bumper = VinylShape(roundRectPath(88f, 242f, 216f, 270f, 13f), ArtTones.Cream)

private val EyeLeft = Offset(116f, 217.4f)
private val EyeRight = Offset(194f, 216.4f)
private val CheekLeft = ovalRect(114f, 238f, 8.4f, 4.8f)
private val CheekRight = ovalRect(198f, 236f, 8.4f, 4.8f)
private val Smile = svgPath("M144 225 Q156.6 236 169.4 225")
private val SmileStroke = itemStroke(3.5f)

// Contact shadows where parts meet: the body just above the wheels and under the mirrors.
private val ArchShadowRear = ovalRect(38f, 226f, 17f, 6f)
private val ArchShadowFront = ovalRect(77f, 229f, 19f, 6f)
private val MirrorShadowLeft = ovalRect(69f, 195f, 10f, 4f)
private val MirrorShadowRight = ovalRect(201f, 190f, 10f, 4f)
