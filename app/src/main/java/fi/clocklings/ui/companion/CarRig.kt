// Literal coordinates place the car in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.glossyEye
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.softRim
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * The car as a shaded vinyl sports car in a three-quarter view, traced from the approved reference: a
 * low, wide body with a long hood, a rear spoiler, a side vent and two side wheels with five-spoke cream
 * rims, a raked windshield and a small side window, and slim side mirrors. Its face is the front: the
 * headlights are almond eyes with glossy pupils, a smile sits above the grille and two dark intakes
 * flank the lower lip. The car breathes and sways like the animals, and partDeg rocks the body gently
 * on its wheels. Hubcap items ([ArtPart.WHEELS]) are drawn on the hubs after the wheels; transformation
 * parts that sit behind the car ([ItemLayer.BEHIND]) are drawn before the body.
 */
internal object CarRig : AnimalRig(
    // The crops keep every item of the slot whole: the roof items rise above the cabin, the front items
    // sit on the face and bumper, and the square wheel crop shows both side wheels inside the grid.
    crops =
        mapOf(
            Slot.ROOF to Rect(10f, 96f, 200f, 226f),
            Slot.FRONT to Rect(84f, 186f, 236f, 296f),
            Slot.WHEELS to Rect(6f, 190f, 116f, 300f),
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
            vinylShape(Under)
            wheel(TireRear, CarHubs[0])
            wheel(TireFront, CarHubs[1])
            items(outfit, ArtPart.WHEELS, ItemLayer.FRONT)
            rotate(pose.partDeg * ROCK, RockPivot) {
                items(outfit, ArtPart.BODY, ItemLayer.BEHIND)
                drawPath(Body, paint.body)
                for (i in ShadeLevels.indices) drawPath(ShadeLevels[i], paint.shade)
                contactShadow(SpoilerShadow)
                softRim(Body, paint.rim)
                specular(RoofShine)
                specular(HoodShineLeft, HOOD_SHINE_DEG)
                specular(HoodShineRight, HOOD_SHINE_DEG)
                specular(SpoilerShine)
                specular(FenderShine)
                vinylShape(SideWindow)
                vinylShape(Windshield)
                specular(WindshieldShine, WINDSHIELD_SHINE_DEG)
                vinylOval(CarMirrorNear, paint.mirrorNear)
                vinylOval(CarMirrorFar, paint.mirrorFar)
                specular(CarMirrorNear)
                specular(CarMirrorFar)
                drawPath(Vent, TireTones.shade)
                drawPath(Intakes, TireTones.shade)
                eye(EyeLeft, PupilLeft, PUPIL_LEFT_RX, PUPIL_LEFT_RY, pose.blink)
                eye(EyeRight, PupilRight, PUPIL_RIGHT_RX, PUPIL_RIGHT_RY, pose.blink)
                drawPath(Mouth, PersonalizationColors.EyeInk)
                items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            }
        }
    }

    /** One side wheel: the tire, then the five-spoke cream rim fitted to the [hub] ellipse. */
    private fun DrawScope.wheel(
        tire: VinylShape,
        hub: CarHub,
    ) {
        vinylShape(tire)
        withTransform({
            translate(hub.center.x, hub.center.y)
            scale(hub.rx / RIM_R, hub.ry / RIM_R, Offset.Zero)
        }) {
            vinylShape(RimDisc)
            drawPath(RimGaps, TireTones.shade)
            vinylShape(RimCap)
        }
        specular(Rect(hub.center.x - hub.rx, hub.center.y - hub.ry, hub.center.x + hub.rx, hub.center.y + hub.ry))
    }

    /** One headlight eye: the white, then the glossy pupil clipped to it so the lid cuts across it. */
    private fun DrawScope.eye(
        white: VinylShape,
        pupil: Offset,
        rx: Float,
        ry: Float,
        blink: Float,
    ) {
        vinylShape(white)
        clipPath(white.path) { glossyEye(pupil, rx, ry, blink, ArtTones.EyeInk) }
    }
}

/** One paint color's brushes: the body, its shade and rim, and the mirrors. */
private class CarPaint(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.CAR, color))
    val body: Brush = vinylBrush(BodyLight, tones)
    val shade: Color = tones.shade.copy(alpha = SHADE_ALPHA)
    val rim: Color = tones.shade.copy(alpha = RIM_ALPHA)
    val mirrorNear: Brush = vinylBrush(CarMirrorNear, tones)
    val mirrorFar: Brush = vinylBrush(CarMirrorFar, tones)
}

/** A visible wheel hub: an ellipse, since the side of the car is seen at an angle. */
internal class CarHub(
    val center: Offset,
    val rx: Float,
    val ry: Float,
)

/** The rear and front side wheel hubs, where hubcap items are drawn. */
internal val CarHubs = arrayOf(CarHub(Offset(21.7f, 228.4f), 8.7f, 23f), CarHub(Offset(75.2f, 246.1f), 14f, 23.1f))

/** The rear and front side tires as (center, radii), for items drawn on the whole tire. */
internal val CarTires = arrayOf(CarHub(Offset(27f, 230f), 17f, 29f), CarHub(Offset(81f, 245.5f), 24f, 33f))

