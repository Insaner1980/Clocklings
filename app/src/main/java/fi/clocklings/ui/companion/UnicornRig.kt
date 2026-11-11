// Literal coordinates place the unicorn in its 240 x 300 design grid.
@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.VioletIris
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.glossyEye
import fi.clocklings.ui.art.ovalPath
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.softRim
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.specularRect
import fi.clocklings.ui.art.svgPath
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.art.vinylTones
import fi.clocklings.ui.theme.PersonalizationColors

/**
 * The unicorn as a shaded vinyl toy standing in three-quarter view, facing left. The body, the neck and
 * all four legs are one shape traced from the approved reference, so the legs grow out of the chest and
 * the haunch with the belly arching between them; the far legs are shaded below the belly. A pastel
 * tail curls behind the rump and a pastel mane falls down the neck. The large round head turns toward
 * the viewer, so both glossy eyes show, with a golden spiral horn, pointed ears, a forelock, a cream
 * muzzle, a smile and cheeks. The body breathes around the feet, the head tilts around the neck and
 * partDeg swings the tail. The horn always shows: head items sit around it.
 */
internal object UnicornRig : AnimalRig(
    // The crops keep every item of the slot whole: the head crop takes in the horn tip and the mane,
    // the neck crop the pendants and the cape clasp.
    headCrop = Rect(12f, 0f, 196f, 172f),
    neckCrop = Rect(48f, 158f, 168f, 278f),
    // Accessory crops: the item's drawing bounds plus 12 units on every side, squared to the thumbnail window.
    itemCrops =
        mapOf(
            "unicorn-extra-cloud" to Rect(46f, 142f, 204f, 300f),
            "unicorn-extra-balloon" to Rect(0f, 157f, 120f, 277f),
            "unicorn-extra-boots" to Rect(56f, 163f, 193f, 300f),
            "unicorn-extra-rainbow" to Rect(3f, 66f, 237f, 300f),
            "unicorn-extra-wings" to Rect(102f, 87f, 222f, 207f),
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
                for (i in Tail.indices) vinylShape(Tail[i])
            }
            drawPath(Body, coat.body)
            // A soft shaded rim inside the outline keeps the white body readable on light backgrounds.
            softRim(Body, coat.rim)
            // The far legs fade into shade below the belly, so they read as standing behind the near ones.
            for (i in FarLegShades.indices) drawPath(FarLegShades[i], coat.farShade)
            specular(ChestShine)
            specular(BackShine)
            specular(HaunchShine)
            for (i in LegShines.indices) specular(LegShines[i])
            for (i in Hooves.indices) {
                vinylShape(Hooves[i])
                specular(Hooves[i].bounds)
            }
            for (i in NearHooves.indices) {
                drawPath(HoofRims[i], ArtTones.Pearl.base, alpha = HOOF_RIM_ALPHA, style = HoofRimStroke)
            }
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
        contactShadow(EarShadowFar)
        contactShadow(EarShadowNear)
        vinylOval(Head, coat.head)
        specular(HeadShine, HEAD_SHINE_DEG)
        vinylShape(Horn)
        drawPath(HornGrooves, ArtTones.Gold.shade, style = GrooveStroke)
        specular(Horn.bounds)
        vinylShape(ForelockPink)
        vinylShape(ForelockPurple)
        specular(ForelockPink.bounds)
        // The cheeks stay on the face: clipped to the head and drawn under the muzzle, they never spill
        // past the head edge or over the muzzle.
        clipPath(HeadPath) { cheeks(CheekFar, CheekNear, ArtTones.CoolCheekColor) }
        vinylShape(Muzzle)
        // The head turns toward the viewer: the far eye is a little smaller.
        glossyEye(EyeFar, FAR_EYE_RX, FAR_EYE_RY, pose.blink, ArtTones.EyeInk, VioletIris)
        glossyEye(EyeNear, EYE_RX, EYE_RY, pose.blink, ArtTones.EyeInk, VioletIris)
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
    val body: Brush = vinylBrush(BodyLight, tones)
    val head: Brush = vinylBrush(Head, tones)
    val ears: Brush = vinylBrush(Ears.getBounds(), tones)
    val rim: Color = tones.shade.copy(alpha = RIM_ALPHA)
    val farShade: Brush =
        Brush.verticalGradient(
            0f to Color.Transparent,
            1f to tones.shade.copy(alpha = FAR_SHADE_ALPHA),
            startY = FAR_SHADE_TOP,
            endY = FAR_SHADE_FULL,
        )
}

