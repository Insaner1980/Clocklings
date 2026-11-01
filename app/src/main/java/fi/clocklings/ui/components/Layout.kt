package fi.clocklings.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import fi.clocklings.R
import fi.clocklings.ui.SkMotion
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.theme.sectionBackground

/** Icon navigation with a separate title row when the available space is narrow. */
@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabel: String = stringResource(R.string.back_description),
    trailing: @Composable (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < SkDimens.GridBreakpoint || LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE) {
            Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                ) {
                    RoundIconButton(SkIcons.Back, backLabel, onBack)
                    trailing?.invoke()
                }
                WordSafeText(title, SkType.Title, Modifier.fillMaxWidth().semantics { heading() })
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) {
                RoundIconButton(SkIcons.Back, backLabel, onBack)
                WordSafeText(title, SkType.Title, Modifier.weight(1f).semantics { heading() })
                if (trailing != null) trailing() else Box(Modifier.size(SkDimens.IconButton))
            }
        }
    }
}

fun adaptiveGridColumns(
    availableWidth: Dp,
    fontScale: Float,
): Int =
    if (availableWidth < SkDimens.GridBreakpoint || fontScale > SkDimens.MAX_ROW_FONT_SCALE) {
        1
    } else {
        2
    }

/**
 * Näkymän runko: reunasta reunaan, turva-alueet huomioiden, 16 dp:n sivumarginaali.
 * [topBar] stays pinned while the content below it scrolls; scrolled content fades out under the bar
 * instead of being cut off. With [scroll] false the content gets the exact remaining height and
 * arranges its own overflow. The clock size is capped by the content width and [clockHeightShare] of its height.
 * [clockFit] can also cap it so that the whole content fits without scrolling (see [ClockFit]). With
 * [animateClock] size changes are animated. A new [scrollState] opens the content at the top.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    section: Section = Section.HOME,
    topBar: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null,
    clockHeightShare: Float = SkDimens.CLOCK_HEIGHT_SHARE,
    clockFit: ClockFit = ClockFit.NONE,
    animateClock: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.(clockSize: Dp) -> Unit,
) {
    // The content height without the clock, measured in the last layout while fitting.
    var restHeight by remember { mutableStateOf<Int?>(null) }
    Column(
        modifier
            .fillMaxSize()
            .sectionBackground(section)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        if (topBar != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(section.color)
                    .padding(start = SkDimens.Gutter, end = SkDimens.Gutter, top = SkDimens.GapS),
            ) {
                topBar()
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val baseClockSize = min(maxWidth - SkDimens.Gutter * 2, maxHeight * clockHeightShare)
            val density = LocalDensity.current
            val rest = restHeight?.let { with(density) { it.toDp() } }
            val clockSize = scaffoldClockSize(baseClockSize, maxHeight, clockFit, rest, animateClock)
            val bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
            val measure = clockFit == ClockFit.FIT
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (scroll) Modifier.verticalScroll(scrollState) else Modifier)
                    .then(if (measure) Modifier.measureRestHeight(clockSize) { restHeight = it } else Modifier)
                    .padding(
                        start = SkDimens.Gutter,
                        end = SkDimens.Gutter,
                        top = SkDimens.GapScreen,
                        bottom = (if (bottomBar == null) bottom else 0.dp) + SkDimens.GapS,
                    ),
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen),
            ) { content(clockSize) }
            if (scroll && scrollState.canScrollBackward) {
                ScaffoldTopFade(section)
            }
            if (bottomBar != null) {
                ScaffoldBottomFade(section)
            } else if (scroll && scrollState.canScrollForward) {
                ScaffoldBottomFade(section, bottom)
            }
        }
        if (bottomBar != null) {
            ScaffoldBottomBar(section, bottomBar)
        }
    }
}

/**
 * Reports the natural content height, not stretched to the viewport, without the [clockSize] it shows;
 * the layout itself is unchanged.
 */
private fun Modifier.measureRestHeight(
    clockSize: Dp,
    onRestHeight: (Int) -> Unit,
) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    onRestHeight(placeable.height - clockSize.roundToPx())
    layout(placeable.width, maxOf(placeable.height, constraints.minHeight)) { placeable.place(0, 0) }
}

/** How [ScreenScaffold] fits its clock to the content below it. */
enum class ClockFit {
    /** The clock takes its full size. */
    NONE,

    /**
     * The content is measured and the clock capped so that all of it fits without scrolling, but never below
     * [SkDimens.FitClockMin]. The content must show the clock once, at exactly the given size.
     */
    FIT,

    /**
     * The size set last stays, whatever the viewport, so the clock keeps its size while other content and a
     * bottom bar show. A new screen starts at the full size.
     */
    KEEP,
}

/** The clock size set while it is still unknown how much room the content leaves for it. */
private class HeldClockSize {
    var size: Dp? = null
}

/**
 * [base], or under [ClockFit.FIT] at most the [viewport] height left after [rest]. Until [rest] is first measured,
 * and under [ClockFit.KEEP], the clock stays at its last size, so it never shows the full size for a frame before
 * fitting. With [animate] a change takes [SkMotion.RESIZE_DURATION], or snaps with reduced motion.
 */
@Composable
private fun scaffoldClockSize(
    base: Dp,
    viewport: Dp,
    fit: ClockFit,
    rest: Dp?,
    animate: Boolean,
): Dp {
    val held = remember { HeldClockSize() }
    val target =
        when {
            fit == ClockFit.NONE -> base
            fit == ClockFit.KEEP -> held.size ?: base
            rest != null -> min(base, max(SkDimens.FitClockMin, viewport - rest))
            else -> held.size ?: base
        }
    SideEffect { held.size = target }
    if (!animate) return target
    val reduced = rememberReduceMotion()
    val shown by animateDpAsState(
        target,
        if (reduced) snap() else tween(SkMotion.RESIZE_DURATION),
        label = "clockSize",
    )
    return shown
}

@Composable
private fun BoxScope.ScaffoldBottomFade(
    section: Section,
    bottom: Dp = 0.dp,
) {
    // Content still below fades out above the system navigation bar instead of running
    // under its buttons, as it does under the top bar.
    val fade = SkDimens.ScrollFade
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(bottom + fade)
            .background(
                Brush.verticalGradient(
                    0f to section.bottomColor.copy(alpha = 0f),
                    fade / (bottom + fade) to section.bottomColor,
                ),
            ),
    )
}

@Composable
private fun ScaffoldBottomBar(
    section: Section,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(section.bottomColor)
            .padding(horizontal = SkDimens.Gutter)
            .padding(
                top = SkDimens.GapS,
                bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + SkDimens.GapS,
            ),
    ) { content() }
}

@Composable
private fun ScaffoldTopFade(section: Section) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SkDimens.ScrollFadeTop)
            .background(
                Brush.verticalGradient(
                    0f to section.color,
                    SkDimens.SCROLL_FADE_TOP_SOLID to section.color,
                    1f to section.color.copy(alpha = 0f),
                ),
            ),
    )
}
