// Literal coordinates place each item on the robot in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.components.HeartPath
import fi.clocklings.ui.components.starPath
import fi.clocklings.ui.theme.PersonalizationColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.
// Hats are HEAD/FRONT items over the head; antennas follow ANTENNA and bounce; the screens replace the eyes.

private const val GLOW_ALPHA = 0.3f
private const val SCREEN_GLOW_ALPHA = 0.45f
private const val SCREEN_GLOW_RADIUS = 21f

/** The robot's own antenna: a dark stalk on a little dome with a shining golden ball. */
private fun antenna(): ItemArt {
    val stalk = svgPath("M120 46 L120 24")
    val stalkBrush = vinylBrush(stalk.getBounds().inflate(3f), ArtTones.Metal)
    val base = VinylShape(ovalPath(120f, 46f, 13f, 8f), ArtTones.Metal)
    val ball = itemCircle(120f, 16f, 12f, ArtTones.Gold)
    val stalkStroke = itemStroke(5f)
    return ItemArt(ArtPart.ANTENNA, ItemLayer.BEHIND) {
        drawPath(stalk, stalkBrush, style = stalkStroke)
        vinylShape(base)
        vinylShape(ball)
        specular(ball.bounds)
    }
}

/** Ruby bow tie fastened with a golden hex bolt under the head. */
private fun bowTie(): ItemArt {
    val wings = itemShape("M116 170 L88 152 Q79 170 88 188 Z M124 170 L152 152 Q161 170 152 188 Z", ArtTones.Ruby)
    val folds = svgPath("M94 162 L104 167 M94 178 L104 173 M146 162 L136 167 M146 178 L136 173")
    val bolt =
        VinylShape(
            Path().apply {
                for (k in 0 until 6) {
                    val a = (k * 60f) * (PI.toFloat() / 180f)
                    val x = 120f + 9.5f * cos(a)
                    val y = 170f + 9.5f * sin(a)
                    if (k == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            },
            ArtTones.Gold,
        )
    val boltHole = ovalRect(120f, 170f, 3f)
    val foldStroke = itemStroke(2f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(wings)
        specular(wings.bounds)
        drawPath(folds, ArtTones.Ruby.shade, style = foldStroke)
        vinylShape(bolt)
        drawOval(ArtTones.Gold.shade, boltHole.topLeft, boltHole.size)
    }
}

/** A steel wrench held upright in the right hand, its open jaw on top. */
private fun wrench(): ItemArt {
    val pivot = Offset(189f, 234f)
    val jaw =
        Path().apply {
            op(ovalPath(189f, 186f, 13f), roundRectPath(184f, 168f, 194f, 188f, 2f), PathOperation.Difference)
        }
    val tool =
        VinylShape(
            Path().apply {
                op(jaw, roundRectPath(184f, 192f, 194f, 258f, 5f), PathOperation.Union)
            },
            ArtTones.Steel,
        )
    val groove = svgPath("M189 200 L189 214")
    val grooveStroke = itemStroke(2.4f)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        rotate(22f, pivot) {
            vinylShape(tool)
            specular(tool.bounds)
            drawPath(groove, ArtTones.Steel.shade, style = grooveStroke)
        }
    }
}

/** Sky beanie with a sunny middle panel, a ruby band and a spinning propeller on top. */
private fun propellerCap(): ItemArt {
    val cap = itemShape("M64 64 Q64 20 120 18 Q176 20 176 64 Q120 52 64 64 Z", ArtTones.Sky)
    val panel = itemShape("M120 18 Q98 34 100 58 Q120 54 140 58 Q142 34 120 18 Z", ArtTones.Sun)
    val band = svgPath("M65 62 Q120 50 175 62")
    val bandBrush = vinylBrush(band.getBounds().inflate(4f), ArtTones.Ruby)
    val post = svgPath("M120 18 L120 9")
    val blades =
        VinylShape(
            Path().apply {
                addPath(ovalPath(105f, 8f, 14f, 4.5f))
                addPath(ovalPath(135f, 8f, 14f, 4.5f))
            },
            ArtTones.Ruby,
        )
    val hub = itemCircle(120f, 8f, 4.5f, ArtTones.Gold)
    val bandStroke = itemStroke(7f)
    val postStroke = itemStroke(4f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(cap)
        vinylShape(panel)
        specular(cap.bounds, -12f)
        drawPath(band, bandBrush, style = bandStroke)
        drawPath(post, ArtTones.Metal.base, style = postStroke)
        rotate(-8f, hub.bounds.center) { vinylShape(blades) }
        vinylShape(hub)
    }
}

/** Denim necktie with sunny stripes under a little pearl collar. */
private fun tie(): ItemArt {
    val collar = itemShape("M100 158 L119 163 L108 178 Z M140 158 L121 163 L132 178 Z", ArtTones.Pearl)
    val knot = itemShape("M111 160 L129 160 L125 175 L115 175 Z", ArtTones.Denim)
    val blade = itemShape("M115 175 L125 175 L133 218 L120 232 L107 218 Z", ArtTones.Denim)
    val stripes = svgPath("M104 192 L136 178 M104 207 L136 193 M104 222 L136 208")
    val stripeStroke = itemStroke(4.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(blade)
        clipPath(blade.path) { drawPath(stripes, ArtTones.Sun.base, style = stripeStroke) }
        specular(blade.bounds)
        vinylShape(collar)
        vinylShape(knot)
    }
}

/**
 * A glowing screen face: [shape] at both eye places, filled with [tones], each on a soft radial glow of
 * its light tone (like the robot's own eyes), so the glow has no hard outline.
 */
private fun screenEyes(
    tones: VinylTones,
    shape: (Offset) -> Path,
): ItemArt {
    val centers = listOf(Offset(95f, 94f), Offset(145f, 94f))
    val eyes = Path().apply { for (center in centers) addPath(shape(center)) }
    val fill = VinylShape(eyes, tones)
    val halos = centers.map { ovalRect(it.x, it.y, SCREEN_GLOW_RADIUS) }
    val haloBrushes =
        centers.map {
            Brush.radialGradient(
                0.45f to tones.light.copy(alpha = SCREEN_GLOW_ALPHA),
                1f to tones.light.copy(alpha = 0f),
                center = it,
                radius = SCREEN_GLOW_RADIUS,
            )
        }
    return ItemArt(ArtPart.HEAD, ItemLayer.SCREEN) {
        for (i in halos.indices) drawOval(haloBrushes[i], halos[i].topLeft, halos[i].size)
        vinylShape(fill)
    }
}

/** Pink hearts on the screen in place of the eyes. */
private fun heartEyes(): ItemArt =
    screenEyes(ArtTones.Pink) { c ->
        Path().apply {
            addPath(HeartPath)
            transform(
                Matrix().apply {
                    translate(c.x, c.y + 2f)
                    scale(34f, 34f)
                },
            )
        }
    }

/** Sunny stars on the screen in place of the eyes. */
private fun starEyes(): ItemArt = screenEyes(ArtTones.Sun) { c -> starPath(c.x, c.y + 1f, 15f) }

/** Coral headphones: a denim band over the head and cups over the ear discs. */
private fun headphones(): ItemArt {
    val band = svgPath("M42 92 C40 16 200 16 198 92")
    val bandBrush = vinylBrush(band.getBounds().inflate(5f), ArtTones.Denim)
    val cups =
        VinylShape(
            Path().apply {
                addPath(roundRectPath(30f, 76f, 56f, 126f, 12f))
                addPath(roundRectPath(184f, 76f, 210f, 126f, 12f))
            },
            ArtTones.Knit,
        )
    val cushions =
        VinylShape(
            Path().apply {
                addPath(roundRectPath(50f, 82f, 60f, 120f, 5f))
                addPath(roundRectPath(180f, 82f, 190f, 120f, 5f))
            },
            ArtTones.Pearl,
        )
    val dots =
        VinylShape(
            Path().apply {
                addPath(ovalPath(41f, 101f, 5f))
                addPath(ovalPath(199f, 101f, 5f))
            },
            ArtTones.Sun,
        )
    val bandStroke = itemStroke(9f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        drawPath(band, bandBrush, style = bandStroke)
        vinylShape(cushions)
        vinylShape(cups)
        specular(LeftCup)
        specular(RightCup)
        vinylShape(dots)
    }
}

/** A golden coil spring wound around the neck joint: four large loops with a darker outline. */
private fun spring(): ItemArt {
    val loops = 4
    val steps = 140
    val coil =
        Path().apply {
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val a = 2f * PI.toFloat() * loops * t
                val x = 88f + 64f * t - 10f * sin(a)
                val y = 165f + 8f * 4f * t * (1f - t) + 11f * cos(a)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    val coilBrush = vinylBrush(coil.getBounds().inflate(3f), ArtTones.Gold)
    val outlineStroke = itemStroke(7.5f)
    val coilStroke = itemStroke(5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(coil, ArtTones.Gold.shade, style = outlineStroke)
        drawPath(coil, coilBrush, style = coilStroke)
    }
}

/** Puffy pearl chef hat with a pleated band. */
private fun chefHat(): ItemArt {
    val puffs =
        VinylShape(
            Path().apply {
                addPath(ovalPath(88f, 30f, 21f))
                addPath(ovalPath(152f, 30f, 21f))
                addPath(ovalPath(120f, 24f, 23f))
            },
            ArtTones.Pearl,
        )
    val band = VinylShape(roundRectPath(76f, 44f, 164f, 64f, 8f), ArtTones.Pearl)
    val pleats = svgPath("M98 48 L98 60 M120 48 L120 60 M142 48 L142 60")
    val pleatStroke = itemStroke(2f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(puffs)
        specular(puffs.bounds, -10f)
        vinylShape(band)
        drawPath(pleats, ArtTones.Pearl.shade, style = pleatStroke)
    }
}

/**
 * A string of five glowing bulbs along the neckline, about 13 units apart and hanging above the chest
 * buttons. Every halo is drawn before the bulbs, so no glow washes over a neighboring bulb.
 */
private fun lights(): ItemArt {
    fun at(t: Float) = itemQuad(80f, 164f, 120f, 186f, 160f, 164f, t)
    val wire =
        Path().apply {
            moveTo(80f, 164f)
            quadraticTo(120f, 186f, 160f, 164f)
        }
    val tones = listOf(ArtTones.Ruby, ArtTones.Sun, ArtTones.Leaf, ArtTones.Sky, ArtTones.Pink)
    val bulbs =
        tones.mapIndexed { i, t ->
            val c = at((i + 1) / 6f)
            VinylShape(ovalPath(c.x, c.y + 6f, 5f, 6.5f), t)
        }
    val halos = bulbs.map { ovalRect(it.bounds.center.x, it.bounds.center.y, 9f) }
    val haloColors = tones.map { it.base }
    val wireStroke = itemStroke(2.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(wire, ArtTones.Metal.shade, style = wireStroke)
        for (i in halos.indices) drawOval(haloColors[i], halos[i].topLeft, halos[i].size, alpha = GLOW_ALPHA)
        for (i in bulbs.indices) {
            vinylShape(bulbs[i])
            specular(bulbs[i].bounds)
        }
    }
}

/** A little sky-blue robot pet at the right foot, with its own screen face and antenna. */
private fun pet(): ItemArt {
    val body = VinylShape(roundRectPath(194f, 240f, 236f, 280f, 13f), ArtTones.Sky)
    val screen = VinylShape(roundRectPath(200f, 248f, 230f, 269f, 7f), ArtTones.Screen)
    val eyes =
        Path().apply {
            addPath(ovalPath(209f, 257f, 3f, 3.8f))
            addPath(ovalPath(221f, 257f, 3f, 3.8f))
        }
    val smile = svgPath("M211 263 Q215 267 219 263")
    val stalk = svgPath("M215 240 L215 229")
    val ball = itemCircle(215f, 226f, 5f, ArtTones.Ruby)
    val wheels =
        VinylShape(
            Path().apply {
                addPath(ovalPath(205f, 282f, 8f, 4.5f))
                addPath(ovalPath(225f, 282f, 8f, 4.5f))
            },
            ArtTones.Metal,
        )
    val shadow = ovalRect(215f, 286f, 24f, 4f)
    val stalkStroke = itemStroke(3f)
    val smileStroke = itemStroke(2f)
    val glow = PersonalizationColors.Glow
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawOval(ArtTones.Metal.shade, shadow.topLeft, shadow.size, alpha = 0.2f)
        vinylShape(wheels)
        drawPath(stalk, ArtTones.Metal.base, style = stalkStroke)
        vinylShape(ball)
        vinylShape(body)
        specular(body.bounds)
        vinylShape(screen)
        drawPath(eyes, glow)
        drawPath(smile, glow, style = smileStroke)
    }
}

/** Two springy antennas with golden stars, bouncing together. */
private fun starAntennas(): ItemArt {
    val stalks = svgPath("M108 48 L86 22 M132 48 L154 22")
    val stalkBrush = vinylBrush(stalks.getBounds().inflate(3f), ArtTones.Metal)
    val bases =
        VinylShape(
            Path().apply {
                addPath(ovalPath(108f, 47f, 8f, 5.5f))
                addPath(ovalPath(132f, 47f, 8f, 5.5f))
            },
            ArtTones.Metal,
        )
    val stars =
        VinylShape(
            Path().apply {
                addPath(starPath(84f, 18f, 15f))
                addPath(starPath(156f, 18f, 15f))
            },
            ArtTones.Sun,
        )
    val stalkStroke = itemStroke(4f)
    return ItemArt(ArtPart.ANTENNA, ItemLayer.BEHIND) {
        drawPath(stalks, stalkBrush, style = stalkStroke)
        vinylShape(bases)
        vinylShape(stars)
        specular(LeftStar)
        specular(RightStar)
    }
}

/** A golden cog medal on a denim ribbon that runs between the chest buttons; the medal hangs below them. */
private fun medal(): ItemArt {
    val ribbon = itemShape("M110 160 L117 212 L123 212 L130 160 L124 160 L120 190 L116 160 Z", ArtTones.Denim)
    val cog =
        VinylShape(
            Path().apply {
                val teeth = 10
                for (k in 0 until teeth * 4) {
                    val a = (k / (teeth * 4f)) * 2f * PI.toFloat()
                    val r = if (k % 4 < 2) 15.5f else 12.5f
                    val x = 120f + r * cos(a)
                    val y = 220f + r * sin(a)
                    if (k == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            },
            ArtTones.Gold,
        )
    val ring = ovalPath(120f, 220f, 9f)
    val star = starPath(120f, 220.5f, 6.5f)
    val ringStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(ribbon)
        vinylShape(cog)
        drawPath(ring, ArtTones.Gold.shade, alpha = 0.7f, style = ringStroke)
        drawPath(star, ArtTones.Gold.shade)
        specular(cog.bounds)
    }
}

/** A clear astronaut bubble over the whole head, sealed by a pearl collar ring. */
private fun astronaut(): ItemArt {
    val bubble = roundRectPath(28f, 8f, 212f, 164f, 70f)
    val collar = VinylShape(roundRectPath(64f, 150f, 176f, 170f, 10f), ArtTones.Pearl)
    val collarBand = svgPath("M68 160 L172 160")
    val gloss = svgPath("M50 84 Q54 38 100 22")
    val glint = ovalRect(114f, 19f, 5f, 3.5f)
    val rimStroke = itemStroke(5f)
    val glossStroke = itemStroke(8f)
    val bandStroke = itemStroke(3f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(collar)
        drawPath(collarBand, ArtTones.Steel.base, style = bandStroke)
        drawPath(bubble, ArtTones.Sky.light, alpha = 0.25f)
        drawPath(bubble, ArtTones.Sky.base, alpha = 0.75f, style = rimStroke)
        drawPath(gloss, Color.White, alpha = 0.7f, style = glossStroke)
        drawOval(Color.White, glint.topLeft, glint.size, alpha = 0.7f)
    }
}

/**
 * Ruby jetpack: two rockets on the back show on both sides of the robot with flames below them, and
 * ruby straps with golden buckles run down the body in front. The rockets lie outside the body, so
 * drawn before the arms they read as behind them.
 */
private fun jetpack(): ItemArt {
    fun tank(x: Float) = roundRectPath(x - 14f, 158f, x + 14f, 236f, 13f)

    fun cone(x: Float) = svgPath("M${x - 13f} 166 Q${x - 12f} 140 $x 132 Q${x + 12f} 140 ${x + 13f} 166 Z")

    fun flame(
        x: Float,
        w: Float,
        h: Float,
    ) = svgPath("M${x - w} 242 Q${x - w} ${242f + h * 0.6f} $x ${242f + h} Q${x + w} ${242f + h * 0.6f} ${x + w} 242 Z")
    val leftX = 30f
    val rightX = 210f
    val tanks = VinylShape(itemPath(tank(leftX), tank(rightX)), ArtTones.Ruby)
    val cones = VinylShape(itemPath(cone(leftX), cone(rightX)), ArtTones.Pearl)
    val nozzles =
        VinylShape(
            Path().apply {
                addPath(roundRectPath(leftX - 10f, 232f, leftX + 10f, 244f, 3f))
                addPath(roundRectPath(rightX - 10f, 232f, rightX + 10f, 244f, 3f))
            },
            ArtTones.Steel,
        )
    val stripes = svgPath("M${leftX - 14f} 190 L${leftX + 14f} 190 M${rightX - 14f} 190 L${rightX + 14f} 190")
    val outerFlames = VinylShape(itemPath(flame(leftX, 10f, 34f), flame(rightX, 10f, 34f)), ArtTones.Knit)
    val innerFlames = VinylShape(itemPath(flame(leftX, 5f, 20f), flame(rightX, 5f, 20f)), ArtTones.Sun)
    val straps = svgPath("M80 166 Q78 200 82 234 M160 166 Q162 200 158 234")
    val strapBrush = vinylBrush(straps.getBounds().inflate(5f), ArtTones.Ruby)
    val buckles =
        VinylShape(
            Path().apply {
                addPath(roundRectPath(73f, 194f, 87f, 204f, 2f))
                addPath(roundRectPath(153f, 194f, 167f, 204f, 2f))
            },
            ArtTones.Gold,
        )
    val stripeStroke = itemStroke(5f)
    val strapStroke = itemStroke(8f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(outerFlames)
        vinylShape(innerFlames)
        vinylShape(nozzles)
        vinylShape(tanks)
        specular(LeftTank)
        specular(RightTank)
        drawPath(stripes, ArtTones.Gold.base, style = stripeStroke)
        vinylShape(cones)
        drawPath(straps, strapBrush, style = strapStroke)
        vinylShape(buckles)
    }
}

// Paired parts get one highlight each; a highlight over both bounds would land between them.
private val LeftCup = Rect(30f, 76f, 56f, 126f)
private val RightCup = Rect(184f, 76f, 210f, 126f)
private val LeftStar = ovalRect(84f, 18f, 15f)
private val RightStar = ovalRect(156f, 18f, 15f)
private val LeftTank = Rect(16f, 158f, 44f, 236f)
private val RightTank = Rect(196f, 158f, 224f, 236f)

/** The robot's item artwork, keyed by item id. Declared last so the shared tones above are ready. */
internal val RobotItemArt: Map<String, ItemArt> =
    mapOf(
        "robot-head-antenna" to antenna(),
        "robot-neck-bow-tie" to bowTie(),
        "robot-extra-wrench" to wrench(),
        "robot-head-propeller" to propellerCap(),
        "robot-neck-tie" to tie(),
        "robot-extra-heart-eyes" to heartEyes(),
        "robot-head-headphones" to headphones(),
        "robot-neck-spring" to spring(),
        "robot-extra-star-eyes" to starEyes(),
        "robot-head-chef-hat" to chefHat(),
        "robot-neck-lights" to lights(),
        "robot-extra-pet" to pet(),
        "robot-head-star-antennas" to starAntennas(),
        "robot-neck-medal" to medal(),
        "robot-head-astronaut" to astronaut(),
        "robot-extra-jetpack" to jetpack(),
    )
