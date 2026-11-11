// Literal coordinates place each item on the car in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import kotlin.math.cos
import kotlin.math.sin
import fi.clocklings.ui.theme.PersonalizationColors as C

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.

/** The hub radius that hubcap artwork is drawn at, around the origin, before it is fitted to each hub. */
private const val HUB = 13f

/** The tire radius that tire artwork is drawn at, around the origin, before it is fitted to each tire. */
private const val TIRE = 27f

/** Draws [block], built around the origin at radius [size], on both side wheels, squashed to each [wheels] ellipse. */
private inline fun DrawScope.onWheels(
    wheels: Array<CarHub>,
    size: Float,
    block: DrawScope.() -> Unit,
) {
    for (i in wheels.indices) {
        val wheel = wheels[i]
        withTransform({
            translate(wheel.center.x, wheel.center.y)
            scale(wheel.rx / size, wheel.ry / size, Offset.Zero)
        }, block)
    }
}

/** Draws hubcap [block], built around the origin at radius [HUB], on both side hubs. */
private inline fun DrawScope.onHubs(block: DrawScope.() -> Unit) = onWheels(CarHubs, HUB, block)

/**
 * Draws roof [block], written for a roof standing on (120, 77), on the sports car's low roof: moved onto
 * the roof and scaled down to its narrower width.
 */
private inline fun DrawScope.onRoof(block: DrawScope.() -> Unit) =
    withTransform({
        translate(RoofTop.x - RoofDrawn.x, RoofTop.y - RoofDrawn.y)
        scale(ROOF_SCALE, ROOF_SCALE, RoofDrawn)
    }, block)

/** Where the roof items are drawn standing, and where they stand on the car's roof. */
private val RoofDrawn = Offset(120f, 77f)
private val RoofTop = Offset(98f, 164f)
private const val ROOF_SCALE = 0.72f

/** Lines from the origin to radius [length], [count] of them evenly spaced, the first pointing up. */
private fun spokes(
    count: Int,
    length: Float,
) = Path().apply {
    for (k in 0 until count) {
        val a = Math.toRadians(-90.0 + k * 360.0 / count)
        moveTo(0f, 0f)
        lineTo(length * cos(a).toFloat(), length * sin(a).toFloat())
    }
}

/** Rims in [tones] with [count] spokes in [spokeColor] on both hubs. */
private fun rims(
    tones: VinylTones,
    spokeColor: Color,
    count: Int,
): ItemArt {
    val rim = itemCircle(0f, 0f, 14f, tones)
    val lines = spokes(count, 11f)
    val spokeStroke = itemStroke(3f)
    val cap = itemCircle(0f, 0f, 4f, tones)
    return ItemArt(ArtPart.WHEELS, ItemLayer.FRONT) {
        onHubs {
            vinylShape(rim)
            specular(rim.bounds)
            drawPath(lines, spokeColor, style = spokeStroke)
            vinylShape(cap)
        }
    }
}

/** Yellow number plate on the bumper with a little clock and the number 12. */
private fun plate(): ItemArt {
    val plate = VinylShape(roundRectPath(130f, 247f, 182f, 266f, 4f), ArtTones.Sun)
    val face = itemCircle(143f, 256.5f, 6.5f, ArtTones.Pearl)
    val hands = svgPath("M143 256.5 L143 252 M143 256.5 L146.5 256.5")
    val twelve = svgPath("M158 251.5 L160 250 L160 262.5 M165 253 Q168 249 171.5 252 Q173 255 165 262.5 L173 262.5")
    val ink = Stroke(1.6f)
    val digits = itemStroke(2.2f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        // Drawn at its first size and fitted onto the lower lip, below the smile.
        withTransform({
            translate(PlateOn.x - PlateDrawn.x, PlateOn.y - PlateDrawn.y)
            scale(PLATE_SCALE, PLATE_SCALE, PlateDrawn)
        }) {
            vinylShape(plate)
            specular(plate.bounds)
            vinylShape(face)
            drawPath(face.path, C.EyeInk, style = ink)
            drawPath(hands, C.EyeInk, style = ink)
            drawPath(twelve, C.EyeInk, style = digits)
        }
    }
}

/** The plate's center as drawn, and where it sits on the lip. */
private val PlateDrawn = Offset(156f, 256.5f)
private val PlateOn = Offset(166f, 262f)
private const val PLATE_SCALE = 0.7f

/** Silver sport rims with six spokes. */
private fun sportRims(): ItemArt = rims(ArtTones.Steel, ArtTones.Steel.shade, 6)

