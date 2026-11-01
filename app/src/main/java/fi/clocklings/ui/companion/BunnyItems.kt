// Literal coordinates place each item on the bunny in its 240 x 300 design grid.
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
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.components.starPath

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.

/** Sky-blue satin ribbon around the neck, tied in a bow under the chin. */
private fun ribbon(): ItemArt {
    val band = itemShape("M94 178 Q120 192 146 178 L146 189 Q120 203 94 189 Z", ArtTones.Sky)
    val tails = itemShape("M116 198 L104 222 L112 224 L120 202 Z M124 198 L136 222 L128 224 L120 202 Z", ArtTones.Sky)
    val loops =
        itemShape(
            "M120 194 C108 176 84 176 86 193 C88 210 108 208 120 194 Z " +
                "M120 194 C132 176 156 176 154 193 C152 210 132 208 120 194 Z",
            ArtTones.Sky,
        )
    val knot = itemCircle(120f, 194f, 7f, ArtTones.Sky)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(band)
        specular(band.bounds)
        vinylShape(tails)
        vinylShape(loops)
        specular(loops.bounds)
        vinylShape(knot)
    }
}

/** A carrot held in the front paws, leaves up. */
private fun carrot(): ItemArt {
    val root = itemShape("M106 224 Q104 238 120 268 Q136 238 134 224 Q120 218 106 224 Z", ArtTones.Knit)
    val leaves =
        itemShape(
            "M112 223 Q102 202 108 194 Q117 204 119 221 Z M121 221 Q121 196 130 192 Q133 208 126 223 Z",
            ArtTones.Leaf,
        )
    val ridges = svgPath("M110 236 L118 238 M125 247 L132 245 M114 255 L121 257")
    val ridgeStroke = itemStroke(2f)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        vinylShape(leaves)
        vinylShape(root)
        drawPath(ridges, ArtTones.Knit.shade, style = ridgeStroke)
        specular(root.bounds)
    }
}

/** A bow tied at the root of the left ear. */
private fun earBow(): ItemArt {
    val center = Offset(80f, 60f)
    val ends = itemShape("M77 62 Q72 72 65 82 L73 82 L80 66 Z M83 62 Q88 72 95 82 L87 82 L80 66 Z", ArtTones.Scarf)
    val left = itemShape("M80 60 C72 44 53 42 54 57 C55 72 72 73 80 60 Z", ArtTones.Scarf)
    val right = itemShape("M80 60 C88 44 107 42 106 57 C105 72 88 73 80 60 Z", ArtTones.Scarf)
    val knot = itemCircle(80f, 60f, 6.5f, ArtTones.Scarf)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        rotate(-16f, center) {
            vinylShape(ends)
            vinylShape(left)
            vinylShape(right)
            specular(left.bounds)
            specular(right.bounds)
            vinylShape(knot)
        }
    }
}

/** A chain of white daisies around the neck. */
private fun daisies(): ItemArt =
    itemFlowerLei(
        { t -> itemQuad(84f, 180f, 120f, 204f, 156f, 180f, t) },
        7,
        5f,
        FlowerPetals(ring = 5.5f, radius = 5f),
        30f,
        petalTones = listOf(ArtTones.Pearl),
    )

/** A woven basket of colored eggs standing beside the right foot. */
private fun basket(): ItemArt {
    val handle = svgPath("M178 250 Q196 206 214 250")
    val handleStroke = itemStroke(4f)
    val eggs =
        listOf(
            VinylShape(ovalPath(186f, 244f, 7f, 9f), ArtTones.Pink),
            VinylShape(ovalPath(198f, 239f, 7f, 9f), ArtTones.Sky),
            VinylShape(ovalPath(209f, 245f, 7f, 9f), ArtTones.Sun),
        )
    val body = itemShape("M174 252 L218 252 L211 281 L181 281 Z", ArtTones.Straw)
    val rim = VinylShape(roundRectPath(170f, 246f, 222f, 256f, 5f), ArtTones.Straw)
    val weave = svgPath("M178 263 L214 263 M180 272 L212 272 M188 256 L189 280 M196 256 L196 280 M204 256 L203 280")
    val weaveStroke = Stroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(handle, ArtTones.Straw.shade, style = handleStroke)
        for (i in eggs.indices) vinylShape(eggs[i])
        vinylShape(body)
        clipPath(body.path) { drawPath(weave, ArtTones.Straw.shade, alpha = 0.7f, style = weaveStroke) }
        vinylShape(rim)
        specular(rim.bounds)
    }
}

/** A wide straw sun hat with a pink band and a white flower. */
private fun sunHat(): ItemArt {
    val brim = VinylShape(ovalPath(120f, 66f, 82f, 14f), ArtTones.Straw)
    val crown = itemShape("M80 66 Q82 22 120 20 Q158 22 160 66 Z", ArtTones.Straw)
    val band = itemShape("M80 66 Q120 56 160 66 L160 55 Q120 45 80 55 Z", ArtTones.Pink)
    val flower = VinylShape(itemPetals(148f, 57f, 6f, 5f, -90f), ArtTones.Pearl)
    val center = itemCircle(148f, 57f, 3.5f, ArtTones.Gold)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(brim)
        vinylShape(crown)
        specular(crown.bounds)
        vinylShape(band)
        vinylShape(flower)
        vinylShape(center)
    }
}

