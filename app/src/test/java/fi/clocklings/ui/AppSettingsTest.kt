package fi.clocklings.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import fi.clocklings.AppServices
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun failedSettingsWriteLeavesCommittedValuesAndCanRetry() =
        runTest(main.dispatcher) {
            val store = SettingsTestStore()
            val repository = ProgressRepository(store)
            val app = AppViewModel(services(repository))
            advanceUntilIdle()
            store.fail = true
            app.setSound(false)
            assertTrue(app.settingsAction.value.pending)
            advanceUntilIdle()
            assertTrue(app.settingsAction.value.failed)
            assertTrue(app.settings.value!!.sound)
            assertEquals(Settings(), repository.settings.first())
            store.fail = false
            app.setSound(false)
            advanceUntilIdle()
            assertFalse(app.settings.value!!.sound)
            assertFalse(app.settingsAction.value.failed)
            app.setHelp(false)
            advanceUntilIdle()
            app.setH24(true)
            advanceUntilIdle()
            assertEquals(Settings(sound = false, help = false, h24 = true), repository.settings.first())
        }

    @Test
    fun dismissingErrorPreservesPendingWriteAndItsLaterFailure() =
        runTest(main.dispatcher) {
            val store = SettingsTestStore()
            val repository = ProgressRepository(store)
            val app = AppViewModel(services(repository))
            advanceUntilIdle()
            store.fail = true
            app.setHelp(false)
            app.clearSettingsError()
            assertTrue(app.settingsAction.value.pending)
            advanceUntilIdle()
            assertTrue(app.settingsAction.value.failed)
            app.clearSettingsError()
            assertEquals(SettingsAction(), app.settingsAction.value)
            assertEquals(Settings(), repository.settings.first())
            store.fail = false
            app.setHelp(false)
            advanceUntilIdle()
            assertFalse(repository.settings.first().help)
            assertEquals(SettingsAction(), app.settingsAction.value)
        }

    @Test
    fun resetFailureAndRepeatedSubmissionDoNotLoseOtherData() =
        runTest(main.dispatcher) {
            val store = SettingsTestStore()
            val repository = ProgressRepository(store)
            val progress = Progress().withLevelResult(0, 3).withName(CompanionKind.CAT, "Mäx")
            repository.updateProgress { progress }
            repository.updateSettings { it.copy(sound = false, h24 = true) }
            repository.setLegacyLanguage("sv")
            val settings = repository.settings.first()
            val app = AppViewModel(services(repository))
            advanceUntilIdle()
            var successes = 0
            store.fail = true
            app.resetProgress { successes++ }
            advanceUntilIdle()
            assertEquals(progress, repository.progress.first())
            assertEquals(0, successes)
            assertTrue(app.settingsAction.value.failed)
            store.fail = false
            val before = store.writes
            app.resetProgress { successes++ }
            app.resetProgress { successes++ }
            advanceUntilIdle()
            assertEquals(1, successes)
            assertEquals(before + 1, store.writes)
            assertEquals(Progress(), repository.progress.first())
            assertEquals(settings, repository.settings.first())
            assertEquals("sv", repository.legacyLanguage.first())
        }

    @Test
    fun startingLevelsBeforeProgressRefreshPreservesBothIntroMarkers() =
        runTest(main.dispatcher) {
            val repository = ProgressRepository(InMemoryPreferencesStore())
            val original = Progress(introShown = setOf("4")).withLevelResult(0, 3)
            repository.updateProgress { original }
            val app = AppViewModel(services(repository))
            advanceUntilIdle()

            assertEquals(listOf("viisarit", "kierros", "tasan"), app.startLevel(0))
            assertEquals(listOf("puoli"), app.startLevel(1))
            advanceUntilIdle()

            assertEquals(original.copy(introShown = setOf("0", "1", "4")), repository.progress.first())
            assertTrue(app.startLevel(0).isEmpty())
            assertTrue(app.startLevel(1).isEmpty())
            advanceUntilIdle()
        }

    private fun services(repository: ProgressRepository): AppServices =
        object : AppServices by FakeServices() {
            override val repository = repository
        }
}

private class SettingsTestStore : DataStore<Preferences> {
    private val delegate = InMemoryPreferencesStore()
    override val data = delegate.data
    var fail = false
    var writes = 0

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        if (fail) throw IOException("Synthetic settings failure")
        writes++
        return delegate.updateData(transform)
    }
}
