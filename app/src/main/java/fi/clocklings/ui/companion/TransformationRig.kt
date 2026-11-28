@file:Suppress("MagicNumber")

package fi.clocklings.ui.companion

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.domain.transformationId
import fi.clocklings.domain.transformationIds
import fi.clocklings.ui.art.groundShadow

internal class ModelSprite(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    left: Int,
    top: Int,
) {
    val source = IntOffset(x, y)
    val size = IntSize(width, height)
    val destination = IntOffset(left, top)
    val centerY = top + height / 2f
}

internal class ModelFrame(
    @DrawableRes val atlas: Int,
    val body: ModelSprite,
    val eyes: List<ModelSprite>,
)

@Composable
internal fun rememberTransformationRig(
    kind: CompanionKind,
    outfit: Outfit,
    color: CompanionColor,
): CompanionRig? {
    val art =
        outfit
            .transformationId()
            ?.takeIf { it in transformationIds[kind].orEmpty() }
            ?.let(transformationArt::get) ?: return null
    val atlas = rememberRenderedAtlas(transformationDyes[art.atlas]?.get(color) ?: art.atlas)
    return remember(kind, atlas, art) {
        TransformationRig(atlas, art, kind == CompanionKind.DRAGON)
    }
}

private const val MODEL_SCALE = 240f / 448f

// The 6.8-unit Blender camera, in the base dragon's 0.02-unit wardrobe grid.
private val DragonCamera = Rect(-50f, -22.5f, 290f, 317.5f)

/** The eyes close over the modelled face. Clothes, hands and held props share the same body transform. */
private class TransformationRig(
    private val atlas: ImageBitmap,
    private val art: ModelFrame,
    private val baseDragon: Boolean,
) : CompanionRig {
    override val width = 240f
    override val height = if (baseDragon) 300f else 240f
    private val body = art.body
    private val origin = if (baseDragon) DragonCamera.topLeft else Offset.Zero
    private val spriteScale = if (baseDragon) DragonCamera.width / 448f else MODEL_SCALE
    private val frame =
        Rect(
            origin.x + body.destination.x * spriteScale - 6f,
            origin.y + body.destination.y * spriteScale - 6f,
            origin.x + (body.destination.x + body.size.width) * spriteScale + 6f,
            if (baseDragon) DragonGround.bottom else height,
        )
    override val groundCenter = if (baseDragon) DragonGround.center else Offset(frame.center.x, 232f)
    private val halfShadow = frame.width * .36f
    private val shadow =
        if (baseDragon) {
            DragonGround
        } else {
            Rect(
                groundCenter.x - halfShadow,
                226f,
                groundCenter.x + halfShadow,
                238f,
            )
        }

    override fun crop(slot: Slot): Rect {
        val radius = maxOf(frame.width, frame.height) / 2f
        return Rect(frame.center, radius)
    }

    override fun full(outfit: Outfit) = if (baseDragon) DragonFullFrame else frame

    override fun DrawScope.draw(
        color: CompanionColor,
        outfit: Outfit,
        pose: CompanionPose,
    ) {
        groundShadow(shadow)
        posedBody(pose, feet = groundCenter) {
            translate(origin.x, origin.y) {
                scale(spriteScale, spriteScale, Offset.Zero) {
                    drawSprite(body)
                    for (eye in art.eyes) {
                        scale(1f, 1f - .94f * pose.blink, Offset(0f, eye.centerY)) { drawSprite(eye) }
                    }
                }
            }
        }
    }

    private fun DrawScope.drawSprite(sprite: ModelSprite) {
        drawImage(
            image = atlas,
            srcOffset = sprite.source,
            srcSize = sprite.size,
            dstOffset = sprite.destination,
            dstSize = sprite.size,
            filterQuality = FilterQuality.Medium,
        )
    }
}