/** How far partDeg rocks the body: a quarter of the part swing. */
private const val ROCK = 0.25f
private const val HOOD_SHINE_DEG = -8f
private const val WINDSHIELD_SHINE_DEG = -14f
private const val SHADE_ALPHA = 0.13f
private const val RIM_ALPHA = 0.05f
private const val PUPIL_LEFT_RX = 9.5f
private const val PUPIL_LEFT_RY = 10.5f
private const val PUPIL_RIGHT_RX = 7.5f
private const val PUPIL_RIGHT_RY = 8.5f

/** The near and far side mirrors on the doors. */
internal val CarMirrorNear = ovalRect(63f, 189.5f, 12f, 5.5f)
internal val CarMirrorFar = ovalRect(179f, 187.5f, 9f, 5.5f)

private val RockPivot = Offset(120f, 276f)
private val Ground = ovalRect(122f, 278f, 104f, 8f)

/** The area the body gradient is spread over: the whole car, so the light falls evenly along it. */
private val BodyLight = Rect(0f, 140f, 240f, 310f)

private val TireTones = vinylTones(PersonalizationColors.CarTire)
private val Under =
    VinylShape(
        svgPath(
            "M193 267 C184.5 266.5 158.5 268 149.5 269 C141 270 134.5 272.5 140.5 273 " +
                "C147 273.5 177 271 185.5 271.5 C194.5 271.5 190 274 193 275 C196 275.5 201 275.5 204 275 " +
                "C207 275 209.5 274.5 212 273.5 C214.5 272 221 268.5 219.5 268 C217.5 267.5 206.5 270 202 270 " +
                "C197.5 269.5 202 267 193 267 Z",
        ),
        VinylTones(TireTones.shade, TireTones.shade, TireTones.shade),
    )

/** The side tires, reaching up under the wheel arches so the rocking body never shows a gap. */
private val TireRear =
    VinylShape(
        svgPath(
            "M21.5 193.5 C20 193.5 19 194 17 196.5 C15.5 199.5 13 204.5 12 210.5 " +
                "C10.5 216.5 10.5 227 10.5 232.5 C10.5 238 11 239 12 242.5 C13 246 14.5 250 15.5 253 " +
                "C17 255.5 18.5 256.5 20 257.5 C21.5 259 22 259.5 25 260 C27.5 260 33.5 260 36 259.5 " +
                "C38.5 259.5 39.5 259 41 258 C42.5 257 44 256.5 45 254.5 C45.5 252 46 247 45 245 " +
                "C44 243 39.5 243.5 38.5 242.5 C37 242 37 244 36.5 240.5 C36 237 36 228 35.5 222.5 " +
                "C35 217 34 212 33 208 C31.5 203.5 29 199 28 196.5 C26.5 194.5 26.5 195 25.5 194.5 " +
                "C24.5 194 23 193 21.5 193.5 Z",
        ),
        TireTones,
    )
private val TireFront =
    VinylShape(
        svgPath(
            "M74.5 203.5 C72.5 204 70.5 205 69 206 C67.5 207 66.5 209 65.5 210 " +
                "C65 211.5 64.5 211.5 64.5 212.5 C64 213.5 65 215.5 64.5 217 C64 218 61.5 218 60.5 219.5 " +
                "C60 220.5 59.5 222.5 59 224.5 C58.5 226.5 57.5 228.5 57.5 232.5 C57 237 57 245 57.5 249.5 " +
                "C57.5 254 57.5 255.5 59 259 C60.5 262.5 63 268 65.5 270.5 C68 273.5 70.5 275.5 73.5 276.5 " +
                "C76.5 278 80 278.5 84 278.5 C88 278.5 93 278 96.5 277 C100 276 103.5 274.5 105 272.5 " +
                "C106.5 270 106.5 265.5 105 263.5 C103.5 261.5 97 264.5 96 261 " +
                "C94.5 257.5 97 248.5 96.5 242.5 C96.5 236.5 95 229 94 225 C93 221 92.5 220 91 217.5 " +
                "C90 214.5 88 211.5 86.5 209.5 C84.5 207.5 83 206 81 205 C79 204 76.5 203.5 74.5 203.5 Z",
        ),
        TireTones,
    )

/** The radius the rim is built at around the origin, before it is fitted to each hub ellipse. */
private const val RIM_R = 13f
private const val RIM_SPOKES = 5
private const val GAP_INNER = 4.6f
private const val GAP_OUTER = 11f
private const val GAP_HALF_DEG = 22f

/** The cream rim face, its center cap and the five dark gaps between its spokes, around the origin. */
private val RimDisc = VinylShape(ovalPath(0f, 0f, RIM_R, RIM_R), ArtTones.Cream)
private val RimCap = VinylShape(ovalPath(0f, 0f, 3.2f, 3.2f), ArtTones.Cream)
private val RimGaps =
    Path().apply {
        for (k in 0 until RIM_SPOKES) {
            val mid = -90.0 + 36.0 + k * 360.0 / RIM_SPOKES

            fun at(
                r: Float,
                deg: Double,
            ) = Offset(r * cos(Math.toRadians(deg)).toFloat(), r * sin(Math.toRadians(deg)).toFloat())
            val a = at(GAP_INNER, mid - GAP_HALF_DEG * 0.6)
            val b = at(GAP_OUTER, mid - GAP_HALF_DEG)
            val c = at(GAP_OUTER + 1.2f, mid)
            val d = at(GAP_OUTER, mid + GAP_HALF_DEG)
            val e = at(GAP_INNER, mid + GAP_HALF_DEG * 0.6)
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            quadraticTo(c.x, c.y, d.x, d.y)
            lineTo(e.x, e.y)
            close()
        }
    }

