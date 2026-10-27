package fi.clocklings.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import fi.clocklings.R
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.BigMenuButton
import fi.clocklings.ui.components.DepthSurface
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.RoundIconButton
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.SpeechBubble
import fi.clocklings.ui.components.adaptiveGridColumns
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.sharedClocklingsElement
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel
import kotlinx.coroutines.delay

/**
 * Home fills the screen: a header row, a companion stage that takes all the free height, and the
 * activity grid at the bottom within thumb reach. Only when the header and grid leave less than
 * [SkDimens.StageMinHeight] (narrow screens, large text) does the page scroll.
 */
@Composable
fun HomeScreen(
    progress: Progress,
    settings: Settings,
    onOpen: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableIntStateOf(ClockTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(NOW_TICK_MS)
            now = ClockTime.now()
        }
    }
    val time = if (settings.h24) now else now % ClockTime.HALF_DAY
    ScreenScaffold(modifier, section = Section.HOME, scroll = false) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val viewport = maxHeight
            val compact = adaptiveGridColumns(maxWidth, LocalDensity.current.fontScale) == 1
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                FillHeightLayout(
                    viewport = viewport,
                    stageMinHeight = if (compact) SkDimens.StageMinHeightCompact else SkDimens.StageMinHeight,
                    header = { HomeHeader(progress, onOpen) },
                    stage = { HomeStage(progress, settings, time, compact, onOpen) },
                    footer = { Menu(onOpen) },
                )
            }
        }
    }
}

/**
 * Header, flexible stage and footer: the stage gets whatever height is left in [viewport],
 * but never less than [SkDimens.StageMinHeight].
 */
@Composable
private fun FillHeightLayout(
    viewport: Dp,
    stageMinHeight: Dp,
    header: @Composable () -> Unit,
    stage: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Layout(contents = listOf(header, stage, footer)) { (headerM, stageM, footerM), constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val gap = SkDimens.Gap.roundToPx()
        val headerP = headerM.map { it.measure(loose) }
        val footerP = footerM.map { it.measure(loose) }
        // The footer tiles' ledges draw below their bounds; reserve that room so scrolling never clips them.
        val fixed =
            headerP.sumOf { it.height } + footerP.sumOf { it.height } + gap * 2 + SkDimens.BigButtonDepth.roundToPx()
        val stageHeight =
            maxOf(with(density) { viewport.roundToPx() } - fixed, stageMinHeight.roundToPx())
        val contentHeight = stageHeight.coerceAtMost(maxOf(stageMinHeight, SkDimens.HomeStageMaxHeight).roundToPx())
        val stageP = stageM.map { it.measure(Constraints.fixed(width, contentHeight)) }
        layout(width, fixed + stageHeight) {
            var y = 0
            headerP.forEach {
                it.place(0, y)
                y += it.height
            }
            y += gap
            stageP.forEach { it.place(0, y) }
            y += stageHeight + gap
            footerP.forEach { it.place(0, y) }
        }
    }
}

@Composable
private fun HomeHeader(
    progress: Progress,
    onOpen: (HomeDestination) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        val title = stringResource(R.string.app_name)
        // Shrinks to fit on narrow screens and with large text instead of cutting the name off.
        BasicText(
            buildAnnotatedString {
                title.forEachIndexed { i, letter ->
                    withStyle(SpanStyle(color = SkColors.logoColor(i))) { append(letter) }
                }
            },
            style = SkType.HomeLogo,
            maxLines = 1,
            // The minimum is in dp so that large system text cannot push it past the space it has.
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = with(LocalDensity.current) { SkDimens.LogoMinSize.toSp() },
                    maxFontSize = SkType.HomeLogo.fontSize,
                ),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Pill(
            stringResource(R.string.home_star_total, progress.totalStars, Wardrobe.MAX_STARS),
            icon = SkIcons.StarOn,
            large = true,
        )
        RoundIconButton(
            SkIcons.Settings,
            stringResource(R.string.settings_title),
            { onOpen(HomeDestination.SETTINGS) },
        )
    }
}

/**
 * The companion stands on a soft ground and tells the time in a speech bubble beside the live
 * clock. Tapping the stage opens the companion choices. Without a companion the clock takes the
 * stage alone.
 */
