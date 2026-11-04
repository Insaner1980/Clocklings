package fi.clocklings.audio

import android.media.AudioTrack
import android.media.SoundPool
import androidx.test.core.app.ApplicationProvider
import fi.clocklings.AppContainer
import fi.clocklings.R
import fi.clocklings.data.InMemoryPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSoundPool
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppAudioLifecycleTest {
    @Test
    fun delayedGameResultCannotStartSoundAfterTheActivityPauses() {
        val container = AppContainer(ApplicationProvider.getApplicationContext(), InMemoryPreferencesStore())
        val sfx = ReflectionHelpers.getField<Sfx>(container, "sfx")
        val pool = ReflectionHelpers.getField<SoundPool>(sfx, "pool")
        val shadow = Shadow.extract<ShadowSoundPool>(pool)
        val scope = ReflectionHelpers.getField<CoroutineScope>(sfx, "scope")
        try {
            runBlocking {
                withTimeout(5_000) {
                    scope.coroutineContext.job.children
                        .toList()
                        .joinAll()
                }
            }
            shadow.notifyResourceLoaded(R.raw.success, true)
            container.resume()
            container.pause()
            container.play(Sound.WIN, force = true, rate = 1f)
            assertFalse(shadow.wasResourcePlayed(R.raw.success))
            container.resume()
            container.play(Sound.WIN, force = true, rate = 1f)
            assertTrue(shadow.wasResourcePlayed(R.raw.success))
        } finally {
            ReflectionHelpers.getField<CoroutineScope>(container, "scope").cancel()
            ReflectionHelpers.getField<Speech>(container, "speech").shutdown()
            scope.cancel()
            sfx.stopAll()
            ReflectionHelpers.getField<Map<Sound, AudioTrack>>(sfx, "tracks").values.forEach { it.release() }
            pool.release()
        }
    }
}
