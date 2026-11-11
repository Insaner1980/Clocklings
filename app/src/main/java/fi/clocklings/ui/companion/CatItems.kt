// Literal coordinates place each item on the cat in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.glossyEye
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.components.starPath
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import fi.clocklings.ui.theme.PersonalizationColors as C

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.

/** Everything above the top edge of the hat band (bobble and sailor hats share it): the ears come out of the crown. */
private val AboveHatBand = svgPath("M0 0 L240 0 L240 86 L184 86 Q120 66 56 86 L0 86 Z")

/** Striped knit scarf from the sample: a wrap around the neck and a fringed end hanging on the right. */
private fun scarf(): ItemArt {
    // The end's stripes run slanted, parallel to its top edge.
    val endStripes =
        Path().apply {
            for (t in floatArrayOf(0.28f, 0.62f)) {
                val t2 = t + 0.13f
                moveTo(138f + 8f * t, 178f + 36f * t)
                lineTo(160f + 8f * t, 180f + 40f * t)
                lineTo(160f + 8f * t2, 180f + 40f * t2)
                lineTo(138f + 8f * t2, 178f + 36f * t2)
                close()
            }
        }
    return itemStripedScarf(
        wrap = "M62 156 Q120 182 178 156 L176 176 Q120 202 64 176 Z",
        end = "M138 178 L160 180 L168 220 L146 214 Z",
        stripes =
            ScarfStripes(
                xs = floatArrayOf(80f, 106f, 132f, 158f),
                top = 150f,
                bottom = 206f,
                end = endStripes,
            ),
        fringeStart = Offset(146f, 212f),
        fringeLength = 15f,
        tones = ArtTones.Knit,
        stripeTones = ArtTones.Pearl,
    )
}

/** Ribbon bow tied at the base of the right ear. */
private fun bow(): ItemArt {
    val center = Offset(148f, 60f)
    val ends =
        itemShape(
            "M145 62 Q140 72 133 82 L141 82 L148 66 Z M151 62 Q156 72 163 82 L155 82 L148 66 Z",
            ArtTones.Scarf,
        )
    val left = itemShape("M148 60 C140 44 121 42 122 57 C123 72 140 73 148 60 Z", ArtTones.Scarf)
    val right = itemShape("M148 60 C156 44 175 42 174 57 C173 72 156 73 148 60 Z", ArtTones.Scarf)
    val knot = itemCircle(148f, 60f, 6.5f, ArtTones.Scarf)
    val folds = svgPath("M141 58 Q133 53 128 56 M155 58 Q163 53 168 56")
    val foldStroke = itemStroke(1.8f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        rotate(14f, center) {
            vinylShape(ends)
            vinylShape(left)
            vinylShape(right)
            drawPath(folds, ArtTones.Scarf.shade, alpha = 0.6f, style = foldStroke)
            specular(left.bounds)
            specular(right.bounds)
            vinylShape(knot)
        }
    }
}

/** Ball of yarn held between the front paws, with a loose strand trailing to the ground. */
private fun yarn(): ItemArt {
    val ball = itemCircle(120f, 254f, 21f, ArtTones.Sky)
    val wraps =
        svgPath(
            "M101 244 Q119 258 109 274 M107 237 Q132 250 125 276 " +
                "M118 233 Q143 245 140 267 M99 259 Q120 249 142 262",
        )
    val strand = svgPath("M136 267 C150 281 164 270 178 279 C188 286 199 284 206 279")
    val wrapStroke = itemStroke(2f)
    val strandStroke = itemStroke(2.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(strand, ArtTones.Sky.base, style = strandStroke)
        vinylShape(ball)
        clipPath(ball.path) { drawPath(wraps, ArtTones.Sky.shade, alpha = 0.75f, style = wrapStroke) }
        specular(ball.bounds)
    }
}

/** Collar with a golden bell, from the sample. */
private fun bellCollar(): ItemArt {
    val band = itemShape("M64 158 Q120 180 176 158 L174 170 Q120 194 66 170 Z", ArtTones.Ruby)
    val loop = itemCircle(120f, 179f, 4f, ArtTones.Gold)
    val bell = itemCircle(120f, 190f, 11f, ArtTones.Gold)
    val slit = svgPath("M114 193 L126 193")
    val slitStroke = itemStroke(2.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(band)
        specular(band.bounds)
        vinylShape(loop)
        vinylShape(bell)
        drawPath(slit, ArtTones.Gold.shade, style = slitStroke)
        drawCircle(ArtTones.Gold.shade, 1.8f, Offset(120f, 197.5f))
        specular(bell.bounds)
    }
}

