package fi.clocklings.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Sound effects. Synthesized PCM is rendered once on a background thread and played with
 * AudioTrack (MODE_STATIC); recorded clips ([Sound.clip]) are decoded and played by a SoundPool.
 * The sound on/off setting is checked in AppContainer.
 */
class Sfx(
    context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracks = mutableMapOf<Sound, AudioTrack>()
    private val attributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attributes)
            .build()

    /** Sample ids whose decoding has finished; loading is asynchronous. */
    private val loaded = mutableSetOf<Int>()

    /** The latest stream of each clip, so a replay restarts it like an AudioTrack. */
    private val streams = mutableMapOf<Sound, Int>()
    private val samples: Map<Sound, Int>

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(loaded) { loaded += sampleId }
            } else {
                Log.w(TAG, "Äänitiedostoa $sampleId ei voitu ladata ($status)")
            }
        }
        samples = Sound.entries.mapNotNull { sound -> sound.clip?.let { sound to pool.load(context, it, 1) } }.toMap()
        scope.launch {
            for (sound in Sound.entries.filter { it.clip == null }) {
                runCatching { createTrack(ToneSynth.render(sound)) }
                    .onSuccess { synchronized(tracks) { tracks[sound] = it } }
                    .onFailure { Log.w(TAG, "Ääntä $sound ei voitu luoda", it) }
            }
        }
    }

    private fun createTrack(pcm: ShortArray): AudioTrack {
        val track =
            AudioTrack
                .Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ToneSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    fun play(sound: Sound) {
        if (sound.clip != null) playClip(sound) else playTone(sound)
    }

    private fun playTone(sound: Sound) {
        val track = synchronized(tracks) { tracks[sound] } ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }.onFailure { Log.w(TAG, "Ääntä $sound ei voitu soittaa", it) }
    }

    private fun playClip(sound: Sound) {
        val sample = samples[sound]?.takeIf { synchronized(loaded) { it in loaded } } ?: return
        synchronized(streams) {
            streams[sound]?.let(pool::stop)
            val stream = pool.play(sample, FULL_VOLUME, FULL_VOLUME, 0, NO_LOOP, NORMAL_RATE)
            if (stream == 0) Log.w(TAG, "Ääntä $sound ei voitu soittaa") else streams[sound] = stream
        }
    }

    /** Pysäyttää kaikki äänet (kutsutaan onPause-vaiheessa). */
    fun stopAll() {
        synchronized(tracks) { tracks.values.toList() }.forEach { runCatching { it.stop() } }
        synchronized(streams) {
            streams.values.forEach(pool::stop)
            streams.clear()
        }
    }

    private companion object {
        const val TAG = "Sfx"
        const val MAX_STREAMS = 4
        const val FULL_VOLUME = 1f
        const val NO_LOOP = 0
        const val NORMAL_RATE = 1f
    }
}
