// Literal coordinates place the unicorn in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.roundRectPath
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The unicorn as a shaded vinyl toy seen from the front, after the reference: a pastel tail, four legs
 * with lilac hooves, a round body with a pink belly patch, a large round head with pointed ears, a
 * pastel mane behind the head and a forelock in front, a golden spiral horn, glossy eyes, a cream
 * muzzle with pink nostrils, a smile and cheeks. The body breathes around the feet, the head tilts
 * around the neck and partDeg swings the tail. The horn always shows: head items sit around it.
 */
internal object UnicornRig : AnimalRig(
    // The crops keep every item of the slot whole: the head crop takes in the horn tip and the mane,
    // the neck crop the pendants and the cape clasp.
    headCrop = Rect(28f, 0f, 212f, 172f),
    neckCrop = Rect(64f, 160f, 176f, 244f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "unicorn-extra-cloud" to Rect(41f, 142f, 199f, 300f),
            "unicorn-extra-balloon" to Rect(0f, 116f, 158f, 274f),
            "unicorn-extra-boots" to Rect(60f, 180f, 180f, 300f),
            "unicorn-extra-rainbow" to Rect(3f, 66f, 237f, 300f),
            "unicorn-extra-wings" to Rect(0f, 52f, 240f, 292f),
        ),
) {
    /** Coat brushes for every companion color, built once and indexed by the color's ordinal. */
    private val coats = CompanionColor.entries.map { UnicornCoat(it) }

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        val coat = coats[color.ordinal]
        posedAnimal(pose, outfit, Ground) {
            rotate(pose.partDeg, TailPivot) {
                for (i in Tail.indices) drawPath(Tail[i].path, Tail[i].brush, style = TailStroke)
            }
            // The legs come out from under the body: the body is drawn over their tops.
            for (i in BackLegs.indices) {
                drawPath(BackLegs[i], coat.backLegs[i])
                vinylShape(BackHooves[i])
            }
            for (i in Haunches.indices) vinylOval(Haunches[i], coat.haunches[i])
            for (i in FrontLegs.indices) {
                drawPath(FrontLegs[i], coat.frontLegs[i])
                vinylShape(FrontHooves[i])
                specular(FrontHooves[i].bounds)
                drawPath(HoofRims[i], ArtTones.Pearl.base, alpha = HOOF_RIM_ALPHA, style = HoofRimStroke)
            }
            vinylOval(Body, coat.body)
            vinylShape(Patch)
            specular(BodyShine)
            contactShadow(LegShadowLeft)
            contactShadow(LegShadowRight)
            items(outfit, ArtPart.BODY, ItemLayer.FRONT)
            contactShadow(HeadShadow)
            rotate(pose.headTiltDeg, Neck) { head(coat, outfit, pose) }
        }
    }

    private fun DrawScope.head(
        coat: UnicornCoat,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        for (i in ManeBehind.indices) vinylShape(ManeBehind[i])
        contactShadow(ManeShadow)
        drawPath(Ears, coat.ears)
        vinylShape(InnerEars)
        contactShadow(EarShadowLeft)
        contactShadow(EarShadowRight)
        vinylOval(Head, coat.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        vinylShape(Horn)
        drawPath(HornGrooves, ArtTones.Gold.shade, style = GrooveStroke)
        specular(Horn.bounds)
        vinylShape(ForelockPink)
        vinylShape(ForelockPurple)
        specular(ForelockPink.bounds)
        vinylShape(Muzzle)
        cheeks(CheekLeft, CheekRight, ArtTones.CoolCheekColor)
        glossyEyes(EyeLeft, EyeRight, EYE_RX, EYE_RY, pose.blink)
        vinylShape(Nostrils)
        drawPath(Smile, PersonalizationColors.EyeInk, style = SmileStroke)
        items(outfit, ArtPart.HEAD, ItemLayer.FRONT)
    }
}