private const val HOOF_RIM_ALPHA = 0.55f
private const val FAR_SHADE_ALPHA = 0.6f
private const val RIM_ALPHA = 0.045f
private const val FAR_SHADE_TOP = 222f
private const val FAR_SHADE_FULL = 246f

private const val HEAD_SHINE_DEG = -18f
private const val EYE_RX = 11.5f
private const val EYE_RY = 13.5f
private const val FAR_EYE_RX = 10.2f
private const val FAR_EYE_RY = 12.7f

/** The mane's three pastel tones, shared with the unicorn's items. */
internal val ManePink = vinylTones(PersonalizationColors.ManePink)
internal val ManePurple = vinylTones(PersonalizationColors.Purple)
internal val ManeBlue = vinylTones(PersonalizationColors.ManeBlue)
private val HoofTones = vinylTones(PersonalizationColors.UnicornHoof)
private val FarHoofTones = VinylTones(HoofTones.base, HoofTones.shade, HoofTones.shade)

private val TailPivot = Offset(166f, 172f)
private val Neck = Offset(100f, 162f)

private val Ground = ovalRect(125f, 276f, 64f, 8f)
private val HeadShadow = ovalRect(102f, 162f, 22f, 7f)

/**
 * The body, the neck and the four legs as one shape, traced from the reference: the chest runs down
 * into the front legs and the haunch into the hind legs, with the belly arching between them. The top
 * of the neck goes up under the head and the mane.
 */
private val Body =
    svgPath(
        "M136 128 C134 130.5 131 137.5 127.5 141.5 C123.5 145 119.5 148.5 114.5 151 " +
            "C109.5 153.5 103 156 97 157 C91.5 158.5 83 156.5 80 158 C77 159 79 160.5 80 164.5 " +
            "C81 168 84.5 174 85 179.5 C85.5 184.5 82 191.5 82.5 197 C82.5 202 85 207.5 86 212 " +
            "C86.5 216.5 86.5 217.5 86 223 C85 229 83.5 240 81 247 C78.5 254 72.5 261 71 265.5 " +
            "C69.5 270 68.5 272.5 72.5 273.5 C76.5 275 91 272.5 95 273 C98.5 274 95 277.5 96 278.5 " +
            "C97 280 97.5 280 101 280 C104.5 280 114.5 280 117 278.5 C120 276.5 118.5 274 117.5 268.5 " +
            "C117 263 113.5 252.5 112.5 246 C111.5 239 112 234 112.5 229 C113 224 113.5 218 115.5 215 " +
            "C117.5 212 122.5 212.5 125 212 C127.5 211 129 209.5 131 212 C132.5 214 135 220 135.5 225 " +
            "C136 230.5 135 237.5 133.5 243 C132 248.5 127.5 254 126.5 258 C125 261.5 124.5 264.5 126.5 266 " +
            "C128.5 267.5 136 267.5 139 267.5 C142 267.5 144 270.5 145 266.5 C146 262 144.5 250 145 242.5 " +
            "C145.5 235 146 224.5 147 220.5 C148 217 149 219.5 151 220.5 C152.5 222 156 225.5 157.5 229 " +
            "C159.5 232.5 160.5 237.5 161 242.5 C161 247 160.5 254 159.5 258 C159 262 156.5 264 156 266.5 " +
            "C155.5 269.5 153.5 273 156.5 274 C159.5 275.5 170 275.5 173.5 275.5 C177.5 275 177.5 275 178 273 " +
            "C179 271 179 268 178 264.5 C177.5 261 175.5 257.5 174.5 251.5 C173.5 245.5 173 234 172 228 " +
            "C170.5 222.5 167.5 222.5 167.5 216.5 C167 211 170 200 170.5 194.5 " +
            "C170.5 189.5 170.5 188.5 169.5 185 C168.5 182 167 179 165 176.5 C163 174 160 172 157.5 170.5 " +
            "C155 169 152.5 168 150.5 167.5 C148.5 167 148 166.5 146 167.5 C144 168 140 172 138.5 173 " +
            "C136.5 174 134.5 175.5 135 173 C135 170.5 139.5 161 140 157 C141 153.5 140 152 139 149.5 " +
            "C138 147 135 144 134 142 C133.5 140.5 133.5 140.5 134 138.5 C135 136.5 138 132 139 130 " +
            "C140 128.5 139.5 128.5 139 128 C138.5 128 138 126 136 128 Z",
    )

