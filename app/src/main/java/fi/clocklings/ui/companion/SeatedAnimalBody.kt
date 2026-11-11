package fi.clocklings.ui.companion

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import fi.clocklings.ui.art.VinylShape
import fi.clocklings.ui.art.VinylTones
import fi.clocklings.ui.art.contactShadow
import fi.clocklings.ui.art.unitOval
import fi.clocklings.ui.art.vinylBrush
import fi.clocklings.ui.art.vinylShape

/** Shared body layers for the seated cat and dog, with separately measured outlines. */
internal class SeatedAnimalBody(
    private val outline: Path,
    private val left: Leg,
    private val right: Leg,
    private val chest: VinylShape,
    private val chinShadow: Rect,
) {
    class Leg(
        val haunch: Rect,
        val thigh: Path,
        val backPaw: Path,
        val frontPaw: Path,
        val backShadow: Rect,
        val frontShadow: Rect,
    )

    inner class Paint(
        tones: VinylTones,
        private val dot: Brush,
    ) {
        private val body = vinylBrush(outline, tones)
        private val thighLeft = vinylBrush(left.thigh, tones)
        private val thighRight = vinylBrush(right.thigh, tones)
        private val backPawLeft = vinylBrush(left.backPaw, tones)
        private val backPawRight = vinylBrush(right.backPaw, tones)
        private val frontPawLeft = vinylBrush(left.frontPaw, tones)
        private val frontPawRight = vinylBrush(right.frontPaw, tones)

        fun draw(
            scope: DrawScope,
            includeFrontPaws: Boolean,
        ) = with(scope) {
            drawPath(outline, body)
            clipPath(outline) {
                unitOval(left.haunch, dot)
                unitOval(right.haunch, dot)
                contactShadow(left.backShadow)
                contactShadow(right.backShadow)
                drawPath(left.backPaw, backPawLeft)
                drawPath(right.backPaw, backPawRight)
                drawPath(left.thigh, thighLeft)
                drawPath(right.thigh, thighRight)
                vinylShape(chest)
                contactShadow(chinShadow)
                if (includeFrontPaws) frontPaws()
            }
        }

        // The cat draws the object it is holding before drawing its front paws over it.
        fun drawFrontPaws(scope: DrawScope) = with(scope) { clipPath(outline) { frontPaws() } }

        private fun DrawScope.frontPaws() {
            contactShadow(left.frontShadow)
            contactShadow(right.frontShadow)
            drawPath(left.frontPaw, frontPawLeft)
            drawPath(right.frontPaw, frontPawRight)
        }
    }
}
