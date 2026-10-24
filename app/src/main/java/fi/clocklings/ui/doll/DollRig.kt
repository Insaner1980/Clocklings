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
import fi.clocklings.domain.CompanionColor
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
 * It is shaded as a vinyl toy with the shared toolkit, keeping the original geometry and drawing order:
 * every fill is a vinyl brush built once, the eyes are glossy, the hair and the dress have a highlight,
 * and there is a contact shadow under the head and a ground shadow under the feet.
 */
internal object DollRig : CompanionRig {
    override val width = DollParts.WIDTH
    override val height = DollParts.HEIGHT

    override fun crop(slot: Slot): Rect = DollParts.crop(slot)

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

/** A vinyl shape with an outline, as the web version's stroked shapes. */
private fun DrawScope.outlined(
    shape: VinylShape,
    outline: Color,
) {
    vinylShape(shape)
    drawPath(shape.path, outline, style = OutlineStroke)
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

@Suppress("LongParameterList")
private fun DrawScope.line(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    width: Float,
) {
    drawLine(color, Offset(x1, y1), Offset(x2, y2), width, StrokeCap.Round)
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
    val left = Sleeve(74f, radius, fill)
    val right = Sleeve(126f, radius, fill)
}

private fun vinylSleeves(
    color: Color,
    radius: Float = SLEEVE_R,
) = Sleeves(radius) { Fill.Paint(vinylBrush(it, vinylTones(color))) }

private fun patternSleeves(
    stars: Boolean,
    background: Color,
) = Sleeves(SLEEVE_R) { Fill.Pattern(stars, vinylBrush(it, vinylTones(background)), it) }

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
        unitCircle(50f, 140f, 7f, WingSpotDot)
        unitCircle(150f, 140f, 7f, WingSpotDot)
    }

