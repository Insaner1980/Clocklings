// Literal SVG coordinates preserve the original artwork and its drawing order.
@file:Suppress("MagicNumber", "TooManyFunctions")

package fi.clocklings.ui.doll

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.glossyEye
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.unitBrush
import fi.clocklings.ui.art.unitCircle
import fi.clocklings.ui.art.unitOval
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.companion.CompanionMotion
import fi.clocklings.ui.companion.CompanionPose
import fi.clocklings.ui.companion.CompanionRig
import fi.clocklings.ui.components.starPath
import fi.clocklings.ui.theme.SkColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import fi.clocklings.ui.theme.SkDollColors as C

/**
 * The dress-up doll in layers, like the web version's dollSVG: wings, back hair, legs, shoes, neck,
 * dress, arms and sleeves, head and face, bangs, headwear and bag. Design grid 200 x 340; wardrobe
 * thumbnails use the [DollParts.crop] areas. The doll has fixed colors and draws its own items, so it
 * ignores the color and never calls items().
 *
 * It is shaded as a vinyl toy with the shared toolkit, in the proportions of the reference: a large head
 * with big glossy eyes, lashes, brows and a small nose, puff sleeves, short legs and domed shoes. Every
 * fill is a vinyl brush built once, the hair and the dress have a highlight, and there is a contact
 * shadow under the head and a ground shadow under the feet.
 */
internal object DollRig : CompanionRig {
    override val width = DollParts.WIDTH
    override val height = DollParts.HEIGHT

    override fun crop(slot: Slot): Rect = DollParts.crop(slot)

    override fun crop(item: Item): Rect = DollParts.crop(item)

    override fun full(): Rect = DollParts.Full

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        groundShadow(Ground)
        posed(pose) { drawDoll(outfit, pose) }
    }
}

/**
 * The hop up, the sway around the feet, the spin around the vertical axis and the breathing squash
 * around the feet, in the same order as the animal companions. The doll does not tilt its head: its
 * back hair is drawn behind the body, apart from the face, bangs and headwear, so a tilt would pull
 * the two hair layers apart.
 */
private inline fun DrawScope.posed(
    p: CompanionPose,
    crossinline block: DrawScope.() -> Unit,
) {
    translate(0f, -p.lift) {
        rotate(p.swayDeg, Feet) {
            scale(p.turn, 1f, pivot = Middle) {
                scale(1f, 1f + p.breathe * CompanionMotion.BREATHE_SQUASH, pivot = Feet) { block() }
            }
        }
    }
}

/** A shape's fill: a brush built once, or a repeating pattern on a vinyl background. */
private sealed interface Fill {
    class Paint(
        val brush: Brush,
    ) : Fill

    /**
     * Stars or dots repeating over [bounds] in user space (web: patternUnits="userSpaceOnUse") on a
     * vinyl [background]. Every mark is built once: [starMarks] holds a yellow and a white star per
     * tile, [dotCenters] the centers of two white dots per tile. Each mark is still painted on its own,
     * as before, because one combined path is anti-aliased differently on the GPU.
     */
    class Pattern(
        stars: Boolean,
        val background: Brush,
        val bounds: Rect,
    ) : Fill {
        val starMarks = mutableListOf<Path>()
        val dotCenters = mutableListOf<Offset>()

        init {
            val tile = if (stars) 22f else 16f
            var y = floor(bounds.top / tile) * tile
            while (y < bounds.bottom) {
                var x = floor(bounds.left / tile) * tile
                while (x < bounds.right) {
                    if (stars) {
                        starMarks += starPath(x + 6f, y + 6f, 4.5f)
                        starMarks += starPath(x + 17f, y + 16f, 3.2f)
                    } else {
                        dotCenters += Offset(x + 4f, y + 4f)
                        dotCenters += Offset(x + 12f, y + 12f)
                    }
                    x += tile
                }
                y += tile
            }
        }
    }
}

private fun DrawScope.fillPath(
    path: Path,
    fill: Fill,
) {
    when (fill) {
        is Fill.Paint -> {
            drawPath(path, fill.brush)
        }

        is Fill.Pattern -> {
            clipPath(path) {
                drawRect(fill.background, fill.bounds.topLeft, fill.bounds.size)
                for (i in fill.starMarks.indices) drawPath(fill.starMarks[i], if (i % 2 == 0) C.StarYellow else C.White)
                for (i in fill.dotCenters.indices) drawCircle(C.White, DOT_R, fill.dotCenters[i])
            }
        }
    }
}