/**
 * The painted body in one shape: the spoiler, the cabin, the side and the front with its lower lip. The
 * glass, the eyes and the dark details are drawn on top of it. The gap under the spoiler wing stays
 * open: it is cut out with an even-odd fill.
 */
private val Body =
    svgPath(
        "M102 160.5 C94 160.5 83.5 162.5 78 163.5 C72.5 164.5 71.5 165.5 68.5 167 " +
            "C65.5 168.5 66.5 171.5 60 173 C54 174 38 174 31.5 175 C25 176 22 178 20.5 179 " +
            "C18.5 180.5 19 182.5 20 183.5 C21 184 24.5 183.5 25.5 184.5 C26.5 185.5 27.5 186 25.5 188.5 " +
            "C24 190.5 18 194 16 197.5 C14 201 12.5 208 12.5 210 C12.5 212 15 211 16 210 " +
            "C16.5 209 16 206.5 17 205 C17.5 204 19 202.5 20.5 202 C22 201.5 24 202 25.5 202.5 " +
            "C27 203.5 27.5 203.5 29 206.5 C30.5 209.5 33.5 215 34.5 221.5 C35.5 228 35.5 242 36 246.5 " +
            "C36.5 251.5 34 248.5 38 250.5 C41.5 252.5 54.5 260 58 258 C61 256 57 243 57.5 238 " +
            "C57.5 233.5 59 230.5 60.5 228 C61.5 226 64.5 226 65 224.5 C66 223 64.5 220.5 65 219 " +
            "C66 217.5 67.5 215.5 69 214.5 C70 213.5 72 212.5 73.5 212 C75 211.5 75.5 211 78 212 " +
            "C80 213 84 215 86.5 218 C89 220.5 91 224 92.5 228.5 C94.5 233 96 240.5 96.5 245.5 " +
            "C97 251 97 256.5 97 260 C96.5 263.5 95 266 95 267.5 C95 269.5 95 269.5 97 270 " +
            "C99 271 100.5 272 106.5 272.5 C113 273 129.5 273.5 135.5 273.5 C141 273.5 138.5 274 141 273 " +
            "C143.5 272.5 140 270 149.5 269 C159 268 187.5 267 196.5 267 C206 267.5 201.5 269.5 205.5 270 " +
            "C209.5 270 217 269 220.5 268 C224.5 267.5 226.5 267 227.5 265.5 C229 264.5 229 262.5 228.5 261 " +
            "C228.5 259.5 226 263 225.5 256 C225 249.5 226 228.5 225 221 C224.5 213.5 223 214.5 221.5 212 " +
            "C220 209 219 207.5 215.5 205 C212.5 202.5 206 198.5 201.5 197 C197 195 189.5 196 187 194.5 " +
            "C185 192.5 189 188 188.5 186 C188 184 185.5 183 183.5 182.5 C181.5 182 177.5 181.5 175.5 182.5 " +
            "C173 183 171 186.5 169.5 187.5 C168 188 169 189.5 166 187.5 C163 185 156.5 177 152 173.5 " +
            "C147.5 170 144 167.5 139.5 165.5 C135 163.5 131.5 162.5 125.5 161.5 " +
            "C119 160.5 109.5 160 102 160.5 Z M48.5 181.5 C47 181.5 41.5 182 40 182.5 " +
            "C39 182.5 41 182.5 41 183 C41 183.5 40 185 41 185.5 C41.5 186 45 185.5 46.5 185 " +
            "C48 184.5 49.5 182.5 49.5 182 C50 181.5 50 181.5 48.5 181.5 Z",
    ).apply { fillType = PathFillType.EvenOdd }

/** The shade under the spoiler wing, where it stands off the rear deck. */
private val SpoilerShadow = ovalRect(42f, 186f, 20f, 3.5f)

/**
 * The paint shading of the reference in four nested levels, each darker area inside the last: drawn as
 * stacked translucent shade, they carry the reference's hood, fenders, arches and lip in any paint color.
 */