    // Hiukset taakse
    val hair = hairBrushes(o[Slot.HAIR])
    when (o[Slot.HAIR]) {
        "hair-letit" -> {
            unitOval(50f, 98f, 16f, 25f, BrownHairDot)
            unitOval(150f, 98f, 16f, 25f, BrownHairDot)
            unitCircle(58f, 76f, 7f, BowDot)
            unitCircle(142f, 76f, 7f, BowDot)
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
    line(86f, 226f, 86f, 298f, LegLeft, 16f)
    line(114f, 226f, 114f, 298f, LegRight, 16f)
    drawShoes(o[Slot.SHOES])

    // Kaula ja mekko
    vinylShape(Neck)
    var sleeves = YellowSleeves
    when (o[Slot.DRESS]) {
        "dress-pilkku" -> {
            sleeves = DotSleeves
            fillPath(DollParts.Dress, DotsDress)
            drawPath(DollParts.Collar, C.White, style = CollarStroke)
        }

        "dress-tahti" -> {
            sleeves = StarSleeves
            fillPath(DollParts.Dress, StarsDress)
            drawPath(DollParts.StarBelt, C.StarYellow, style = BeltStroke)
        }

        "dress-prinsessa" -> {
            sleeves = PrincessSleeves
            drawPath(DollParts.Dress, PrincessDress)
            vinylShape(PrincessSkirt)
            vinylShape(PrincessBelt)
            for (i in PrincessStars.indices) drawPath(PrincessStars[i], C.White)
        }

        "dress-sateenkaari" -> {
            sleeves = RainbowSleeves
            clipPath(DollParts.Dress) {
                for (i in RainbowStripes.indices) {
                    drawRect(RainbowStripes[i], Offset(30f, 118f + i * 19.6f), Size(140f, 20.6f))
                }
            }
        }

        else -> {
            drawPath(DollParts.Dress, YellowDress)
            drawPath(DollParts.Collar, C.White, style = CollarStroke)
            drawPath(DollParts.Hem, C.DressHem, style = HemStroke)
        }
    }
    specular(DressShine, DRESS_SHINE_DEG)

    // Kädet ja hihat. Vasen käsi nousee vilkuttamaan olkapään ympäri (laukku on oikeassa kädessä).
    rotate(pose.armDeg, Offset(74f, 136f)) {
        line(72f, 140f, 54f, 206f, ArmLeft, 14f)
        unitCircle(53f, 210f, 9f, SkinDot)
    }
    line(128f, 140f, 146f, 206f, ArmRight, 14f)
    unitCircle(147f, 210f, 9f, SkinDot)
    fillPath(sleeves.left.path, sleeves.left.fill)
    fillPath(sleeves.right.path, sleeves.right.fill)

    // Pää ja kasvot
    contactShadow(HeadShadow)
    vinylOval(Head, HeadBrush)
    glossyEye(EyeLeft, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
    glossyEye(EyeRight, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk)
    drawOval(ArtTones.CheekColor, CheekLeft.topLeft, CheekLeft.size)
    drawOval(ArtTones.CheekColor, CheekRight.topLeft, CheekRight.size)
    drawPath(DollParts.Mouth, C.Mouth, style = MouthStroke)

    // Otsatukka
    drawPath(DollParts.Bangs, hair.bangs)
    if (o[Slot.HAIR] == CURLY_HAIR) {
        unitCircle(58f, 78f, 9f, PinkHairDot)
        unitCircle(142f, 78f, 9f, PinkHairDot)
    }
    specular(HairShine, HAIR_SHINE_DEG)

    // Päähän
    drawHeadwear(o[Slot.HEAD])

    // Laukku käteen
    if (o[Slot.EXTRA] == "extra-laukku") {
        drawPath(DollParts.BagStrap, C.BagDeep, style = BagStrapStroke)
        outlined(Bag, C.BagDeep)
        drawPath(DollParts.BagFlap, C.BagDeep, style = BagFlapStroke)
        unitCircle(153f, 210f, 3f, StarYellowDot)
    }
}

private fun DrawScope.drawShoes(id: String?) {
    when (id) {
        "shoes-punaiset" -> {
            outlinedOval(83f, 305f, 14f, 8f, RedDot, C.RedDeep)
            outlinedOval(117f, 305f, 14f, 8f, RedDot, C.RedDeep)
            line(79f, 297f, 93f, 297f, C.RedDeep, 3f)
            line(107f, 297f, 121f, 297f, C.RedDeep, 3f)
        }

        "shoes-saappaat" -> {
            outlined(BootLeft, C.VioletDeep)
            outlined(BootRight, C.VioletDeep)
            outlinedOval(83f, 306f, 14f, 8f, VioletDot, C.VioletDeep)
            outlinedOval(117f, 306f, 14f, 8f, VioletDot, C.VioletDeep)
            vinylShape(BootCuffLeft)
            vinylShape(BootCuffRight)
        }

        "shoes-kimalle" -> {
            outlinedOval(83f, 305f, 14f, 8f, GoldDot, C.GoldDeep)
            outlinedOval(117f, 305f, 14f, 8f, GoldDot, C.GoldDeep)
            for (i in ShoeStars.indices) drawPath(ShoeStars[i], C.White)
        }

        else -> {
            outlinedOval(83f, 305f, 14f, 8f, WhiteDot, C.ShoeOutline)
            outlinedOval(117f, 305f, 14f, 8f, WhiteDot, C.ShoeOutline)
        }
    }
    specular(ShoeLeft)
    specular(ShoeRight)
}

private fun DrawScope.drawHeadwear(id: String?) {
    when (id) {
        "head-rusetti" -> {
            vinylShape(BowLeft)
            vinylShape(BowRight)
            unitCircle(128f, 40f, 6f, RedDeepDot)
        }

        "head-kukat" -> {
            for (i in FlowerAngles.indices) {
                val a = FlowerAngles[i] * PI.toFloat() / 180f
                val x = 100f + 46f * sin(a)
                val y = 78f - 46f * cos(a)
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
            unitCircle(100f, 32f, 4.5f, BowRedDot)
            unitCircle(86f, 35f, 3.2f, JewelDot)
            unitCircle(114f, 35f, 3.2f, JewelDot)
        }
    }
}

private const val EYE_RX = 5.5f
private const val EYE_RY = 7.5f
private const val SLEEVE_Y = 136f
private const val SLEEVE_R = 12f
private const val PRINCESS_SLEEVE_R = 16f
private const val HAIR_SHINE_DEG = -20f
private const val DRESS_SHINE_DEG = 14f
private const val CENTER_X = 100f
private const val CURLY_HAIR = "hair-kiharat"

private val Feet = Offset(CENTER_X, 312f)
private val Middle = Offset(CENTER_X, 170f)

private val OutlineStroke = Stroke(2.5f)
private val WingStroke = Stroke(3f)
private val PetalStroke = Stroke(1f)
private val CrownStroke = Stroke(2.5f, join = StrokeJoin.Round)
private val CollarStroke = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val BeltStroke = Stroke(7f, join = StrokeJoin.Round)
private val HemStroke = Stroke(4f, join = StrokeJoin.Round)
private val MouthStroke = Stroke(3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val BagStrapStroke = Stroke(3f, join = StrokeJoin.Round)
private val BagFlapStroke = Stroke(2.5f, join = StrokeJoin.Round)

/** A unit-box vinyl brush for [unitOval], one per pigment. */
private fun dot(color: Color): Brush = unitBrush(vinylTones(color))

private const val DOT_R = 2.6f

private val Ground = ovalRect(CENTER_X, 311f, 46f, 8f)
private val HeadShadow = ovalRect(CENTER_X, 127f, 22f, 7f)

private val SkinTones = vinylTones(C.Skin)
private val SkinDot = unitBrush(SkinTones)
private val Head = ovalRect(CENTER_X, 78f, 44f)
private val HeadBrush = vinylBrush(Head, SkinTones)
private val Neck = VinylShape(roundRectPath(92f, 110f, 108f, 134f, 6f), SkinTones)
private val LegLeft = vinylBrush(Rect(78f, 218f, 94f, 306f), SkinTones)
private val LegRight = vinylBrush(Rect(106f, 218f, 122f, 306f), SkinTones)
private val ArmLeft = vinylBrush(Rect(47f, 133f, 79f, 213f), SkinTones)
private val ArmRight = vinylBrush(Rect(121f, 133f, 153f, 213f), SkinTones)

private val EyeLeft = Offset(85f, 86f)
private val EyeRight = Offset(115f, 86f)
private val CheekLeft = ovalRect(74f, 100f, 8f, 5.5f)
private val CheekRight = ovalRect(126f, 100f, 8f, 5.5f)

private val LongHairBounds = DollParts.LongHair.getBounds()
private val BangsBounds = DollParts.Bangs.getBounds()
private val BrownHair = vinylHair(C.HairBrown)
private val GoldHair = vinylHair(C.HairGold)
private val PinkHair = vinylHair(C.HairPink)
private val RainbowHair = HairBrushes(rainbowBrush(LongHairBounds), rainbowBrush(BangsBounds))
private val BrownHairDot = dot(C.HairBrown)
private val PinkHairDot = dot(C.HairPink)
private val BowDot = dot(C.Bow)
private val HairShine = specularRect(84f, 42f, 14f, 5f)

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
        Ellipse(60f, 60f, 24f),
        Ellipse(52f, 98f, 22f),
        Ellipse(56f, 134f, 20f),
        Ellipse(68f, 164f, 16f),
        Ellipse(140f, 60f, 24f),
        Ellipse(148f, 98f, 22f),
        Ellipse(144f, 134f, 20f),
        Ellipse(132f, 164f, 16f),
        Ellipse(100f, 36f, 32f),
    )

private val DressBounds = DollParts.Dress.getBounds()
private val YellowDress = dressBrush(C.DressYellow)
private val DotsDress = Fill.Pattern(stars = false, background = dressBrush(C.DotsBg), bounds = DressBounds)
private val StarsDress = Fill.Pattern(stars = true, background = dressBrush(C.StarsBg), bounds = DressBounds)
private val PrincessDress = dressBrush(C.Princess)
private val PrincessSkirt = VinylShape(DollParts.PrincessSkirt, vinylTones(C.PrincessLight))
private val PrincessBelt = VinylShape(roundRectPath(63f, 157f, 137f, 167f, 5f), vinylTones(C.Gold))
private val RainbowStripes = SkColors.Rainbow7.map(::dressBrush)
private val PrincessStars = listOf(starPath(78f, 205f, 5f), starPath(118f, 196f, 4f), starPath(100f, 222f, 4.5f))
private val DressShine = specularRect(88f, 178f, 7f, 17f)

private val YellowSleeves = vinylSleeves(C.DressYellow)
private val DotSleeves = patternSleeves(stars = false, background = C.DotsBg)
private val StarSleeves = patternSleeves(stars = true, background = C.StarsBg)
private val PrincessSleeves = vinylSleeves(C.PrincessLight, PRINCESS_SLEEVE_R)
private val RainbowSleeves = vinylSleeves(C.RainbowSleeve)

private val ShoeLeft = ovalRect(83f, 305f, 14f, 8f)
private val ShoeRight = ovalRect(117f, 305f, 14f, 8f)
private val WhiteDot = dot(C.White)
private val RedDot = dot(C.Red)
private val VioletDot = dot(C.Violet)
private val GoldDot = dot(C.Gold)
private val ShoeStars = listOf(starPath(80f, 303f, 4f), starPath(114f, 303f, 4f), starPath(91f, 300f, 2.5f))
private val VioletTones = vinylTones(C.Violet)
private val BootLeft = VinylShape(roundRectPath(77f, 268f, 95f, 306f, 6f), VioletTones)
private val BootRight = VinylShape(roundRectPath(105f, 268f, 123f, 306f, 6f), VioletTones)
private val CuffTones = vinylTones(C.PrincessLight)
private val BootCuffLeft = VinylShape(roundRectPath(75f, 266f, 97f, 274f, 4f), CuffTones)
private val BootCuffRight = VinylShape(roundRectPath(103f, 266f, 125f, 274f, 4f), CuffTones)

private val Wings =
    listOf(
        Ellipse(56f, 150f, 32f, 46f, -30f),
        Ellipse(64f, 202f, 20f, 28f, 28f),
        Ellipse(144f, 150f, 32f, 46f, 30f),
        Ellipse(136f, 202f, 20f, 28f, -28f),
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

private val Bag = VinylShape(roundRectPath(138f, 198f, 168f, 222f, 7f), vinylTones(C.Bag))
private val StarYellowDot = dot(C.StarYellow)
