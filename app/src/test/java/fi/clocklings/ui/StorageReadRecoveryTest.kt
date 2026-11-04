package fi.clocklings.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import fi.clocklings.AppServices
import fi.clocklings.data.InMemoryPreferencesStore
import fi.clocklings.data.ProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StorageReadRecoveryTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = RecoveringReadStore()
    private val repository = ProgressRepository(store)
    private val services =
        object : AppServices by FakeServices() {
            override val repository = this@StorageReadRecoveryTest.repository
        }

    @Test
    fun startupReadFailureDoesNotDisconnectProgressAndSettings() =
        runTest(main.dispatcher) {
            store.readable.value = false
            val app = AppViewModel(services)
            try {
                runCurrent()
                assertEquals(0, app.progress.value!!.totalStars)
                store.readable.value = true
                repository.updateProgress { it.withLevelResult(0, 3) }
                repository.updateSettings { it.copy(sound = false) }
                advanceTimeBy(2_000)
                runCurrent()
                assertEquals(3, app.progress.value!!.totalStars)
                assertEquals(false, app.settings.value!!.sound)
                repository.updateProgress { it.withLevelResult(1, 2) }
                runCurrent()
                assertEquals(5, app.progress.value!!.totalStars)
            } finally {
                app.viewModelScope.cancel()
            }
        }

    @Test
    fun laterReadFailureKeepsLastKnownDataAndResumesUpdates() =
        runTest(main.dispatcher) {
            repository.updateProgress { it.withLevelResult(0, 3) }
            repository.updateSettings { it.copy(sound = false) }
            val app = AppViewModel(services)
            try {
                runCurrent()
                store.readable.value = false
                runCurrent()
                assertEquals(3, app.progress.value!!.totalStars)
                assertEquals(false, app.settings.value!!.sound)
                store.readable.value = true
                repository.updateProgress { it.withLevelResult(1, 2) }
                advanceTimeBy(2_000)
                runCurrent()
                assertEquals(5, app.progress.value!!.totalStars)
            } finally {
                app.viewModelScope.cancel()
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class RecoveringReadStore : DataStore<Preferences> {
    private val delegate = InMemoryPreferencesStore()
    val readable = MutableStateFlow(true)
    override val data =
        readable.flatMapLatest { ready ->
            if (ready) delegate.data else flow { throw IOException("Synthetic read failure") }
        }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        delegate.updateData(transform)
}