/** The far front and far hind leg below the belly, where their shade is drawn. */
private val FarLegShades =
    listOf(
        svgPath(
            "M85.5 226 C83 229.5 82.5 241.5 81 247 C79.5 252 77 255.5 75.5 258 C74.5 260 73 260.5 72 262 " +
                "C71.5 263 71 264 71 265.5 C71 267.5 70.5 270 71 271.5 C71 273 69.5 273 72.5 273.5 " +
                "C75.5 274 85 274.5 88.5 274 C92 273.5 92.5 276 93.5 271.5 C94 267 92 254.5 92.5 247 " +
                "C93 239.5 97 229.5 96 226 C95 222.5 88 222.5 85.5 226 Z",
        ),
        svgPath(
            "M135.5 226 C133.5 229 134.5 238.5 133.5 243 C132.5 247.5 131 250.5 130 252.5 " +
                "C128.5 255 127.5 255 126.5 257 C126 259 125 263 125.5 264.5 C126 266.5 126.5 266.5 129 267 " +
                "C132 267.5 138.5 267.5 141 267.5 C143.5 267.5 144.5 267 145 266.5 " +
                "C146 266 146 268.5 146 264.5 C146 260.5 145 249 145 242.5 C145 236 148.5 229 147 226 " +
                "C145.5 223.5 138 223.5 135.5 226 Z",
        ),
    )

/**
 * The area the body gradient is spread over: wider and deeper than the body, so the light falls over the
 * whole standing figure and the legs stay in the base tone instead of the core shadow.
 */
private val BodyLight = Rect(56f, 116f, 212f, 312f)
private val ChestShine = specularRect(90f, 190f, 5f, 9f)
private val BackShine = specularRect(138f, 178f, 10f, 4f)
private val HaunchShine = specularRect(162f, 182f, 6f, 4f)

/** Narrow highlights down the front of the near legs. */
private val LegShines = listOf(specularRect(102f, 238f, 2.2f, 11f), specularRect(164f, 240f, 2.2f, 10f))

/** The unicorn's near hooves, front and hind, where boots go. */
internal val UnicornNearHooves = listOf(Rect(93f, 261f, 120f, 281f), Rect(154f, 257f, 180f, 276f))

/** The far hooves, front and hind, set higher behind the near ones. */
internal val UnicornFarHooves = listOf(Rect(69f, 256f, 95f, 275f), Rect(124f, 251f, 148f, 268f))

/** A rounded hoof inside [r]: a domed top over the leg and a flat, rounded sole. */
private fun hoofPath(r: Rect): Path {
    val mid = r.center.x
    val dome = r.top + 7f
    return svgPath(
        "M${r.left} $dome C${r.left} ${r.top + 1f} ${mid - 7f} ${r.top} $mid ${r.top} " +
            "C${mid + 7f} ${r.top} ${r.right} ${r.top + 1f} ${r.right} $dome " +
            "L${r.right} ${r.bottom - 6f} Q${r.right} ${r.bottom} ${r.right - 7f} ${r.bottom} " +
            "L${r.left + 7f} ${r.bottom} Q${r.left} ${r.bottom} ${r.left} ${r.bottom - 6f} Z",
    )
}

private val NearHooves = UnicornNearHooves.map { VinylShape(hoofPath(it), HoofTones) }

/** All four hooves, far ones first. */
private val Hooves = UnicornFarHooves.map { VinylShape(hoofPath(it), FarHoofTones) } + NearHooves

/** A soft light rim along the top of each near hoof. */
private val HoofRims =
    UnicornNearHooves.map {
        svgPath("M${it.left + 4f} ${it.top + 5f} Q${it.center.x} ${it.top + 1f} ${it.right - 4f} ${it.top + 5f}")
    }
private val HoofRimStroke = itemStroke(2f)

