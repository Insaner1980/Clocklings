package fi.clocklings.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.clocklings.AppServices
import fi.clocklings.audio.Sound
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Lessons
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Wardrobe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Koko sovelluksen tila: edistyminen ja asetukset (DataStoresta), äänet ja puhe.
 * Arvot ovat null, kunnes tallennus on luettu ensimmäisen kerran.
 *
 * One application facade exposes the existing settings, progress, and audio actions.
 *
 * Vakaa: viittaus ei vaihdu, ja muuttuva tila välitetään StateFlow-virroilla.
 */
@Stable
@Suppress("TooManyFunctions")
class AppViewModel(
    val services: AppServices,
) : ViewModel() {
    val progress: StateFlow<Progress?> =
        services.repository.progress.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<Settings?> =
        services.repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val speechAvailable: StateFlow<Boolean> = services.speechAvailable
    private val mutableSettingsAction = MutableStateFlow(SettingsAction())
    val settingsAction = mutableSettingsAction.asStateFlow()

    private val currentSettings get() = settings.value ?: Settings()
    private val currentProgress get() = progress.value ?: Progress()

    fun play(sound: Sound) = services.play(sound)

    fun speak(text: String) = services.speak(text)

    fun stopSpeech() = services.stopSpeech()

    /** Web: äänen kytkeminen päälle kuittaa poksahduksella. */
    fun toggleSound() {
        setSound(!currentSettings.sound)
    }

    fun setSound(enabled: Boolean) =
        settingsWrite {
            services.repository.updateSettings { it.copy(sound = enabled) }
            if (enabled) services.play(Sound.POP, force = true)
        }

    fun setHelp(enabled: Boolean) = settingsWrite { services.repository.updateSettings { it.copy(help = enabled) } }

    fun toggleHelp() = updateSettings { it.copy(help = !it.help) }

    fun setH24(h24: Boolean) = settingsWrite { services.repository.updateSettings { it.copy(h24 = h24) } }

    fun personalize(transform: (Personalization) -> Personalization) =
        updateSettings { it.copy(personalization = transform(it.personalization)) }

    private fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { services.repository.updateSettings(transform) }
    }

    fun updateProgress(transform: (Progress) -> Progress) {
        viewModelScope.launch { services.repository.updateProgress(transform) }
    }

    fun resetProgress(onSuccess: () -> Unit = {}) {
        settingsWrite {
            services.repository.resetProgress()
            onSuccess()
        }
    }

    fun clearSettingsError() {
        if (!mutableSettingsAction.value.pending) mutableSettingsAction.value = SettingsAction()
    }

    private fun settingsWrite(action: suspend () -> Unit) {
        if (mutableSettingsAction.value.pending) return
        mutableSettingsAction.value = SettingsAction(pending = true)
        viewModelScope.launch {
            try {
                action()
                mutableSettingsAction.value = SettingsAction()
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                mutableSettingsAction.value = SettingsAction(failed = true)
            }
        }
    }

    fun toggleItem(
        kind: CompanionKind,
        item: Item,
    ) = updateProgress { it.withOutfit(kind, Wardrobe.toggle(it.outfitFor(kind), item)) }

    fun setName(
        kind: CompanionKind,
        name: String,
    ) = updateProgress { it.withName(kind, name) }

    fun setColor(
        kind: CompanionKind,
        color: CompanionColor,
    ) = personalize { it.withStyle(kind, it.styleFor(kind).copy(color = color)) }

    /** Pelaa-napin painallus: palauttaa ennen tasoa näytettävät oppitunnit ja merkitsee ne näytetyiksi. */
    fun startLevel(level: Int): List<String> {
        val plan = Lessons.introPlan(level, currentSettings.h24, currentProgress)
        updateProgress { it.copy(introShown = plan.introShown) }
        return plan.queue
    }
}

data class SettingsAction(
    val pending: Boolean = false,
    val failed: Boolean = false,
)

val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> { error("AppViewModel puuttuu") }
