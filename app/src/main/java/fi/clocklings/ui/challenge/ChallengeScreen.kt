package fi.clocklings.ui.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import fi.clocklings.domain.EnglishTime
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
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.confettiAnchor
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
    val confetti = LocalConfetti.current
    val finalAnchor = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(state.phase, state.newRecord) {
        if (state.phase == ChallengePhase.END && state.newRecord) {
            confetti?.burst(finalAnchor.value, RECORD_CONFETTI)
        }
    }
    ScreenScaffold(modifier) { clockSize ->
        TopBar(
            title = stringResource(R.string.challenge_title),
            backLabel = stringResource(R.string.back_levels),
            onBack = onBack,
            trailing = { Pill(stringResource(R.string.challenge_score, state.score)) },
        )
        TimerBar(state.runId, running = state.phase == ChallengePhase.RUN)
        ClockBox(clockSize) {
            ClockFace(command = state.clock, period = ClockTime.period(h24), help = help)
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
                    Text(stringResource(R.string.question_read), style = SkType.Question)
                    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions)) {
                        for (t in state.options) {
                            OptionButton(
                                words = timeLabel(t, h24),
                                digital = EnglishTime.digital(t, h24),
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

/** Sateenkaaren väreillä täyttyvä ajastinpalkki, joka tyhjenee minuutissa (web: .timer). */
@Composable
private fun TimerBar(
    runId: Int,
    running: Boolean,
) {
    // Ajastin on tietoa eikä koristetta, joten se lasketaan ruutujen ajasta (ei animaatioasetuksen mukaan).
    var fraction by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(runId, running) {
        if (!running) return@LaunchedEffect
        fraction = 1f
        val start = withFrameNanos { it }
        while (fraction > 0f) {
            withFrameNanos { now -> fraction = (1f - (now - start) / DURATION_NANOS).coerceAtLeast(0f) }
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
private const val DURATION_NANOS = ChallengeViewModel.DURATION_MS * 1_000_000f

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
