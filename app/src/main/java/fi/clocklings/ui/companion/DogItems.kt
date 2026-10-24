// Literal coordinates place each item on the dog in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.components.starPath

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.
// The ears hang in front of the head, so every hat is a HEAD/FRONT item that covers their roots.

/** Red bandana: a band around the neck and a triangle with pearl dots. */
private fun bandana(): ItemArt {
    val band = itemShape("M68 160 Q120 182 172 160 L170 172 Q120 194 70 172 Z", ArtTones.Ruby)
    val point = itemShape("M82 170 Q120 188 158 170 Q140 198 120 216 Q100 198 82 170 Z", ArtTones.Ruby)
    val dots =
        Path().apply {
            for ((x, y) in listOf(102f to 184f, 120f to 190f, 138f to 184f, 111f to 200f, 129f to 200f, 120f to 209f)) {
                addPath(ovalPath(x, y, 3f))
            }
        }
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(band)
        vinylShape(point)
        clipPath(point.path) { drawPath(dots, ArtTones.Pearl.base) }
        specular(point.bounds)
    }
}

/** Sky baseball cap with a denim brim, seams, a top button and a star. */
private fun cap(): ItemArt {
    val crown = itemShape("M60 76 Q58 26 120 24 Q182 26 180 76 Q120 62 60 76 Z", ArtTones.Sky)
    val brim = itemShape("M64 72 Q120 56 176 72 Q186 86 160 89 Q120 78 80 89 Q54 86 64 72 Z", ArtTones.Denim)
    val seams = svgPath("M120 28 Q100 40 94 66 M120 28 Q140 40 146 66")
    val button = itemCircle(120f, 26f, 4.5f, ArtTones.Denim)
    val star = VinylShape(starPath(120f, 48f, 9f), ArtTones.Sun)
    val seamStroke = itemStroke(1.8f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(crown)
        specular(crown.bounds, -12f)
        drawPath(seams, ArtTones.Sky.shade, alpha = 0.6f, style = seamStroke)
        vinylShape(star)
        vinylShape(brim)
        specular(brim.bounds)
        vinylShape(button)
    }
}

/** Tennis ball between the front paws, with its white seams. */
private fun tennisBall(): ItemArt {
    val ball = itemCircle(120f, 257f, 20f, ArtTones.Tennis)
    val seams = svgPath("M103 243 Q119 257 103 271 M137 243 Q121 257 137 271")
    val seamStroke = itemStroke(2.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(ball)
        clipPath(ball.path) { drawPath(seams, ArtTones.Pearl.base, style = seamStroke) }
        specular(ball.bounds)
    }
}

/** Teal collar with a golden bone-shaped name tag. */
private fun boneCollar(): ItemArt {
    val band = itemShape("M68 160 Q120 182 172 160 L170 171 Q120 195 70 171 Z", ArtTones.Teal)
    val ring = itemCircle(120f, 185f, 3.5f, ArtTones.Gold)
    val bone =
        VinylShape(
            Path().apply {
                for ((x, y) in listOf(104f to 192f, 104f to 201f, 136f to 192f, 136f to 201f)) {
                    addPath(ovalPath(x, y, 5.5f))
                }
                addPath(svgPath("M104 191 L136 191 L136 202 L104 202 Z"))
            },
            ArtTones.Gold,
        )
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(band)
        specular(band.bounds)
        vinylShape(ring)
        vinylShape(bone)
        specular(bone.bounds)
    }
}

/** Wide straw sun hat with a red band and a pink flower. */
private fun sunHat(): ItemArt {
    val brim = VinylShape(ovalPath(120f, 66f, 86f, 17f), ArtTones.Straw)
    val weave = svgPath("M48 70 Q120 92 192 70")
    val crown = itemShape("M82 68 Q80 22 120 20 Q160 22 158 68 Q120 76 82 68 Z", ArtTones.Straw)
    val band = itemShape("M82 56 Q120 64 158 56 L158 67 Q120 75 82 67 Z", ArtTones.Ruby)
    val petals = VinylShape(itemPetals(146f, 61f, 6.5f, 5.5f, -90f), ArtTones.Pink)
    val center = itemCircle(146f, 61f, 3.8f, ArtTones.Sun)
    val weaveStroke = itemStroke(1.8f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(brim)
        drawPath(weave, ArtTones.Straw.shade, alpha = 0.5f, style = weaveStroke)
        vinylShape(crown)
        specular(crown.bounds, -12f)
        vinylShape(band)
        vinylShape(petals)
        vinylShape(center)
    }
}

