package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.art.ArtTones
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.glossyEye
import fi.clocklings.ui.art.groundShadow
import fi.clocklings.ui.art.ovalRect
import fi.clocklings.ui.art.specular
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylOval
import fi.clocklings.ui.art.vinylShape
import fi.clocklings.ui.doll.DollRig

/**
 * Where an item is drawn relative to its part: behind it, in front of it, over the ears before they are
 * drawn, or on the robot's face screen in place of its eyes.
 */
internal enum class ItemLayer { BEHIND, FRONT, OVER_EARS, SCREEN }

/**
 * The companion part whose transform an item follows. [ANTENNA] bounces on top of the robot's head;
 * [WHEELS] items are drawn on the car's hubs after its body and wheels.
 */
internal enum class ArtPart { BODY, HEAD, WINGS, HAND, ANTENNA, WHEELS }

/**
 * One item's artwork, drawn in the rig's design grid inside the transform of [part]. An
 * [ItemLayer.OVER_EARS] item may give an [earClip]: the area where the ears, drawn after it, stay
 * visible, so they come out of the hat above its band. An item drawn on another layer may also give a
 * [back]: the pieces that go behind the companion, drawn at the [ItemLayer.BEHIND] point of [part].
 */
internal class ItemArt(
    val part: ArtPart,
    val layer: ItemLayer,
    val earClip: Path? = null,
    val back: (DrawScope.() -> Unit)? = null,
    val draw: DrawScope.() -> Unit,
)

/**
 * Draws one companion in its own [width] x [height] design grid. [CompanionView] fits the grid (or a
 * [crop]) into the canvas and supplies the pose; the rig applies its own part transforms and calls
 * [items] at every part and layer point inside that part's transform.
 */
internal interface CompanionRig {
    val width: Float
    val height: Float

    /** The design-grid area that shows the [slot] in a wardrobe thumbnail. */
    fun crop(slot: Slot): Rect

    /** The design-grid area that shows [item] in a wardrobe thumbnail: its own crop, else its slot's. */
    fun crop(item: Item): Rect = crop(item.slot)

    /** The whole design grid. */
    fun full(): Rect

    fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    )
}

/**
 * The frame shared by the animal companions and the car: the 240 x 300 design grid, the wardrobe
 * thumbnail [crops] by slot, the [itemCrops] that zoom an item's thumbnail further by item id, and the
 * whole grid for every other slot.
 */
internal abstract class AnimalRig(
    private val crops: Map<Slot, Rect>,
    private val itemCrops: Map<String, Rect> = emptyMap(),
) : CompanionRig {
    constructor(headCrop: Rect, neckCrop: Rect, itemCrops: Map<String, Rect> = emptyMap()) :
        this(mapOf(Slot.HEAD to headCrop, Slot.NECK to neckCrop), itemCrops)

    final override val width = ANIMAL_GRID_WIDTH
    final override val height = ANIMAL_GRID_HEIGHT

    private val grid = Rect(0f, 0f, width, height)

    final override fun crop(slot: Slot): Rect = crops[slot] ?: grid

    final override fun crop(item: Item): Rect = itemCrops[item.id] ?: crop(item.slot)

    final override fun full(): Rect = grid
}

private const val ANIMAL_GRID_WIDTH = 240f
private const val ANIMAL_GRID_HEIGHT = 300f

/** Where an animal stands in its grid: the sway and the breathing turn around this point. */
internal val AnimalFeet = Offset(ANIMAL_GRID_WIDTH / 2, 280f)

/** The animal's vertical axis passes through this point; the spin turns around it. */
internal val AnimalMiddle = Offset(ANIMAL_GRID_WIDTH / 2, 150f)

/**
 * Draws [block] in the animal body pose: the hop lifts it, it sways around [feet], spins around
 * [AnimalMiddle] and breathes as a vertical squash around [feet].
 */
internal inline fun DrawScope.posedBody(
    pose: CompanionPose,
    feet: Offset = AnimalFeet,
    block: DrawScope.() -> Unit,
) = withTransform({
    translate(0f, -pose.lift)
    rotate(pose.swayDeg, feet)
    scale(pose.turn, 1f, AnimalMiddle)
    scale(1f, 1f + pose.breathe * CompanionMotion.BREATHE_SQUASH, feet)
}, block)

/** Ground shadow and rear clothing for animals whose body starts with those layers. */
internal inline fun DrawScope.posedAnimal(
    pose: CompanionPose,
    outfit: Outfit,
    ground: Rect,
    block: DrawScope.() -> Unit,
) {
    groundShadow(ground)
    posedBody(pose) {
        items(outfit, ArtPart.BODY, ItemLayer.BEHIND)
        block()
    }
}

internal fun DrawScope.cheeks(
    left: Rect,
    right: Rect,
    color: Color = ArtTones.CheekColor,
) {
    drawOval(color, left.topLeft, left.size)
    drawOval(color, right.topLeft, right.size)
}

