package fi.clocklings.ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.ClockTime
import fi.clocklings.domain.Levels
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.ClockBox
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.components.DigitalPill
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.HelpTopBar
import fi.clocklings.ui.components.LocalConfetti
import fi.clocklings.ui.components.RoundIconButton
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.adaptiveGridColumns
import fi.clocklings.ui.components.confettiAnchor
import fi.clocklings.ui.digitalLabel
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.resources
import fi.clocklings.ui.spokenTime
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.timeLabel
import kotlin.random.Random

private enum class AskPhase { SET, ASK, REVEALED }

/** Partners set, answer, reveal, and swap turns without recording a scored result. */
@Composable
fun AskScreen(
    h24: Boolean,
    help: Boolean,
    speechAvailable: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    var phase by rememberSaveable { mutableStateOf(AskPhase.SET) }
    var setter by rememberSaveable { mutableIntStateOf(1) }
    // Not 12:00, where both hands lie on top of each other.
    var time by rememberSaveable { mutableIntStateOf(ClockTime.start(h24)) }
    var command by remember { mutableStateOf(ClockCommand(time)) }
    ScreenScaffold(
        modifier,
        clockHeightShare = SkDimens.PLAY_CLOCK_HEIGHT_SHARE,
        section = Section.TOGETHER,
        topBar = { HelpTopBar(stringResource(R.string.ask_title), help, onBack, app::toggleHelp) },
    ) { clockSize ->
        ClockBox(clockSize) {
            ClockFace(
                command = command,
                period = ClockTime.period(h24),
                step = STEP,
                interactive = phase == AskPhase.SET,
                help = help,
                describeTime = phase != AskPhase.ASK,
                onChange = { time = it },
            )
        }
        val role = askRoleDescription(phase, setter)
        FlatPanel(Modifier.semantics { contentDescription = role }) {
            if (phase == AskPhase.SET) {
                AskSetup(
                    onRandom = {
                        time = Levels.randomClockTime(ClockTime.period(h24), time, Random.Default)
                        command = ClockCommand(time, RANDOM_MOVE_MS)
                        app.play(Sound.POP)
                    },
                    onAsk = {
                        app.play(Sound.POP)
                        phase = AskPhase.ASK
                    },
                )
            } else {
                AskQuestion(
                    revealed = phase == AskPhase.REVEALED,
                    answer = { AskAnswer(time, h24, speechAvailable) },
                    onReveal = { phase = AskPhase.REVEALED },
                    onSwap = {
                        setter = PLAYER_NUMBER_SUM - setter
                        phase = AskPhase.SET
                    },
                )
            }
        }
    }
}

private const val STEP = 5
private const val RANDOM_MOVE_MS = 900
private const val REVEAL_CONFETTI = 40
private const val PLAYER_NUMBER_SUM = 3

@Composable
private fun ColumnScope.AskSetup(
    onRandom: () -> Unit,
    onAsk: () -> Unit,
) {
    Text(stringResource(R.string.ask_big_question), style = SkType.Question)
    CompanionTip(stringResource(R.string.ask_big_text))
    SkButton(stringResource(R.string.random_time), onRandom, Modifier.fillMaxWidth(), icon = SkIcons.Shuffle)
    SkButton(
        stringResource(R.string.ask_go),
        onAsk,
        Modifier.fillMaxWidth(),
        kind = ButtonKind.PRIMARY,
        icon = SkIcons.Pass,
    )
}

/** The partner answers; revealing celebrates, then the players can swap roles. */
@Composable
private fun ColumnScope.AskQuestion(
    revealed: Boolean,
    answer: @Composable () -> Unit,
    onReveal: () -> Unit,
    onSwap: () -> Unit,
) {
    val app = LocalAppViewModel.current
    val confetti = LocalConfetti.current
    val haptic = LocalHapticFeedback.current
    val wordsAnchor = remember { mutableStateOf(Offset.Zero) }
    Text(stringResource(R.string.question_read), style = SkType.Question)
    CompanionTip(stringResource(R.string.ask_small_text))
    if (!revealed) {
        SkButton(stringResource(R.string.ask_reveal), {
            onReveal()
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            app.play(Sound.OK)
            confetti?.burst(wordsAnchor.value, REVEAL_CONFETTI)
        }, kind = ButtonKind.PRIMARY, modifier = Modifier.confettiAnchor { wordsAnchor.value = it })
    } else {
        answer()
        SkButton(stringResource(R.string.ask_new), {
            app.stopSpeech()
            app.play(Sound.POP)
            onSwap()
        }, kind = ButtonKind.GREEN)
    }
}

@Composable
private fun AskAnswer(
    time: Int,
    h24: Boolean,
    speechAvailable: Boolean,
) {
    val app = LocalAppViewModel.current
    val res = resources()
    Text(timeLabel(LocalAppLanguage.current, time, h24), style = SkType.Words)
    DigitalPill(digitalLabel(res, time, h24))
    if (speechAvailable) {
        RoundIconButton(
            SkIcons.Listen,
            stringResource(R.string.listen),
            { app.speak(spokenTime(res, time, h24)) },
            large = true,
        )
    }
}

@Composable
private fun askRoleDescription(
    phase: AskPhase,
    setter: Int,
): String =
    stringResource(
        if (phase == AskPhase.SET) R.string.ask_big_eyebrow else R.string.ask_small_eyebrow,
        if (phase == AskPhase.SET) setter else PLAYER_NUMBER_SUM - setter,
    )
