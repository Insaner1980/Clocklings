package fi.clocklings

import android.app.LocaleManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.AppLanguage
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import kotlinx.coroutines.runBlocking
import java.util.Locale

internal class LocaleTestEnvironment {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val context get() = instrumentation.targetContext
    val repository get() = (context.applicationContext as ClocklingsApplication).container.repository

    fun resources(language: AppLanguage) =
        context
            .createConfigurationContext(
                Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language.tag)) },
            ).resources

    fun prepare(
        progress: Progress,
        settings: Settings,
    ): String {
        runBlocking {
            repository.updateProgress { progress }
            repository.updateSettings { settings }
            repository.setLegacyLanguage(if (Build.VERSION.SDK_INT < 33) "en" else null)
        }
        return setPlatformLanguage("en")
    }

    fun setPlatformLanguage(tag: String): String {
        var previous = ""
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.runOnMainSync {
                val manager = context.getSystemService(LocaleManager::class.java)
                previous = manager.applicationLocales.toLanguageTags()
                manager.applicationLocales = LocaleList.forLanguageTags(tag)
            }
        }
        return previous
    }

    fun changeLanguage(
        language: AppLanguage,
        scenario: ActivityScenario<MainActivity>,
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            setPlatformLanguage(language.tag)
        } else {
            runBlocking { repository.setLegacyLanguage(language.tag) }
            scenario.recreate()
        }
    }
}
