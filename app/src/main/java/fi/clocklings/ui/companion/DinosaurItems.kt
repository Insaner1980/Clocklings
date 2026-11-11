// Literal coordinates place each item on the dinosaur in its 240 x 300 design grid.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.components.starPath
import fi.clocklings.ui.theme.PersonalizationColors

// Every path, brush and stroke is built once when the map is created; the draw lambdas only paint them.
// The dinosaur faces the viewer's right: its back (and anything worn on it) is on the left, and held
// items hang from the short arms on the belly. Neckwear follows the neckline under the jaw.

/** A point on the dinosaur's neckline at [t] (0 at the back, 1 at the throat). */
private fun neckAt(t: Float) = itemQuad(86f, 148f, 122f, 174f, 160f, 150f, t)

/** The hand at the end of the dinosaur's front arm, where held strings and handles end. */
private val FrontHand = Offset(176f, 204f)

/** Balloon and kite strings: heavy enough to stay visible in the wardrobe tiles. */
private const val STRING_WIDTH = 2.4f
private const val STRING_ALPHA = 0.85f

/**
 * The free backpack: a sunny pack on the back that shows where it comes out from behind the body,
 * and a denim shoulder strap with a buckle in front, so the one item reads as worn. It is drawn with
 * the held items, before the arms, so the strap runs on under the back arm.
 */
private fun backpack(): ItemArt {
    val pack = VinylShape(roundRectPath(30f, 150f, 86f, 240f, 16f), ArtTones.Sun)
    val flap = VinylShape(roundRectPath(30f, 150f, 86f, 178f, 14f), ArtTones.Gold)
    val pocket = VinylShape(roundRectPath(36f, 196f, 70f, 230f, 7f), ArtTones.Knit)
    val pocketSeam = svgPath("M36 206 L70 206")
    val strap = svgPath("M84 146 Q102 162 112 198")
    val strapBrush = vinylBrush(strap.getBounds().inflate(5f), ArtTones.Denim)
    val buckle = itemShape("M93 160 L104 158 L106 168 L95 170 Z", ArtTones.Gold)
    val strapStroke = itemStroke(9f)
    val seamStroke = itemStroke(2f)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        clipPath(DinosaurBodySilhouette, ClipOp.Difference) {
            vinylShape(pack)
            specular(pack.bounds)
            vinylShape(flap)
            vinylShape(pocket)
            drawPath(pocketSeam, ArtTones.Knit.shade, style = seamStroke)
        }
        drawPath(strap, strapBrush, style = strapStroke)
        vinylShape(buckle)
    }
}

/** A big pink flower with a sunny center and two leaves, tucked on the upper back of the head. */
private fun flower(): ItemArt {
    val leaves = itemShape("M80 56 Q54 28 38 40 Q52 62 80 56 Z M80 56 Q72 22 88 12 Q98 36 80 56 Z", ArtTones.Leaf)
    val petals = VinylShape(itemPetals(84f, 60f, 13f, 12f, -54f), ArtTones.Pink)
    val center = itemCircle(84f, 60f, 7.5f, ArtTones.Sun)
    val veins = svgPath("M74 52 L50 42 M82 48 L88 22")
    val veinStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(leaves)
        drawPath(veins, ArtTones.Leaf.shade, alpha = 0.6f, style = veinStroke)
        vinylShape(petals)
        specular(petals.bounds)
        vinylShape(center)
    }
}

/** A sky sand bucket full of sand beside the front foot, with a sunny handle, a star and a red spade. */
private fun bucket(): ItemArt {
    val handle = svgPath("M176 236 Q199 194 222 236")
    val spade = itemShape("M208 208 Q212 190 226 196 Q227 212 215 214 Z", ArtTones.Ruby)
    val stick = svgPath("M208 232 L214 210")
    val pail = itemShape("M176 233 L222 233 L215 284 L183 284 Z", ArtTones.Sky)
    val sand = itemShape("M178 234 Q194 214 220 234 Z", ArtTones.Straw)
    val rim = VinylShape(ovalPath(199f, 234f, 23f, 6f), ArtTones.Sky)
    val star = VinylShape(starPath(199f, 260f, 11f), ArtTones.Sun)
    val handleStroke = itemStroke(3f)
    val stickStroke = itemStroke(4f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(handle, ArtTones.Sun.shade, style = handleStroke)
        drawPath(stick, ArtTones.Ruby.shade, style = stickStroke)
        vinylShape(spade)
        vinylShape(pail)
        specular(pail.bounds)
        vinylShape(rim)
        vinylShape(sand)
        vinylShape(star)
    }
}

