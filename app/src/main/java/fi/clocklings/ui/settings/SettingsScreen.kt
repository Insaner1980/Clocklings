package fi.clocklings.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.clocklings.R
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Settings
import fi.clocklings.domain.timeLanguage
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.Panel
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.locale.LocalAppLocale
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
fun SettingsScreen(
    settings: Settings,
    speechAvailable: Boolean,
    onLanguage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    val action by app.settingsAction.collectAsStateWithLifecycle()
    val locale by LocalAppLocale.current.selection.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    // A language change or activity recreation dismisses a destructive confirmation safely.
    var confirmReset by remember(language) { mutableStateOf(false) }
    LaunchedEffect(app) { app.clearSettingsError() }
    val words = timeLanguage(language)
    ScreenScaffold(modifier) {
        TopBar(stringResource(R.string.settings_title), stringResource(R.string.back), onBack)
        Panel {
            SkButton("${stringResource(R.string.language_title)}: ${languageName(locale.choice)}", onLanguage)
            Text(stringResource(R.string.settings_language_description), style = SkType.Body)
        }
        Panel {
            SettingsHeading(stringResource(R.string.settings_clock_display))
            Column(Modifier.selectableGroup()) {
                RadioRow(stringResource(R.string.mode_12), !settings.h24, !action.pending) { app.setH24(false) }
                RadioRow(stringResource(R.string.mode_24), settings.h24, !action.pending) { app.setH24(true) }
            }
            Text(
                stringResource(
                    R.string.settings_clock_example,
                    words.digital(EXAMPLE_TIME),
                    words.digital(EXAMPLE_TIME, true),
                ),
                style = SkType.Body,
            )
        }
        Panel {
            SettingsSwitch(stringResource(R.string.settings_guides_title), settings.help, !action.pending, app::setHelp)
            Text(stringResource(R.string.settings_guides_description), style = SkType.Body)
            SettingsSwitch(
                stringResource(R.string.settings_sound_title),
                settings.sound,
                !action.pending,
                app::setSound,
            )
            Text(stringResource(R.string.settings_sound_description), style = SkType.Body)
        }
        SpeechAvailability(speechAvailable)
        ResetProgressPanel(!action.pending) {
            app.clearSettingsError()
            confirmReset = true
        }
        if (action.failed) Text(stringResource(R.string.settings_save_error), style = SkType.Body)
    }
    if (confirmReset) {
        ResetProgressDialog(
            pending = action.pending,
            failed = action.failed,
            onDismiss = { confirmReset = false },
            onReset = { app.resetProgress { confirmReset = false } },
        )
    }
}

@Composable
private fun ResetProgressPanel(
    enabled: Boolean,
    onReset: () -> Unit,
) {
    Panel {
        SettingsHeading(stringResource(R.string.settings_progress_title))
        SkButton(stringResource(R.string.settings_reset_action), onReset, enabled = enabled)
    }
}

@Composable
private fun SpeechAvailability(available: Boolean) {
    Panel {
        SettingsHeading(stringResource(R.string.settings_speech_title))
        Text(
            stringResource(if (available) R.string.settings_speech_available else R.string.settings_speech_unavailable),
            style = SkType.Body,
        )
        Text(stringResource(R.string.settings_speech_description), style = SkType.Body)
    }
}

@Composable
private fun ResetProgressDialog(
    pending: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reset_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.settings_reset_body))
                if (failed) Text(stringResource(R.string.settings_save_error))
            }
        },
        confirmButton = {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
            ) {
                SkButton(stringResource(R.string.settings_cancel), onDismiss)
                SkButton(stringResource(R.string.settings_reset_action), onReset, enabled = !pending)
            }
        },
    )
}

@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = LocalAppLocale.current
    val selection by controller.selection.collectAsStateWithLifecycle()
    val app = LocalAppViewModel.current
    ScreenScaffold(modifier) {
        TopBar(stringResource(R.string.language_title), stringResource(R.string.back), onBack)
        Panel {
            Column(Modifier.selectableGroup()) {
                (listOf<AppLanguage?>(null) + AppLanguage.entries).forEach { language ->
                    RadioRow(languageName(language), selection.choice == language, !selection.pending) {
                        if (selection.choice != language) app.stopSpeech()
                        controller.select(language)
                    }
                }
            }
        }
        if (selection.failed) Text(stringResource(R.string.language_save_error), style = SkType.Body)
    }
}

@Composable
private fun languageName(language: AppLanguage?): String =
    stringResource(
        when (language) {
            null -> R.string.language_device
            AppLanguage.ENGLISH -> R.string.language_english
            AppLanguage.FINNISH -> R.string.language_finnish
            AppLanguage.SWEDISH -> R.string.language_swedish
        },
    )

@Composable
private fun SettingsHeading(text: String) {
    Text(text, style = SkType.Question, modifier = Modifier.semantics { heading() })
}

@Composable
private fun RadioRow(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = SkDimens.GapS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        RadioButton(selected, onClick = null, enabled = enabled)
        Text(text, style = SkType.Body, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsSwitch(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
    ) {
        Text(text, style = SkType.Body, modifier = Modifier.weight(1f))
        Switch(checked, onCheckedChange = null, enabled = enabled)
    }
}

private const val EXAMPLE_TIME = 15 * 60 + 30