/** A frisbee carried in the mouth, below the nose. */
private fun frisbee(): ItemArt {
    val center = Offset(120f, 152f)
    val disc = VinylShape(ovalPath(center.x, center.y, 38f, 12f), ArtTones.Knit)
    val rim = ovalPath(center.x, center.y - 1f, 27f, 7f)
    val rimStroke = itemStroke(2.4f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        rotate(-8f, center) {
            vinylShape(disc)
            drawPath(rim, ArtTones.Knit.shade, alpha = 0.55f, style = rimStroke)
            specular(disc.bounds)
        }
    }
}

/** Denim bow tie with sunny polka dots under the chin. */
private fun bowTie(): ItemArt =
    itemPolkaDotBowTie(Offset(120f, 182f), 24f, 14f, ArtTones.Denim, ArtTones.Sun.base, 2.6f)

/** Pink party hat with dots and a pearl pom, tilted on the right side of the head. */
private fun partyHat(): ItemArt {
    val pivot = Offset(132f, 64f)
    val cone = itemShape("M106 66 L132 12 L158 64 Q132 74 106 66 Z", ArtTones.Pink)
    val dots =
        Path().apply {
            for ((x, y) in listOf(126f to 32f, 140f to 44f, 120f to 50f, 132f to 60f, 148f to 58f, 114f to 62f)) {
                addPath(ovalPath(x, y, 3.2f))
            }
        }
    val rim = svgPath("M106 66 Q132 74 158 64")
    val pom = itemCircle(132f, 12f, 7.5f, ArtTones.Pearl)
    val rimStroke = itemStroke(4.5f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        rotate(12f, pivot) {
            vinylShape(cone)
            clipPath(cone.path) { drawPath(dots, ArtTones.Sun.base) }
            specular(cone.bounds, -8f)
            drawPath(rim, ArtTones.Teal.base, style = rimStroke)
            vinylShape(pom)
        }
    }
}

/** Yellow rain boots over both front paws, with a gold cuff and sole. */
private fun boots(): ItemArt {
    fun boot(x: Float) =
        Path().apply {
            addPath(roundRectPath(RoundRect(x - 14f, 238f, x + 14f, 272f, CornerRadius(6f))))
            addPath(ovalPath(x, 276f, 25f, 11f))
        }
    val left = VinylShape(boot(94f), ArtTones.Sun)
    val right = VinylShape(boot(146f), ArtTones.Sun)
    val cuffs =
        VinylShape(
            Path().apply {
                addPath(svgPath("M78 238 L110 238 L110 247 L78 247 Z"))
                addPath(svgPath("M130 238 L162 238 L162 247 L130 247 Z"))
            },
            ArtTones.Gold,
        )
    val soles = svgPath("M71 281 Q94 291 117 281 M123 281 Q146 291 169 281")
    val soleStroke = itemStroke(3f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(left)
        vinylShape(right)
        specular(left.bounds)
        specular(right.bounds)
        vinylShape(cuffs)
        drawPath(soles, ArtTones.Gold.shade, style = soleStroke)
    }
}

/** Flower lei: nine flowers in four colors along the neckline, on a few leaves. */
private fun lei(): ItemArt =
    itemFlowerLei({ t -> itemQuad(80f, 170f, 120f, 200f, 160f, 170f, t) }, 9, 5f, 5.5f, 5f, 23f)

