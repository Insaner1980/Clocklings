package fi.clocklings.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** On-demand speech using an installed offline voice in the content language. */
class Speech(
    context: Context,
    private val locale: Locale = Locale.ENGLISH,
) {
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts =
            TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) configure() else Log.i(TAG, "Speech unavailable: $status")
            }
    }

    private fun configure() {
        val engine = tts ?: return
        _available.value =
            runCatching {
                val voice = selectOfflineVoice(engine.voices.orEmpty(), locale) ?: return@runCatching false
                engine.setVoice(voice) == TextToSpeech.SUCCESS &&
                    engine.setSpeechRate(RATE) == TextToSpeech.SUCCESS
            }.getOrDefault(false)
    }

    fun speak(text: String) {
        if (!_available.value) return
        val result = runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) }.getOrNull()
        if (result != TextToSpeech.SUCCESS) _available.value = false
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    private companion object {
        const val TAG = "Speech"
        const val RATE = 0.9f
        const val UTTERANCE_ID = "clocklings"
    }
}

/** Regional variants share content; prefer an exact locale when one is requested. */
internal fun selectOfflineVoice(
    voices: Set<Voice>,
    locale: Locale,
): Voice? =
    voices
        .filter {
            it.locale.language == locale.language &&
                !it.isNetworkConnectionRequired &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
        }.sortedWith(compareBy<Voice> { it.locale != locale }.thenBy { it.name })
        .firstOrNull()
