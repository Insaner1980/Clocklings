package fi.clocklings

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextReplacement
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Progress
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.theme.ClocklingsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/** Characterize unsaved drafts without inventing a durable retry-restoration contract. */
@RunWith(AndroidJUnit4::class)
class RetryActivityRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val environment = LocaleTestEnvironment()
    private val repository = ProgressRepository(RejectedNameStore())
    private val observations = mutableListOf<String>()

    @Test
    fun failedNameAcrossRealActivityAndLocaleReplacementKeepsCommittedData() {
        val previousLocale = environment.setPlatformLanguage("")
        try {
            render()
            failName("Draft")
            observe("failed-before-recreate")
            val first = compose.activity
            compose.activityRule.scenario.recreate()
            assertNotSame(first, compose.activity)
            render()
            observe("after-real-recreate")

            failName("DraftTwo")
            observe("failed-before-locale")
            val second = compose.activity
            environment.setPlatformLanguage("fi")
            compose.waitUntil(10_000) { compose.activity !== second }
            assertNotSame(second, compose.activity)
            render()
            observe("after-locale-replacement")
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(checkNotNull(context.getExternalFilesDir(null)), "edge-retry-recreation.txt")
                .writeText(observations.joinToString("\n"))
        } finally {
            environment.setPlatformLanguage(previousLocale)
        }
    }

    private fun render() {
        val application = compose.activity.application as ClocklingsApplication
        val services =
            object : AppServices by application.container {
                override val repository = this@RetryActivityRecreationTest.repository
            }
        val factory = viewModelFactory { initializer { AppViewModel(services) } }
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val app = viewModel<AppViewModel>(factory = factory)
                val progress by repository.progress.collectAsStateWithLifecycle(initialValue = Progress())
                CompositionLocalProvider(LocalAppViewModel provides app) {
                    AppLocaleProvider(repository) {
                        ClocklingsTheme {
                            WardrobeScreen(CompanionKind.CAT, progress, CompanionColor.ORIGINAL, {})
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun failName(value: String) {
        val res = compose.activity.resources
        compose
            .onNodeWithContentDescription(res.getString(R.string.doll_name_label))
            .performTextReplacement(value)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(res.getString(R.string.settings_retry)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun observe(boundary: String) {
        compose.waitForIdle()
        val res = compose.activity.resources
        val draft =
            compose
                .onNode(hasSetTextAction())
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text
        val retryCount = compose.onAllNodesWithText(res.getString(R.string.settings_retry)).fetchSemanticsNodes().size
        val errorCount =
            compose
                .onAllNodesWithText(
                    res.getString(R.string.settings_save_error),
                ).fetchSemanticsNodes()
                .size
        val saved = runBlocking { repository.progress.first().nameFor(CompanionKind.CAT) }
        assertEquals("Failed write must never change committed name", "", saved)
        observations += "$boundary: draft=$draft; retryCount=$retryCount; errorCount=$errorCount; committed='$saved'"
    }
}

private class RejectedNameStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        throw IOException("Synthetic name write failure")
}