/** Knit bobble hat from the sample: crown, ribbed band and pom. The ears come through it. */
private fun bobbleHat(): ItemArt {
    val crown = itemShape("M58 96 Q60 40 120 38 Q180 40 182 96 Q120 80 58 96 Z", ArtTones.Teal)
    val band = itemShape("M54 100 Q120 80 186 100 L184 86 Q120 66 56 86 Z", ArtTones.Pearl)
    val ribs =
        Path().apply {
            var x = 66f
            while (x <= 176f) {
                val t = (x - 56f) / 128f
                val s = (x - 54f) / 132f
                moveTo(x, 86f - 40f * t * (1 - t) + 3f)
                lineTo(x, 100f - 40f * s * (1 - s) - 3f)
                x += 11f
            }
        }
    val pom = itemCircle(120f, 34f, 15f, ArtTones.Knit)
    val ribStroke = itemStroke(2.5f)
    return ItemArt(ArtPart.HEAD, ItemLayer.OVER_EARS, AboveHatBand) {
        vinylShape(crown)
        specular(crown.bounds, -12f)
        vinylShape(band)
        drawPath(ribs, ArtTones.Pearl.shade, alpha = 0.45f, style = ribStroke)
        vinylShape(pom)
        specular(pom.bounds)
    }
}

/** Round glasses over the eyes, with lightly tinted lenses. */
private fun glasses(): ItemArt {
    val lenses =
        Path().apply {
            addPath(ovalPath(90f, 111f, 18f))
            addPath(ovalPath(152f, 111f, 18f))
        }
    val frames =
        Path().apply {
            addPath(lenses)
            addPath(svgPath("M108 108 Q121 100 134 108 M72 107 L47 100 M170 107 L194 100"))
        }
    val frameBrush = vinylBrush(frames.getBounds(), ArtTones.Denim)
    val lensColor = ArtTones.Sky.light.copy(alpha = 0.3f)
    val glints = svgPath("M96 100 Q102 102 104 108 M158 100 Q164 102 166 108")
    val glintColor = Color.White.copy(alpha = 0.8f)
    val frameStroke = itemStroke(4f)
    val glintStroke = itemStroke(2.2f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        drawPath(lenses, lensColor)
        drawPath(frames, frameBrush, style = frameStroke)
        drawPath(glints, glintColor, style = glintStroke)
    }
}

/** Teal polka-dot bow tie under the chin. */
private fun bowTie(): ItemArt =
    itemPolkaDotBowTie(Offset(120f, 182f), 23f, 13f, ArtTones.Teal, ArtTones.Pearl.base, 2.4f)

/** White sailor hat with a navy band and an anchor. The ears come through it. */
private fun sailorHat(): ItemArt {
    val dome = itemShape("M62 94 Q64 46 120 44 Q176 46 178 94 Q120 80 62 94 Z", ArtTones.Pearl)
    val brim = itemShape("M54 100 Q120 80 186 100 L184 84 Q120 64 56 84 Z", ArtTones.Pearl)
    val band = svgPath("M55 92 Q120 72 185 92")
    val anchor = svgPath("M120 53 L120 70 M114 58 L126 58 M111 64 Q120 76 129 64")
    val ring = ovalPath(120f, 49.5f, 3.2f)
    val bandStroke = itemStroke(4.5f)
    val anchorStroke = itemStroke(2.6f)
    val ringStroke = Stroke(2.2f)
    return ItemArt(ArtPart.HEAD, ItemLayer.OVER_EARS, AboveHatBand) {
        vinylShape(dome)
        specular(dome.bounds, -12f)
        drawPath(anchor, ArtTones.Denim.base, style = anchorStroke)
        drawPath(ring, ArtTones.Denim.base, style = ringStroke)
        vinylShape(brim)
        drawPath(band, ArtTones.Denim.base, style = bandStroke)
    }
}

/** A fish-shaped backpack peeking out behind the cat's left side, head up. */
private fun fishBackpack(): ItemArt {
    val fins = itemShape("M50 234 L29 266 Q50 256 71 266 Z M31 194 L16 204 L31 214 Z", ArtTones.Denim)
    val fish = VinylShape(ovalPath(50f, 194f, 22f, 48f), ArtTones.Sky)
    val markings = svgPath("M33 178 Q46 187 64 176 M30 202 Q50 212 70 202 M31 222 Q50 232 69 222")
    val mouth = svgPath("M43 152 Q48 156 53 152")
    val markStroke = itemStroke(2.4f)
    val mouthStroke = itemStroke(2f)
    val eye = Offset(43f, 166f)
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        vinylShape(fins)
        vinylShape(fish)
        specular(fish.bounds)
        drawPath(markings, ArtTones.Denim.base, alpha = 0.55f, style = markStroke)
        drawPath(mouth, C.EyeInk, style = mouthStroke)
        glossyEye(eye, 4.5f, 5.5f, 0f, ArtTones.EyeInk)
    }
}

