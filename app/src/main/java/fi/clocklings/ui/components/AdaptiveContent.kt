package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import fi.clocklings.ui.theme.SkDimens

fun adaptiveGridColumns(
    availableWidth: Dp,
    fontScale: Float,
): Int =
    if (availableWidth < SkDimens.GridBreakpoint || fontScale > SkDimens.MAX_ROW_FONT_SCALE) {
        1
    } else {
        2
    }

/** The available content width, including room for enlarged control labels, determines the arrangement. */
internal fun hasSideBySideRoom(
    width: Dp,
    fontScale: Float,
): Boolean =
    width >= SkDimens.SideBySideWidth &&
        width >= SkDimens.GridBreakpoint + SkDimens.ControlsMinWidth * fontScale + SkDimens.GapL

internal fun visualPaneWidth(
    width: Dp,
    fontScale: Float,
): Dp {
    val paneWidth = width - SkDimens.GapL
    val preferred = min(SkDimens.VisualPaneWidth, max(SkDimens.GridBreakpoint, paneWidth * VISUAL_WIDTH_SHARE))
    return min(preferred, paneWidth - SkDimens.ControlsMinWidth * fontScale)
}

/**
 * Repositions two parts of the same task without replacing their composition, focus or editor state.
 * Both parts share the screen's saved scroll position and remain reachable in short windows.
 */
@Composable
fun AdaptiveContent(
    clockSize: Dp,
    visual: @Composable (Dp) -> Unit,
    modifier: Modifier = Modifier,
    controlsAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val sideBySide = hasSideBySideRoom(maxWidth, fontScale)
        val visualWidth = if (sideBySide) visualPaneWidth(maxWidth, fontScale) else maxWidth
        Layout(
            contents =
                listOf(
                    {
                        Column(
                            Modifier.fillMaxWidth().testTag("adaptive_visual"),
                            verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen),
                        ) { visual(min(clockSize, visualWidth)) }
                    },
                    {
                        Column(
                            Modifier.fillMaxWidth().testTag("adaptive_controls"),
                            verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen),
                            content = content,
                        )
                    },
                ),
        ) { (visualItems, controlsItems), constraints ->
            val width = constraints.maxWidth
            val gap = (if (sideBySide) SkDimens.GapL else SkDimens.GapScreen).roundToPx()
            val firstWidth = if (sideBySide) visualWidth.roundToPx() else width
            val secondWidth = if (sideBySide) width - firstWidth - gap else width
            val first = visualItems.single().measure(Constraints.fixedWidth(firstWidth))
            val second = controlsItems.single().measure(Constraints.fixedWidth(secondWidth))
            val height = if (sideBySide) maxOf(first.height, second.height) else first.height + gap + second.height
            layout(width, height) {
                first.placeRelative(0, 0)
                second.placeRelative(
                    if (sideBySide) firstWidth + gap else 0,
                    if (sideBySide) controlsAlignment.align(second.height, height) else first.height + gap,
                )
            }
        }
    }
}

private const val VISUAL_WIDTH_SHARE = 0.43f