private val ShadeLevels =
    listOf(
        svgPath(
            "M52.5 177.5 C47 178.5 27 180.5 21.5 181.5 C16.5 182.5 20 183 21.5 183 " +
                "C23 183.5 29 182.5 30.5 183 C32 183.5 29.5 186 31 186.5 C32.5 187 38.5 187 39.5 186 " +
                "C41 185 37 182.5 39 181.5 C41 180.5 49 180.5 51 180.5 C53.5 181 51.5 182 52 182 " +
                "C52 182.5 53 182.5 53.5 182 C54 181 54.5 178.5 54.5 177.5 C54.5 177 58 177 52.5 177.5 Z " +
                "M185 188 C183 188.5 174.5 189 172 190.5 C169.5 191.5 174 195 171 196 " +
                "C168 197 156 195.5 153 196 C150 196 152 197.5 153 197.5 C154 198 157.5 197 159 197.5 " +
                "C160.5 198 160 200 161.5 200.5 C163.5 201 167.5 200 169 200.5 C171 200.5 172.5 201.5 173 202 " +
                "C173.5 202.5 172 203.5 173 204 C174.5 204 179.5 203.5 180.5 204 " +
                "C182 204.5 179.5 205.5 180.5 205.5 C182 206 187 207 188 206.5 C189 206 187.5 204 186 202.5 " +
                "C184.5 201 179.5 198.5 178.5 197.5 C177 196 178 195.5 178.5 195 " +
                "C179 194.5 181 195.5 181.5 195 C182 194.5 180.5 194 181.5 193 " +
                "C182.5 192 186 190.5 186.5 189.5 C187.5 189 187 188 186.5 188 " +
                "C186.5 187.5 187.5 187.5 185 188 Z M50.5 191.5 C48.5 191.5 50.5 193.5 51 194 " +
                "C51.5 195 51.5 196 54 196.5 C56.5 197.5 63.5 198.5 65.5 199.5 C67.5 200 66.5 200 65.5 201 " +
                "C64.5 202.5 61 204.5 60 205.5 C59 207 59.5 208 60.5 208.5 C61 209.5 63.5 209 64 209.5 " +
                "C64.5 210 65 211 64 211.5 C63 212 61 214.5 57.5 213.5 C53.5 212.5 45.5 206.5 42 205.5 " +
                "C38.5 204 37.5 205 37 206 C36 207 36 211 36.5 212 C36.5 213 37.5 212.5 38 212 " +
                "C38.5 211.5 38 208.5 38.5 208 C39.5 207.5 42 208 43 208.5 C44 209 44 207 45 211 " +
                "C45.5 215 47.5 228.5 47.5 232 C47 235.5 45 233.5 44 232 C42.5 230.5 41 224.5 40 223 " +
                "C39.5 221.5 39 223 38.5 223 C38.5 223.5 39.5 224.5 38.5 224.5 C38 225 36 221 35.5 224.5 " +
                "C35 228 36 242 36.5 246 C37 250.5 35.5 248.5 39 250.5 C42.5 252.5 54.5 259 57.5 257.5 " +
                "C60.5 255.5 56.5 245 56.5 240.5 C57 236 58.5 232.5 59 230 C60 227.5 60 227 61 226 " +
                "C62 225 64.5 225 65 224 C65.5 223 63 222 64 220 C64.5 218.5 68.5 215 69.5 213.5 " +
                "C70.5 212.5 70 212.5 69.5 212 C69 212 67.5 212.5 67 212 C66.5 211.5 66 210 67 208.5 " +
                "C68 207 71 204.5 72.5 203 C74.5 201.5 77.5 201 78.5 200 C79.5 199 79 198 78.5 197.5 " +
                "C78 197.5 75.5 198 74.5 198 C74 198 73.5 197.5 73 197 C73 196.5 75 194.5 73 194 " +
                "C71.5 193.5 66.5 194 62.5 193.5 C58.5 193.5 52 191.5 50.5 191.5 Z M17 198 " +
                "C16.5 200 14 207.5 13.5 209.5 C13 211.5 15 210 15.5 209.5 C16 209 15.5 207 16 206 " +
                "C16.5 204.5 17.5 203 18.5 202.5 C20 201.5 23 201.5 24 201 C25 201 25 200 24 199.5 " +
                "C23 199.5 19.5 200 18.5 200 C17.5 199.5 19 198.5 18.5 198 C18.5 197.5 18 196 17 198 Z " +
                "M96.5 242 C96.5 245 97.5 256.5 97.5 260.5 C97 264.5 95.5 266 95.5 267.5 " +
                "C95.5 269 95 268.5 96.5 269 C98.5 270 104 271 105.5 271.5 C107 272 100 272 105.5 272.5 " +
                "C111 273 131.5 274 139 273.5 C146 272.5 140 269.5 149 268 C158 267 184.5 266.5 192.5 266 " +
                "C200.5 266 194.5 265.5 196.5 266 C198.5 266.5 201 269 204.5 269 " +
                "C208.5 269.5 216.5 268 220 267.5 C223.5 266.5 225.5 265.5 226.5 265 " +
                "C227.5 264.5 227 263.5 226.5 263.5 C226 263 228.5 263 224.5 263.5 " +
                "C220.5 264 208 267 203.5 267 C199 267 199 264.5 197.5 264 C195.5 263.5 201 263.5 193 264 " +
                "C185 264 159 264.5 149.5 265.5 C140.5 266.5 145 269.5 138 270 C131 270.5 114.5 270 108 269 " +
                "C102 268 102 265 100.5 264 C99.5 262.5 100 262 100.5 262 C101.5 261.5 104 264 105 262.5 " +
                "C106.5 260.5 108 253 108 251 C108 249 106 252.5 104.5 251 C102.5 249.5 99.5 243.5 98 242 " +
                "C96.5 240.5 96.5 239 96.5 242 Z M212 242 C208.5 242.5 207.5 243 204.5 244.5 " +
                "C202 246 199 250 195.5 251 C192 252.5 185 252 182.5 252 C180.5 252 183 250.5 182.5 250.5 " +
                "C182.5 250 183 250 181 250.5 C179.5 251 176 252.5 173 252.5 C170 253 165 251.5 163.5 252 " +
                "C162 252 167 254 163.5 254 C160 254 148 253.5 142.5 252.5 C137 251 134.5 248 130 247 " +
                "C126 245.5 120.5 245 117 244.5 C114 244.5 112 245 110.5 245.5 C109 246.5 107 249 108.5 249.5 " +
                "C110 250 114 248 118.5 248.5 C122.5 249.5 131.5 252 134 253.5 C137 255 135.5 255.5 135 257 " +
                "C135 258.5 133.5 260.5 132.5 262 C131.5 263 129 263 128.5 263.5 " +
                "C128 264 127.5 264.5 128.5 264.5 C129.5 265 132 265 134.5 264.5 " +
                "C136.5 263.5 135.5 261.5 141.5 261 C147 260.5 161 261.5 169.5 261.5 " +
                "C177.5 261 186 260 191 259.5 C195.5 259.5 196 259.5 197.5 260 " +
                "C199.5 260.5 200.5 262.5 201.5 263 C202.5 263.5 203.5 263.5 204.5 263.5 " +
                "C205.5 263.5 207.5 262.5 208 262 C208.5 261.5 209 261 208 260.5 C207 259.5 203 258 201.5 257 " +
                "C200.5 256 200 255.5 199.5 255 C199 254 198.5 254 199.5 253 " +
                "C200.5 251.5 202.5 250 204.5 248.5 C207 247.5 211 245.5 213.5 245.5 " +
                "C215.5 245 217.5 244 218.5 246 C220 248.5 220 256.5 221 258.5 " +
                "C222 260.5 223.5 261 224.5 258.5 C225 255.5 227 245 225 242 C223 239.5 215 241.5 212 242 Z",
        ),
        svgPath(
            "M50.5 178.5 C46 179 30.5 180.5 26.5 181.5 C22.5 182 26 183 26.5 183 " +
                "C27 183.5 30 182.5 30.5 183 C31.5 183.5 30 186 31 186.5 C32 187 35 186.5 35.5 186 " +
                "C36.5 186 36 185 36.5 184.5 C37 184 38.5 185 39 184.5 C39.5 184 37 182 39 181.5 " +
                "C41 181 49 181 51.5 180.5 C54 180 53.5 178.5 53 178.5 C53 178 54.5 178 50.5 178.5 Z " +
                "M185 188.5 C182.5 189 174.5 189.5 172 190.5 C169.5 192 169.5 195 171 195.5 " +
                "C172.5 196 177.5 195 180 194.5 C182.5 193.5 185 191.5 186 190.5 " +
                "C187 189.5 186.5 188.5 186.5 188.5 C186.5 188 187 188 185 188.5 Z M50.5 192 " +
                "C48.5 192 50.5 193.5 51.5 194.5 C52 195.5 53 196.5 55.5 197.5 C58 198 65 198.5 67 199 " +
                "C69 199.5 67.5 200.5 67 201 C66 202 63.5 203 62.5 204 C62 205.5 62 207.5 63 208 " +
                "C64 208.5 66 209 68 207.5 C70.5 206.5 75 202 76.5 200.5 C78 199 77 199 76.5 199 " +
                "C76 199 73.5 201 72.5 200.5 C72 199.5 74.5 195 73 194 C72 193 68 194.5 64.5 194 " +
                "C60.5 194 53 192 50.5 192 Z M38.5 205.5 C36.5 205 37 206 37 206.5 " +
                "C36.5 207 36 209 36.5 209.5 C37.5 209.5 40.5 207.5 41.5 208 C43 208.5 44 208.5 45 211.5 " +
                "C46 215 47 223.5 47.5 227 C47.5 231 46 233.5 46 235 C46 236.5 46.5 236 47.5 236.5 " +
                "C48 236.5 49.5 235.5 50.5 236.5 C52 237 53 239.5 54 240 C55 241 55.5 243 56.5 241 " +
                "C57.5 238.5 58.5 230.5 60 227.5 C61 225 64 225 65 224 C65.5 222.5 63.5 221.5 64 220 " +
                "C64 218.5 66.5 216.5 66 215.5 C66 214.5 64 214.5 62.5 214.5 C61.5 214.5 61 217 58.5 216 " +
                "C56.5 215 53 211.5 49.5 209.5 C46.5 208 40.5 206 38.5 205.5 Z M36.5 233 " +
                "C36 235.5 36.5 243.5 36.5 246 C37 249 34.5 247.5 38 249.5 C41.5 251.5 54.5 258 57.5 257.5 " +
                "C60.5 256.5 57.5 247.5 56.5 245 C56 243 54 244.5 52 243.5 C50 242 47 239 45.5 238 " +
                "C44 237 43 238 42 237.5 C40.5 236.5 39 234 38 233 C37 232.5 36.5 231 36.5 233 Z M212.5 242 " +
                "C210 242.5 206 244 203 246 C200.5 248 198.5 252.5 195 254.5 " +
                "C191.5 256.5 188.5 256.5 181.5 257 C174.5 257.5 159.5 257.5 153 257 " +
                "C146.5 257 146.5 257.5 143 256 C139.5 254 136.5 249.5 132 248 C127.5 246 120.5 245 117 245 " +
                "C113.5 244.5 112 245 110.5 246 C109 246.5 107.5 249 109 249.5 C110 250 114 248 118.5 248.5 " +
                "C122.5 249.5 130.5 252 133.5 253 C136.5 254 135 253 135 255 " +
                "C135.5 256.5 132.5 262.5 134 263.5 C135.5 264.5 138 260.5 144.5 260.5 " +
                "C151 260 163.5 261 172.5 261 C181.5 260.5 193 259 198 259 C203 259 202 261.5 202 260.5 " +
                "C202.5 259.5 199 255 199.5 253 C200 251 202 250 204.5 248.5 C207 247.5 212 245.5 214.5 245 " +
                "C216.5 244.5 217.5 244 218.5 246 C219.5 248.5 220 256 221 258 C222 260 223.5 260 224.5 257.5 " +
                "C225 255 225 244.5 225 242 C225 239.5 224 241.5 223.5 242 C223 242.5 223 244 222.5 244 " +
                "C222 244.5 221 244.5 220.5 244 C220 244 220.5 242.5 219 242 " +
                "C217.5 241.5 215 241.5 212.5 242 Z M97.5 253 C96.5 253.5 97.5 258 97.5 260.5 " +
                "C97 263 95.5 266 95.5 267.5 C95.5 269 96 269 97.5 269.5 C99.5 270.5 104.5 271 105.5 271.5 " +
                "C107 272 103.5 272 105.5 272.5 C108 273 113.5 273 119 273.5 C124.5 273.5 135 273.5 139 273.5 " +
                "C143 273 142.5 271.5 143 270.5 C144 270 144 269.5 143 269 C142.5 269 144 270.5 138 270.5 " +
                "C132 270.5 113.5 270.5 107 269 C100.5 268 101 264.5 99.5 263.5 C98.5 262 99 262 99.5 261.5 " +
                "C100.5 261.5 104 263 105 262 C106.5 261.5 106.5 257 106 256 C105.5 255 104 257 102.5 256 " +
                "C101 255.5 98 252 97.5 253 Z M225 263.5 C221.5 264 210 267 205.5 267 " +
                "C201 267 206.5 264.5 197.5 264 C188.5 264 159.5 265 150.5 266 " +
                "C141.5 266.5 145.5 267 144 267.5 C143 268 142.5 269.5 143.5 269.5 " +
                "C144 270 140.5 268.5 149 268 C157 267.5 184.5 266.5 192.5 266 " +
                "C200.5 266 194.5 265.5 196.5 266 C198.5 266.5 200 269 204.5 269 " +
                "C209 269.5 220 267 223.5 266.5 C227 266 226 265.5 226.5 265 C227 264.5 226.5 264 226.5 263.5 " +
                "C226 263.5 228.5 263 225 263.5 Z",
        ),
        svgPath(
            "M36.5 180.5 C35 181 31.5 181 30.5 182 C29.5 183 30 186 31 186.5 C32 187 35.5 186.5 36.5 186 " +
                "C37.5 185.5 36 184.5 36.5 184 C37 184 38.5 184.5 39 184 C39.5 183.5 39.5 181 39 180.5 " +
                "C38.5 180 38 180.5 36.5 180.5 Z M184.5 189 C184 189 183.5 190 181 190.5 " +
                "C179 190.5 173.5 190 172 191 C170.5 191.5 170 194.5 171 195 C172.5 195.5 177.5 195 180 194.5 " +
                "C182.5 193.5 185 191.5 186 190.5 C187 189.5 186.5 189 186 189 " +
                "C186 188.5 185.5 188.5 184.5 189 Z M51.5 192.5 C47.5 192.5 50.5 194 51.5 194.5 " +
                "C52.5 195.5 53.5 197 56.5 197.5 C59 198.5 65.5 198.5 67 199 C69 200 67.5 201 67.5 202 " +
                "C67 202.5 65.5 203 65 204 C64.5 205 64 206.5 65 206.5 C66 207 68 206 69.5 205.5 " +
                "C71.5 205 73.5 203 74 202.5 C75 201.5 74.5 201 74 201 C74 200.5 72 201 71.5 200 " +
                "C71.5 199 76.5 195.5 73 194.5 C70 193 55 192.5 51.5 192.5 Z M44 208 " +
                "C43 211.5 46 227.5 47.5 232 C48.5 236.5 49 235.5 50.5 236 C51.5 236.5 54 234.5 55 235 " +
                "C55.5 236 54.5 239 55 240 C55 241 56 242.5 57 240.5 C58 238.5 59 230 60 227.5 " +
                "C60.5 225.5 60.5 226 61 226 C61.5 225.5 63.5 226 64 225 C64.5 224.5 65 223 65 222 " +
                "C64.5 221.5 62.5 221.5 62 221 C61.5 220.5 62.5 219 62 218.5 C61.5 218 58.5 218 58 217.5 " +
                "C57 217 57.5 216 56.5 215 C55.5 214 53 212.5 51 211.5 C49 210 44.5 204.5 44 208 Z " +
                "M36.5 239.5 C36 240.5 36.5 244.5 36.5 246 C37 248 35 248 38.5 250 " +
                "C42 252 54.5 257.5 57.5 257.5 C60.5 257 58.5 250 56.5 248.5 C55 246.5 50.5 247 48 246 " +
                "C45.5 244.5 43 241.5 41 240.5 C39 239.5 37.5 238.5 36.5 239.5 Z M213.5 242 " +
                "C210.5 242.5 206.5 244 203.5 246 C200.5 248.5 197 254.5 195 256 C192.5 258 191 257 190 257.5 " +
                "C189.5 258 188.5 258.5 190 259 C191.5 259 198 260 199.5 259 C201.5 258 198.5 254 200.5 251.5 " +
                "C202.5 249.5 209 247 212 245.5 C214.5 244.5 216.5 244.5 218 245.5 " +
                "C219 246 219 249 219.5 249.5 C220 250.5 221.5 251 221 249.5 C221 248.5 220.5 243.5 219 242.5 " +
                "C217.5 241 216 241.5 213.5 242 Z M113.5 245 C111.5 245 111.5 245.5 110.5 246 " +
                "C110 247 108 248.5 109 249 C110.5 249.5 114.5 248 118.5 248.5 C122 249 128.5 251 131.5 252 " +
                "C134 253 134.5 253.5 135 254.5 C135.5 255.5 135 257 135 258 C135.5 258.5 137 258.5 137.5 259 " +
                "C138 260 136 261.5 138 261.5 C139.5 261.5 146.5 260.5 148 260.5 C149.5 260 149 259.5 148 259 " +
                "C147 258 145.5 258.5 143 257 C140.5 255.5 137 251 133.5 249 C130.5 247 127 246.5 123.5 246 " +
                "C120.5 245.5 116 245 113.5 245 Z M97.5 259.5 C96.5 261 95.5 266 95.5 268 " +
                "C95.5 269.5 96 269 97.5 269.5 C99 270 104.5 271 105.5 271.5 C107 272 100 272 105.5 272.5 " +
                "C111 273 133 273.5 139 273.5 C144.5 273 140.5 272.5 141 272 " +
                "C141.5 271.5 146.5 270.5 141 270.5 C135.5 270 114 270.5 107.5 270 " +
                "C101 269.5 103.5 269 102 268 C100.5 267.5 99 267 98.5 265.5 C98 264 99.5 260.5 99.5 259.5 " +
                "C99 258.5 98 258 97.5 259.5 Z M224 264 C221 264.5 210 267 206.5 267.5 " +
                "C203 268 204.5 268 203 267.5 C201.5 267 200.5 265 197 264.5 C193.5 264 184.5 265 182 265.5 " +
                "C180 266 180 267 182 267 C184.5 267 192.5 266 196.5 266 C200 266.5 200 269 204.5 269 " +
                "C209 269.5 220 267 223.5 266.5 C227 266 225.5 266 225.5 265.5 C226 265 226 264.5 225.5 264 " +
                "C225.5 264 227 263.5 224 264 Z M149.5 266 C146 266.5 147 266.5 146.5 267 " +
                "C146 267 143 268 146.5 268.5 C150 268.5 164.5 268.5 168 268 C171.5 267.5 168 266.5 168 266.5 " +
                "C167.5 266 169.5 266.5 166 266 C163 266 152.5 266 149.5 266 Z",
        ),
        svgPath(
            "M183 190.5 C181 190.5 174 191 172 191.5 C170 192 171.5 193 172 193.5 " +
                "C172.5 194 174 193.5 174 193.5 C174.5 193.5 173 194.5 174 195 C175 195 178.5 195 180 194.5 " +
                "C182 194 183.5 192.5 184.5 192 C185 191 184.5 190.5 184.5 190.5 C184 190 185 190 183 190.5 Z " +
                "M59 194.5 C56.5 194.5 56.5 194.5 56.5 195 C56 195.5 55.5 196 56.5 196.5 " +
                "C57 197 58 198 60 198 C61.5 198.5 66 198 67 199 C68.5 200 67 204 68 205 " +
                "C69 205.5 72.5 204 72.5 203 C73 202 71 199.5 71 198.5 C71 197.5 72.5 198 73 197.5 " +
                "C73.5 197 75.5 195.5 73 195 C70.5 195 62 194.5 59 194.5 Z M45 210 " +
                "C44 210.5 45 215.5 45.5 216.5 C46 218 47 217 48 217 C48.5 216.5 50 215.5 50.5 214.5 " +
                "C51 214 51.5 213 50.5 212.5 C49.5 211.5 45.5 209 45 210 Z M214 242 " +
                "C212.5 242.5 210.5 243 210 243.5 C209 244 208 245.5 209 246 C210 246 215 245 217 245 " +
                "C219 245 220 247.5 220.5 247 C220.5 246.5 219.5 243 218.5 242.5 " +
                "C217.5 241.5 215.5 242 214 242 Z M114 245.5 C111.5 245.5 111 246 110.5 246.5 " +
                "C109.5 247 107 248.5 109.5 249 C112 249 122 248.5 124.5 248 C127 247.5 125.5 247 124 246.5 " +
                "C122 246 116 245 114 245.5 Z M37 247 C36 247 35.5 248.5 39 250.5 " +
                "C42.5 252 54.5 257 57.5 257.5 C60.5 257.5 58 253 57 252 C56 251 53.5 252.5 51.5 251.5 " +
                "C49.5 251 47.5 249.5 45 248.5 C42.5 247.5 38 246.5 37 247 Z M105.5 270.5 " +
                "C102 271 102 272 105.5 272.5 C109 273 123.5 273.5 127 273.5 C130.5 273 130 271.5 126.5 271 " +
                "C123 270.5 109 270.5 105.5 270.5 Z",
        ),
    )
