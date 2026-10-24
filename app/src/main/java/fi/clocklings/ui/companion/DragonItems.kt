// Literal coordinates place each item on the dragon in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.components.starPath
import fi.clocklings.ui.theme.PersonalizationColors as C

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.
// Neckwear follows the neckline below the chin and stays inside the body outline at both ends.

/** A point on the dragon's neckline at [t] (0 at the left end, 1 at the right end). */
private fun neckAt(t: Float) = itemQuad(82f, 162f, 120f, 190f, 158f, 162f, t)

/** A diamond-shaped gem centered at [cx], [cy] with half width [rx] and half height [ry]. */
private fun gemPath(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
) = Path().apply {
    moveTo(cx, cy - ry)
    lineTo(cx + rx, cy)
    lineTo(cx, cy + ry)
    lineTo(cx - rx, cy)
    close()
}

/** Everything above the knight helmet's rim, where the horns come out of it. */
private val AboveHelmetRim = svgPath("M0 0 L240 0 L240 96 L190 96 Q120 80 50 96 L0 96 Z")

/** The free pendant: a ruby cord with a golden diamond holding a teal gem, like the tail tip. */
private fun pendant(): ItemArt {
    val cord = svgPath("M84 162 Q120 190 156 162")
    val cordBrush = vinylBrush(cord.getBounds().inflate(3f), ArtTones.Scarf)
    val bail = itemCircle(120f, 179f, 4f, ArtTones.Gold)
    val frame = VinylShape(gemPath(120f, 197f, 13f, 18f), ArtTones.Gold)
    val gem = VinylShape(gemPath(120f, 197f, 8f, 12f), ArtTones.Teal)
    val cordStroke = itemStroke(3.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(cord, cordBrush, style = cordStroke)
        vinylShape(bail)
        vinylShape(frame)
        vinylShape(gem)
        specular(gem.bounds)
    }
}

/** Daisies and pink blossoms on a leafy vine around the top of the head, in front of the horn roots. */
private fun flowerCrown(): ItemArt {
    fun at(t: Float) = itemQuad(58f, 88f, 120f, 26f, 182f, 88f, t)
    val vine = svgPath("M58 88 Q120 26 182 88")
    val leaves =
        VinylShape(
            Path().apply {
                for (i in 0 until 6) at(0.16f + 0.136f * i).let { addPath(ovalPath(it.x, it.y + 4f, 6.5f, 4f)) }
            },
            ArtTones.Leaf,
        )
    val flowers =
        (0 until 7).map { i ->
            val c = at(0.04f + 0.153f * i)
            val daisy = i % 2 == 0
            val tones = if (daisy) ArtTones.Pearl else ArtTones.Pink
            val petals = VinylShape(itemPetals(c.x, c.y, 6.5f, 5.6f, i * 19f), tones)
            petals to itemCircle(c.x, c.y, 3.8f, if (daisy) ArtTones.Sun else ArtTones.Gold)
        }
    val vineStroke = itemStroke(3f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        drawPath(vine, ArtTones.Leaf.shade, style = vineStroke)
        vinylShape(leaves)
        for (i in flowers.indices) {
            vinylShape(flowers[i].first)
            vinylShape(flowers[i].second)
        }
    }
}

/** A ruby coin pouch tied with a golden cord, between the feet, with coins peeking out and spilled beside it. */
private fun coinPouch(): ItemArt {
    val pouch =
        itemShape("M100 252 Q90 288 120 288 Q150 288 140 252 Q130 244 120 246 Q110 244 100 252 Z", ArtTones.Ruby)
    val ruffle = itemShape("M104 238 Q112 230 120 238 Q128 230 136 238 L132 250 Q120 246 108 250 Z", ArtTones.Ruby)
    val peeking = itemCircle(126f, 233f, 7f, ArtTones.Gold)
    val tie = svgPath("M106 249 Q120 244 134 249")
    val emblem = itemCircle(120f, 270f, 9f, ArtTones.Gold)
    val star = starPath(120f, 270.5f, 5.5f)
    val spilled =
        listOf(
            VinylShape(ovalPath(84f, 290f, 8f, 3.6f), ArtTones.Gold),
            VinylShape(ovalPath(158f, 291f, 8f, 3.6f), ArtTones.Gold),
            VinylShape(ovalPath(170f, 286f, 7f, 3.2f), ArtTones.Gold),
        )
    val tieStroke = itemStroke(3.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        for (i in spilled.indices) vinylShape(spilled[i])
        vinylShape(peeking)
        vinylShape(ruffle)
        vinylShape(pouch)
        specular(pouch.bounds)
        drawPath(tie, ArtTones.Gold.base, style = tieStroke)
        vinylShape(emblem)
        drawPath(star, ArtTones.Gold.shade)
    }
}