/** One coat color's brushes. */
private class UnicornCoat(
    color: CompanionColor,
) {
    private val tones = vinylTones(PersonalizationColors.companion(CompanionKind.UNICORN, color))
    val body: Brush = vinylBrush(Body, tones)
    val head: Brush = vinylBrush(Head, tones)
    val ears: Brush = vinylBrush(Ears.getBounds(), tones)
    private val farTones = VinylTones(tones.base, tones.shade, tones.shade)
    val frontLegs: List<Brush> = FrontLegs.map { vinylBrush(it.getBounds(), tones) }
    val backLegs: List<Brush> = BackLegs.map { vinylBrush(it.getBounds(), farTones) }
    val haunches: List<Brush> = Haunches.map { vinylBrush(it, farTones) }
}

private const val HOOF_RIM_ALPHA = 0.55f

private const val HEAD_SHINE_DEG = -18f
private const val EYE_RX = 12f
private const val EYE_RY = 14f
private const val TAIL_WIDTH = 15f

/** The mane's three pastel tones, shared with the unicorn's items. */
internal val ManePink = vinylTones(PersonalizationColors.ManePink)
internal val ManePurple = vinylTones(PersonalizationColors.Purple)
internal val ManeBlue = vinylTones(PersonalizationColors.ManeBlue)
private val HoofTones = vinylTones(PersonalizationColors.UnicornHoof)

private val TailPivot = Offset(160f, 214f)
private val Neck = Offset(120f, 168f)

private val Ground = ovalRect(120f, 284f, 88f, 10f)
private val HeadShadow = ovalRect(120f, 172f, 46f, 11f)

private val TailStroke = itemStroke(TAIL_WIDTH)
private val Tail =
    listOf(
        "M162 206 C194 194 222 212 214 240" to ManePurple,
        "M160 218 C198 212 212 238 200 258 C192 270 204 280 216 276" to ManePink,
        "M164 230 C196 236 204 258 188 272" to ManeBlue,
    ).map { (data, tones) ->
        val path = svgPath(data)
        TailStrand(path, vinylBrush(path.getBounds().inflate(TAIL_WIDTH / 2), tones))
    }

/** One stroked strand of the tail and its brush. */
private class TailStrand(
    val path: Path,
    val brush: Brush,
)

/** The unicorn's front hooves, where boots go. */
internal val UnicornFrontHooves = listOf(Rect(87f, 259f, 119f, 284f), Rect(121f, 259f, 153f, 284f))

/** A rounded hoof inside [r]: a domed top over the leg and a flat, rounded sole. */
private fun hoofPath(r: Rect): Path {
    val mid = r.center.x
    val dome = r.top + 9f
    return svgPath(
        "M${r.left} $dome C${r.left} ${r.top + 2f} ${mid - 8f} ${r.top} $mid ${r.top} " +
            "C${mid + 8f} ${r.top} ${r.right} ${r.top + 2f} ${r.right} $dome " +
            "L${r.right} ${r.bottom - 8f} Q${r.right} ${r.bottom} ${r.right - 9f} ${r.bottom} " +
            "L${r.left + 9f} ${r.bottom} Q${r.left} ${r.bottom} ${r.left} ${r.bottom - 8f} Z",
    )
}

/** Back legs: short, slightly flared and set wide, behind the haunches. */
private val BackLegs =
    listOf(
        svgPath("M64 236 C62 252 60 262 61 270 L83 270 C84 262 84 252 82 236 Z"),
        svgPath("M176 236 C178 252 180 262 179 270 L157 270 C156 262 156 252 158 236 Z"),
    )
private val BackHooves =
    listOf(Rect(58f, 262f, 86f, 281f), Rect(154f, 262f, 182f, 281f)).map {
        VinylShape(hoofPath(it), VinylTones(HoofTones.base, HoofTones.shade, HoofTones.shade))
    }

/** Round thighs at the sides, partly hidden by the body. */
private val Haunches = listOf(ovalRect(78f, 228f, 22f, 24f), ovalRect(162f, 228f, 22f, 24f))
private val Body = ovalRect(120f, 203f, 54f, 42f)
private val Patch = VinylShape(ovalPath(120f, 209f, 24f, 25f), vinylTones(PersonalizationColors.UnicornPatch))
private val BodyShine = specularRect(98f, 178f, 10f, 6f)