/** Orange knitted scarf with sunny stripes, its fringed end hanging down the back. */
private fun scarf() =
    itemStripedScarf(
        wrap = "M84 140 Q122 170 162 142 L160 162 Q122 192 86 162 Z",
        end = "M86 156 L106 164 L100 208 L78 202 Z",
        stripes =
            ScarfStripes(
                xs = floatArrayOf(102f, 124f, 146f),
                top = 130f,
                bottom = 200f,
                end = itemScarfBands(74f, 108f, 176f, 190f),
            ),
        fringeStart = Offset(78f, 202f),
        fringeLength = 14f,
        tones = ArtTones.Knit,
        stripeTones = ArtTones.Sun,
    )

/** Red baseball cap whose brim points forward over the snout, with a pearl button and star. */
private fun cap(): ItemArt {
    val crown = itemShape("M64 66 Q60 20 114 18 Q166 20 168 62 Q116 52 64 66 Z", ArtTones.Ruby)
    val brim = itemShape("M146 52 Q178 46 202 58 Q202 68 188 68 Q166 64 146 66 Z", ArtTones.Ruby)
    val seams = svgPath("M114 22 Q96 34 92 58 M114 22 Q132 34 138 56")
    val button = itemCircle(114f, 19f, 4.5f, ArtTones.Pearl)
    val star = VinylShape(starPath(116f, 42f, 9f), ArtTones.Pearl)
    val seamStroke = itemStroke(1.8f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(brim)
        specular(brim.bounds)
        vinylShape(crown)
        specular(crown.bounds, -12f)
        drawPath(seams, ArtTones.Ruby.shade, alpha = 0.6f, style = seamStroke)
        vinylShape(star)
        vinylShape(button)
    }
}

/** A red balloon floating above the snout on a string held in the front hand. */
private fun balloon(): ItemArt {
    val string = svgPath("M212 62 Q214 120 190 160 Q180 182 ${FrontHand.x} ${FrontHand.y}")
    val ball = VinylShape(ovalPath(212f, 32f, 20f, 24f), ArtTones.Ruby)
    val knot = itemShape("M212 54 L206 63 L218 63 Z", ArtTones.Ruby)
    val stringStroke = itemStroke(STRING_WIDTH)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        drawPath(string, ArtTones.EyeInk.base, alpha = STRING_ALPHA, style = stringStroke)
        vinylShape(knot)
        vinylShape(ball)
        specular(ball.bounds, -20f)
    }
}

/** Red bow tie with pearl polka dots under the jaw. */
private fun bowTie(): ItemArt = itemPolkaDotBowTie(neckAt(0.55f), 24f, 14f, ArtTones.Ruby, ArtTones.Pearl.base, 2.6f)

/** Teal party hat with sunny stripes and a pink pom, tilted back on the head. */
private fun partyHat(): ItemArt {
    val pivot = Offset(112f, 64f)
    val cone = itemShape("M80 64 L112 10 L144 62 Q112 74 80 64 Z", ArtTones.Teal)
    val stripes =
        Path().apply {
            for (y in floatArrayOf(26f, 44f, 62f)) {
                moveTo(74f, y + 5f)
                lineTo(150f, y - 5f)
                lineTo(150f, y + 2f)
                lineTo(74f, y + 12f)
                close()
            }
        }
    val rim = svgPath("M80 64 Q112 74 144 62")
    val pom = itemCircle(112f, 10f, 8f, ArtTones.Pink)
    val stripeBrush = vinylBrush(cone.bounds, ArtTones.Sun)
    val rimStroke = itemStroke(4.5f)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        rotate(-10f, pivot) {
            vinylShape(cone)
            clipPath(cone.path) { drawPath(stripes, stripeBrush) }
            specular(cone.bounds, -8f)
            drawPath(rim, ArtTones.Pearl.base, style = rimStroke)
            vinylShape(pom)
        }
    }
}