/** A heart locket on a thin gold chain. */
private fun locket(): ItemArt {
    val chain = svgPath("M78 178 Q100 202 120 206 Q140 202 162 178")
    val chainStroke = itemStroke(2.2f)
    val bail = itemCircle(120f, 207f, 3.5f, ArtTones.Gold)
    val heart =
        itemShape(
            "M120 228 C104 216 104 204 112 203 C116 202 119 205 120 208 " +
                "C121 205 124 202 128 203 C136 204 136 216 120 228 Z",
            ArtTones.Ruby,
        )
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(chain, ArtTones.Gold.base, style = chainStroke)
        vinylShape(bail)
        vinylShape(heart)
        specular(heart.bounds)
    }
}

/** A blue watering can held in the paws, a few drops falling from the spout. */
private fun wateringCan(): ItemArt {
    val handle = svgPath("M104 228 Q92 206 116 214")
    val handleStroke = itemStroke(5f)
    val spout = svgPath("M136 236 L164 210")
    val spoutStroke = itemStroke(7f)
    val rose = itemCircle(166f, 208f, 7f, ArtTones.Sky)
    val can = VinylShape(roundRectPath(100f, 220f, 140f, 254f, 8f), ArtTones.Sky)
    val drops =
        VinylShape(
            itemPath(
                ovalPath(176f, 216f, 2.5f, 3.5f),
                ovalPath(182f, 228f, 2.5f, 3.5f),
                ovalPath(173f, 234f, 2.5f, 3.5f),
            ),
            ArtTones.Sky,
        )
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        drawPath(handle, ArtTones.Sky.shade, style = handleStroke)
        drawPath(spout, ArtTones.Sky.base, style = spoutStroke)
        vinylShape(rose)
        vinylShape(can)
        specular(can.bounds)
        vinylShape(drops)
    }
}

/** A magician's top hat with a red band and two twinkles. */
private fun topHat(): ItemArt {
    val crown = itemShape("M92 58 L90 8 Q120 2 150 8 L148 58 Z", ArtTones.EyeInk)
    val band = itemShape("M91.5 46 L148.5 46 L148 58 L92 58 Z", ArtTones.Ruby)
    val brim = VinylShape(ovalPath(120f, 58f, 44f, 8f), ArtTones.EyeInk)
    val twinkles =
        VinylShape(
            Path().apply {
                addItemTwinkle(74f, 22f, 8f)
                addItemTwinkle(168f, 30f, 6f)
            },
            ArtTones.Gold,
        )
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(crown)
        specular(crown.bounds)
        vinylShape(band)
        vinylShape(brim)
        vinylShape(twinkles)
    }
}

/** Red bow tie with pearl dots. */
private fun bowTie(): ItemArt =
    itemPolkaDotBowTie(Offset(120f, 190f), 24f, 13f, ArtTones.Ruby, ArtTones.Pearl.base, 2.6f)

/** A bubble wand held in the paws, with bubbles floating off to the right. */
private fun bubbles(): ItemArt {
    val stick = svgPath("M126 254 L164 214")
    val stickStroke = itemStroke(4f)
    val ring = Stroke(3.5f)
    val bubbleRim = Stroke(2f)
    val glint = itemStroke(2f)
    val spots = arrayOf(Offset(194f, 232f) to 13f, Offset(222f, 214f) to 8f, Offset(214f, 256f) to 6f)
    val glints =
        Path().apply {
            for ((c, r) in spots) {
                moveTo(c.x - r * 0.55f, c.y - r * 0.2f)
                quadraticTo(c.x - r * 0.5f, c.y - r * 0.6f, c.x - r * 0.1f, c.y - r * 0.6f)
            }
        }
    val fill = ArtTones.Sky.light.copy(alpha = 0.3f)
    val rim = ArtTones.Sky.base.copy(alpha = 0.85f)
    // Drawn after the paws and feet so the bubbles float in front of them.
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(stick, ArtTones.Gold.base, style = stickStroke)
        drawCircle(ArtTones.Gold.shade, 10f, Offset(170f, 206f), style = ring)
        for ((c, r) in spots) {
            drawCircle(fill, r, c)
            drawCircle(rim, r, c, style = bubbleRim)
        }
        drawPath(glints, Color.White, alpha = 0.85f, style = glint)
    }
}

/** A crown of flowers across the top of the head. */
private fun flowerCrown(): ItemArt =
    itemFlowerLei(
        { t -> itemQuad(62f, 82f, 120f, 22f, 178f, 82f, t) },
        7,
        5f,
        FlowerPetals(ring = 6f, radius = 5.5f),
        30f,
        part = ArtPart.HEAD,
    )