private val RoofShine = specularRect(82f, 167f, 22f, 4.5f)
private val HoodShineLeft = specularRect(96f, 184f, 10f, 6f)
private val HoodShineRight = specularRect(156f, 180f, 9f, 9f)
private val SpoilerShine = specularRect(30f, 192f, 12f, 3.5f)
private val FenderShine = specularRect(86f, 203f, 8f, 4f)

private val GlassTones = vinylTones(PersonalizationColors.CarGlass)
private val SideWindow =
    VinylShape(
        svgPath(
            "M71 171.5 C69.5 172 68.5 172 66 173.5 C63.5 175 59.5 177.5 56.5 180 " +
                "C54 182 52 184.5 50.5 186 C49 188 49 189 49 189.5 C49 190.5 50 191 50.5 190.5 " +
                "C51.5 190 52.5 188 54 187.5 C55.5 186.5 57.5 185.5 59.5 185.5 C62 185 64.5 185 66.5 185.5 " +
                "C69 186 72 187.5 73 188.5 C74.5 189.5 74.5 191 75 192 C75 193 75 193.5 75 194.5 " +
                "C74.5 195 72.5 196 73.5 196.5 C74 196.5 78.5 200.5 79.5 197 C80 193.5 78 179.5 77 175 " +
                "C76.5 171 76 172.5 75 172 C74 171.5 72.5 171.5 71 171.5 Z",
        ),
        GlassTones,
    )