/**
 * An open umbrella above the head with knit and sunny panels, its denim shaft running behind the head
 * down to the front hand, which holds the crook.
 */
private fun umbrella(): ItemArt {
    val apex = Offset(146f, 8f)
    val scallop = "M62 48 Q72 2 146 6 Q220 2 230 48 Q209 38 188 48 Q167 38 146 48 Q125 38 104 48 Q83 38 62 48 Z"
    val canopy = itemShape(scallop, ArtTones.Sun)
    val panels =
        itemPath(
            svgPath("M146 6 L62 48 L104 50 Z"),
            svgPath("M146 6 L146 50 L188 50 Z"),
        )
    val panelBrush = vinylBrush(canopy.bounds, ArtTones.Knit)
    val tip = itemCircle(apex.x, 4f, 3.5f, ArtTones.Gold)
    val shaft = svgPath("M${apex.x} ${apex.y} L${FrontHand.x - 2f} ${FrontHand.y + 8f}")
    val crook = svgPath("M${FrontHand.x - 2f} ${FrontHand.y + 8f} Q${FrontHand.x} 226 166 224 Q160 222 162 214")
    val shaftStroke = itemStroke(4f)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        drawPath(shaft, ArtTones.Denim.base, style = shaftStroke)
        drawPath(crook, ArtTones.Denim.base, style = shaftStroke)
        rotate(-8f, apex) {
            vinylShape(canopy)
            clipPath(canopy.path) { drawPath(panels, panelBrush) }
            specular(canopy.bounds)
            vinylShape(tip)
        }
    }
}

/** Sunny hard hat with a raised ridge, a wide brim and a pearl star sticker. */
private fun hardHat(): ItemArt {
    val brim = itemShape("M58 64 Q114 50 176 62 Q190 68 176 74 Q114 64 58 74 Q48 70 58 64 Z", ArtTones.Gold)
    val dome = itemShape("M68 64 Q64 16 116 14 Q166 16 164 62 Q116 52 68 64 Z", ArtTones.Sun)
    val ridge = itemShape("M108 16 Q116 13 124 16 L124 58 Q116 56 108 58 Z", ArtTones.Gold)
    val star = VinylShape(starPath(144f, 40f, 8f), ArtTones.Pearl)
    return ItemArt(ArtPart.HEAD, ItemLayer.FRONT) {
        vinylShape(brim)
        vinylShape(dome)
        specular(dome.bounds, -12f)
        vinylShape(ridge)
        vinylShape(star)
    }
}

/**
 * A diamond kite flying behind the head on the back side, with a ribbon tail of bows, on a string held
 * by the back hand.
 */
private fun kite(): ItemArt {
    val top = Offset(38f, 12f)
    val center = Offset(38f, 44f)
    val quarters =
        listOf(
            itemShape("M${top.x} ${top.y} L62 44 L${center.x} ${center.y} Z", ArtTones.Teal),
            itemShape("M62 44 L38 90 L${center.x} ${center.y} Z", ArtTones.Sun),
            itemShape("M38 90 L14 44 L${center.x} ${center.y} Z", ArtTones.Teal),
            itemShape("M14 44 L${top.x} ${top.y} L${center.x} ${center.y} Z", ArtTones.Sun),
        )
    val spars = svgPath("M38 12 L38 90 M14 44 L62 44")
    val tail = svgPath("M38 90 Q26 104 36 118 Q46 132 32 146")
    val bows =
        VinylShape(
            Path().apply {
                for ((x, y) in listOf(31f to 104f, 41f to 126f, 33f to 144f)) {
                    addPath(svgPath("M$x $y L${x - 8f} ${y - 5f} L${x - 8f} ${y + 5f} Z"))
                    addPath(svgPath("M$x $y L${x + 8f} ${y - 5f} L${x + 8f} ${y + 5f} Z"))
                }
            },
            ArtTones.Pink,
        )
    val string = svgPath("M${center.x} ${center.y} Q70 150 118 206")
    val sparStroke = itemStroke(2f)
    val tailStroke = itemStroke(2.6f)
    val stringStroke = itemStroke(STRING_WIDTH)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        drawPath(string, ArtTones.EyeInk.base, alpha = STRING_ALPHA, style = stringStroke)
        drawPath(tail, ArtTones.EyeInk.base, alpha = STRING_ALPHA, style = tailStroke)
        vinylShape(bows)
        for (i in quarters.indices) vinylShape(quarters[i])
        drawPath(spars, ArtTones.Teal.shade, alpha = 0.7f, style = sparStroke)
        specular(quarters[3].bounds)
    }
}

