// Literal coordinates place each item on the unicorn in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Rect
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

/** The neckline in three-quarter view: from the front of the neck down to the chest and up behind the mane. */
private const val NECK_CHAIN = "M82 168 Q92 190 104 194 Q122 194 138 166"

/** A collar band along the neckline, for the neckerchief and the cape. */
private const val NECK_BAND = "M82 166 Q106 186 138 164 L138 174 Q106 198 84 176 Z"

/** Sizes of the near and far wing; the feather lines are laid out for the near one. */
private const val NEAR_WING = 0.8f
private const val FAR_WING = 0.72f

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
    itemFlowerLei({ t -> itemQuad(80f, 168f, 104f, 196f, 140f, 164f, t) }, 7, 5f, FlowerPetals(6f, 5.5f), 25f)

/** Pastel rings around the horn: pink, blue and pink again toward the tip. */
private fun hornRings(): ItemArt {
    val rings =
        listOf(
            VinylShape(ovalPath(79f, 43.5f, 8.2f, 3f), ManePink),
            VinylShape(ovalPath(79f, 31.5f, 6f, 2.6f), ManeBlue),
            VinylShape(ovalPath(79f, 20f, 4f, 2.2f), ManePink),
        )
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) { vinylShapes(rings) }
}

/** A soft white cloud the unicorn stands in. */
private fun cloud(): ItemArt {
    val cloud =
        VinylShape(
            Path().apply {
                addPath(ovalPath(73f, 280f, 15f))
                addPath(ovalPath(97f, 272f, 17f))
                addPath(ovalPath(125f, 276f, 19f))
                addPath(ovalPath(153f, 272f, 17f))
                addPath(ovalPath(177f, 280f, 15f))
                addPath(roundRectPath(61f, 276f, 189f, 294f, 9f))
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
    val chain = svgPath(NECK_CHAIN)
    val chainStroke = itemStroke(2f)
    val stars =
        listOf(
            VinylShape(starPath(93f, 188f, 6f), ArtTones.Gold),
            VinylShape(starPath(105f, 199f, 7.5f), ArtTones.Gold),
            VinylShape(starPath(119f, 192f, 6f), ArtTones.Gold),
        )
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(chain, ArtTones.Gold.base, style = chainStroke)
        vinylShapes(stars)
    }
}

/** A crown of flowers across the forehead, around the base of the horn. */
private fun flowerCrown(): ItemArt =
    itemFlowerLei(
        { t -> itemQuad(34f, 86f, 80f, 38f, 134f, 78f, t) },
        7,
        5f,
        FlowerPetals(6f, 5.5f),
        30f,
        part = ArtPart.HEAD,
    )

/** A gold star balloon tied to the near front hoof, floating in front of the chest. */
private fun starBalloon(): ItemArt {
    val string = svgPath("M104 264 Q76 254 62 234 Q50 216 38 208")
    val stringStroke = itemStroke(1.6f)
    val star = VinylShape(starPath(34f, 190f, 20f), ArtTones.Sun)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(string, C.EyeInk, alpha = 0.55f, style = stringStroke)
        vinylShape(star)
        specular(star.bounds)
    }
}