internal fun DrawScope.glossyEyes(
    left: Offset,
    right: Offset,
    radiusX: Float,
    radiusY: Float,
    blink: Float,
) {
    glossyEye(left, radiusX, radiusY, blink, ArtTones.EyeInk)
    glossyEye(right, radiusX, radiusY, blink, ArtTones.EyeInk)
}

/** Cached limb brushes and the shared clothing order for the dragon and dinosaur. */
internal class ClawedLimbs(
    private val arms: Pair<Path, Path>,
    private val armStroke: Stroke,
    private val feet: Pair<Rect, Rect>,
    private val claws: VinylShape,
    tones: VinylTones,
) {
    private val armLeft = vinylBrush(arms.first.getBounds().inflate(armStroke.width / 2), tones)
    private val armRight = vinylBrush(arms.second.getBounds().inflate(armStroke.width / 2), tones)
    private val footLeft = vinylBrush(feet.first, tones)
    private val footRight = vinylBrush(feet.second, tones)

    // Contact shadows: under each arm on the belly and on the body just above each foot.
    private val armShadows =
        listOf(arms.first, arms.second).map {
            it.getBounds().inflate(armStroke.width / 2).translate(ARM_SHADOW_DX, ARM_SHADOW_DY)
        }
    private val footShadows =
        listOf(feet.first, feet.second).map {
            ovalRect(it.center.x, it.top + FOOT_SHADOW_DROP, it.width * FOOT_SHADOW_SPREAD, FOOT_SHADOW_RY)
        }

    fun draw(
        scope: DrawScope,
        outfit: Outfit,
    ) = with(scope) {
        items(outfit, ArtPart.HAND, ItemLayer.FRONT)
        for (i in armShadows.indices) contactShadow(armShadows[i])
        for (i in footShadows.indices) contactShadow(footShadows[i])
        drawPath(arms.first, armLeft, style = armStroke)
        drawPath(arms.second, armRight, style = armStroke)
        vinylOval(feet.first, footLeft)
        vinylOval(feet.second, footRight)
        specular(feet.first)
        specular(feet.second)
        vinylShape(claws)
        items(outfit, ArtPart.BODY, ItemLayer.FRONT)
    }
}

/** The renderer for [kind]; null for no companion. Returns shared instances, so it never allocates. */
internal fun rigFor(kind: CompanionKind): CompanionRig? =
    when (kind) {
        CompanionKind.NONE -> null
        CompanionKind.HUMAN -> DollRig
        CompanionKind.CAT -> CatRig
        CompanionKind.DOG -> DogRig
        CompanionKind.ROBOT -> RobotRig
        CompanionKind.DRAGON -> DragonRig
        CompanionKind.DINOSAUR -> DinosaurRig
        CompanionKind.BUNNY -> BunnyRig
        CompanionKind.UNICORN -> UnicornRig
        CompanionKind.CAR -> CarRig
    }

/** Width to height of [kind]'s whole design grid, for sizing its [CompanionView]; 1 for no companion. */
internal fun companionAspectRatio(kind: CompanionKind): Float = rigFor(kind)?.let { it.width / it.height } ?: 1f

/**
 * The item artwork of [kind] keyed by item id, or null when its rig draws its own items. Each animal's
 * map lives in its own file, so it is built only when that animal's items are first drawn.
 */
private fun itemArtOf(kind: CompanionKind): Map<String, ItemArt>? =
    when (kind) {
        CompanionKind.CAT -> CatItemArt
        CompanionKind.DOG -> DogItemArt
        CompanionKind.ROBOT -> RobotItemArt
        CompanionKind.DRAGON -> DragonItemArt
        CompanionKind.DINOSAUR -> DinosaurItemArt
        CompanionKind.BUNNY -> BunnyItemArt
        CompanionKind.UNICORN -> UnicornItemArt
        CompanionKind.CAR -> CarItemArt
        CompanionKind.NONE, CompanionKind.HUMAN -> null
    }

/** The registered artwork of item [id], or null when no rig draws it. */
internal fun itemArt(id: String): ItemArt? {
    val kind = Wardrobe.itemOrNull(id)?.companion ?: return null
    return itemArtOf(kind)?.get(id)
}

private val Slots = Slot.entries

/**
 * Draws the worn items that follow [part] on [layer], in slot order. Rigs call this inside the part's
 * transform.
 */
internal fun DrawScope.items(
    outfit: Outfit,
    part: ArtPart,
    layer: ItemLayer,
) {
    // Indexed loop: rigs call this several times per frame, so it must not allocate an iterator.
    for (i in Slots.indices) {
        val art = outfit[Slots[i]]?.let(::itemArt)
        if (art == null || art.part != part) continue
        if (art.layer == layer) {
            art.draw(this)
        } else if (layer == ItemLayer.BEHIND) {
            art.back?.invoke(this)
        }
    }
}

private const val ARM_SHADOW_DX = 3f
private const val ARM_SHADOW_DY = 6f
private const val FOOT_SHADOW_DROP = 2f
private const val FOOT_SHADOW_SPREAD = 0.42f
private const val FOOT_SHADOW_RY = 6f