/** Dark binoculars with sky lenses hanging on the chest from a denim strap. */
private fun binoculars(): ItemArt {
    val strap = svgPath("M88 148 Q96 172 108 184 M158 150 Q152 170 144 184")
    val strapBrush = vinylBrush(strap.getBounds().inflate(3f), ArtTones.Denim)
    val bridge = VinylShape(roundRectPath(112f, 180f, 140f, 192f, 4f), ArtTones.Metal)
    val barrels = VinylShape(itemPath(ovalPath(112f, 196f, 13f), ovalPath(140f, 196f, 13f)), ArtTones.Metal)
    val glass = VinylShape(itemPath(ovalPath(112f, 197f, 8.5f), ovalPath(140f, 197f, 8.5f)), ArtTones.Sky)
    val leftGlass = ovalRect(112f, 197f, 8.5f)
    val rightGlass = ovalRect(140f, 197f, 8.5f)
    val knob = itemCircle(126f, 181f, 4.5f, ArtTones.Steel)
    val strapStroke = itemStroke(4f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(strap, strapBrush, style = strapStroke)
        vinylShape(bridge)
        vinylShape(barrels)
        vinylShape(glass)
        specular(leftGlass)
        specular(rightGlass)
        vinylShape(knob)
    }
}

/** Gold medal stamped with a dinosaur footprint, on a sky ribbon with a pearl stripe. */
private fun medal(): ItemArt {
    val ribbon = svgPath("M90 148 L120 194 M158 150 L132 194")
    val ribbonBrush = vinylBrush(ribbon.getBounds().inflate(5f), ArtTones.Sky)
    val disc = itemCircle(126f, 206f, 15f, ArtTones.Gold)
    val footprint =
        itemPath(
            ovalPath(126f, 210f, 6f, 5f),
            ovalPath(119f, 201f, 2.6f, 3.2f),
            ovalPath(126f, 199f, 2.6f, 3.2f),
            ovalPath(133f, 201f, 2.6f, 3.2f),
        )
    val ring = ovalPath(126f, 206f, 11.5f)
    val ribbonStroke = itemStroke(10f)
    val stripeStroke = itemStroke(3f)
    val ringStroke = itemStroke(1.6f)
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT) {
        drawPath(ribbon, ribbonBrush, style = ribbonStroke)
        drawPath(ribbon, ArtTones.Pearl.base, style = stripeStroke)
        vinylShape(disc)
        drawPath(ring, ArtTones.Gold.shade, alpha = 0.7f, style = ringStroke)
        drawPath(footprint, ArtTones.Gold.shade)
        specular(disc.bounds)
    }
}

/** A teal skateboard with sunny wheels and stars under the feet. */
private fun skateboard(): ItemArt {
    val deck =
        itemShape(
            "M26 272 Q32 281 46 281 L194 281 Q208 281 214 272 Q218 284 208 291 L32 291 Q22 284 26 272 Z",
            ArtTones.Teal,
        )
    val trucks =
        VinylShape(
            itemPath(roundRectPath(50f, 289f, 82f, 294f, 2f), roundRectPath(158f, 289f, 190f, 294f, 2f)),
            ArtTones.Steel,
        )
    val wheels =
        VinylShape(
            Path().apply { for (x in floatArrayOf(56f, 76f, 164f, 184f)) addPath(ovalPath(x, 294f, 5.5f)) },
            ArtTones.Sun,
        )
    val stars = itemPath(starPath(42f, 285f, 5f), starPath(198f, 285f, 5f))
    return ItemArt(ArtPart.BODY, ItemLayer.BEHIND) {
        vinylShape(trucks)
        vinylShape(wheels)
        vinylShape(deck)
        specular(deck.bounds)
        drawPath(stars, ArtTones.Sun.base)
    }
}