/** A [unitOval] with an [outline], as the web version's stroked ellipses. */
@Suppress("LongParameterList")
private fun DrawScope.outlinedOval(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    brush: Brush,
    outline: Color,
    stroke: Stroke = OutlineStroke,
) {
    unitOval(cx, cy, rx, ry, brush)
    drawOval(outline, Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry), style = stroke)
}

// SVG line endpoints and stroke attributes map directly to the drawing call.
@Suppress("LongParameterList")
private fun DrawScope.line(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    brush: Brush,
    width: Float,
) {
    drawLine(brush, Offset(x1, y1), Offset(x2, y2), width, StrokeCap.Round)
}

/** The brushes of one hair color: the long back hair and the bangs. */
private class HairBrushes(
    val back: Brush,
    val bangs: Brush,
)

private fun vinylHair(color: Color): HairBrushes {
    val tones = vinylTones(color)
    return HairBrushes(vinylBrush(LongHairBounds, tones), vinylBrush(BangsBounds, tones))
}

private fun rainbowBrush(bounds: Rect): Brush =
    Brush.verticalGradient(SkColors.Rainbow7, startY = bounds.top, endY = bounds.bottom)

private fun hairBrushes(id: String?): HairBrushes =
    when (id) {
        "hair-pitkat" -> GoldHair
        CURLY_HAIR -> PinkHair
        "hair-sateenkaari" -> RainbowHair
        else -> BrownHair
    }

/** One sleeve: the shoulder circle and its fill, built once. */
private class Sleeve(
    cx: Float,
    radius: Float,
    fill: (Rect) -> Fill,
) {
    val path = ovalPath(cx, SLEEVE_Y, radius)
    val fill = fill(ovalRect(cx, SLEEVE_Y, radius))
}

private class Sleeves(
    radius: Float,
    fill: (Rect) -> Fill,
) {
    val left = Sleeve(64.5f, radius, fill)
    val right = Sleeve(135f, radius, fill)
}

private fun vinylSleeves(
    color: Color,
    radius: Float = SLEEVE_R,
) = Sleeves(radius) { Fill.Paint(vinylBrush(it, vinylTones(color))) }

private fun sleevesFor(dress: String?): Sleeves? =
    when (dress) {
        "dress-sateenkaari" -> RainbowSleeves
        else -> null
    }

private fun dressBrush(color: Color): Brush = vinylBrush(DressBounds, vinylTones(color))

