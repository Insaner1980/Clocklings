package fi.clocklings.ui.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
import fi.clocklings.ui.components.Eyebrow
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.OptionButton
import fi.clocklings.ui.components.OptionState
import fi.clocklings.ui.components.Panel
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.ReadOptions
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel

@Composable
fun ChallengeRoute(
    h24: Boolean,
    help: Boolean,
    onBack: () -> Unit,
    app: AppViewModel = LocalAppViewModel.current,
    vm: ChallengeViewModel =
        viewModel(factory = viewModelFactory { initializer { ChallengeViewModel(h24, app.services) } }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ChallengeScreen(
        state = state,
        h24 = h24,
        help = help,
        onBack = onBack,
        onStart = {
            app.play(Sound.POP)
            vm.start()
        },
        onPick = vm::pick,
    )
}

@Composable
fun ChallengeScreen(
    state: ChallengeUiState,
    h24: Boolean,
    help: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val finalAnchor = remember { mutableStateOf(Offset.Zero) }
    RecordCelebrationEffect(state) { finalAnchor.value }
    ScreenScaffold(modifier) { clockSize ->
        TopBar(
            title = stringResource(R.string.challenge_title),
            backLabel = stringResource(R.string.back_levels),
            onBack = onBack,
            trailing = { Pill(stringResource(R.string.challenge_score, state.score)) },
        )
        TimerBar(state)
        ClockBox(clockSize) {
            ClockFace(
                command = state.clock,
                period = ClockTime.period(h24),
                help = help,
                describeTime = state.phase != ChallengePhase.RUN || state.busy,
            )
        }
        Panel {
            when (state.phase) {
                ChallengePhase.START -> {
                    Text(stringResource(R.string.challenge_desc), style = SkType.Question)
                    Text(stringResource(R.string.challenge_intro), style = SkType.Body)
                    Eyebrow(
                        if (state.best >
                            0
                        ) {
                            stringResource(R.string.challenge_best, state.best)
                        } else {
                            stringResource(R.string.challenge_no_best)
                        },
                    )
                    SkButton(stringResource(R.string.challenge_go), onStart, kind = ButtonKind.PRIMARY)
                }

                ChallengePhase.RUN -> {
                    ReadOptions {
                        for (t in state.options) {
                            OptionButton(
                                words = timeLabel(LocalAppLanguage.current, t, h24),
                                digital = timeLanguage(LocalAppLanguage.current).digital(t, h24),
                                state = state.optionStates[t] ?: OptionState.NORMAL,
                                enabled = !state.busy,
                                onClick = { onPick(t) },
                            )
                        }
                    }
                }

                ChallengePhase.END -> {
                    ChallengeResult(state, onAnchorChange = { finalAnchor.value = it }, onStart, onBack)
                }
            }
        }
    }
}

@Composable
private fun RecordCelebrationEffect(
    state: ChallengeUiState,
    anchor: () -> Offset,
) {
    val confetti = LocalConfetti.current
    val currentAnchor by rememberUpdatedState(anchor)
    var celebratedRun by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(state.phase, state.newRecord, state.runId) {
        if (state.phase == ChallengePhase.END && state.newRecord && celebratedRun != state.runId) {
            celebratedRun = state.runId
            confetti?.burst(currentAnchor(), RECORD_CONFETTI)
        }
    }
}

/** Sateenkaaren väreillä täyttyvä ajastinpalkki, joka tyhjenee minuutissa (web: .timer). */
@Composable
private fun TimerBar(state: ChallengeUiState) {
    // Ajastin on tietoa eikä koristetta, joten se lasketaan ruutujen ajasta (ei animaatioasetuksen mukaan).
    var fraction by remember(
        state.runId,
        state.phase,
    ) { mutableFloatStateOf(state.remainingFraction(System.nanoTime())) }
    LaunchedEffect(state.runId, state.phase) {
        if (state.phase != ChallengePhase.RUN) return@LaunchedEffect
        while (fraction > 0f) {
            withFrameNanos { fraction = state.remainingFraction(System.nanoTime()) }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(SkDimens.TimerHeight)
            .clip(SkShapes.Timer)
            .background(SkColors.DotEmpty)
            .clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Brush.horizontalGradient(SkColors.Rainbow7)),
        )
    }
}

private const val RECORD_CONFETTI = 120

@Composable
private fun ColumnScope.ChallengeResult(
    state: ChallengeUiState,
    onAnchorChange: (Offset) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Eyebrow(stringResource(R.string.challenge_over))
    Text(
        state.score.toString(),
        style = SkType.BigNumber,
        modifier = Modifier.confettiAnchor(onAnchorChange),
    )
    Text(
        if (state.newRecord) {
            stringResource(R.string.challenge_new_record, state.score)
        } else {
            stringResource(R.string.challenge_result, state.score, state.best)
        },
        style = SkType.ResultText,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
        SkButton(stringResource(R.string.challenge_again), onStart, kind = ButtonKind.PRIMARY)
        SkButton(stringResource(R.string.levels_short), onBack)
    }
}