/**
 * A big two-scoop ice cream, traced from its reference and placed as on the sheet: the far hand reaches
 * out and closes around the lower half of the waffle cone, and the cone leans so the scoops stand beside
 * the jaw. The rig draws the fist over the cone and the scoops over the snout.
 */
private fun iceCream(): ItemArt {
    val cone = itemShape(IceCreamTrace.CONE, ArtTones.Gold)
    // The waffle: two sets of diagonals across the cone's bounds, clipped to it.
    val waffle =
        Path().apply {
            val b = cone.bounds
            var d = -b.height
            while (d < b.width) {
                moveTo(b.left + d, b.top)
                lineTo(b.left + d + b.height, b.bottom)
                moveTo(b.right - d, b.top)
                lineTo(b.right - d - b.height, b.bottom)
                d += WAFFLE_STEP
            }
        }
    val lower = itemShape(IceCreamTrace.BLUE, ArtTones.Sky)
    val upper = itemShape(IceCreamTrace.PINK, ArtTones.Pink)
    val waffleStroke = itemStroke(1.3f)
    val shine = specularRect(180f, 136f, 9f, 5f)
    return ItemArt(ArtPart.HAND, ItemLayer.FRONT) {
        rotate(ICE_CREAM_LEAN_DEG, IceCreamPivot) {
            vinylShape(cone)
            clipPath(cone.path) { drawPath(waffle, ArtTones.Gold.shade, alpha = 0.7f, style = waffleStroke) }
            vinylShape(lower)
            vinylShape(upper)
            specular(shine)
        }
    }
}

private const val WAFFLE_STEP = 8f
private const val ICE_CREAM_LEAN_DEG = 15f
private val IceCreamPivot = Offset(190f, 170f)

/** The area where the rig redraws the scoops over the snout. */
internal val IceCreamScoops = Rect(150f, 100f, 240f, 181f)

/**
 * A sunny duck swim ring around the waist, traced from its reference and placed as on the sheet. The
 * whole ring is drawn behind the body; its front band, below the hole, and the duck's head are drawn
 * again in front of the body and the arms' ends, so the dinosaur stands in the ring with its hands on it.
 */
private fun swimRing(): ItemArt {
    val ring = itemShape(SwimRingTrace.RING, ArtTones.Sun)
    val beak = itemShape(SwimRingTrace.BEAK, ArtTones.Knit)
    val eye = ovalRect(169.8f, 187f, 3.2f, 4.4f)
    val eyeGlint = ovalRect(168.8f, 185.2f, 1f)
    val shine = specularRect(98f, 220f, 24f, 5f)
    // Below the hole's front edge, and everything on the right where the duck's head rises.
    val front = svgPath("M40 201 L92 201 Q128 215 163 201 L150 160 L240 160 L240 270 L40 270 Z")
    return ItemArt(ArtPart.BODY, ItemLayer.FRONT, back = { vinylShape(ring) }) {
        clipPath(front) { vinylShape(ring) }
        specular(shine)
        vinylShape(beak)
        drawOval(PersonalizationColors.EyeInk, eye.topLeft, eye.size)
        drawOval(Color.White, eyeGlint.topLeft, eyeGlint.size)
    }
}

/** The dinosaur's item artwork, keyed by item id. Declared last so the shared tones above are ready. */
internal val DinosaurItemArt: Map<String, ItemArt> =
    mapOf(
        "dinosaur-extra-backpack" to backpack(),
        "dinosaur-head-flower" to flower(),
        "dinosaur-extra-bucket" to bucket(),
        "dinosaur-neck-scarf" to scarf(),
        "dinosaur-head-cap" to cap(),
        "dinosaur-extra-balloon" to balloon(),
        "dinosaur-neck-bow-tie" to bowTie(),
        "dinosaur-head-party-hat" to partyHat(),
        "dinosaur-extra-umbrella" to umbrella(),
        "dinosaur-extra-swim-ring" to swimRing(),
        "dinosaur-head-hard-hat" to hardHat(),
        "dinosaur-extra-kite" to kite(),
        "dinosaur-neck-binoculars" to binoculars(),
        "dinosaur-neck-medal" to medal(),
        "dinosaur-extra-ice-cream" to iceCream(),
        "dinosaur-extra-skateboard" to skateboard(),
    )