@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun DrawScope.drawDoll(
    o: Outfit,
    pose: CompanionPose,
) {
    // Siivet selän taakse
    if (o[Slot.EXTRA] == "extra-siivet") {
        // Indexed loops here and below: the doll is drawn on every frame, so it must not allocate.
        for (i in Wings.indices) {
            val w = Wings[i]
            rotate(w.deg, Offset(w.cx, w.cy)) { outlinedOval(w.cx, w.cy, w.rx, w.ry, WingDot, C.Violet, WingStroke) }
        }
        unitCircle(42f, 150f, 7f, WingSpotDot)
        unitCircle(158f, 150f, 7f, WingSpotDot)
    }

    // Back hair: the cap behind the face, then the style's own back pieces.
    val hair = hairBrushes(o[Slot.HAIR])
    drawOval(hair.bangs, HairCap.topLeft, HairCap.size)
    when (o[Slot.HAIR]) {
        "hair-letit" -> {
            vinylShape(PigtailLeft)
            vinylShape(PigtailRight)
            specular(PigtailLeft.bounds)
            specular(PigtailRight.bounds)
            rotate(-TIE_DEG, TieLeft) { unitOval(TieLeft.x, TieLeft.y, TIE_RX, TIE_RY, BowDot) }
            rotate(TIE_DEG, TieRight) { unitOval(TieRight.x, TieRight.y, TIE_RX, TIE_RY, BowDot) }
        }

        "hair-pitkat", "hair-sateenkaari" -> {
            drawPath(DollParts.LongHair, hair.back)
        }

        CURLY_HAIR -> {
            for (i in CurlCircles.indices) {
                val c = CurlCircles[i]
                unitOval(c.cx, c.cy, c.rx, c.ry, PinkHairDot)
            }
        }
    }

    // Jalat ja kengät
    line(80f, 252f, 80f, 290f, LegLeft, LEG_WIDTH)
    line(117.5f, 252f, 117.5f, 290f, LegRight, LEG_WIDTH)
    contactShadow(HemShadow)
    contactShadow(ShoeShadowLeft)
    contactShadow(ShoeShadowRight)
    drawShoes(o[Slot.SHOES])

    // Arms and puff sleeves, under the dress so the sleeves grow out of its shoulders. The left arm
    // waves around its shoulder (the bag hangs from the right hand).
    rotate(pose.armDeg, ShoulderLeft) {
        drawPath(DollParts.ArmLeft, ArmLeft)
    }
    drawPath(DollParts.ArmRight, ArmRight)

    // Neck and dress
    vinylShape(Neck)
    when (o[Slot.DRESS]) {
        "dress-pilkku" -> {
            fillPath(DollParts.Dress, DotsDress)
            vinylShape(Collar)
        }

        "dress-tahti" -> {
            fillPath(DollParts.Dress, StarsDress)
            drawPath(DollParts.StarBelt, C.StarYellow, style = BeltStroke)
        }

        OVERALLS -> {
            vinylShape(Overalls)
            vinylShape(ShirtSleeveLeft)
            vinylShape(ShirtNeck)
            vinylShape(ShirtSleeveRight)
            drawPath(OverallsSeams, Overalls.brush, style = SeamStroke)
            drawPath(OverallsSeams, SeamShade, style = SeamStroke)
            vinylShape(OverallsButtons)
        }

        "dress-sateenkaari" -> {
            clipPath(DollParts.Dress) {
                for (i in RainbowStripes.indices) {
                    drawRect(RainbowStripes[i], Offset(30f, 136f + i * 18f), Size(140f, 19f))
                }
            }
        }

        else -> {
            drawPath(DollParts.Dress, YellowDress)
            drawPath(DollParts.Hem, C.DressHem, style = HemStroke)
            vinylShape(Collar)
        }
    }
    // The traced dress has its puff sleeves; a dress with sleeves of another color paints them over it.
    val sleeves = sleevesFor(o[Slot.DRESS])
    if (sleeves != null) {
        clipPath(DollParts.Dress) {
            fillPath(sleeves.left.path, sleeves.left.fill)
            fillPath(sleeves.right.path, sleeves.right.fill)
        }
    }
    specular(DressShine, DRESS_SHINE_DEG)

    // Head and face
    contactShadow(HeadShadow)
    vinylOval(Head, HeadBrush)
    glossyEye(EyeLeft, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
    glossyEye(EyeRight, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
    drawPath(DollParts.Lashes, C.Lash, style = LashStroke)
    drawPath(DollParts.Brows, C.HairBrown, alpha = BROW_ALPHA, style = BrowStroke)
    drawOval(ArtTones.CheekColor, CheekLeft.topLeft, CheekLeft.size)
    drawOval(ArtTones.CheekColor, CheekRight.topLeft, CheekRight.size)
    drawOval(SkinTones.shade, Nose.topLeft, Nose.size, alpha = NOSE_ALPHA)
    drawPath(DollParts.Mouth, C.Mouth, style = MouthStroke)

    // The hair on the head
    drawPath(DollParts.Bangs, hair.bangs)
    if (o[Slot.HAIR] == CURLY_HAIR) {
        unitCircle(50f, 84f, 11f, PinkHairDot)
        unitCircle(150f, 84f, 11f, PinkHairDot)
    }
    specular(HairShine, HAIR_SHINE_DEG)

    // Päähän
    drawHeadwear(o[Slot.HEAD])

    // Laukku käteen
    if (o[Slot.EXTRA] == "extra-strawberry-bag") {
        drawPath(BerryStrap, C.Bow, style = BerryStrapStroke)
        vinylShape(Berry)
        specular(Berry.bounds)
        drawPath(BerrySeeds, C.Seed)
        vinylShape(BerryLeaves)
    }
}

/** One pair of domed shoes in one pigment, with the color of its sole line. */
private class Shoes(
    color: Color,
    val sole: Color,
) {
    private val tones = vinylTones(color)
    val left = VinylShape(DollParts.ShoeLeft, tones)
    val right = VinylShape(DollParts.ShoeRight, tones)
}

/** One roller skate at the left foot: the side wheels, the boot on its cream band, the strap and the front wheel. */
private fun DrawScope.skate() {
    vinylShape(SkateWheelLeft)
    vinylShape(SkateWheelRight)
    vinylShape(SkateBoot)
    specular(SkateBoot.bounds)
    vinylShape(SkateBand)
    vinylShape(SkateStrap)
    vinylShape(SkateWheelFront)
}

private fun DrawScope.drawShoes(id: String?) {
    if (id == SKATES) {
        skate()
        translate(SKATE_GAP, 0f) { skate() }
        return
    }
    val shoes =
        when (id) {
            "shoes-punaiset" -> RedShoes
            BOOTS -> VioletShoes
            else -> WhiteShoes
        }
    if (id == BOOTS) {
        vinylShape(BootLeft)
        vinylShape(BootRight)
    }
    vinylShape(shoes.left)
    vinylShape(shoes.right)
    specular(shoes.left.bounds)
    specular(shoes.right.bounds)
    drawPath(DollParts.Soles, shoes.sole, alpha = SOLE_ALPHA, style = SoleStroke)
    when (id) {
        "shoes-punaiset" -> {
            drawPath(DollParts.ShoeStraps, C.RedDeep, style = StrapStroke)
        }

        BOOTS -> {
            vinylShape(BootCuffLeft)
            vinylShape(BootCuffRight)
        }
    }
}

private fun DrawScope.drawHeadwear(id: String?) {
    when (id) {
        "head-rusetti" -> {
            vinylShape(BowLeft)
            vinylShape(BowRight)
            unitCircle(134f, 38f, 6.5f, RedDeepDot)
        }

        "head-kukat" -> {
            for (i in FlowerAngles.indices) {
                val a = FlowerAngles[i] * PI.toFloat() / 180f
                val x = CENTER_X + FLOWER_ARC * sin(a)
                val y = 82f - FLOWER_ARC * cos(a)
                for (k in 0 until 5) {
                    val b = k * PI.toFloat() * 2f / 5f
                    val c = Offset(x + 6f * cos(b), y + 6f * sin(b))
                    unitCircle(c.x, c.y, 5.2f, PetalDots[i])
                    drawCircle(C.Bow, 5.2f, c, style = PetalStroke)
                }
                unitCircle(x, y, 3.6f, FlowerCenterDot)
            }
        }

        "head-kruunu" -> {
            vinylShape(Crown)
            drawPath(DollParts.Crown, C.GoldDeep, style = CrownStroke)
            unitCircle(100f, 30f, 4.5f, BowRedDot)
            unitCircle(86f, 33f, 3.2f, JewelDot)
            unitCircle(114f, 33f, 3.2f, JewelDot)
        }
    }
}

private const val EYE_RX = 8.5f
private const val EYE_RY = 10.3f
private const val SLEEVE_Y = 156f
private const val SLEEVE_R = 14f
private const val OVERALLS = "dress-overalls"
private const val SKATES = "shoes-roller-skates"
private const val SKATE_GAP = 44.2f
private const val SEAM_ALPHA = 0.5f
private const val LEG_WIDTH = 27f
private const val TIE_RX = 6.7f
private const val TIE_RY = 12.5f
private const val TIE_DEG = 12f
private const val FLOWER_ARC = 54f
private const val BROW_ALPHA = 0.75f
private const val NOSE_ALPHA = 0.5f
private const val SOLE_ALPHA = 0.7f
private const val SKIN_SHADE_MIX = 0.6f
private const val SKIN_LIGHT_MIX = 0.14f
private const val HAIR_SHINE_DEG = -20f
private const val DRESS_SHINE_DEG = 14f
private const val CENTER_X = 100f
private const val CURLY_HAIR = "hair-kiharat"
private const val BOOTS = "shoes-saappaat"

private val Feet = Offset(CENTER_X, 312f)
private val Middle = Offset(CENTER_X, 170f)
private val ShoulderLeft = Offset(62f, 166f)

private val OutlineStroke = Stroke(2.5f)
private val WingStroke = Stroke(3f)
private val PetalStroke = Stroke(1f)
private val CrownStroke = Stroke(2.5f, join = StrokeJoin.Round)
private val BeltStroke = Stroke(7f, join = StrokeJoin.Round)
private val HemStroke = Stroke(3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val MouthStroke = Stroke(2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val BrowStroke = Stroke(2.2f, cap = StrokeCap.Round)
private val LashStroke = Stroke(1.8f, cap = StrokeCap.Round)
private val SoleStroke = Stroke(2f, cap = StrokeCap.Round)
private val StrapStroke = Stroke(3f, cap = StrokeCap.Round)
private val BerryStrapStroke = Stroke(4f, cap = StrokeCap.Round)
private val SeamStroke = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** A unit-box vinyl brush for [unitOval], one per pigment. */
private fun dot(color: Color): Brush = unitBrush(vinylTones(color))

private const val DOT_R = 2.6f

private val Ground = ovalRect(CENTER_X, 311f, 52f, 8f)
private val HeadShadow = ovalRect(CENTER_X, 136f, 20f, 5f)

// Skin shades more gently than vinyl clothes, so the face stays clean on its shaded side.
private val SkinTones =
    vinylTones(C.Skin).let {
        it.copy(light = lerp(it.base, Color.White, SKIN_LIGHT_MIX), shade = lerp(it.base, it.shade, SKIN_SHADE_MIX))
    }
private val Head = ovalRect(CENTER_X, 89f, 47.3f, 47f)
private val HeadBrush = vinylBrush(Head, SkinTones)
private val Neck = VinylShape(roundRectPath(91f, 128f, 109f, 150f, 6f), SkinTones)
private val LegLeft = vinylBrush(Rect(66f, 240f, 94f, 304f), SkinTones)
private val LegRight = vinylBrush(Rect(103.5f, 240f, 131.5f, 304f), SkinTones)
private val ArmLeft = vinylBrush(DollParts.ArmLeft.getBounds(), SkinTones)
private val ArmRight = vinylBrush(DollParts.ArmRight.getBounds(), SkinTones)

private val EyeLeft = Offset(77f, 92.4f)
private val EyeRight = Offset(123.1f, 92.4f)
private val CheekLeft = ovalRect(66f, 106.3f, 9f, 6f)
private val CheekRight = ovalRect(133.4f, 106.3f, 9f, 6f)
private val Nose = ovalRect(99.5f, 103.5f, 3f, 2.2f)

private val LongHairBounds = DollParts.LongHair.getBounds()
private val BangsBounds = DollParts.Bangs.getBounds()
private val BrownHair = vinylHair(C.HairBrown)
private val GoldHair = vinylHair(C.HairGold)
private val PinkHair = vinylHair(C.HairPink)
private val RainbowHair = HairBrushes(rainbowBrush(LongHairBounds), rainbowBrush(BangsBounds))
private val PinkHairDot = dot(C.HairPink)
private val BowDot = dot(C.Bow)
private val HairShine = specularRect(80f, 42f, 16f, 5f)

// The hair behind the face shows beside the cheeks down to the pigtail ties.
private val HairCap = ovalRect(CENTER_X, 74f, 56f, 50f)
private val BrownHairTones = vinylTones(C.HairBrown)
private val PigtailLeft = VinylShape(DollParts.PigtailLeft, BrownHairTones)
private val PigtailRight = VinylShape(DollParts.PigtailRight, BrownHairTones)
private val TieLeft = Offset(44.5f, 97.2f)
private val TieRight = Offset(155.6f, 97.2f)
private val Collar = VinylShape(DollParts.Collar, vinylTones(C.White))

/** An SVG ellipse: center, radii and rotation around the center. */
private class Ellipse(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float = rx,
    val deg: Float = 0f,
)

private val CurlCircles =
    listOf(
        Ellipse(54f, 56f, 26f),
        Ellipse(42f, 96f, 24f),
        Ellipse(44f, 134f, 22f),
        Ellipse(54f, 168f, 18f),
        Ellipse(146f, 56f, 26f),
        Ellipse(158f, 96f, 24f),
        Ellipse(156f, 134f, 22f),
        Ellipse(146f, 168f, 18f),
        Ellipse(100f, 36f, 34f),
    )

private val DressBounds = DollParts.Dress.getBounds()
private val YellowDress = dressBrush(C.DressYellow)
private val DotsDress = Fill.Pattern(stars = false, background = dressBrush(C.DotsBg), bounds = DressBounds)
private val StarsDress = Fill.Pattern(stars = true, background = dressBrush(C.StarsBg), bounds = DressBounds)
private val RainbowStripes = SkColors.Rainbow7.map(::dressBrush)
private val DressShine = specularRect(82f, 192f, 10f, 22f)

private val RainbowSleeves = vinylSleeves(C.RainbowSleeve)

private val WhiteShoes = Shoes(C.White, C.ShoeOutline)
private val RedShoes = Shoes(C.Red, C.RedDeep)
private val VioletShoes = Shoes(C.Violet, C.VioletDeep)
private val WhiteDot = dot(C.White)
private val VioletTones = vinylTones(C.Violet)
private val BootLeft = VinylShape(roundRectPath(65.3f, 262f, 95f, 300f, 7f), VioletTones)
private val BootRight = VinylShape(roundRectPath(103.4f, 262f, 130.6f, 300f, 7f), VioletTones)
private val CuffTones = vinylTones(C.PrincessLight)
private val BootCuffLeft = VinylShape(roundRectPath(63.3f, 259f, 97f, 268f, 4.5f), CuffTones)
private val BootCuffRight = VinylShape(roundRectPath(101.4f, 259f, 132.6f, 268f, 4.5f), CuffTones)

private val Wings =
    listOf(
        Ellipse(48f, 160f, 32f, 46f, -30f),
        Ellipse(58f, 214f, 20f, 28f, 28f),
        Ellipse(152f, 160f, 32f, 46f, 30f),
        Ellipse(142f, 214f, 20f, 28f, -28f),
    )
private val WingDot = dot(C.Wing)
private val WingSpotDot = dot(C.WingSpot)

private val BowRedTones = vinylTones(C.BowRed)
private val BowLeft = VinylShape(DollParts.BowLeft, BowRedTones)
private val BowRight = VinylShape(DollParts.BowRight, BowRedTones)
private val RedDeepDot = dot(C.RedDeep)

private val FlowerAngles = floatArrayOf(-70f, -35f, 0f, 35f, 70f)
private val PetalDots = listOf(dot(C.FlowerPink), WhiteDot, dot(C.FlowerViolet), WhiteDot, dot(C.FlowerPink))
private val FlowerCenterDot = dot(C.FlowerCenter)

private val Crown = VinylShape(DollParts.Crown, vinylTones(C.Gold))
private val BowRedDot = dot(C.BowRed)
private val JewelDot = dot(C.Jewel)

// The overalls over a cream shirt, traced from their reference: the bib and both legs are one outline.
private val Overalls = VinylShape(DollParts.Overalls, vinylTones(C.Overalls))
private val ShirtTones = vinylTones(C.Shirt)
private val ShirtSleeveLeft = VinylShape(DollParts.ShirtSleeveLeft, ShirtTones)
private val ShirtNeck = VinylShape(DollParts.ShirtNeck, ShirtTones)
private val ShirtSleeveRight = VinylShape(DollParts.ShirtSleeveRight, ShirtTones)
private val OverallsButtons = VinylShape(DollParts.OverallsButtons, vinylTones(C.Gold))
private val OverallsSeams = DollParts.OverallsSeams
private val SeamShade = vinylTones(C.Overalls).shade.copy(alpha = SEAM_ALPHA)

// One roller skate, traced from its reference at the left foot; the right foot's is the same, moved over.
private val SkateBoot = VinylShape(DollParts.SkateBoot, vinylTones(C.Overalls))
private val SkateBand = VinylShape(DollParts.SkateBand, ShirtTones)
private val SkateStrap = VinylShape(DollParts.SkateStrap, vinylTones(C.Coral))
private val WheelTones = vinylTones(C.Gold)
private val SkateWheelFront = VinylShape(DollParts.SkateWheelFront, WheelTones)
private val SkateWheelLeft = VinylShape(DollParts.SkateWheelLeft, WheelTones)
private val SkateWheelRight = VinylShape(DollParts.SkateWheelRight, WheelTones)

// The strawberry bag at the left hip, traced from its reference, on a strap from the right shoulder.
private val Berry = VinylShape(DollParts.Berry, vinylTones(C.Red))
private val BerryLeaves = VinylShape(DollParts.BerryLeaves, vinylTones(C.Leaf))
private val BerrySeeds = DollParts.BerrySeeds
private val BerryStrap = DollParts.BerryStrap

// Contact shadows on the legs: under the hem and just above the shoes.
private val HemShadow = ovalRect(100f, 260f, 30f, 9f)
private val ShoeShadowLeft = ovalRect(80f, 286f, 13f, 4f)
private val ShoeShadowRight = ovalRect(117.5f, 286f, 13f, 4f)