@Composable
private fun HomeStage(
    progress: Progress,
    settings: Settings,
    time: Int,
    compact: Boolean,
    onOpen: (HomeDestination) -> Unit,
) {
    val kind = settings.personalization.companion
    val description = "${stringResource(R.string.menu_nukke)}. ${stringResource(R.string.home_companion_choices)}"
    val words = timeLabel(LocalAppLanguage.current, time, settings.h24)
    DepthSurface(
        { onOpen(HomeDestination.NUKKE) },
        Color.Transparent,
        Color.Transparent,
        SkShapes.Card,
        Modifier.fillMaxSize().testTag("companion_stage").semantics { contentDescription = description },
        depth = 0.dp,
        pressedDepth = 0.dp,
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().drawBehind {
                drawCloud(Offset(size.width * CLOUD_A_X, size.height * CLOUD_A_Y), size.width * CLOUD_A_SIZE)
                drawCloud(Offset(size.width * CLOUD_B_X, size.height * CLOUD_B_Y), size.width * CLOUD_B_SIZE)
                // A gentle hill for the companion and the clock to stand on.
                val groundHeight = size.height * GROUND_HEIGHT
                drawOval(
                    SkColors.HomeGround,
                    Offset(-size.width * GROUND_OVERHANG, size.height - groundHeight),
                    Size(size.width * (1 + 2 * GROUND_OVERHANG), groundHeight * 2),
                )
            },
        ) {
            if (kind == CompanionKind.NONE) {
                ClockOnlyStage(words, time, maxWidth, maxHeight)
            } else if (compact) {
                CompactCompanionStage(kind, progress, words, time, maxWidth, onOpen)
            } else {
                CompanionStage(kind, progress, words, time, maxWidth, maxHeight, onOpen)
            }
        }
    }
}

/** Without a companion the live clock takes the stage, with the time in words above it. */
@Composable
private fun ClockOnlyStage(
    words: String,
    time: Int,
    stageWidth: Dp,
    stageHeight: Dp,
) {
    Column(
        Modifier.fillMaxSize().padding(bottom = SkDimens.GapM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.Bottom),
    ) {
        SpeechBubble(
            words,
            Modifier.widthIn(max = stageWidth),
            style = SkType.NowWords,
            fill = false,
            color = SkColors.Surface,
        )
        ClockFace(
            ClockCommand(time),
            Modifier.sharedClocklingsElement("clock").size(min(stageWidth, stageHeight * NONE_CLOCK_SHARE)),
            mini = true,
        )
    }
}

/**
 * Narrow screens and large text: the bubble gets the full width on top, with the companion and the
 * clock side by side below it.
 */
@Composable
private fun CompactCompanionStage(
    kind: CompanionKind,
    progress: Progress,
    words: String,
    time: Int,
    stageWidth: Dp,
    onOpen: (HomeDestination) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(SkDimens.GapS),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        SpeechBubble(words, style = SkType.NowWords, color = SkColors.Surface)
        Row(
            Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HomeCompanion(
                kind,
                progress,
                onOpen,
                Modifier
                    .sharedClocklingsElement("companion")
                    .fillMaxHeight()
                    .aspectRatio(companionAspectRatio(kind), matchHeightConstraintsFirst = true),
            )
            ClockFace(
                ClockCommand(time),
                Modifier.sharedClocklingsElement("clock").size(stageWidth * CLOCK_WIDTH_SHARE),
                mini = true,
            )
        }
    }
}

/** The companion stands on the left; the speech bubble and the live clock stand to its right. */
@Composable
private fun BoxScope.CompanionStage(
    kind: CompanionKind,
    progress: Progress,
    words: String,
    time: Int,
    stageWidth: Dp,
    stageHeight: Dp,
    onOpen: (HomeDestination) -> Unit,
) {
    val companionHeight =
        min(
            stageHeight * COMPANION_HEIGHT_SHARE,
            stageWidth * COMPANION_WIDTH_SHARE / companionAspectRatio(kind),
        )
    val greeting = remember(kind) { CompanionCelebration(newOutfit = null, jump = true) }
    Box(
        Modifier
            .align(Alignment.BottomStart)
            .padding(start = SkDimens.GapS, bottom = SkDimens.GapS)
            .sharedClocklingsElement("companion")
            .height(companionHeight)
            .width(companionHeight * companionAspectRatio(kind))
            .drawBehind {
                drawOval(
                    SkColors.StageShadow,
                    Offset(size.width * SHADOW_X, size.height * SHADOW_Y),
                    Size(size.width * SHADOW_WIDTH, size.height * SHADOW_HEIGHT),
                )
            },
    ) {
        HomeCompanion(kind, progress, onOpen, Modifier.fillMaxSize(), greeting)
    }
    Column(
        Modifier
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .width(stageWidth * RIGHT_COLUMN_SHARE)
            .padding(top = SkDimens.GapS, bottom = SkDimens.GapM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.Bottom),
    ) {
        SpeechBubble(words, style = SkType.NowWords, fill = false, color = SkColors.Surface)
        ClockFace(
            ClockCommand(time),
            Modifier.sharedClocklingsElement("clock").size(
                min(stageWidth * CLOCK_WIDTH_SHARE, stageHeight * CLOCK_HEIGHT_SHARE),
            ),
            mini = true,
        )
    }
}