/** Two-row ruffle collar: a pink back row and a pearl front row with pleat lines. */
private fun ruffle(): ItemArt {
    fun at(t: Float) = itemQuad(66f, 160f, 120f, 192f, 174f, 160f, t)
    val back =
        Path().apply {
            for (i in 0..8) at(i / 8f).let { addPath(ovalPath(it.x, it.y + 7f, 11f)) }
        }
    val front =
        Path().apply {
            for (i in 0..7) at((2 * i + 1) / 16f).let { addPath(ovalPath(it.x, it.y + 2f, 9.5f)) }
        }
    val pleats =
        Path().apply {
            for (i in 0..7) {
                val p = at((2 * i + 1) / 16f)
                moveTo(p.x, p.y - 1f)
                lineTo(p.x, p.y + 7f)
            }
        }
    val backShape = VinylShape(back, ArtTones.Pink)
    val frontShape = VinylShape(front, ArtTones.Pearl)
    val pleatStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(backShape)
        vinylShape(frontShape)
        drawPath(pleats, ArtTones.Pearl.shade, alpha = 0.6f, style = pleatStroke)
    }
}

/** Seven flowers with leaves around the top of the head, in front of the ears. */
private fun flowerCrown(): ItemArt {
    fun at(t: Float) = itemQuad(60f, 96f, 120f, 40f, 180f, 96f, t)
    val petalTones =
        listOf(ArtTones.Pink, ArtTones.Pearl, ArtTones.Sky, ArtTones.Pink, ArtTones.Sky, ArtTones.Pearl, ArtTones.Pink)
    val leaves =
        (0 until 6).map { i ->
            val t = 0.125f + 0.15f * i
            val p = at(t)
            val angle = atan2(-112f * (1 - 2 * t), 120f)
            val dx = 8f * cos(angle)
            val dy = 8f * sin(angle)
            val nx = -3.5f * sin(angle)
            val ny = 3.5f * cos(angle)
            val leaf =
                Path().apply {
                    moveTo(p.x - dx, p.y - dy)
                    quadraticTo(p.x + nx, p.y + ny - 2f, p.x + dx, p.y + dy)
                    quadraticTo(p.x - nx, p.y - ny - 2f, p.x - dx, p.y - dy)
                    close()
                }
            VinylShape(leaf, ArtTones.Leaf)
        }
    val flowers =
        petalTones.mapIndexed { i, tones ->
            val c = at(0.05f + 0.15f * i)
            VinylShape(itemPetals(c.x, c.y, 6f, 5.4f, i * 17f), tones) to itemCircle(c.x, c.y, 3.6f, ArtTones.Sun)
        }
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        for (i in leaves.indices) vinylShape(leaves[i])
        for (i in flowers.indices) {
            vinylShape(flowers[i].first)
            vinylShape(flowers[i].second)
        }
    }
}

/** Pearl necklace on a fine gold chain, with a pearl drop. */
private fun pearls(): ItemArt {
    fun at(t: Float) = itemQuad(68f, 158f, 120f, 194f, 172f, 158f, t)
    val chain = svgPath("M68 158 Q120 194 172 158")
    val beads = (0..12).map { i -> at(i / 12f).let { itemCircle(it.x, it.y, 4.6f, ArtTones.Pearl) } }
    val cap = itemCircle(120f, 183.5f, 2.6f, ArtTones.Gold)
    val drop = itemCircle(120f, 190f, 6.5f, ArtTones.Pearl)
    val chainStroke = itemStroke(1.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(chain, ArtTones.Gold.base, style = chainStroke)
        for (i in beads.indices) vinylShape(beads[i])
        vinylShape(cap)
        vinylShape(drop)
        specular(drop.bounds)
    }
}

/** Star wand held upright in the cat's right paw, with twinkles around the star. */
private fun wand(): ItemArt {
    val stick = svgPath("M94 266 L56 198")
    val stickBrush = vinylBrush(stick.getBounds().inflate(3f), ArtTones.Denim)
    val star = VinylShape(starPath(50f, 188f, 20f), ArtTones.Gold)
    val twinkles =
        Path().apply {
            addItemTwinkle(23f, 163f, 7f)
            addItemTwinkle(80f, 162f, 5.5f)
            addItemTwinkle(24f, 218f, 5.5f)
        }
    val stickStroke = itemStroke(7f)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        drawPath(stick, stickBrush, style = stickStroke)
        vinylShape(star)
        specular(star.bounds)
        drawPath(twinkles, ArtTones.Sun.base)
    }
}

