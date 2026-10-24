package fi.clocklings.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Wardrobe
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.BigMenuButton
import fi.clocklings.ui.components.ChipButton
import fi.clocklings.ui.components.Eyebrow
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SegmentedControl
import fi.clocklings.ui.components.SkCard
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.StarIcon
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    progress: Progress,
    settings: Settings,
    onOpen: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    ScreenScaffold(modifier) {
        Hero()
        NowCard(settings.h24)
        if (settings.personalization.companion != CompanionKind.NONE) {
            DollCard(progress) { onOpen(HomeDestination.NUKKE) }
        }
        Menu(onOpen)
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            SegmentedControl(
                label = stringResource(R.string.mode_label),
                options = listOf(stringResource(R.string.mode_12), stringResource(R.string.mode_24)),
                selectedIndex = if (settings.h24) 1 else 0,
                onSelect = {
                    app.setH24(it == 1)
                    app.play(Sound.POP)
                },
            )
            ChipButton(
                stringResource(if (settings.sound) R.string.sound_on else R.string.sound_off),
                app::toggleSound,
                selected = settings.sound,
            )
            ResetChip(onReset = app::resetProgress)
            ChipButton(stringResource(R.string.clock_styles_title), { onOpen(HomeDestination.CLOCK_STYLES) })
        }
    }
}

/** Sateenkaarikaaret ja monivärinen otsikko (web: .hero ja .logo). */
@Composable
private fun Hero() {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.width(SkDimens.HeroLogo).aspectRatio(HERO_W / HERO_H).clearAndSetSemantics { }) {
            val s = size.width / HERO_W
            SkColors.HeroArcs.forEachIndexed { i, color ->
                val r = (RAINBOW_RADIUS - i * RAINBOW_GAP) * s
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(RAINBOW_CENTER_X * s - r, RAINBOW_CENTER_Y * s - r),
                    size = Size(2 * r, 2 * r),
                    style = Stroke(RAINBOW_STROKE * s, cap = StrokeCap.Round),
                )
            }
        }
        val title = stringResource(R.string.app_name)
        Text(
            buildAnnotatedString {
                title.forEachIndexed {
                    i,
                    c,
                    ->
                    withStyle(SpanStyle(color = SkColors.logoColor(i))) { append(c) }
                }
            },
            style = SkType.Logo,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.semantics {
                    heading()
                    contentDescription = title
                },
        )
        Text(stringResource(R.string.tagline), style = SkType.Tagline, textAlign = TextAlign.Center)
    }
}

/** Oikean ajan kello, päivittyy 10 sekunnin välein (web: tickNow). */
@Composable
private fun NowCard(h24: Boolean) {
    var now by remember { mutableIntStateOf(ClockTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(NOW_TICK_MS)
            now = ClockTime.now()
        }
    }
    val t = if (h24) now else now % ClockTime.HALF_DAY
    SkCard(Modifier.fillMaxWidth()) {
        HomeCardContent {
            ClockFace(command = ClockCommand(t), mini = true, modifier = Modifier.size(SkDimens.MiniClockHome))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(stringResource(R.string.now_eyebrow))
                Text(timeLabel(t, h24), style = SkType.NowWords)
            }
        }
    }
}

@Composable
private fun DollCard(
    progress: Progress,
    onClick: () -> Unit,
) {
    SkCard(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        HomeCardContent {
            val kind = LocalPersonalization.current.companion
            CompanionView(
                kind,
                progress.outfitFor(kind),
                Modifier.width(SkDimens.DollMini).aspectRatio(companionAspectRatio(kind)),
            )
            Column(Modifier.weight(1f)) {
                Eyebrow(progress.nameFor(kind).ifEmpty { stringResource(R.string.doll_default_name) })
                Row(
                    Modifier.padding(top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
                ) {
                    StarIcon(true, size = SkDimens.StarCountIcon)
                    Text(
                        stringResource(R.string.home_star_total, progress.totalStars, Wardrobe.MAX_STARS),
                        style = SkType.StarCount,
                    )
                }
                Text(
                    stringResource(R.string.home_doll_hint),
                    style = SkType.BodySoft,
                )
            }
        }
    }
}

@Composable
private fun HomeCardContent(content: @Composable FlowRowScope.() -> Unit) {
    BoxWithConstraints {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapL, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapL),
            maxItemsInEachRow =
                if (maxWidth < 320.dp || LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE) 1 else 2,
            content = content,
        )
    }
}

@Composable
private fun Menu(onOpen: (HomeDestination) -> Unit) {
    val gap = 14.dp
    BoxWithConstraints {
        val columns = if (maxWidth < 320.dp || LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE) 1 else 2
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
            LearnMenuButton { onOpen(HomeDestination.KOULU) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                maxItemsInEachRow = columns,
            ) {
                BigMenuButton(
                    stringResource(R.string.menu_vapaa),
                    stringResource(R.string.menu_vapaa_sub),
                    SkIcons.Clock,
                    SkColors.VapaaFill,
                    SkColors.VapaaShadow,
                    { onOpen(HomeDestination.VAPAA) },
                    Modifier.weight(1f),
                )
                BigMenuButton(
                    stringResource(R.string.menu_pelaa),
                    stringResource(R.string.menu_pelaa_sub),
                    SkIcons.Star,
                    SkColors.PelaaFill,
                    SkColors.PelaaShadow,
                    { onOpen(HomeDestination.PELAA) },
                    Modifier.weight(1f),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                maxItemsInEachRow = columns,
            ) {
                BigMenuButton(
                    stringResource(R.string.menu_kysy),
                    stringResource(R.string.menu_kysy_sub),
                    SkIcons.Speech,
                    SkColors.KysyFill,
                    SkColors.KysyShadow,
                    { onOpen(HomeDestination.KYSY) },
                    Modifier.weight(1f),
                )
                BigMenuButton(
                    stringResource(R.string.menu_nukke),
                    stringResource(R.string.home_companion_choices),
                    SkIcons.Dress,
                    SkColors.NukkeFill,
                    SkColors.NukkeShadow,
                    { onOpen(HomeDestination.NUKKE) },
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LearnMenuButton(onClick: () -> Unit) {
    BigMenuButton(
        stringResource(R.string.menu_koulu),
        stringResource(R.string.menu_koulu_sub),
        SkIcons.Book,
        SkColors.KouluFill,
        SkColors.KouluShadow,
        onClick,
        Modifier.fillMaxWidth(),
    )
}

/** Kaksivaiheinen nollaus: ensimmäinen painallus kysyy varmistusta 5 sekunnin ajan (web: resetBtn). */
@Composable
private fun ResetChip(onReset: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(RESET_TIMEOUT_MS)
            armed = false
        }
    }
    ChipButton(stringResource(if (armed) R.string.reset_confirm else R.string.reset), {
        if (armed) {
            armed = false
            onReset()
        } else {
            armed = true
        }
    })
}

private const val HERO_W = 120f
private const val HERO_H = 66f
private const val NOW_TICK_MS = 10_000L
private const val RESET_TIMEOUT_MS = 5_000L

private const val RAINBOW_RADIUS = 54
private const val RAINBOW_GAP = 8
private const val RAINBOW_CENTER_X = 60
private const val RAINBOW_CENTER_Y = 62
private const val RAINBOW_STROKE = 7