private val Windshield =
    VinylShape(
        svgPath(
            "M124 171 C115 171 93 173 86.5 173.5 C80 174.5 85 175 84.5 176 C84.5 177 84.5 176 84.5 179.5 " +
                "C85 183 81.5 194.5 87 197 C92 199.5 102.5 195 116.5 195 C130.5 194.5 162.5 196.5 170.5 195 " +
                "C178 194 167 190.5 163 187 C159.5 183 151.5 176 148 173 C144 170.5 145.5 171.5 141.5 171.5 " +
                "C137.5 171 133 170.5 124 171 Z",
        ),
        GlassTones,
    )
private val WindshieldShine = specularRect(140f, 178f, 14f, 4f)

private val Vent =
    svgPath(
        "M40 208.5 C39 208.5 38.5 208 38.5 210 C38.5 212 39 216.5 40 219.5 C40.5 223 43 227.5 43.5 229.5 " +
            "C44.5 231.5 44.5 231 45 231.5 C45.5 232 46 232.5 46 232 C46.5 231 47 231 46.5 227.5 " +
            "C46.5 224.5 45 215.5 44.5 212.5 C44 209.5 44 210.5 43 210 C42.5 209 41 208.5 40 208.5 Z",
    )
private val Intakes =
    svgPath(
        "M112.5 249 C109 249 110.5 250 109.5 251 C109 251.5 108.5 252.5 108 254.5 " +
            "C107 256.5 103.5 260.5 106.5 262 C109 263.5 120 263 124 263 C128 263 127.5 262 129 261 " +
            "C131 260 134 257.5 134.5 256.5 C135.5 255.5 134.5 255 133.5 254.5 C132.5 253.5 132 253 128.5 252 " +
            "C125 251 115.5 249.5 112.5 249 Z M215 245.5 C213 246.5 206.5 249 204 250 " +
            "C201.5 251.5 199.5 252.5 200 254.5 C201 256 204.5 259 208 259.5 C211 260 217.5 258.5 219.5 258 " +
            "C221.5 257.5 220.5 258 220.5 256 C220 254.5 219 250 218.5 248 C218 246.5 217.5 246.5 217 246 " +
            "C216.5 245.5 217 245 215 245.5 Z",
    )

