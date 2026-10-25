package fi.clocklings.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import fi.clocklings.domain.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** On-demand speech using an installed offline voice in the content language. */
class Speech internal constructor(
    createEngine: ((Int) -> Unit) -> SpeechEngine,
) {
    constructor(context: Context) : this({ onInit -> AndroidSpeechEngine(context, onInit) })

    private val lock = Any()
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()
    private var engine: SpeechEngine? = null
    private var initialization: Int? = null
    private var language = AppLanguage.ENGLISH
    private var closed = false
    private var utteranceNumber = 0L
    private var currentUtterance: String? = null

    init {
        synchronized(lock) {
            engine =
                runCatching {
                    createEngine { status ->
                        synchronized(lock) {
                            initialization = status
                            if (!closed && engine != null) configure()
                        }
                    }
                }.getOrNull()
            engine?.setErrorListener { utteranceId ->
                synchronized(lock) {
                    if (utteranceId == currentUtterance) _available.value = false
                }
            }
            // A factory/engine may invoke its initialization callback before returning.
            configure()
        }
    }

    private fun configure() {
        _available.value = false
        val activeEngine = engine ?: return
        if (closed || initialization != TextToSpeech.SUCCESS) return
        _available.value =
            runCatching {
                val locale = Locale.forLanguageTag(language.tag)
                val voice = selectOfflineVoice(activeEngine.voices, locale) ?: return@runCatching false
                activeEngine.setVoice(voice) == TextToSpeech.SUCCESS &&
                    activeEngine.setSpeechRate(RATE) == TextToSpeech.SUCCESS
            }.getOrDefault(false)
    }

    fun setLanguage(language: AppLanguage) =
        synchronized(lock) {
            if (closed || this.language == language) return@synchronized
            this.language = language
            _available.value = false
            if (stopCurrent()) configure()
        }

    /** Recheck voice data after returning from the background; never replay old text. */
    fun refresh() =
        synchronized(lock) {
            if (!closed) {
                _available.value = false
                if (stopCurrent()) configure()
            }
        }

    fun speak(text: String) {
        synchronized(lock) {
            if (!_available.value || closed) return
            val utteranceId = "clocklings-${++utteranceNumber}"
            currentUtterance = utteranceId
            val result = runCatching { engine?.speak(text, utteranceId) }.getOrNull()
            if (result != TextToSpeech.SUCCESS) _available.value = false
        }
    }

    fun stop() =
        synchronized(lock) {
            if (!stopCurrent()) _available.value = false
        }

    private fun stopCurrent(): Boolean {
        currentUtterance = null
        if (initialization != TextToSpeech.SUCCESS) return true
        return runCatching { engine?.stop() == TextToSpeech.SUCCESS }.getOrDefault(false)
    }

    /** Test-owned/replaced instances are closed explicitly; Activity recreation does not own this. */
    fun shutdown() =
        synchronized(lock) {
            if (closed) return@synchronized
            closed = true
            _available.value = false
            stopCurrent()
            runCatching { engine?.shutdown() }
            engine = null
        }

    private companion object {
        const val RATE = 0.9f
    }
}

/** The small engine boundary lets tests control initialization and Android failure results. */
internal interface SpeechEngine {
    val voices: Set<Voice>

    fun setErrorListener(listener: (String) -> Unit)

    fun setVoice(voice: Voice): Int

    fun setSpeechRate(rate: Float): Int

    fun speak(
        text: String,
        utteranceId: String,
    ): Int

    fun stop(): Int

    fun shutdown()
}

private class AndroidSpeechEngine(
    context: Context,
    onInit: (Int) -> Unit,
) : SpeechEngine {
    private val tts = TextToSpeech(context.applicationContext, onInit)
    override val voices: Set<Voice> get() = tts.voices.orEmpty()

    override fun setErrorListener(listener: (String) -> Unit) {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) = Unit

                override fun onDone(utteranceId: String) = Unit

                @Deprecated("Required by the platform listener")
                override fun onError(utteranceId: String) = listener(utteranceId)

                override fun onError(
                    utteranceId: String,
                    errorCode: Int,
                ) = listener(utteranceId)
            },
        )
    }

    override fun setVoice(voice: Voice): Int = tts.setVoice(voice)

    override fun setSpeechRate(rate: Float): Int = tts.setSpeechRate(rate)

    override fun speak(
        text: String,
        utteranceId: String,
    ): Int = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

    override fun stop(): Int = tts.stop()

    override fun shutdown() = tts.shutdown()
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