/** A roof rack with a red suitcase strapped on. */
private fun suitcase(): ItemArt {
    val rack = svgPath("M80 77 L80 64 M160 77 L160 64 M70 64 L170 64")
    val rackStroke = itemStroke(4.5f)
    val case = VinylShape(roundRectPath(84f, 32f, 156f, 63f, 7f), ArtTones.Ruby)
    val straps = svgPath("M102 33 L102 62 M138 33 L138 62")
    val strapStroke = Stroke(5f)
    val handle = svgPath("M110 32 L110 25 Q120 20 130 25 L130 32")
    val handleStroke = itemStroke(3.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        onRoof {
            drawPath(rack, ArtTones.Steel.base, style = rackStroke)
            drawPath(handle, ArtTones.Steel.shade, style = handleStroke)
            vinylShape(case)
            drawPath(straps, ArtTones.Straw.base, style = strapStroke)
            specular(case.bounds)
        }
    }
}

/** Dark sunglasses over the headlight eyes, the near arm reaching back along the side. */
private fun sunglasses(): ItemArt {
    val lenses = VinylShape(itemPath(ovalPath(123f, 229f, 21f, 14f), ovalPath(209f, 227f, 14.5f, 13f)), ArtTones.EyeInk)
    val frame = svgPath("M143 225 Q170 216 195 222 M103 226 L88 216 M223 224 L229 219")
    val frameStroke = itemStroke(4f)
    val glints = svgPath("M111 223 L119 218 M201 221 L207 216")
    val glintStroke = itemStroke(3f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(frame, C.EyeInk, style = frameStroke)
        vinylShape(lenses)
        drawPath(glints, Color.White, alpha = 0.7f, style = glintStroke)
    }
}

/** Chunky off-road tires: tread blocks all around each tire. */
private fun offRoadTires(): ItemArt {
    val tread =
        Path().apply {
            for (k in 0 until TREAD_BLOCKS) {
                val a = k * 2 * Math.PI / TREAD_BLOCKS
                val c = cos(a).toFloat()
                val s = sin(a).toFloat()
                // A block straddling the tire's edge, turned to face outward.
                moveTo(23f * c - 4f * s, 23f * s + 4f * c)
                lineTo(31f * c - 4f * s, 31f * s + 4f * c)
                lineTo(31f * c + 4f * s, 31f * s - 4f * c)
                lineTo(23f * c + 4f * s, 23f * s - 4f * c)
                close()
            }
        }
    val tones = vinylTones(C.CarTire)
    val groove = Stroke(2.5f)
    return ItemArt(ArtPart.WHEELS, ItemLayer.FRONT) {
        onWheels(CarTires, TIRE) {
            drawPath(tread, tones.shade)
            drawCircle(tones.shade, 21f, Offset.Zero, style = groove)
        }
    }
}

private const val TREAD_BLOCKS = 14

/**
 * Race car kit: twin white stripes down the hood between the eyes, a white number roundel on the door
 * and a steel splitter under the lower lip. The car has its own spoiler.
 */
private fun raceCar(): ItemArt {
    val stripes =
        itemShape("M146 199 L154 199 L162 238 L154 238 Z M158 199 L166 199 L175 236 L167 236 Z", ArtTones.Pearl)
    // The roundel sits on the door, between the side vent and the front wheel.
    val roundel = itemCircle(54f, 214f, 7f, ArtTones.Pearl)
    val one = svgPath("M52.5 210.5 L55 209 L55 219")
    val oneStroke = itemStroke(2.2f)
    val splitter = VinylShape(roundRectPath(100f, 268f, 228f, 275f, 3.5f), ArtTones.Steel)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(stripes)
        vinylShape(roundel)
        drawPath(one, C.EyeInk, style = oneStroke)
        vinylShape(splitter)
        specular(splitter.bounds)
    }
}

/** A surfboard on a roof rack: a pointed nose to the front, a rounded tail with a fin, and a stripe. */
private fun surfboard(): ItemArt {
    val rack = svgPath("M78 77 L78 66 M162 77 L162 66 M68 66 L172 66")
    val rackStroke = itemStroke(4.5f)
    val fin = itemShape("M34 62 L40 76 L52 63 Z", ArtTones.Teal)
    val board = itemShape("M20 57 Q24 46 110 46 Q196 46 226 55 Q198 64 110 65 Q24 66 20 57 Z", ArtTones.Teal)
    val stripe = svgPath("M32 56 Q120 50 212 55")
    val stripeStroke = itemStroke(4f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        onRoof {
            drawPath(rack, ArtTones.Steel.base, style = rackStroke)
            vinylShape(fin)
            vinylShape(board)
            drawPath(stripe, ArtTones.Sun.base, style = stripeStroke)
            specular(board.bounds)
        }
    }
}

