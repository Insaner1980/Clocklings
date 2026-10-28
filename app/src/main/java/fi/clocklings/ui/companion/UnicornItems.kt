// Literal coordinates place each item on the unicorn in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.components.starPath
import fi.clocklings.ui.theme.PersonalizationColors as C

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.

/** A small ribbon bow centered at [cx], [cy], [s] times the base size, as loops, tails and a knot. */
private fun bowShapes(
    cx: Float,
    cy: Float,
    s: Float,
    tones: VinylTones,
): List<VinylShape> {
    fun x(d: Float) = cx + d * s

    fun y(d: Float) = cy + d * s
    return listOf(
        itemShape(
            "M${x(-3f)} ${y(2f)} L${x(-10f)} ${y(16f)} L${x(-4f)} ${y(17f)} L${x(0f)} ${y(5f)} Z " +
                "M${x(3f)} ${y(2f)} L${x(10f)} ${y(16f)} L${x(4f)} ${y(17f)} L${x(0f)} ${y(5f)} Z",
            tones,
        ),
        itemShape(
            "M$cx $cy C${x(-6f)} ${y(-13f)} ${x(-20f)} ${y(-12f)} ${x(-19f)} ${y(0f)} " +
                "C${x(-18f)} ${y(11f)} ${x(-6f)} ${y(9f)} $cx $cy Z " +
                "M$cx $cy C${x(6f)} ${y(-13f)} ${x(20f)} ${y(-12f)} ${x(19f)} ${y(0f)} " +
                "C${x(18f)} ${y(11f)} ${x(6f)} ${y(9f)} $cx $cy Z",
            tones,
        ),
        itemCircle(cx, cy, 5f * s, tones),
    )
}

private fun DrawScope.vinylShapes(shapes: List<VinylShape>) {
    for (i in shapes.indices) vinylShape(shapes[i])
}

/** A garland of flowers around the neck. */
private fun lei(): ItemArt =
    itemFlowerLei({ t -> itemQuad(82f, 176f, 120f, 202f, 158f, 176f, t) }, 7, 5f, 6f, 5.5f, 25f)

/** Pastel rings around the horn: pink, blue and pink again toward the tip. */
private fun hornRings(): ItemArt {
    val rings =
        listOf(
            VinylShape(ovalPath(120f, 41f, 8.2f, 3f), ManePink),
            VinylShape(ovalPath(120f, 29f, 6f, 2.6f), ManeBlue),
            VinylShape(ovalPath(120f, 17.5f, 4f, 2.2f), ManePink),
        )
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) { vinylShapes(rings) }
}

/** A soft white cloud the unicorn stands in. */
private fun cloud(): ItemArt {
    val cloud =
        VinylShape(
            Path().apply {
                addPath(ovalPath(68f, 280f, 15f))
                addPath(ovalPath(92f, 272f, 17f))
                addPath(ovalPath(120f, 276f, 19f))
                addPath(ovalPath(148f, 272f, 17f))
                addPath(ovalPath(172f, 280f, 15f))
                addPath(roundRectPath(56f, 276f, 184f, 294f, 9f))
            },
            ArtTones.Pearl,
        )
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(cloud)
        specular(cloud.bounds)
    }
}

/** Three little gold stars on a chain. */
private fun starNecklace(): ItemArt {
    val chain = svgPath("M80 174 Q100 194 120 197 Q140 194 160 174")
    val chainStroke = itemStroke(2f)
    val stars =
        listOf(
            VinylShape(starPath(103f, 196f, 6.5f), ArtTones.Gold),
            VinylShape(starPath(120f, 204f, 8f), ArtTones.Gold),
            VinylShape(starPath(137f, 196f, 6.5f), ArtTones.Gold),
        )
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(chain, ArtTones.Gold.base, style = chainStroke)
        vinylShapes(stars)
    }
}

/** A crown of flowers across the forehead, around the base of the horn. */
private fun flowerCrown(): ItemArt =
    itemFlowerLei({ t -> itemQuad(62f, 96f, 120f, 40f, 178f, 96f, t) }, 7, 5f, 6f, 5.5f, 30f, part = ArtPart.HEAD)

/** A gold star balloon tied to the left front hoof. */
private fun starBalloon(): ItemArt {
    val string = svgPath("M100 262 Q70 230 72 200 Q74 180 50 168")
    val stringStroke = itemStroke(1.6f)
    val star = VinylShape(starPath(42f, 150f, 22f), ArtTones.Sun)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(string, C.EyeInk, alpha = 0.55f, style = stringStroke)
        vinylShape(star)
        specular(star.bounds)
    }
}