/** Olive aviator cap with a fleece trim, ear flaps and goggles pushed up on the forehead. */
private fun aviator(): ItemArt {
    val cap = itemShape("M58 84 Q56 28 120 26 Q184 28 182 84 Q120 70 58 84 Z", ArtTones.Olive)
    val flaps =
        itemShape(
            "M58 76 Q48 94 52 114 Q62 126 74 114 Q76 94 72 80 Z " +
                "M182 76 Q192 94 188 114 Q178 126 166 114 Q164 94 168 80 Z",
            ArtTones.Olive,
        )
    val seam = svgPath("M120 28 L120 46")
    val trim = svgPath("M58 84 Q120 70 182 84")
    val strap = svgPath("M60 62 Q120 46 180 62")
    val lenses =
        Path().apply {
            addPath(ovalPath(100f, 58f, 12f))
            addPath(ovalPath(140f, 58f, 12f))
        }
    val lensShape = VinylShape(lenses, ArtTones.Sky)
    val leftLens = ovalRect(100f, 58f, 12f)
    val rightLens = ovalRect(140f, 58f, 12f)
    val rimBrush = vinylBrush(lensShape.bounds.inflate(3f), ArtTones.Gold)
    val bridge = svgPath("M112 56 Q120 52 128 56")
    val seamStroke = itemStroke(2f)
    val trimStroke = itemStroke(7f)
    val strapStroke = itemStroke(5f)
    val rimStroke = itemStroke(4f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(flaps)
        vinylShape(cap)
        specular(cap.bounds, -12f)
        drawPath(seam, ArtTones.Olive.shade, style = seamStroke)
        drawPath(trim, ArtTones.Pearl.base, style = trimStroke)
        drawPath(strap, ArtTones.Olive.shade, style = strapStroke)
        vinylShape(lensShape)
        drawPath(lenses, rimBrush, style = rimStroke)
        drawPath(bridge, rimBrush, style = rimStroke)
        specular(leftLens)
        specular(rightLens)
    }
}

/**
 * Red backpack: the pack shows where it comes out from behind the dog's left side, and its denim
 * straps with sunny buckles run over the shoulders in front. Drawn in front of the body but clipped
 * out of the body outline, so one item can show both.
 */
private fun backpack(): ItemArt {
    val pack = VinylShape(roundRectPath(RoundRect(26f, 164f, 88f, 246f, CornerRadius(15f))), ArtTones.Knit)
    val pocket = VinylShape(roundRectPath(RoundRect(30f, 204f, 62f, 238f, CornerRadius(6f))), ArtTones.Knit)
    val pocketSeam = svgPath("M30 214 L62 214")
    val straps = svgPath("M88 162 Q82 194 72 226 M152 162 Q158 194 168 226")
    val strapBrush = vinylBrush(straps.getBounds().inflate(5f), ArtTones.Denim)
    val buckles =
        VinylShape(
            Path().apply {
                addPath(svgPath("M77 188 L89 188 L88 198 L76 198 Z"))
                addPath(svgPath("M151 188 L163 188 L164 198 L152 198 Z"))
            },
            ArtTones.Sun,
        )
    val strapStroke = itemStroke(9f)
    val seamStroke = itemStroke(2f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        clipPath(DogBodySilhouette, ClipOp.Difference) {
            vinylShape(pack)
            specular(pack.bounds)
            vinylShape(pocket)
            drawPath(pocketSeam, ArtTones.Knit.shade, style = seamStroke)
        }
        drawPath(straps, strapBrush, style = strapStroke)
        vinylShape(buckles)
    }
}

/** Gold medal with a star on a red ribbon. */
private fun medal(): ItemArt {
    val neckRibbon = svgPath("M70 160 Q120 186 170 160")
    val ribbonBrush = vinylBrush(neckRibbon.getBounds().inflate(4f), ArtTones.Ruby)
    val tails = itemShape("M106 172 L114 200 L126 200 L134 172 L125 170 L120 188 L115 170 Z", ArtTones.Ruby)
    val disc = itemCircle(120f, 210f, 14f, ArtTones.Gold)
    val ring = ovalPath(120f, 210f, 10.5f)
    val star = starPath(120f, 210.5f, 7f)
    val ribbonStroke = itemStroke(6f)
    val ringStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(neckRibbon, ribbonBrush, style = ribbonStroke)
        vinylShape(tails)
        vinylShape(disc)
        drawPath(ring, ArtTones.Gold.shade, alpha = 0.7f, style = ringStroke)
        drawPath(star, ArtTones.Gold.shade)
        specular(disc.bounds)
    }
}