/** A pink ribbon around the neck tied in a big bow under the chin. */
private fun bow(): ItemArt {
    val band = itemShape("M84 160 Q120 188 156 160 L156 170 Q120 198 84 170 Z", ArtTones.Pink)
    val tails =
        itemShape(
            "M116 186 L104 212 L111 210 L116 216 L120 190 Z M124 186 L136 212 L129 210 L124 216 L120 190 Z",
            ArtTones.Pink,
        )
    val loops =
        itemShape(
            "M120 184 C108 164 86 164 88 181 C90 198 110 196 120 184 Z " +
                "M120 184 C132 164 154 164 152 181 C150 198 130 196 120 184 Z",
            ArtTones.Pink,
        )
    val knot = VinylShape(ovalPath(120f, 184f, 7f, 8f), ArtTones.Pink)
    val folds = svgPath("M112 182 Q102 176 94 179 M128 182 Q138 176 146 179")
    val foldStroke = itemStroke(1.8f)
    val leftLoopShine = ovalRect(98f, 178f, 10f, 8f)
    val rightLoopShine = ovalRect(142f, 178f, 10f, 8f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(band)
        vinylShape(tails)
        vinylShape(loops)
        drawPath(folds, ArtTones.Pink.shade, alpha = 0.6f, style = foldStroke)
        specular(leftLoopShine)
        specular(rightLoopShine)
        vinylShape(knot)
    }
}

/** A tall denim wizard hat with a flopped tip, a golden band and stars, over the horn roots. */
private fun wizardHat(): ItemArt {
    val brim = VinylShape(ovalPath(120f, 64f, 70f, 13f), ArtTones.Denim)
    val cone =
        itemShape(
            "M84 62 C96 44 112 22 142 4 C136 24 140 46 156 62 Q120 70 84 62 Z",
            ArtTones.Denim,
        )
    val band = itemShape("M85 54 Q120 62 155 54 L157 63 Q120 71 83 63 Z", ArtTones.Gold)
    val stars =
        Path().apply {
            addPath(starPath(108f, 42f, 7f))
            addPath(starPath(128f, 24f, 5f))
            addPath(starPath(136f, 46f, 4.5f))
        }
    val twinkles =
        Path().apply {
            addItemTwinkle(121f, 38f, 3.5f)
            addItemTwinkle(132f, 14f, 3f)
        }
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(brim)
        specular(brim.bounds)
        vinylShape(cone)
        specular(cone.bounds, -20f)
        drawPath(stars, ArtTones.Sun.base)
        drawPath(twinkles, ArtTones.Pearl.base)
        vinylShape(band)
    }
}

/** An open red spell book held against the belly, with twinkles rising from its pages. */
private fun magicBook(): ItemArt {
    val cover =
        itemShape(
            "M78 212 L80 254 Q100 248 120 256 Q140 248 160 254 L162 212 Q140 206 120 216 Q100 206 78 212 Z",
            ArtTones.Ruby,
        )
    val pages =
        itemShape(
            "M120 218 Q102 208 84 214 L86 248 Q104 242 120 250 Z M120 218 Q138 208 156 214 L154 248 Q136 242 120 250 Z",
            ArtTones.Pearl,
        )
    val lines =
        Path().apply {
            for (dy in floatArrayOf(0f, 7f, 14f, 21f)) {
                moveTo(91f, 223f + dy)
                quadraticTo(103f, 219f + dy, 114f, 225f + dy)
                moveTo(149f, 223f + dy)
                quadraticTo(137f, 219f + dy, 126f, 225f + dy)
            }
        }
    val gutter = svgPath("M120 218 L120 250")
    val sparkles =
        Path().apply {
            addItemTwinkle(102f, 198f, 6f)
            addItemTwinkle(140f, 194f, 7f)
            addItemTwinkle(130f, 206f, 4f)
        }
    val lineStroke = itemStroke(1.6f)
    val gutterStroke = itemStroke(2f)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        vinylShape(cover)
        vinylShape(pages)
        drawPath(lines, ArtTones.Denim.base, alpha = 0.35f, style = lineStroke)
        drawPath(gutter, ArtTones.Pearl.shade, style = gutterStroke)
        drawPath(sparkles, ArtTones.Sun.base)
    }
}