/** A steel bull bar in front of the bumper with two yellow fog lights. */
private fun bullBar(): ItemArt {
    val bar = svgPath("M106 280 L106 264 Q106 257 113 257 L215 257 Q222 257 222 264 L222 280 M106 270 L222 270")
    val barStroke = itemStroke(5f)
    val lights = VinylShape(itemPath(ovalPath(130f, 263.5f, 5.5f), ovalPath(198f, 263.5f, 5.5f)), ArtTones.Sun)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(bar, ArtTones.Steel.base, style = barStroke)
        vinylShape(lights)
        specular(lights.bounds)
    }
}

/** Orange flames licking out from the middle of both hubs. */
private fun flameHubcaps(): ItemArt {
    val outer =
        itemShape("M0 12 C-11 8 -11 -2 -5 -12 C-5 -5 -1 -4 0 -9 C3 -4 4 -2 6 -11 C12 -2 11 8 0 12 Z", ArtTones.Knit)
    val inner = itemShape("M0 10 C-6 7 -6 1 -2 -4 C-1 0 1 0 2 -3 C6 1 6 7 0 10 Z", ArtTones.Sun)
    return ItemArt(ArtPart.WHEELS, ItemLayer.FRONT) {
        onHubs {
            vinylShape(outer)
            vinylShape(inner)
        }
    }
}

/** A black and white checkered flag on a pole on the roof. */
private fun checkeredFlag(): ItemArt {
    val pole = svgPath("M100 74 L100 8")
    val poleStroke = itemStroke(4f)
    val flag = Rect(102f, 10f, 158f, 42f)
    val cloth = svgPath("M102 10 Q130 4 158 10 L158 42 Q130 36 102 42 Z")
    val squares =
        Path().apply {
            val w = flag.width / FLAG_COLUMNS
            val h = flag.height / FLAG_ROWS
            for (row in 0 until FLAG_ROWS) {
                for (col in (row % 2) until FLAG_COLUMNS step 2) {
                    addRect(
                        Rect(
                            flag.left + col * w,
                            flag.top + row * h - 4f,
                            flag.left + (col + 1) * w,
                            flag.top + (row + 1) * h - 4f,
                        ),
                    )
                }
            }
        }
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        onRoof {
            drawPath(pole, ArtTones.Steel.base, style = poleStroke)
            drawPath(cloth, ArtTones.Pearl.base)
            clipPath(cloth) { drawPath(squares, C.EyeInk) }
            drawCircle(ArtTones.Gold.base, 4f, Offset(100f, 8f))
        }
    }
}

private const val FLAG_COLUMNS = 6
private const val FLAG_ROWS = 4

/** Fire engine: a red and a blue flashing light on the mirrors and a ladder leaning behind the car. */
private fun fireEngine(): ItemArt {
    val rails = svgPath("M20 236 L58 112 M34 240 L72 116")
    val railStroke = itemStroke(5f)
    val rungs =
        Path().apply {
            for (k in 1..7) {
                val t = k / 8f
                moveTo(20f + 38f * t, 236f - 124f * t)
                lineTo(34f + 38f * t, 240f - 124f * t)
            }
        }
    val rungStroke = itemStroke(3.5f)
    // The lights stand on the mirrors.
    val red = itemCircle(63f, 177f, 6.5f, ArtTones.Ruby)
    val blue = itemCircle(179f, 175.5f, 6f, ArtTones.Denim)
    val bases =
        VinylShape(
            itemPath(roundRectPath(55f, 181f, 71f, 186f, 2.5f), roundRectPath(172f, 179f, 186f, 184f, 2.5f)),
            ArtTones.Steel,
        )
    val glowRed = ArtTones.Ruby.light.copy(alpha = 0.35f)
    val glowBlue = ArtTones.Sky.light.copy(alpha = 0.45f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT, back = {
        drawPath(rungs, ArtTones.Pearl.shade, style = rungStroke)
        drawPath(rails, ArtTones.Steel.base, style = railStroke)
    }) {
        drawCircle(glowRed, 11.5f, Offset(63f, 176f))
        drawCircle(glowBlue, 11f, Offset(179f, 174.5f))
        vinylShape(red)
        vinylShape(blue)
        vinylShape(bases)
        specular(red.bounds)
        specular(blue.bounds)
    }
}