/** The tail: a pink curl with a lilac band on top and a blue band below, traced from the reference. */
private val Tail =
    listOf(
        VinylShape(
            svgPath(
                "M176.5 156 C175.5 156.5 178 157.5 176.5 158 C175 158.5 170 157.5 168 158 " +
                    "C165.5 158.5 164.5 159.5 163 161.5 C161.5 163 159 166 158 168 " +
                    "C157.5 169.5 157 170.5 158 172 C159.5 173 163.5 174 165 176.5 " +
                    "C167 178.5 167.5 183.5 168.5 185 C169.5 187 171 186 172 186.5 " +
                    "C173.5 187.5 175 187.5 175.5 191 C176 194.5 174.5 203 175.5 207 " +
                    "C176 211 177 213 179.5 215 C182 217.5 186 220.5 190 221 C194 222 200 221.5 203.5 220 " +
                    "C207 219 209 216 210.5 213.5 C212 210.5 212.5 205.5 212.5 203.5 " +
                    "C212.5 201.5 213.5 202.5 211.5 202 C209.5 202 203 206 201.5 202 " +
                    "C200 198 203 183 203 177.5 C202.5 172.5 202 173 201 170.5 " +
                    "C200 168.5 198 165.5 196.5 163.5 C194.5 161.5 192.5 160 190.5 158.5 " +
                    "C188 157.5 185.5 156.5 183.5 156 C181 155.5 178 155.5 176.5 156 Z",
            ),
            ManePink,
        ),
        VinylShape(
            svgPath(
                "M177 156.5 C175.5 156.5 178.5 158 177 158.5 C175.5 159 170.5 158 168.5 158.5 " +
                    "C166 159 165 160 163.5 161.5 C162 163 159.5 166 159 167.5 " +
                    "C158 169.5 158.5 170.5 159 171.5 C159.5 172 160.5 172 162.5 171.5 " +
                    "C164.5 170.5 167.5 168 170.5 167.5 C173.5 166.5 177.5 166.5 181.5 167.5 " +
                    "C185 168.5 190 171 193 173.5 C196 175.5 197 179 198.5 180.5 " +
                    "C200 181.5 201.5 182 202 180.5 C202.5 179 202 174 200.5 171 C199 168 196 164 193.5 161.5 " +
                    "C191 159.5 189 158.5 186.5 157.5 C183.5 156.5 178.5 156 177 156.5 Z",
            ),
            ManePurple,
        ),
        VinylShape(
            svgPath(
                "M171 175 C169 175 167 175 166.5 176 C165.5 176.5 166 177.5 166 179 " +
                    "C166.5 180.5 168 183.5 169 185 C170 186 171.5 185 172.5 186.5 " +
                    "C174 187.5 175.5 188 176.5 191.5 C177 195 175.5 203.5 176 206.5 " +
                    "C176 210 176 209.5 177.5 211.5 C179 213.5 181.5 217 184.5 218.5 " +
                    "C187.5 220 193.5 221.5 195 221 C196.5 220.5 193.5 218 193 216 " +
                    "C193 213.5 193 211 193 207.5 C193.5 204 195.5 197.5 195.5 194 " +
                    "C195.5 191 195 190 193.5 187.5 C191.5 185 187.5 180.5 185 178.5 " +
                    "C182.5 176.5 180.5 176 178 175.5 C175.5 175 173 175 171 175 Z",
            ),
            ManeBlue,
        ),
    )