/** A sky-blue neckerchief with gold stars. */
private fun neckerchief(): ItemArt {
    val band = itemShape("M84 172 Q120 186 156 172 L154 182 Q120 196 86 182 Z", ManeBlue)
    val tip = itemShape("M100 184 Q120 192 140 184 L122 214 Q120 217 118 214 Z", ManeBlue)
    val stars =
        VinylShape(
            Path().apply {
                addPath(starPath(112f, 195f, 3.5f))
                addPath(starPath(128f, 195f, 3.5f))
                addPath(starPath(120f, 205f, 3.5f))
            },
            ArtTones.Gold,
        )
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(tip)
        vinylShape(band)
        specular(band.bounds)
        vinylShape(stars)
    }
}

/** Two ribbon bows in the mane: pink on the forelock and blue in the mane on the right. */
private fun maneRibbons(): ItemArt {
    val pink = bowShapes(88f, 72f, 0.8f, ManePink)
    val blue = bowShapes(180f, 112f, 0.8f, ManeBlue)
    val pinkLoops = pink[1]
    val blueLoops = blue[1]
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShapes(pink)
        vinylShapes(blue)
        specular(pinkLoops.bounds)
        specular(blueLoops.bounds)
    }
}

/** Sky-blue boots with pearl cuffs and gold sparkles on the front hooves. */
private fun sparkleBoots(): ItemArt {
    val boots =
        UnicornFrontHooves.map {
            VinylShape(roundRectPath(it.left - 1f, it.top - 14f, it.right + 1f, it.bottom + 1f, 9f), ManeBlue)
        }
    val cuffs =
        UnicornFrontHooves.map {
            VinylShape(roundRectPath(it.left - 2f, it.top - 18f, it.right + 2f, it.top - 10f, 4f), ArtTones.Pearl)
        }
    val sparkles =
        VinylShape(
            Path().apply {
                for (h in UnicornFrontHooves) {
                    addItemTwinkle(h.left + 8f, h.top + 2f, 4f)
                    addItemTwinkle(h.right - 8f, h.top + 12f, 3f)
                }
            },
            ArtTones.Gold,
        )
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        for (i in boots.indices) {
            vinylShape(boots[i])
            specular(boots[i].bounds)
            vinylShape(cuffs[i])
        }
        vinylShape(sparkles)
    }
}

/** A striped rainbow scarf wrapped around the neck, one end hanging down on the right. */
private fun rainbowScarf(): ItemArt {
    val wrap = itemShape("M82 170 Q120 188 158 170 L156 188 Q120 206 84 188 Z", ArtTones.Ruby)
    val end = itemShape("M134 186 L152 182 L158 222 L140 224 Z", ArtTones.Ruby)
    val bands =
        svgPath(
            "M80 176 Q120 194 160 176 L160 182 Q120 200 80 182 Z " +
                "M130 196 L162 192 L162 200 L130 204 Z M130 210 L162 206 L162 214 L130 218 Z",
        )
    val blue = svgPath("M80 182 Q120 200 160 182 L160 190 Q120 208 80 190 Z M130 204 L162 200 L162 206 L130 210 Z")
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(end)
        clipPath(end.path) {
            drawPath(bands, ArtTones.Sun.base)
            drawPath(blue, ArtTones.Sky.base)
        }
        vinylShape(wrap)
        clipPath(wrap.path) {
            drawPath(bands, ArtTones.Sun.base)
            drawPath(blue, ArtTones.Sky.base)
        }
        specular(wrap.bounds)
    }
}

/** A butterfly clip in the mane on the right. */
private fun butterfly(): ItemArt {
    val upper =
        itemShape(
            "M160 72 C150 56 136 58 140 70 C142 78 152 80 160 74 Z " +
                "M162 72 C172 56 186 58 182 70 C180 78 170 80 162 74 Z",
            ManePink,
        )
    val lower =
        itemShape(
            "M160 76 C152 82 146 92 154 94 C160 94 162 86 160 78 Z " +
                "M162 76 C170 82 176 92 168 94 C162 94 160 86 162 78 Z",
            ManeBlue,
        )
    val body = VinylShape(roundRectPath(158.5f, 64f, 163.5f, 88f, 2.5f), ArtTones.EyeInk)
    val dots = VinylShape(itemPath(ovalPath(148f, 68f, 2.5f), ovalPath(174f, 68f, 2.5f)), ArtTones.Pearl)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(lower)
        vinylShape(upper)
        vinylShape(dots)
        vinylShape(body)
    }
}

/** A big rainbow arching behind the unicorn. */
private fun rainbow(): ItemArt {
    val arc = svgPath("M20 250 A100 100 0 0 1 220 250")
    val radii = floatArrayOf(0f, 10f, 20f, 30f)
    val arcs = radii.map { d -> svgPath("M${20f + d} 250 A${100f - d} ${100f - d} 0 0 1 ${220f - d} 250") }
    val colors = arrayOf(ArtTones.Ruby.base, ArtTones.Sun.base, ArtTones.Leaf.base, ArtTones.Sky.base)
    val band = itemStroke(10f)
    val rim = itemStroke(3f)
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        for (i in arcs.indices) drawPath(arcs[i], colors[i], style = band)
        drawPath(arc, ArtTones.Ruby.light, alpha = 0.4f, style = rim)
    }
}