/** Sparkle cape: a starry cape flaring out behind the body. */
private fun cape(): ItemArt {
    val cape = itemShape("M72 148 Q120 136 168 148 Q202 200 214 274 Q120 290 26 274 Q38 200 72 148 Z", ArtTones.Denim)
    val stars =
        Path().apply {
            addPath(starPath(66f, 172f, 4f))
            addPath(starPath(174f, 172f, 4f))
            addPath(starPath(44f, 238f, 6f))
            addPath(starPath(50f, 205f, 4.5f))
            addPath(starPath(36f, 264f, 4f))
            addPath(starPath(196f, 238f, 6f))
            addPath(starPath(190f, 205f, 4.5f))
            addPath(starPath(204f, 264f, 4f))
        }
    val twinkles =
        Path().apply {
            addItemTwinkle(40f, 222f, 3.5f)
            addItemTwinkle(57f, 255f, 3f)
            addItemTwinkle(200f, 222f, 3.5f)
            addItemTwinkle(183f, 255f, 3f)
        }
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        vinylShape(cape)
        drawPath(stars, ArtTones.Gold.base)
        drawPath(twinkles, ArtTones.Pearl.base)
    }
}

/** Golden crown from the sample, with pearl tips and gems. */
private fun crown(): ItemArt {
    val crown = itemShape("M86 62 L90 26 L105 44 L120 18 L135 44 L150 26 L154 62 Q120 54 86 62 Z", ArtTones.Gold)
    val tips =
        listOf(
            itemCircle(90f, 26f, 3.4f, ArtTones.Pearl),
            itemCircle(120f, 18f, 3.8f, ArtTones.Pearl),
            itemCircle(150f, 26f, 3.4f, ArtTones.Pearl),
        )
    val gems =
        listOf(
            itemCircle(120f, 50f, 5f, ArtTones.Ruby),
            itemCircle(101f, 54f, 3.5f, ArtTones.Teal),
            itemCircle(139f, 54f, 3.5f, ArtTones.Teal),
        )
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(crown)
        specular(crown.bounds)
        for (i in tips.indices) vinylShape(tips[i])
        for (i in gems.indices) vinylShape(gems[i])
    }
}

/** Butterfly wings behind the back: sky upper wings and pink lower wings with pearl spots. */
private fun wings(): ItemArt {
    val upper =
        itemShape(
            "M98 176 C84 118 26 92 14 122 C4 150 36 186 98 190 Z " +
                "M142 176 C156 118 214 92 226 122 C236 150 204 186 142 190 Z",
            ArtTones.Sky,
        )
    val lower =
        itemShape(
            "M98 192 C58 192 26 212 34 240 C42 264 84 244 100 206 Z " +
                "M142 192 C182 192 214 212 206 240 C198 264 156 244 140 206 Z",
            ArtTones.Pink,
        )
    val upperVeins = svgPath("M96 182 L30 126 M144 182 L210 126")
    val lowerVeins = svgPath("M98 198 L44 236 M142 198 L196 236")
    val spots =
        Path().apply {
            addPath(ovalPath(36f, 128f, 8f))
            addPath(ovalPath(60f, 154f, 4.5f))
            addPath(ovalPath(204f, 128f, 8f))
            addPath(ovalPath(180f, 154f, 4.5f))
            addPath(ovalPath(50f, 232f, 5.5f))
            addPath(ovalPath(190f, 232f, 5.5f))
        }
    val veinStroke = itemStroke(1.8f)
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        vinylShape(upper)
        vinylShape(lower)
        drawPath(upperVeins, ArtTones.Sky.shade, alpha = 0.5f, style = veinStroke)
        drawPath(lowerVeins, ArtTones.Pink.shade, alpha = 0.5f, style = veinStroke)
        drawPath(spots, ArtTones.Pearl.base, alpha = 0.85f)
    }
}

/** The cat's item artwork, keyed by item id. Declared last so the shared tones above are ready. */
internal val CatItemArt: Map<String, ItemArt> =
    mapOf(
        "cat-neck-scarf" to scarf(),
        "cat-head-bow" to bow(),
        "cat-extra-yarn" to yarn(),
        "cat-neck-bell-collar" to bellCollar(),
        "cat-head-bobble-hat" to bobbleHat(),
        "cat-extra-glasses" to glasses(),
        "cat-neck-bow-tie" to bowTie(),
        "cat-head-sailor-hat" to sailorHat(),
        "cat-extra-fish-backpack" to fishBackpack(),
        "cat-neck-ruffle" to ruffle(),
        "cat-head-flower-crown" to flowerCrown(),
        "cat-neck-pearls" to pearls(),
        "cat-extra-wand" to wand(),
        "cat-neck-cape" to cape(),
        "cat-head-crown" to crown(),
        "cat-extra-wings" to wings(),
    )