/** The mane behind the head and down the neck, traced from the reference. */
private val ManeBehind =
    listOf(
        VinylShape(
            svgPath(
                "M141 60.5 C140 63 141 75.5 142 78.5 C142.5 82 144 79 146 80.5 " +
                    "C147.5 81.5 150.5 84 152.5 86 C154.5 88 156.5 91 158 93.5 C159 95.5 159.5 99 160 100 " +
                    "C161 101 161.5 102 161.5 100 C162 97.5 162.5 90.5 161.5 86.5 C161 82.5 160 79 158 75.5 " +
                    "C156 72 152.5 67.5 149.5 65 C147 63 142.5 58.5 141 60.5 Z",
            ),
            ManePurple,
        ),
        VinylShape(
            svgPath(
                "M128 76 C127 76 126.5 75 128 77.5 C130 80 135.5 88 138 90.5 " +
                    "C140.5 92.5 142 90 143.5 91.5 C145.5 93 146.5 96 147 99.5 C147.5 102.5 147.5 108 147 111 " +
                    "C146.5 114 144.5 117 143.5 118.5 C143 119.5 143.5 116.5 142.5 118.5 " +
                    "C142 120.5 140.5 127.5 139 131 C137.5 134.5 134.5 138 133.5 139.5 " +
                    "C133 141.5 133 140 133.5 141.5 C134.5 143 138 146.5 139.5 149.5 " +
                    "C140.5 152 140.5 156 140 158.5 C140 161.5 139 164 137.5 166.5 " +
                    "C135.5 169 131.5 173 130 174.5 C129 176 129.5 175.5 130 176 C130.5 176 129.5 177 132 176 " +
                    "C134.5 174.5 141.5 171 145 168 C148.5 164.5 152 159.5 153.5 156 " +
                    "C155.5 152.5 155.5 150.5 156 146.5 C156 142.5 154.5 135.5 155.5 131 " +
                    "C156 127 159 125.5 160 121.5 C161 117.5 162 112 161.5 108 C161.5 103.5 160.5 99 158.5 95 " +
                    "C157 91.5 154 87.5 151.5 84.5 C149 82 145.5 80 142.5 78.5 C139.5 77.5 136 76.5 133.5 76 " +
                    "C131.5 75.5 129 75.5 128 76 Z",
            ),
            ManeBlue,
        ),
        VinylShape(
            svgPath(
                "M132 141.5 C131 142.5 129 146 126 148 C123.5 150.5 117 154 115.5 156 " +
                    "C113.5 157.5 114 158 115 158.5 C115.5 159.5 119 158 120 159 " +
                    "C120.5 160.5 121.5 162.5 120.5 166 C119.5 169 115.5 174.5 114 178 " +
                    "C113 181 114 184 114 185.5 C114.5 186.5 115 186 115.5 185.5 " +
                    "C116.5 184.5 116.5 183.5 119 181 C122 179 128.5 176 132 173 " +
                    "C135 170 137.5 165.5 138.5 163.5 C140 161 140 161 140 159 C140 157 140 153.5 139.5 151 " +
                    "C139 148.5 137 145.5 136 144 C135 142.5 134.5 142 133.5 141.5 " +
                    "C133 141 133.5 140 132 141.5 Z",
            ),
            ManePink,
        ),
    )
private val Ears =
    Path().apply {
        addPath(svgPath("M42 78 L34 40 Q36 33 43 37 L70 60 Z"))
        addPath(svgPath("M116 64 L132 38 Q136 32 140 38 L144 74 Z"))
    }
private val InnerEars =
    VinylShape(
        Path().apply {
            addPath(svgPath("M46 70 L41 47 L62 61 Z"))
            addPath(svgPath("M123 62 L134 44 L138 69 Z"))
        },
        ArtTones.Pink,
    )
private val Head = ovalRect(86f, 108f, 60f, 54f)
private val HeadPath = ovalPath(86f, 108f, 60f, 54f)
private val HeadShine = specularRect(56f, 74f, 20f, 8f)
private val Horn =
    VinylShape(svgPath("M67 62 Q68 54 71 48 L79 5 L87 48 Q90 54 91 62 Q79 66 67 62 Z"), ArtTones.Gold)
private val HornGrooves = svgPath("M69.5 54 L87 46 M72 40 L84.5 33 M74.5 27 L82 22 M77 16 L80.5 13.5")
private val GrooveStroke = itemStroke(2.2f)
private val ForelockPink =
    VinylShape(
        svgPath("M76 58 C54 50 30 62 34 92 C42 78 56 74 66 78 C60 86 62 96 70 100 C70 86 78 72 76 58 Z"),
        ManePink,
    )
private val ForelockPurple =
    VinylShape(svgPath("M82 56 C98 48 122 56 124 76 C114 68 100 68 90 72 Z"), ManePurple)

private val Muzzle = VinylShape(ovalPath(78f, 145f, 30f, 19f), ArtTones.Cream)
private val Nostrils =
    VinylShape(itemPath(ovalPath(67.5f, 141f, 3.2f, 2.4f), ovalPath(89f, 141f, 3.5f, 2.5f)), ArtTones.Pink)
private val CheekFar = ovalRect(44f, 134f, 7.5f, 6f)
private val CheekNear = ovalRect(119f, 135f, 11f, 7.5f)
private val EyeFar = Offset(55f, 118f)
private val EyeNear = Offset(102f, 116f)
private val Smile = svgPath("M66 151.5 Q78.5 160 91 151.5")
private val SmileStroke = itemStroke(2.8f)

// Contact shadows where parts meet: the ear roots and the mane beside the head.
private val EarShadowFar = ovalRect(56f, 66f, 13f, 6f)
private val EarShadowNear = ovalRect(130f, 70f, 12f, 6f)
private val ManeShadow = ovalRect(142f, 112f, 10f, 40f)