/** A wrapped bouquet of three flowers held in the paws. */
private fun bouquet(): ItemArt {
    val leaves = VinylShape(itemPath(ovalPath(102f, 214f, 8f, 4.5f), ovalPath(138f, 214f, 8f, 4.5f)), ArtTones.Leaf)
    val blooms =
        listOf(
            VinylShape(itemPetals(106f, 206f, 7f, 6f, -90f), ArtTones.Knit),
            VinylShape(itemPetals(134f, 206f, 7f, 6f, -54f), ArtTones.Sky),
            VinylShape(itemPetals(120f, 196f, 7f, 6f, -72f), ArtTones.Sun),
        )
    val centers =
        VinylShape(
            itemPath(ovalPath(106f, 206f, 3.5f), ovalPath(134f, 206f, 3.5f), ovalPath(120f, 196f, 3.5f)),
            ArtTones.Gold,
        )
    val wrap = itemShape("M100 214 L140 214 L125 264 L115 264 Z", ArtTones.Pink)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        vinylShape(leaves)
        for (i in blooms.indices) vinylShape(blooms[i])
        vinylShape(centers)
        vinylShape(wrap)
        specular(wrap.bounds)
    }
}

/** A blue cape with gold stars, fastened at the neck with a star clasp. */
private fun cape(): ItemArt {
    val cloth = itemShape("M76 174 Q48 228 28 284 Q120 300 212 284 Q192 228 164 174 Z", ArtTones.Denim)
    val stars =
        VinylShape(
            Path().apply {
                addPath(starPath(44f, 246f, 6f))
                addPath(starPath(58f, 214f, 4.5f))
                addPath(starPath(196f, 246f, 6f))
                addPath(starPath(182f, 214f, 4.5f))
            },
            ArtTones.Gold,
        )
    val band = itemShape("M82 176 Q120 196 158 176 L157 186 Q120 206 83 186 Z", ArtTones.Denim)
    val clasp = VinylShape(starPath(120f, 194f, 10f), ArtTones.Gold)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT, back = {
        vinylShape(cloth)
        vinylShape(stars)
    }) {
        vinylShape(band)
        specular(band.bounds)
        vinylShape(clasp)
        specular(clasp.bounds)
    }
}

/** A gold tiara with three points and gems. */
private fun tiara(): ItemArt {
    val band = itemShape("M80 66 Q120 46 160 66 L156 73 Q120 55 84 73 Z", ArtTones.Gold)
    val points =
        itemShape("M97 60 L103 40 L111 56 Z M111 55 L120 28 L129 55 Z M129 56 L137 40 L143 60 Z", ArtTones.Gold)
    val pink = itemCircle(120f, 46f, 4.5f, ArtTones.Pink)
    val blue = VinylShape(itemPath(ovalPath(104f, 53f, 3f), ovalPath(136f, 53f, 3f)), ArtTones.Sky)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(points)
        vinylShape(band)
        specular(band.bounds)
        vinylShape(pink)
        vinylShape(blue)
    }
}

/** Translucent sky-blue fairy wings behind the body, with gold twinkles. */
private fun wings(): ItemArt {
    val upper =
        itemShape(
            "M98 194 C70 164 2 160 4 202 C6 240 70 236 100 214 Z " +
                "M142 194 C170 164 238 160 236 202 C234 240 170 236 140 214 Z",
            ArtTones.Sky,
        )
    val lower =
        itemShape(
            "M100 218 C76 226 22 244 30 268 C38 290 88 260 104 228 Z " +
                "M140 218 C164 226 218 244 210 268 C202 290 152 260 136 228 Z",
            ArtTones.Sky,
        )
    val twinkles =
        VinylShape(
            Path().apply {
                addItemTwinkle(16f, 220f, 6f)
                addItemTwinkle(224f, 220f, 6f)
                addItemTwinkle(30f, 270f, 4.5f)
                addItemTwinkle(210f, 270f, 4.5f)
            },
            ArtTones.Gold,
        )
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        drawPath(lower.path, lower.brush, alpha = WING_ALPHA)
        drawPath(upper.path, upper.brush, alpha = WING_ALPHA)
        vinylShape(twinkles)
    }
}

private const val WING_ALPHA = 0.85f

/** The bunny's item artwork keyed by item id. */
internal val BunnyItemArt: Map<String, ItemArt> =
    mapOf(
        "bunny-neck-ribbon" to ribbon(),
        "bunny-extra-carrot" to carrot(),
        "bunny-head-ear-bow" to earBow(),
        "bunny-neck-daisies" to daisies(),
        "bunny-extra-basket" to basket(),
        "bunny-head-sun-hat" to sunHat(),
        "bunny-neck-locket" to locket(),
        "bunny-extra-watering-can" to wateringCan(),
        "bunny-head-top-hat" to topHat(),
        "bunny-neck-bow-tie" to bowTie(),
        "bunny-extra-bubbles" to bubbles(),
        "bunny-head-flower-crown" to flowerCrown(),
        "bunny-extra-bouquet" to bouquet(),
        "bunny-neck-cape" to cape(),
        "bunny-head-tiara" to tiara(),
        "bunny-extra-wings" to wings(),
    )