/** A crescent moon pendant on a gold chain. */
private fun moonPendant(): ItemArt {
    val chain = svgPath("M80 174 Q100 194 120 196 Q140 194 160 174")
    val chainStroke = itemStroke(2f)
    val moon = itemShape("M126 195 C104 196 102 224 126 224 C112 218 112 201 126 195 Z", ArtTones.Gold)
    val star = VinylShape(starPath(126f, 209f, 4.5f), ArtTones.Gold)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(chain, ArtTones.Gold.base, style = chainStroke)
        vinylShape(moon)
        specular(moon.bounds)
        vinylShape(star)
    }
}

/** A gold crown around the base of the horn, the horn rising through its middle. */
private fun crown(): ItemArt {
    val points =
        itemShape(
            "M96 68 L90 42 L106 58 Z M144 68 L150 42 L134 58 Z M104 62 L104 48 L114 58 Z M136 62 L136 48 L126 58 Z",
            ArtTones.Gold,
        )
    val band = itemShape("M94 70 Q120 58 146 70 L144 80 Q120 68 96 80 Z", ArtTones.Gold)
    val gems =
        listOf(
            VinylShape(ovalPath(90f, 42f, 4f), ArtTones.Ruby),
            VinylShape(ovalPath(150f, 42f, 4f), ArtTones.Ruby),
            VinylShape(ovalPath(120f, 70f, 4f), ManeBlue),
        )
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(points)
        vinylShape(band)
        specular(band.bounds)
        vinylShapes(gems)
    }
}

/** A royal purple cape fastened with a gold clasp. */
private fun cape(): ItemArt {
    val tones = vinylTones(C.CustomPurple)
    val cloth = itemShape("M80 172 Q50 230 38 282 Q120 296 202 282 Q190 230 160 172 Z", tones)
    val trim = itemShape("M38 282 Q120 296 202 282 L203 274 Q120 288 37 274 Z", ArtTones.Pearl)
    val band = itemShape("M84 172 Q120 188 156 172 L154 182 Q120 198 86 182 Z", tones)
    val clasp = itemCircle(120f, 190f, 7f, ArtTones.Gold)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT, back = {
        vinylShape(cloth)
        vinylShape(trim)
    }) {
        vinylShape(band)
        specular(band.bounds)
        vinylShape(clasp)
        specular(clasp.bounds)
    }
}

/** Pale blue feathered wings spreading from behind the body. */
private fun wings(): ItemArt {
    val left =
        "M96 190 C72 140 20 118 2 140 C8 152 14 156 22 156 C12 168 16 182 28 182 " +
            "C22 194 30 204 42 200 C46 212 62 214 70 206 C82 212 92 206 98 200 Z"
    val right =
        "M144 190 C168 140 220 118 238 140 C232 152 226 156 218 156 C228 168 224 182 212 182 " +
            "C218 194 210 204 198 200 C194 212 178 214 170 206 C158 212 148 206 142 200 Z"
    val feathers = listOf(itemShape(left, ManeBlue), itemShape(right, ManeBlue))
    val lines =
        svgPath(
            "M86 184 Q56 162 26 158 M88 192 Q60 182 32 182 M92 198 Q70 198 46 200 " +
                "M154 184 Q184 162 214 158 M152 192 Q180 182 208 182 M148 198 Q170 198 194 200",
        )
    val lineStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        vinylShapes(feathers)
        drawPath(lines, ManeBlue.shade, style = lineStroke)
        for (i in feathers.indices) specular(feathers[i].bounds)
    }
}

/** The unicorn's item artwork keyed by item id. */
internal val UnicornItemArt: Map<String, ItemArt> =
    mapOf(
        "unicorn-neck-lei" to lei(),
        "unicorn-head-horn-rings" to hornRings(),
        "unicorn-extra-cloud" to cloud(),
        "unicorn-neck-stars" to starNecklace(),
        "unicorn-head-flower-crown" to flowerCrown(),
        "unicorn-extra-balloon" to starBalloon(),
        "unicorn-neck-scarf" to neckerchief(),
        "unicorn-head-ribbons" to maneRibbons(),
        "unicorn-extra-boots" to sparkleBoots(),
        "unicorn-neck-rainbow-scarf" to rainbowScarf(),
        "unicorn-head-butterfly" to butterfly(),
        "unicorn-extra-rainbow" to rainbow(),
        "unicorn-neck-moon" to moonPendant(),
        "unicorn-head-crown" to crown(),
        "unicorn-neck-cape" to cape(),
        "unicorn-extra-wings" to wings(),
    )