/** A sky-blue neckerchief with gold stars. */
private fun neckerchief(): ItemArt {
    val band = itemShape(NECK_BAND, ManeBlue)
    val tip = itemShape("M88 178 Q104 192 124 184 L101 215 Q98 218 96 214 Z", ManeBlue)
    val stars =
        VinylShape(
            Path().apply {
                addPath(starPath(98f, 191f, 3.5f))
                addPath(starPath(111f, 190f, 3.5f))
                addPath(starPath(101f, 202f, 3.5f))
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

/** Two ribbon bows: pink on the forelock and blue in the mane behind the head. */
private fun maneRibbons(): ItemArt {
    val pink = bowShapes(52f, 70f, 0.8f, ManePink)
    val blue = bowShapes(152f, 126f, 0.8f, ManeBlue)
    val pinkLoops = pink[1]
    val blueLoops = blue[1]
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShapes(pink)
        vinylShapes(blue)
        specular(pinkLoops.bounds)
        specular(blueLoops.bounds)
    }
}

/** A boot over [hoof]: as narrow as the leg at its top, flaring out over the hoof to a rounded sole. */
private fun bootPath(hoof: Rect): Path {
    val cx = hoof.center.x
    val top = hoof.top - 10f
    val l = hoof.left - 1f
    val r = hoof.right + 1f
    val b = hoof.bottom + 1f
    return svgPath(
        "M${cx - 10f} $top L${cx + 10f} $top C${cx + 11f} ${top + 6f} $r ${hoof.top + 2f} $r ${hoof.top + 8f} " +
            "L$r ${b - 5f} Q$r $b ${r - 5f} $b L${l + 5f} $b Q$l $b $l ${b - 5f} L$l ${hoof.top + 8f} " +
            "C$l ${hoof.top + 2f} ${cx - 11f} ${top + 6f} ${cx - 10f} $top Z",
    )
}

/** Sky-blue boots with pearl cuffs on all four hooves, the far ones in shade, and gold sparkles on the near ones. */
private fun sparkleBoots(): ItemArt {
    val farBlue = VinylTones(ManeBlue.base, ManeBlue.shade, ManeBlue.shade)
    val farPearl = VinylTones(ArtTones.Pearl.base, ArtTones.Pearl.shade, ArtTones.Pearl.shade)
    val hooves = UnicornFarHooves + UnicornNearHooves
    val boots =
        hooves.mapIndexed { i, hoof ->
            val tones = if (i < UnicornFarHooves.size) farBlue else ManeBlue
            VinylShape(bootPath(hoof), tones)
        }
    val cuffs =
        hooves.mapIndexed { i, hoof ->
            val tones = if (i < UnicornFarHooves.size) farPearl else ArtTones.Pearl
            val cx = hoof.center.x
            VinylShape(roundRectPath(cx - 12f, hoof.top - 13f, cx + 12f, hoof.top - 6f, 3.5f), tones)
        }
    val sparkles =
        VinylShape(
            Path().apply {
                for (h in UnicornNearHooves) {
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

/** A striped rainbow scarf wrapped around the neck, one end hanging down in front of the chest. */
private fun rainbowScarf(): ItemArt {
    val wrap = itemShape("M80 164 Q106 184 140 160 L140 176 Q106 200 82 182 Z", ArtTones.Ruby)
    val end = itemShape("M86 182 L104 188 L102 228 L84 226 Z", ArtTones.Ruby)
    val bands =
        svgPath(
            "M78 170 Q106 190 142 166 L142 172 Q106 196 78 176 Z " +
                "M80 198 L108 198 L108 206 L80 206 Z M80 214 L108 214 L108 222 L80 222 Z",
        )
    val blue = svgPath("M78 176 Q106 196 142 172 L142 180 Q106 204 78 184 Z M80 206 L108 206 L108 214 L80 214 Z")
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

/** A butterfly clip in the mane behind the head. */
private fun butterfly(): ItemArt {
    val upper =
        itemShape(
            "M149 86 C139 70 125 72 129 84 C131 92 141 94 149 88 Z " +
                "M151 86 C161 70 175 72 171 84 C169 92 159 94 151 88 Z",
            ManePink,
        )
    val lower =
        itemShape(
            "M149 90 C141 96 135 106 143 108 C149 108 151 100 149 92 Z " +
                "M151 90 C159 96 165 106 157 108 C151 108 149 100 151 92 Z",
            ManeBlue,
        )
    val body = VinylShape(roundRectPath(147.5f, 78f, 152.5f, 102f, 2.5f), ArtTones.EyeInk)
    val dots = VinylShape(itemPath(ovalPath(137f, 82f, 2.5f), ovalPath(163f, 82f, 2.5f)), ArtTones.Pearl)
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
    val chain = svgPath(NECK_CHAIN)
    val chainStroke = itemStroke(2f)
    val moon = itemShape("M116 197 C94 198 92 226 116 226 C102 220 102 203 116 197 Z", ArtTones.Gold)
    val star = VinylShape(starPath(116f, 211f, 4.5f), ArtTones.Gold)
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
            "M55 71 L49 45 L65 61 Z M103 71 L109 45 L93 61 Z M63 65 L63 51 L73 61 Z M95 65 L95 51 L85 61 Z",
            ArtTones.Gold,
        )
    val band = itemShape("M53 73 Q79 61 105 73 L103 83 Q79 71 55 83 Z", ArtTones.Gold)
    val gems =
        listOf(
            VinylShape(ovalPath(49f, 45f, 4f), ArtTones.Ruby),
            VinylShape(ovalPath(109f, 45f, 4f), ArtTones.Ruby),
            VinylShape(ovalPath(79f, 73f, 4f), ManeBlue),
        )
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(points)
        vinylShape(band)
        specular(band.bounds)
        vinylShapes(gems)
    }
}

/**
 * A royal purple cape fastened at the neck with a gold clasp. It billows behind the unicorn: it shows
 * above the back, past the rump and between the legs, with a pearl trim along its wavy hem.
 */
private fun cape(): ItemArt {
    val tones = vinylTones(C.CustomPurple)
    val cloth =
        itemShape(
            "M98 162 C126 140 178 144 212 168 C226 192 228 222 222 252 C208 246 196 254 182 248 " +
                "C168 254 156 248 142 252 C130 238 120 220 114 200 Z",
            tones,
        )
    val trim =
        itemShape(
            "M222 252 C208 246 196 254 182 248 C168 254 156 248 142 252 L141 245 " +
                "C155 241 167 247 181 241 C195 247 207 239 222 245 Z",
            ArtTones.Pearl,
        )
    val band = itemShape(NECK_BAND, tones)
    val clasp = itemCircle(100f, 184f, 6.5f, ArtTones.Gold)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT, back = {
        vinylShape(cloth)
        specular(cloth.bounds)
        vinylShape(trim)
    }) {
        vinylShape(band)
        specular(band.bounds)
        vinylShape(clasp)
        specular(clasp.bounds)
    }
}

/** One feathered wing rooted at [x], [y], sweeping up and back, [s] times the base size. */
private fun wingPath(
    x: Float,
    y: Float,
    s: Float,
): Path {
    fun px(d: Float) = x + d * s

    fun py(d: Float) = y + d * s
    return svgPath(
        "M$x $y C${px(-6f)} ${py(-34f)} ${px(20f)} ${py(-70f)} ${px(74f)} ${py(-90f)} " +
            "C${px(82f)} ${py(-80f)} ${px(78f)} ${py(-72f)} ${px(70f)} ${py(-68f)} " +
            "C${px(84f)} ${py(-62f)} ${px(80f)} ${py(-50f)} ${px(68f)} ${py(-48f)} " +
            "C${px(78f)} ${py(-40f)} ${px(72f)} ${py(-28f)} ${px(60f)} ${py(-28f)} " +
            "C${px(64f)} ${py(-16f)} ${px(52f)} ${py(-8f)} ${px(42f)} ${py(-12f)} " +
            "C${px(34f)} $y ${px(18f)} ${py(6f)} $x $y Z",
    )
}

/** Pale blue feathered wings: the near one lies on the shoulder, the far one peeks out behind it. */
private fun wings(): ItemArt {
    val near = VinylShape(wingPath(128f, 184f, NEAR_WING), ManeBlue)
    val far = VinylShape(wingPath(140f, 170f, FAR_WING), VinylTones(ManeBlue.base, ManeBlue.shade, ManeBlue.shade))
    val lines =
        svgPath(
            "M134.4 177.6 Q158.4 155.2 182.4 132.8 M139.2 180.8 Q161.6 166.4 180.8 150.4 " +
                "M145.6 184 Q163.2 177.6 174.4 168",
        )
    val lineStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT, back = { vinylShape(far) }) {
        vinylShape(near)
        drawPath(lines, ManeBlue.shade, style = lineStroke)
        specular(near.bounds)
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
