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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val currentSettings get() = settings.value ?: Settings()
    private val currentProgress get() = progress.value ?: Progress()

    fun play(sound: Sound) = services.play(sound)

    fun speak(text: String) = services.speak(text)

    fun stopSpeech() = services.stopSpeech()

    /** Web: äänen kytkeminen päälle kuittaa poksahduksella. */
    fun toggleSound() {
        val on = !currentSettings.sound
        updateSettings { it.copy(sound = on) }
        if (on) services.play(Sound.POP, force = true)
    }

    fun toggleHelp() = updateSettings { it.copy(help = !it.help) }

    fun setH24(h24: Boolean) = updateSettings { it.copy(h24 = h24) }

    fun personalize(transform: (Personalization) -> Personalization) =
        updateSettings { it.copy(personalization = transform(it.personalization)) }

    private fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { services.repository.updateSettings(transform) }
    }

    fun updateProgress(transform: (Progress) -> Progress) {
        viewModelScope.launch { services.repository.updateProgress(transform) }
    }

    fun resetProgress() {
        viewModelScope.launch { services.repository.resetProgress() }
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

val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> { error("AppViewModel puuttuu") }