/** A teal knit scarf with sunny stripes: a wrap around the neck and a fringed end hanging on the left. */
private fun scarf() =
    itemStripedScarf(
        wrap = "M80 158 Q120 186 160 158 L158 176 Q120 204 82 176 Z",
        end = "M80 174 L102 180 L96 222 L74 216 Z",
        stripes =
            ScarfStripes(
                xs = floatArrayOf(96f, 120f, 144f),
                top = 150f,
                bottom = 210f,
                end = itemScarfBands(70f, 106f, 190f, 204f),
            ),
        fringeStart = Offset(74f, 216f),
        fringeLength = 15f,
        tones = ArtTones.Teal,
        stripeTones = ArtTones.Sun,
    )

/** A golden tiara on the forehead between the horns: three peaks with pearls and a pink diamond gem. */
private fun tiara(): ItemArt {
    val band =
        itemShape(
            "M82 72 Q120 56 158 72 L156 62 Q146 56 136 54 L130 38 L124 48 L120 24 " +
                "L116 48 L110 38 L104 54 Q94 56 84 62 Z",
            ArtTones.Gold,
        )
    val pearls =
        listOf(
            itemCircle(120f, 22f, 4.5f, ArtTones.Pearl),
            itemCircle(110f, 36f, 3.4f, ArtTones.Pearl),
            itemCircle(130f, 36f, 3.4f, ArtTones.Pearl),
        )
    val gem = VinylShape(gemPath(120f, 54f, 7f, 10f), ArtTones.Pink)
    val sideGems =
        listOf(
            itemCircle(96f, 63f, 3.5f, ArtTones.Sky),
            itemCircle(144f, 63f, 3.5f, ArtTones.Sky),
        )
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(band)
        specular(band.bounds)
        for (i in pearls.indices) vinylShape(pearls[i])
        vinylShape(gem)
        for (i in sideGems.indices) vinylShape(sideGems[i])
    }
}

/** A knight's shield held at the left side: a denim field with a golden rim and a golden star. */
private fun shield(): ItemArt {
    val outline = "M30 186 Q58 178 86 186 L86 222 Q86 252 58 266 Q30 252 30 222 Z"
    val rim = itemShape(outline, ArtTones.Gold)
    val field = itemShape("M36 191 Q58 185 80 191 L80 222 Q80 247 58 259 Q36 247 36 222 Z", ArtTones.Denim)
    val band = itemShape("M36 214 L80 214 L80 226 L36 226 Z", ArtTones.Ruby)
    val star = VinylShape(starPath(58f, 220f, 13f), ArtTones.Gold)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        vinylShape(rim)
        vinylShape(field)
        clipPath(field.path) { vinylShape(band) }
        vinylShape(star)
        specular(field.bounds)
    }
}

/** A chunky golden chain along the neckline with a round medallion. */
private fun goldChain(): ItemArt {
    val links = (0..10).map { i -> neckAt(i / 10f) }
    val linkPath =
        Path().apply {
            for (i in links.indices) addPath(ovalPath(links[i].x, links[i].y + 4f, 5.5f, 4.5f))
        }
    val linkBrush = vinylBrush(linkPath.getBounds().inflate(3f), ArtTones.Gold)
    val medal = itemCircle(120f, 194f, 11f, ArtTones.Gold)
    val ring = ovalPath(120f, 194f, 7.5f)
    val linkStroke = itemStroke(3.6f)
    val ringStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(linkPath, linkBrush, style = linkStroke)
        vinylShape(medal)
        drawPath(ring, ArtTones.Gold.shade, alpha = 0.7f, style = ringStroke)
        specular(medal.bounds)
    }
}

/** A steel knight helmet with a visor band, a crest, rivets and a red plume. The horns come out of it. */
private fun knightHelmet(): ItemArt {
    val dome = itemShape("M50 100 Q46 34 120 30 Q194 34 190 100 Q120 82 50 100 Z", ArtTones.Steel)
    val visor = itemShape("M56 80 Q120 62 184 80 L184 92 Q120 74 56 92 Z", ArtTones.Steel)
    val crest = svgPath("M120 48 L120 72")
    val plume =
        itemShape("M120 34 C108 12 122 -2 146 2 C134 8 134 16 142 24 C132 22 126 26 120 34 Z", ArtTones.Ruby)
    val rivets =
        Path().apply {
            for ((x, y) in listOf(64f to 94f, 86f to 90f, 154f to 90f, 176f to 94f)) addPath(ovalPath(x, y, 2.6f))
        }
    val crestStroke = itemStroke(5f)
    return ItemArt(ArtPart.HEAD, ItemLayer.OVER_EARS, AboveHelmetRim) {
        vinylShape(plume)
        vinylShape(dome)
        specular(dome.bounds, -12f)
        drawPath(crest, ArtTones.Steel.shade, style = crestStroke)
        vinylShape(visor)
        drawPath(rivets, ArtTones.Pearl.base)
    }
}