/** A soft three-puff cloud centred on [center], [width] wide. */
private fun DrawScope.drawCloud(
    center: Offset,
    width: Float,
) {
    val puff = width * CLOUD_PUFF
    drawCircle(SkColors.Cloud, puff, center + Offset(-width * CLOUD_SIDE, puff * CLOUD_DROP))
    drawCircle(SkColors.Cloud, puff * CLOUD_TOP, center)
    drawCircle(SkColors.Cloud, puff, center + Offset(width * CLOUD_SIDE, puff * CLOUD_DROP))
    drawRoundRect(
        SkColors.Cloud,
        topLeft = center + Offset(-width / 2, 0f),
        size = Size(width, puff * 2 * CLOUD_DROP + puff),
        cornerRadius = CornerRadius(puff),
    )
}

@Composable
private fun Menu(onOpen: (HomeDestination) -> Unit) {
    val titles = listOf(R.string.menu_koulu, R.string.menu_vapaa, R.string.menu_pelaa, R.string.menu_kysy)
    val descriptions =
        listOf(R.string.menu_koulu_sub, R.string.menu_vapaa_sub, R.string.menu_pelaa_sub, R.string.menu_kysy_sub)
    val icons = listOf(SkIcons.Book, SkIcons.Clock, SkIcons.Star, SkIcons.Together)
    val fills = listOf(SkColors.KouluFill, SkColors.VapaaFill, SkColors.PelaaFill, SkColors.KysyFill)
    val shadows = listOf(SkColors.KouluShadow, SkColors.VapaaShadow, SkColors.PelaaShadow, SkColors.KysyShadow)
    val destinations = listOf(HomeDestination.KOULU, HomeDestination.VAPAA, HomeDestination.PELAA, HomeDestination.KYSY)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = adaptiveGridColumns(maxWidth, LocalDensity.current.fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            titles.indices.chunked(columns).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                    row.forEach { i ->
                        BigMenuButton(
                            stringResource(titles[i]),
                            stringResource(descriptions[i]),
                            icons[i],
                            fills[i],
                            shadows[i],
                            { onOpen(destinations[i]) },
                            Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

private const val NOW_TICK_MS = 10_000L
private const val SHADOW_X = 0.18f
private const val SHADOW_Y = 0.9f
private const val SHADOW_WIDTH = 0.64f
private const val SHADOW_HEIGHT = 0.08f
private const val GROUND_HEIGHT = 0.22f
private const val GROUND_OVERHANG = 0.15f
private const val COMPANION_HEIGHT_SHARE = 0.88f
private const val COMPANION_WIDTH_SHARE = 0.52f
private const val RIGHT_COLUMN_SHARE = 0.46f
private const val CLOCK_WIDTH_SHARE = 0.4f
private const val CLOCK_HEIGHT_SHARE = 0.52f
private const val NONE_CLOCK_SHARE = 0.6f
private const val CLOUD_A_X = 0.22f
private const val CLOUD_A_Y = 0.12f
private const val CLOUD_A_SIZE = 0.26f
private const val CLOUD_B_X = 0.7f
private const val CLOUD_B_Y = 0.3f
private const val CLOUD_B_SIZE = 0.18f
private const val CLOUD_PUFF = 0.22f
private const val CLOUD_TOP = 1.35f
private const val CLOUD_SIDE = 0.24f
private const val CLOUD_DROP = 0.35f

@Composable
private fun HomeCompanion(
    kind: CompanionKind,
    progress: Progress,
    onOpen: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
    celebration: CompanionCelebration? = null,
) {
    Box(modifier) {
        CompanionView(kind, progress.outfitFor(kind), Modifier.fillMaxSize(), celebration = celebration)
        RoundIconButton(
            SkIcons.Hanger,
            stringResource(R.string.menu_nukke),
            { onOpen(HomeDestination.NUKKE) },
            Modifier.align(Alignment.BottomEnd),
        )
    }
}