/** Red firefighter helmet with a wide brim, a ridge and a golden shield. */
private fun helmet(): ItemArt {
    val brim = VinylShape(ovalPath(120f, 72f, 80f, 14f), ArtTones.Ruby)
    val dome = itemShape("M72 72 Q70 22 120 20 Q170 22 168 72 Q120 82 72 72 Z", ArtTones.Ruby)
    val ridge = svgPath("M120 22 L120 34")
    val shield = itemShape("M104 34 L136 34 L136 50 Q136 62 120 68 Q104 62 104 50 Z", ArtTones.Gold)
    val star = starPath(120f, 50f, 7f)
    val ridgeStroke = itemStroke(6f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(brim)
        vinylShape(dome)
        specular(dome.bounds, -12f)
        drawPath(ridge, ArtTones.Ruby.shade, style = ridgeStroke)
        vinylShape(shield)
        drawPath(star, ArtTones.Ruby.base)
        specular(shield.bounds)
    }
}

/** Open yellow raincoat with a collar, denim toggles and a pocket. */
private fun raincoat(): ItemArt {
    val panels =
        itemShape(
            "M70 166 Q92 158 112 170 L108 254 Q82 260 56 250 Q54 206 70 166 Z " +
                "M170 166 Q148 158 128 170 L132 254 Q158 260 184 250 Q186 206 170 166 Z",
            ArtTones.Sun,
        )
    val collar = itemShape("M84 160 L112 170 L98 184 Z M156 160 L128 170 L142 184 Z", ArtTones.Sun)
    val toggles =
        VinylShape(
            Path().apply {
                for (y in floatArrayOf(196f, 218f, 240f)) addRect(Rect(101f, y - 3f, 113f, y + 3f))
            },
            ArtTones.Denim,
        )
    val pocket = svgPath("M144 222 L170 222")
    val pocketStroke = itemStroke(3f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        vinylShape(panels)
        specular(panels.bounds)
        vinylShape(collar)
        drawPath(pocket, ArtTones.Sun.shade, style = pocketStroke)
        vinylShape(toggles)
    }
}

/**
 * Red hero cape blowing out to the dog's left: the flowing part shows only where it comes out from
 * behind the body (clipped out of the body outline and kept clear of the tail), and a shoulder mantle
 * with a golden star clasp sits in front, so one item reads as a worn cape.
 */
private fun cape(): ItemArt {
    val flow =
        itemShape(
            "M76 158 Q40 176 20 232 Q10 262 32 276 Q72 288 112 282 L150 282 Q176 280 182 252 Q186 202 164 158 Z",
            ArtTones.Ruby,
        )
    val hem = svgPath("M20 236 Q10 262 32 276 Q72 288 112 282")
    val mantle = itemShape("M68 158 Q120 182 172 158 L172 180 Q150 196 120 192 Q90 196 68 180 Z", ArtTones.Ruby)
    val clasp = itemCircle(120f, 186f, 9f, ArtTones.Gold)
    val star = starPath(120f, 186.5f, 5f)
    val hemStroke = itemStroke(5f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        clipPath(DogBodySilhouette, ClipOp.Difference) {
            vinylShape(flow)
            drawPath(hem, ArtTones.Gold.base, style = hemStroke)
        }
        vinylShape(mantle)
        specular(mantle.bounds)
        vinylShape(clasp)
        drawPath(star, ArtTones.Ruby.base)
    }
}

/** The dog's item artwork, keyed by item id. Declared last so the shared tones above are ready. */
internal val DogItemArt: Map<String, ItemArt> =
    mapOf(
        "dog-neck-bandana" to bandana(),
        "dog-head-cap" to cap(),
        "dog-extra-ball" to tennisBall(),
        "dog-neck-bone-collar" to boneCollar(),
        "dog-head-sun-hat" to sunHat(),
        "dog-extra-frisbee" to frisbee(),
        "dog-neck-bow-tie" to bowTie(),
        "dog-head-party-hat" to partyHat(),
        "dog-extra-boots" to boots(),
        "dog-neck-lei" to lei(),
        "dog-head-aviator" to aviator(),
        "dog-extra-backpack" to backpack(),
        "dog-neck-medal" to medal(),
        "dog-head-helmet" to helmet(),
        "dog-extra-raincoat" to raincoat(),
        "dog-neck-cape" to cape(),
    )
