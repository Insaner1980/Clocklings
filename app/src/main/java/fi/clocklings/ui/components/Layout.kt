package fi.clocklings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import fi.clocklings.R
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
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    section: Section = Section.HOME,
    topBar: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null,
    clockHeightShare: Float = SkDimens.CLOCK_HEIGHT_SHARE,
    content: @Composable ColumnScope.(clockSize: Dp) -> Unit,
) {
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
            val clockSize = min(maxWidth - SkDimens.Gutter * 2, maxHeight * clockHeightShare)
            val bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (scroll) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(
                        start = SkDimens.Gutter,
                        end = SkDimens.Gutter,
                        top = SkDimens.GapScreen,
                        bottom = (if (bottomBar == null) bottom else 0.dp) + SkDimens.GapS,
                    ),
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen),
            ) { content(clockSize) }
            if (scroll && scrollState.canScrollBackward) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(SkDimens.ScrollFade)
                        .background(Brush.verticalGradient(listOf(section.color, section.color.copy(alpha = 0f)))),
                )
            }
            if (bottomBar != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(SkDimens.ScrollFade)
                        .background(
                            Brush.verticalGradient(listOf(section.bottomColor.copy(alpha = 0f), section.bottomColor)),
                        ),
                )
            }
        }
        if (bottomBar != null) {
            ScaffoldBottomBar(section, bottomBar)
        }
    }
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

/**
 * Keskitetty teksti, joka ei katkea kesken sanan: fonttia pienennetään niin, että pisin sana
 * mahtuu riville (esim. "Tasatunnit" tai "Sateenkaarihiukset" kapealla näytöllä).
 */
@Composable
fun WordSafeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    minScale: Float = MIN_SCALE,
    textAlign: TextAlign = TextAlign.Center,
) {
    BasicText(
        text = text,
        style = style.merge(TextStyle(textAlign = textAlign)),
        modifier = modifier,
        autoSize = WordSafeAutoSize(style.fontSize.value, style.fontSize.value * minScale),
    )
}

/** Suurin fonttikoko, jolla mikään rivinvaihto ei osu sanan keskelle. Ei käytä alikoostamista. */
private data class WordSafeAutoSize(
    val maxSp: Float,
    val minSp: Float,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        var size = maxSp
        while (size > minSp) {
            val layout = performLayout(constraints, text, size.sp)
            if (!layout.didOverflowWidth && !layout.breaksInsideWord(text.text)) return size.sp
            size -= STEP_SP
        }
        return minSp.sp
    }

    private fun TextLayoutResult.breaksInsideWord(s: String): Boolean =
        (0 until lineCount - 1).any { line ->
            val end = getLineEnd(line)
            end in 1 until s.length && !s[end - 1].isWhitespace() && s[end - 1] != '-' && !s[end].isWhitespace()
        }

    private companion object {
        const val STEP_SP = 0.5f
    }
}

private const val MIN_SCALE = 0.68f