/** Front legs: they taper in under the body and flare a little toward the hooves. */
private val FrontLegs =
    listOf(
        svgPath("M94 226 C93 244 90 258 89 268 L117 268 C116 258 113 244 112 226 Z"),
        svgPath("M146 226 C147 244 150 258 151 268 L123 268 C124 258 127 244 128 226 Z"),
    )
private val FrontHooves = UnicornFrontHooves.map { VinylShape(hoofPath(it), HoofTones) }

/** A soft light rim along the top of each front hoof. */
private val HoofRims =
    UnicornFrontHooves.map {
        svgPath("M${it.left + 4f} ${it.top + 6f} Q${it.center.x} ${it.top + 1f} ${it.right - 4f} ${it.top + 6f}")
    }
private val HoofRimStroke = itemStroke(2f)

private val ManeBehind =
    listOf(
        VinylShape(svgPath("M140 58 C176 44 198 76 186 106 C172 96 158 92 146 96 Z"), ManePurple),
        VinylShape(
            svgPath("M150 72 C190 62 208 102 192 132 C206 152 198 182 174 188 C180 162 170 142 156 132 Z"),
            ManeBlue,
        ),
        VinylShape(svgPath("M160 142 C190 152 196 186 168 198 C170 178 162 162 150 156 Z"), ManePink),
    )
private val Ears =
    Path().apply {
        addPath(svgPath("M70 82 L65 42 Q67 35 74 39 L102 62 Z"))
        addPath(svgPath("M170 82 L175 42 Q173 35 166 39 L138 62 Z"))
    }
private val InnerEars =
    VinylShape(
        Path().apply {
            addPath(svgPath("M74 72 L71 49 L92 63 Z"))
            addPath(svgPath("M166 72 L169 49 L148 63 Z"))
        },
        ArtTones.Pink,
    )
private val Head = ovalRect(120f, 110f, 64f, 58f)
private val HeadShine = specularRect(90f, 76f, 22f, 9f)
private val Horn =
    VinylShape(svgPath("M108 60 Q109 52 112 46 L120 2 L128 46 Q131 52 132 60 Q120 64 108 60 Z"), ArtTones.Gold)
private val HornGrooves = svgPath("M110.5 52 L128 44 M113 38 L125.5 31 M115.5 25 L123 20 M118 13 L121.5 10.5")
private val GrooveStroke = itemStroke(2.2f)
private val ForelockPink =
    VinylShape(
        svgPath("M118 58 C96 50 72 62 76 92 C84 78 98 74 108 78 C102 86 104 96 112 100 C112 86 120 72 118 58 Z"),
        ManePink,
    )
private val ForelockPurple =
    VinylShape(svgPath("M124 56 C140 48 164 56 166 76 C156 68 142 68 132 72 Z"), ManePurple)

private val Muzzle = VinylShape(ovalPath(120f, 146f, 30f, 20f), ArtTones.Cream)
private val Nostrils =
    VinylShape(itemPath(ovalPath(110f, 141f, 3.5f, 2.5f), ovalPath(130f, 141f, 3.5f, 2.5f)), ArtTones.Pink)
private val CheekLeft = ovalRect(78f, 138f, 12f, 8f)
private val CheekRight = ovalRect(162f, 138f, 12f, 8f)
private val EyeLeft = Offset(92f, 116f)
private val EyeRight = Offset(148f, 116f)
private val Smile = svgPath("M108 151 Q120 161 132 151")
private val SmileStroke = itemStroke(2.8f)

// Contact shadows where parts meet: the leg tops under the body, the ear roots and the mane beside the head.
private val LegShadowLeft = ovalRect(103f, 248f, 15f, 6f)
private val LegShadowRight = ovalRect(137f, 248f, 15f, 6f)
private val EarShadowLeft = ovalRect(88f, 66f, 14f, 6f)
private val EarShadowRight = ovalRect(152f, 66f, 14f, 6f)
private val ManeShadow = ovalRect(182f, 116f, 12f, 44f)