/** A yellow snowplow blade in front of the bumper, with black warning stripes. */
private fun snowplow(): ItemArt {
    val blade = itemShape("M94 258 Q162 250 230 256 L232 284 Q162 278 90 290 Z", ArtTones.Sun)
    val stripes = svgPath("M116 254 L108 290 M146 252 L138 286 M176 252 L168 284 M206 254 L198 284")
    val stripeStroke = Stroke(7f)
    val edge = svgPath("M90 290 Q162 278 232 284")
    val edgeStroke = itemStroke(4f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(blade)
        clipPath(blade.path) { drawPath(stripes, C.EyeInk, alpha = 0.85f, style = stripeStroke) }
        drawPath(edge, ArtTones.Steel.base, style = edgeStroke)
        specular(blade.bounds)
    }
}

/** A yellow taxi sign with a checkered band on the roof. */
private fun taxiSign(): ItemArt {
    val sign = itemShape("M90 72 L95 46 Q96 42 100 42 L140 42 Q144 42 145 46 L150 72 Z", ArtTones.Sun)
    val band = Rect(93f, 54f, 147f, 62f)
    val checks =
        Path().apply {
            val w = band.width / TAXI_CHECKS
            for (k in 0 until TAXI_CHECKS) {
                val top = if (k % 2 == 0) band.top else band.top + band.height / 2
                addRect(Rect(band.left + k * w, top, band.left + (k + 1) * w, top + band.height / 2))
            }
        }
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        onRoof {
            vinylShape(sign)
            clipPath(sign.path) { drawPath(checks, C.EyeInk) }
            specular(sign.bounds)
        }
    }
}

private const val TAXI_CHECKS = 12

/** Gold rims with five spokes. */
private fun goldRims(): ItemArt = rims(ArtTones.Gold, ArtTones.Gold.shade, 5)

/** A rally light bar on the roof: four round lamps on a steel bar. */
private fun rallyLights(): ItemArt {
    val posts = svgPath("M80 76 L80 60 M160 76 L160 60")
    val postStroke = itemStroke(4f)
    val bar = VinylShape(roundRectPath(68f, 56f, 172f, 63f, 3f), ArtTones.Steel)
    val xs = floatArrayOf(84f, 108f, 132f, 156f)
    val housings = xs.map { itemCircle(it, 46f, 11f, ArtTones.Steel) }
    val lenses = xs.map { itemCircle(it, 46f, 7.5f, ArtTones.Sun) }
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        onRoof {
            drawPath(posts, ArtTones.Steel.shade, style = postStroke)
            vinylShape(bar)
            for (i in housings.indices) {
                vinylShape(housings[i])
                vinylShape(lenses[i])
                specular(lenses[i].bounds)
            }
        }
    }
}

/**
 * Rocket car: two tall rocket fins rising from the back of the car, a rocket nozzle at the rear with a
 * flame shooting out and a lightning bolt on the door.
 */
private fun rocketCar(): ItemArt {
    val farFin = itemShape("M66 182 L54 136 Q54 128 62 134 L88 176 Z", ArtTones.Ruby)
    val nearFin = itemShape("M34 214 L12 160 Q10 150 20 156 L56 196 Z", ArtTones.Ruby)
    val nozzle = itemShape("M22 212 L6 207 L6 237 L22 232 Z", ArtTones.Steel)
    val outer = itemShape("M7 209 Q-16 222 7 235 Z", ArtTones.Knit)
    val inner = itemShape("M7 214 Q-4 222 7 230 Z", ArtTones.Sun)
    // On the door, between the side vent and the front wheel.
    val bolt = itemShape("M58 207 L49 218 L54.5 218 L50.5 226 L63 214 L57.5 214 L62 207 Z", ArtTones.Sun)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT, back = {
        vinylShape(farFin)
        vinylShape(nearFin)
        specular(nearFin.bounds)
    }) {
        vinylShape(outer)
        vinylShape(inner)
        vinylShape(nozzle)
        vinylShape(bolt)
    }
}

/** The car's item artwork keyed by item id. */
internal val CarItemArt: Map<String, ItemArt> =
    mapOf(
        "car-front-plate" to plate(),
        "car-wheels-sport" to sportRims(),
        "car-roof-suitcase" to suitcase(),
        "car-front-sunglasses" to sunglasses(),
        "car-wheels-offroad" to offRoadTires(),
        "car-model-race" to raceCar(),
        "car-roof-surfboard" to surfboard(),
        "car-front-bull-bar" to bullBar(),
        "car-wheels-flames" to flameHubcaps(),
        "car-roof-flag" to checkeredFlag(),
        "car-model-fire-engine" to fireEngine(),
        "car-front-plow" to snowplow(),
        "car-roof-taxi" to taxiSign(),
        "car-wheels-gold" to goldRims(),
        "car-roof-rally-lights" to rallyLights(),
        "car-model-rocket" to rocketCar(),
    )
