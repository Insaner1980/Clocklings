package fi.clocklings.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fi.clocklings.R
import fi.clocklings.domain.ClockFrame
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionCelebration
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.companionAspectRatio
import fi.clocklings.ui.components.ChipButton
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkCard
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

@Composable
fun AppearanceScreen(
    appearance: Personalization,
    progress: Progress,
    initiallyClocks: Boolean,
    onOutfits: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var clocks by rememberSaveable { mutableStateOf(initiallyClocks) }
    ScreenScaffold(modifier) {
        TopBar(stringResource(R.string.appearance_title), stringResource(R.string.back), onBack)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
            ChipButton(stringResource(R.string.companions_title), { clocks = false }, selected = !clocks)
            ChipButton(stringResource(R.string.clock_styles_title), { clocks = true }, selected = clocks)
        }
        if (clocks) ClockChoices(appearance) else CompanionChoices(appearance, progress, onOutfits)
    }
}

@Composable
private fun CompanionChoices(
    appearance: Personalization,
    progress: Progress,
    onOutfits: () -> Unit,
) {
    val app = LocalAppViewModel.current
    val kind = appearance.companion
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapL)) {
        Text(stringResource(R.string.companion_choice_hint), style = SkType.Body)
        if (kind != CompanionKind.NONE) {
            SkCard(Modifier.fillMaxWidth().testTag("companion_preview")) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val greeting = remember(kind) { CompanionCelebration(newOutfit = null, jump = true) }
                    CompanionView(
                        kind,
                        progress.outfitFor(kind),
                        Modifier.height(SkDimens.CompanionPreview).aspectRatio(companionAspectRatio(kind)),
                        celebration = greeting,
                    )
                    Text(stringResource(companionName(kind)), style = SkType.SectionTitle)
                }
            }
        }
        ChoiceGrid(CompanionKind.entries) { option, cell ->
            ChoiceCard(
                stringResource(companionName(option)),
                kind == option,
                { app.personalize { it.copy(companion = option) } },
                cell.testTag("choose_companion_${option.name}"),
            ) {
                if (option == CompanionKind.NONE) {
                    ClockFace(ClockCommand(PREVIEW_TIME), Modifier.size(96.dp), mini = true)
                } else {
                    CompanionView(
                        option,
                        progress.outfitFor(option),
                        Modifier.height(SkDimens.CompanionChoiceArt).aspectRatio(companionAspectRatio(option)),
                        animated = false,
                    )
                }
            }
        }
        if (kind == CompanionKind.NONE) {
            Text(stringResource(R.string.no_companion_hint), style = SkType.BodySoft)
        } else {
            SkButton(stringResource(R.string.dress_doll), onOutfits, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ClockChoices(appearance: Personalization) {
    val app = LocalAppViewModel.current
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapL)) {
        Text(stringResource(R.string.clock_choice_hint), style = SkType.Body)
        ClockFace(
            ClockCommand(PREVIEW_TIME),
            Modifier.fillMaxWidth().testTag("selected_clock"),
            appearance = appearance.clock,
        )
        ChoiceGrid(ClockTheme.entries) { theme, cell ->
            ChoiceCard(
                stringResource(clockThemeName(theme)),
                appearance.clock.theme == theme,
                { app.personalize { it.copy(clock = it.clock.copy(theme = theme)) } },
                cell.testTag("choose_clock_${theme.name}"),
            ) {
                ClockFace(
                    ClockCommand(PREVIEW_TIME),
                    Modifier.size(110.dp),
                    mini = true,
                    appearance = appearance.clock.copy(theme = theme),
                )
            }
        }
        if (appearance.clock.theme == ClockTheme.CUSTOM) {
            CustomClockControls(appearance.clock.custom) { transform ->
                app.personalize { it.copy(clock = it.clock.copy(custom = transform(it.clock.custom))) }
            }
        }
    }
}

@Composable
private fun CustomClockControls(
    custom: CustomClock,
    onChange: ((CustomClock) -> CustomClock) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapL)) {
        Text(stringResource(R.string.custom_clock_hint), style = SkType.BodySoft)
        ChoiceChips(
            R.string.clock_color,
            ClockPalette.entries,
            custom.palette,
            { value -> onChange { it.copy(palette = value) } },
            {
                when (it) {
                    ClockPalette.BLUE -> R.string.color_blue
                    ClockPalette.GREEN -> R.string.color_green
                    ClockPalette.PURPLE -> R.string.color_purple
                    ClockPalette.CORAL -> R.string.color_coral
                }
            },
        )
        ChoiceChips(
            R.string.clock_frame,
            ClockFrame.entries,
            custom.frame,
            { value -> onChange { it.copy(frame = value) } },
            {
                when (it) {
                    ClockFrame.ROUND -> R.string.frame_round
                    ClockFrame.SCALLOPED -> R.string.frame_scalloped
                    ClockFrame.BLOCKS -> R.string.frame_blocks
                }
            },
        )
        ChoiceChips(
            R.string.clock_numerals,
            NumeralStyle.entries,
            custom.numerals,
            { value -> onChange { it.copy(numerals = value) } },
            { if (it == NumeralStyle.ROUNDED) R.string.numerals_rounded else R.string.numerals_plain },
        )
        ChoiceChips(
            R.string.clock_hands,
            HandStyle.entries,
            custom.hands,
            { value -> onChange { it.copy(hands = value) } },
            { if (it == HandStyle.ROUND) R.string.hands_round else R.string.hands_pointer },
        )
        ClockFace(
            ClockCommand(PREVIEW_TIME),
            Modifier.fillMaxWidth(),
            appearance =
                LocalPersonalization.current.clock.copy(theme = ClockTheme.CUSTOM, custom = custom),
        )
    }
}

@Composable
private fun <T> ChoiceChips(
    title: Int,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapS)) {
        Text(stringResource(title), style = SkType.SectionTitle, modifier = Modifier.semantics { heading() })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        ) {
            for (option in options) {
                ChipButton(
                    stringResource(label(option)),
                    { onSelect(option) },
                    selected =
                        option == selected,
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceGrid(
    options: List<T>,
    content: @Composable (T, Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 320.dp || LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            for (row in options.chunked(columns)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
                    for (option in row) content(option, Modifier.weight(1f))
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
) {
    Column(
        modifier
            .semantics { selected = on }
            .background(if (on) SkColors.ItemOnBg else SkColors.Surface, SkShapes.Item)
            .border(SkDimens.Border, if (on) SkColors.ItemOnBorder else SkColors.Line, SkShapes.Item)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(SkDimens.GapM),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        Column(Modifier.clearAndSetSemantics { }) { art() }
        WordSafeText(title, SkType.ItemName)
        if (on) Text(stringResource(R.string.appearance_selected), style = SkType.Small)
    }
}

private const val PREVIEW_TIME = 610