/** A fluffy cloud the dragon sits on, with golden stars and twinkles. */
private fun starCloud(): ItemArt {
    val cloud =
        VinylShape(
            Path().apply {
                addPath(ovalPath(48f, 276f, 20f))
                addPath(ovalPath(78f, 268f, 24f))
                addPath(ovalPath(112f, 274f, 24f))
                addPath(ovalPath(148f, 268f, 26f))
                addPath(ovalPath(182f, 274f, 22f))
                addPath(ovalPath(206f, 282f, 15f))
                addPath(ovalPath(120f, 286f, 96f, 11f))
            },
            ArtTones.Sky,
        )
    val stars =
        Path().apply {
            addPath(starPath(66f, 274f, 7f))
            addPath(starPath(134f, 266f, 8f))
            addPath(starPath(186f, 278f, 6f))
        }
    val twinkles =
        Path().apply {
            addItemTwinkle(22f, 246f, 6f)
            addItemTwinkle(218f, 244f, 7f)
            addItemTwinkle(98f, 284f, 4f)
        }
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(cloud)
        drawPath(stars, ArtTones.Gold.base)
        drawPath(twinkles, ArtTones.Sun.base)
    }
}

/** A sparkle necklace: sky and pink crystals on a fine gold chain, a large drop and twinkles. */
private fun sparkleNecklace(): ItemArt {
    val chain = svgPath("M82 162 Q120 190 158 162")
    val crystals =
        (0..6).map { i ->
            val p = neckAt((i + 0.5f) / 7f)
            VinylShape(gemPath(p.x, p.y + 4f, 4.5f, 6f), if (i % 2 == 0) ArtTones.Sky else ArtTones.Pink)
        }
    val drop = VinylShape(gemPath(120f, 194f, 9f, 13f), ArtTones.Sky)
    val twinkles =
        Path().apply {
            addItemTwinkle(98f, 194f, 5f)
            addItemTwinkle(142f, 194f, 5f)
            addItemTwinkle(132f, 208f, 3.5f)
        }
    val chainStroke = itemStroke(1.5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(chain, ArtTones.Gold.base, style = chainStroke)
        for (i in crystals.indices) vinylShape(crystals[i])
        vinylShape(drop)
        specular(drop.bounds)
        drawPath(twinkles, ArtTones.Pearl.base)
    }
}

/**
 * A royal cape behind the dragon: a ruby cape flaring out to the ground with an ermine hem, and an
 * ermine collar standing up behind the shoulders.
 */
private fun royalCape(): ItemArt {
    val cape = itemShape("M70 154 Q120 142 170 154 Q206 206 218 270 Q120 290 22 270 Q34 206 70 154 Z", ArtTones.Ruby)
    val hem = itemShape("M22 270 Q120 290 218 270 L220 284 Q120 302 20 284 Z", ArtTones.Pearl)
    val collar = itemShape("M60 162 Q54 140 74 132 Q120 152 166 132 Q186 140 180 162 Q120 146 60 162 Z", ArtTones.Pearl)
    val spots =
        Path().apply {
            for ((x, y) in listOf(36f to 280f, 62f to 283f, 90f to 285f, 150f to 285f, 178f to 283f, 204f to 280f)) {
                addPath(gemPath(x, y, 2.2f, 3f))
            }
            for (x in floatArrayOf(66f, 174f)) addPath(gemPath(x, 152f, 2.2f, 3f))
        }
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        vinylShape(cape)
        specular(cape.bounds)
        vinylShape(hem)
        vinylShape(collar)
        drawPath(spots, C.EyeInk)
    }
}

/** A royal crown: a golden band with an ermine rim, a ruby velvet cap, golden arches and an orb. */
private fun crown(): ItemArt {
    val velvet = itemShape("M90 62 Q88 26 120 24 Q152 26 150 62 Z", ArtTones.Ruby)
    val arches = svgPath("M92 60 Q96 26 120 24 Q144 26 148 60 M120 24 L120 60")
    val archBrush = vinylBrush(arches.getBounds().inflate(3f), ArtTones.Gold)
    val band = itemShape("M88 54 Q120 46 152 54 L152 66 Q120 58 88 66 Z", ArtTones.Gold)
    val rim = itemShape("M90 64 Q120 56 150 64 Q154 68 150 72 Q120 64 90 72 Q86 68 90 64 Z", ArtTones.Pearl)
    val rimSpots =
        Path().apply {
            for (x in floatArrayOf(98f, 120f, 142f)) addPath(ovalPath(x, if (x == 120f) 64.5f else 66.5f, 1.8f, 2.4f))
        }
    val orb = itemCircle(120f, 18f, 7f, ArtTones.Gold)
    val gems =
        listOf(
            itemCircle(120f, 55f, 4f, ArtTones.Teal),
            itemCircle(102f, 57f, 3f, ArtTones.Ruby),
            itemCircle(138f, 57f, 3f, ArtTones.Ruby),
        )
    val archStroke = itemStroke(4.5f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(velvet)
        specular(velvet.bounds)
        drawPath(arches, archBrush, style = archStroke)
        vinylShape(orb)
        vinylShape(band)
        for (i in gems.indices) vinylShape(gems[i])
        vinylShape(rim)
        drawPath(rimSpots, C.EyeInk)
    }
}

