package fi.clocklings.audio

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.ext.junit.runners.AndroidJUnit4
import fi.clocklings.domain.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SpeechTest {
    @Test
    fun latestLanguageWinsBeforeAsynchronousInitializationWithoutReplayingText() {
        val engine = FakeEngine()
        lateinit var initialize: (Int) -> Unit
        val speech =
            Speech { callback ->
                initialize = callback
                engine
            }
        try {
            speech.setLanguage(AppLanguage.FINNISH)
            speech.speak("old Finnish")
            speech.setLanguage(AppLanguage.SWEDISH)
            speech.speak("old Swedish")
            assertFalse(speech.available.value)
            initialize(TextToSpeech.SUCCESS)
            assertTrue(speech.available.value)
            assertEquals(listOf("sv"), engine.selectedLanguages)
            assertTrue(engine.spoken.isEmpty())
            speech.speak("new Swedish")
            assertEquals(listOf("new Swedish"), engine.spoken)
        } finally {
            speech.shutdown()
        }
        assertEquals(1, engine.shutdowns)
    }

    @Test
    fun synchronousInitializationConfiguresAfterEngineAssignment() {
        withSpeech { speech, engine ->
            assertTrue(speech.available.value)
            assertEquals(listOf("en"), engine.selectedLanguages)
            assertEquals(listOf(0.9f), engine.rates)
        }
    }

    @Test
    fun languageSwitchStopsSpeakingAndKeepsUnavailableUntilVoiceAndRateSucceed() {
        withSpeech { speech, engine ->
            speech.speak("English")
            val oldUtterance = engine.utteranceIds.single()
            val stopsBefore = engine.stops
            engine.beforeSetVoice = { assertFalse(speech.available.value) }
            engine.beforeSetRate = { assertFalse(speech.available.value) }
            speech.setLanguage(AppLanguage.FINNISH)
            speech.setLanguage(AppLanguage.SWEDISH)
            assertTrue(speech.available.value)
            assertEquals(stopsBefore + 2, engine.stops)
            assertEquals(listOf("en", "fi", "sv"), engine.selectedLanguages)
            assertEquals(listOf("English"), engine.spoken)
            engine.reportError(oldUtterance)
            assertTrue(speech.available.value)
            speech.speak("Swedish")
            engine.reportError(engine.utteranceIds.last())
            assertFalse(speech.available.value)
        }
    }

    @Test
    fun missingEngineAndFailedInitializationRemainUnavailable() {
        val missing = Speech { throw IllegalStateException("No engine") }
        try {
            missing.refresh()
            missing.speak("ignored")
            assertFalse(missing.available.value)
        } finally {
            missing.shutdown()
        }
        val engine = FakeEngine()
        val failed =
            Speech { callback ->
                callback(TextToSpeech.ERROR)
                engine
            }
        try {
            failed.setLanguage(AppLanguage.FINNISH)
            failed.refresh()
            failed.speak("ignored")
            assertFalse(failed.available.value)
            assertTrue(engine.selectedLanguages.isEmpty())
            assertTrue(engine.spoken.isEmpty())
        } finally {
            failed.shutdown()
        }
    }

    @Test
    fun absentNetworkMissingDataAndWrongLanguageVoicesCannotEnableSpeech() {
        val rejected =
            listOf(
                emptySet(),
                setOf(voice("network", "sv", network = true)),
                setOf(voice("missing", "sv", missing = true)),
                setOf(voice("wrong", "en")),
            )
        rejected.forEach { voices ->
            withSpeech { speech, engine ->
                engine.voices = voices
                speech.setLanguage(AppLanguage.SWEDISH)
                speech.speak("ignored")
                assertFalse(speech.available.value)
                assertTrue(engine.spoken.isEmpty())
            }
        }
    }

    @Test
    fun voiceRateSpeakAndStopFailuresMakeSpeechUnavailableAndRefreshCanRecover() {
        withSpeech { speech, engine ->
            engine.voiceResult = TextToSpeech.ERROR
            speech.setLanguage(AppLanguage.FINNISH)
            assertFalse(speech.available.value)
            engine.voiceResult = TextToSpeech.SUCCESS
            engine.rateResult = TextToSpeech.ERROR
            speech.refresh()
            assertFalse(speech.available.value)
            engine.rateResult = TextToSpeech.SUCCESS
            speech.refresh()
            assertTrue(speech.available.value)
            engine.speakResult = TextToSpeech.ERROR
            speech.speak("failed")
            assertFalse(speech.available.value)
            engine.speakResult = TextToSpeech.SUCCESS
            speech.refresh()
            assertTrue(speech.available.value)
            engine.stopResult = TextToSpeech.ERROR
            speech.setLanguage(AppLanguage.SWEDISH)
            assertFalse(speech.available.value)
            engine.stopResult = TextToSpeech.SUCCESS
            speech.refresh()
            assertTrue(speech.available.value)
            assertEquals("sv", engine.selectedLanguages.last())
        }
    }

    @Test
    fun refreshReflectsInstalledAndRemovedVoiceDataWithoutAutomaticSpeech() {
        withSpeech { speech, engine ->
            engine.voices = emptySet()
            speech.setLanguage(AppLanguage.SWEDISH)
            assertFalse(speech.available.value)
            engine.voices = setOf(voice("swedish", "sv-FI"))
            speech.refresh()
            assertTrue(speech.available.value)
            engine.voices = emptySet()
            speech.refresh()
            assertFalse(speech.available.value)
            assertTrue(engine.spoken.isEmpty())
        }
    }

    @Test
    fun shutdownIsFinalEvenWhenInitializationFinishesLate() {
        val engine = FakeEngine()
        lateinit var initialize: (Int) -> Unit
        val speech =
            Speech { callback ->
                initialize = callback
                engine
            }
        speech.shutdown()
        initialize(TextToSpeech.SUCCESS)
        speech.setLanguage(AppLanguage.FINNISH)
        speech.refresh()
        speech.speak("ignored")
        speech.shutdown()
        assertFalse(speech.available.value)
        assertTrue(engine.selectedLanguages.isEmpty())
        assertTrue(engine.spoken.isEmpty())
        assertEquals(1, engine.shutdowns)
    }

    private fun withSpeech(check: (Speech, FakeEngine) -> Unit) {
        val engine = FakeEngine()
        val speech =
            Speech { callback ->
                callback(TextToSpeech.SUCCESS)
                engine
            }
        try {
            check(speech, engine)
        } finally {
            speech.shutdown()
        }
    }

    private class FakeEngine : SpeechEngine {
        override var voices = setOf(voice("english", "en-GB"), voice("finnish", "fi-FI"), voice("swedish", "sv-SE"))
        var voiceResult = TextToSpeech.SUCCESS
        var rateResult = TextToSpeech.SUCCESS
        var speakResult = TextToSpeech.SUCCESS
        var stopResult = TextToSpeech.SUCCESS
        var beforeSetVoice: () -> Unit = {}
        var beforeSetRate: () -> Unit = {}
        val selectedLanguages = mutableListOf<String>()
        val rates = mutableListOf<Float>()
        val spoken = mutableListOf<String>()
        val utteranceIds = mutableListOf<String>()
        var stops = 0
        var shutdowns = 0
        private var onError: (String) -> Unit = {}

        override fun setErrorListener(listener: (String) -> Unit) {
            onError = listener
        }

        override fun setVoice(voice: Voice): Int {
            beforeSetVoice()
            selectedLanguages += voice.locale.language
            return voiceResult
        }

        override fun setSpeechRate(rate: Float): Int {
            beforeSetRate()
            rates += rate
            return rateResult
        }

        override fun speak(
            text: String,
            utteranceId: String,
        ): Int {
            spoken += text
            utteranceIds += utteranceId
            return speakResult
        }

        override fun stop(): Int {
            stops++
            return stopResult
        }

        override fun shutdown() {
            shutdowns++
        }

        fun reportError(utteranceId: String) = onError(utteranceId)
    }

    private companion object {
        fun voice(
            name: String,
            tag: String,
            network: Boolean = false,
            missing: Boolean = false,
        ) = Voice(
            name,
            Locale.forLanguageTag(tag),
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            network,
            if (missing) setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) else emptySet(),
        )
    }
}