/** The headlight eyes: almond whites, the left one larger since it is nearer, with their pupils. */
private val EyeLeft =
    VinylShape(
        svgPath(
            "M104 233 C108 223 124 218 136 222 C141 224 143 229 141 233 " +
                "C138 239 128 241 118 240 C111 239 106 237 104 233 Z",
        ),
        ArtTones.Cream,
    )
private val EyeRight =
    VinylShape(
        svgPath(
            "M196 229 C198 221 208 216 216 219 C220 221 221 226 219 231 " +
                "C216 236 208 238 202 236 C198 235 196 232 196 229 Z",
        ),
        ArtTones.Cream,
    )
private val PupilLeft = Offset(127f, 231f)
private val PupilRight = Offset(206.5f, 229f)
private val Mouth =
    svgPath(
        "M185 243.5 C184 244 181.5 246 179.5 247 C177.5 247.5 175.5 248 173 248.5 " +
            "C170.5 248.5 166.5 248 164.5 247.5 C162 247 160.5 246.5 159.5 245.5 C158 245 157.5 244 156.5 244 " +
            "C156 243.5 155.5 243.5 155 244 C154.5 244.5 154 245 154.5 246 C155 246.5 156.5 248 158 248.5 " +
            "C159.5 249.5 160.5 250 163 250.5 C165.5 251 170 252 173 251.5 C175.5 251.5 178 251 180 250 " +
            "C182 249.5 184.5 248 185.5 247.5 C186.5 246.5 186.5 246.5 187 246 " +
            "C187.5 245.5 187.5 244.5 187.5 244 C187 243.5 186.5 243 185 243.5 Z",
    )