/** One sparkle wing's pieces on the viewer's left; [x] mirrors a left-side x coordinate to the wing's side. */
private class SparkleWing(
    x: (Float) -> Float,
) {
    val outer =
        svgPath(
            "M${x(100f)} 188 Q${x(60f)} 152 ${x(8f)} 152 Q${x(22f)} 170 ${x(16f)} 190 Q${x(38f)} 186 ${x(42f)} 206 " +
                "Q${x(62f)} 202 ${x(70f)} 224 Q${x(88f)} 216 ${x(100f)} 234 Z",
        )
    val inner =
        svgPath(
            "M${x(98f)} 194 Q${x(70f)} 172 ${x(40f)} 168 Q${x(46f)} 182 ${x(40f)} 194 Q${x(56f)} 194 ${x(60f)} 208 " +
                "Q${x(76f)} 208 ${x(82f)} 222 Q${x(92f)} 216 ${x(98f)} 226 Z",
        )
    val ribs =
        Path().apply {
            for ((fx, fy) in listOf(16f to 190f, 42f to 206f, 70f to 224f)) {
                moveTo(x(56f), 168f)
                lineTo(x(fx), fy)
            }
            moveTo(x(100f), 188f)
            quadraticTo(x(60f), 152f, x(8f), 152f)
        }
    val stars =
        Path().apply {
            addPath(starPath(x(30f), 172f, 5f))
            addPath(starPath(x(64f), 190f, 4f))
        }
    val twinkles =
        Path().apply {
            addItemTwinkle(x(8f), 140f, 6f)
            addItemTwinkle(x(34f), 218f, 4.5f)
        }
}

/**
 * Sparkle wings in place of the dragon's own: sky membrane wings with pink inner panels, pearl ribs,
 * golden stars and twinkles. The rig draws them once per wing so each side flaps on its own shoulder.
 */
private fun sparkleWings(): ItemArt {
    val left = SparkleWing { it }
    val right = SparkleWing { 240f - it }
    val outer = VinylShape(itemPath(left.outer, right.outer), ArtTones.Sky)
    val inner = VinylShape(itemPath(left.inner, right.inner), ArtTones.Pink)
    val ribs = itemPath(left.ribs, right.ribs)
    val stars = itemPath(left.stars, right.stars)
    val twinkles = itemPath(left.twinkles, right.twinkles)
    val ribStroke = itemStroke(2.6f)
    return ItemArt(ArtPart.WINGS, ItemLayer.BEHIND) {
        vinylShape(outer)
        vinylShape(inner)
        drawPath(ribs, ArtTones.Pearl.base, alpha = 0.85f, style = ribStroke)
        drawPath(stars, ArtTones.Gold.base)
        drawPath(twinkles, ArtTones.Sun.base)
    }
}

/** The dragon's item artwork, keyed by item id. Declared last so the shared tones above are ready. */
internal val DragonItemArt: Map<String, ItemArt> =
    mapOf(
        "dragon-neck-pendant" to pendant(),
        "dragon-head-flower-crown" to flowerCrown(),
        "dragon-extra-coins" to coinPouch(),
        "dragon-neck-bow" to bow(),
        "dragon-head-wizard-hat" to wizardHat(),
        "dragon-extra-book" to magicBook(),
        "dragon-neck-scarf" to scarf(),
        "dragon-head-tiara" to tiara(),
        "dragon-extra-shield" to shield(),
        "dragon-neck-chain" to goldChain(),
        "dragon-head-knight-helmet" to knightHelmet(),
        "dragon-extra-star-cloud" to starCloud(),
        "dragon-neck-sparkles" to sparkleNecklace(),
        "dragon-neck-cape" to royalCape(),
        "dragon-head-crown" to crown(),
        "dragon-extra-wings" to sparkleWings(),
    )
