package fi.clocklings

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.transformationId
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.itemName
import fi.clocklings.ui.theme.ClocklingsTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class WardrobeResizeDeviceTest : StorageDeviceTest() {
    @Test
    fun pendingTransformationCommitsAfterWindowCrossesPaneBreakpoint() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("wardrobeResize") == "true")
        val size = displayOverride("size")
        val density = displayOverride("density")
        val job = SupervisorJob()
        val hold = AtomicBoolean(false)
        val writes = AtomicInteger()
        val entered = CompletableDeferred<Unit>()
        val writable = CompletableDeferred<Unit>()
        val file = files.newFolder().resolve("wardrobe.preferences_pb")
        val services =
            servicesFor(
                writeGatedPreferences(file, job) {
                    if (hold.get()) {
                        writes.incrementAndGet()
                        entered.complete(Unit)
                        writable.await()
                    }
                },
            )
        val item = Wardrobe.item("car-model-fire-engine")
        val title = context.getString(itemName(item.id))
        val tile = compose.onNode(hasText(title) and hasClickAction())
        val stage =
            compose.onNode(
                hasTestTag("companion_CAR") and hasAnyAncestor(hasTestTag("adaptive_visual")),
                useUnmergedTree = true,
            )
        try {
            runBlocking { services.repository.updateProgress { Progress(stars = List(5) { 3 }) } }
            shell("wm density 160")
            resize(320, 900)
            val model = render(services)
            assertArrangement(sideBySide = false)
            hold.set(true)
            tile.scrollToIfScrollable().assertIsNotSelected().performClick()
            runBlocking { withTimeout(10_000) { entered.await() } }

            val beforeResize = compose.activity
            resize(960, 1100)
            if (compose.activity !== beforeResize) assertSame(model, render(services))
            assertArrangement(sideBySide = true)
            assertTransformationPending(services.repository, tile)
            val before = stage.scrollToIfScrollable().assertIsDisplayed().captureToImage()

            writable.complete(Unit)
            awaitTransformationCommit(services.repository, item)
            tile.scrollToIfScrollable().assertIsSelected()
            val after = stage.scrollToIfScrollable().assertIsDisplayed().captureToImage()
            assertPreviewChanged(before, after)
            assertEquals(1, writes.get())
            runBlocking { job.cancelAndJoin() }
            assertRestoredTransformation(file, item.id)
        } finally {
            writable.cancel()
            runBlocking { job.cancelAndJoin() }
            shell("wm size $size")
            shell("wm density $density")
        }
    }

    private fun assertTransformationPending(
        repository: ProgressRepository,
        tile: SemanticsNodeInteraction,
    ) {
        val pending =
            runBlocking {
                repository.progress
                    .first()
                    .outfitFor(CompanionKind.CAR)
            }
        assertEquals(null, pending.transformationId())
        tile.scrollToIfScrollable().assertIsNotSelected()
    }

    private fun awaitTransformationCommit(
        repository: ProgressRepository,
        item: Item,
    ) {
        runBlocking {
            withTimeout(10_000) {
                repository.progress.first { it.outfitFor(CompanionKind.CAR).isOn(item) }
            }
        }
    }

    private fun assertPreviewChanged(
        before: ImageBitmap,
        after: ImageBitmap,
    ) {
        assertEquals(before.width, after.width)
        assertEquals(before.height, after.height)
        val previousPixels = before.toPixelMap()
        val currentPixels = after.toPixelMap()
        val changed =
            (0 until after.height).sumOf { y ->
                (0 until after.width).count { x -> previousPixels[x, y] != currentPixels[x, y] }
            }
        assertTrue("Full preview changes to the equipped model", changed > after.width * after.height / 10)
    }

    private fun assertRestoredTransformation(
        file: File,
        expectedId: String,
    ) {
        val reopenJob = SupervisorJob()
        try {
            val reopened =
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(reopenJob + Dispatchers.IO),
                ) { file }
            val restored = runBlocking { ProgressRepository(reopened).progress.first() }
            assertEquals(expectedId, restored.outfitFor(CompanionKind.CAR).transformationId())
        } finally {
            runBlocking { reopenJob.cancelAndJoin() }
        }
    }

    private fun render(services: AppServices): AppViewModel {
        lateinit var model: AppViewModel
        val factory = viewModelFactory { initializer { AppViewModel(services) } }
        compose.activityRule.scenario.onActivity { activity ->
            model = ViewModelProvider(activity, factory)[AppViewModel::class]
            activity.setContent {
                val progress by model.progress.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalAppViewModel provides model) {
                    ClocklingsTheme {
                        progress?.let { WardrobeScreen(CompanionKind.CAR, it, CompanionColor.ORIGINAL, {}) }
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("adaptive_visual"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        return model
    }

    private fun assertArrangement(sideBySide: Boolean) {
        val visual = compose.onNodeWithTag("adaptive_visual", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val controls = compose.onNodeWithTag("adaptive_controls", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(
            "Expected sideBySide=$sideBySide, visual=$visual, controls=$controls",
            if (sideBySide) controls.left >= visual.right else controls.top >= visual.bottom,
        )
    }

    private fun resize(
        width: Int,
        height: Int,
    ) {
        shell("wm size ${width}x$height")
        compose.waitUntil(15_000) {
            val activity = compose.activity
            val decor = activity.window.decorView
            decor.width == width && decor.height == height && activity.resources.configuration.densityDpi == 160
        }
    }

    private fun displayOverride(setting: String): String =
        shell("wm $setting")
            .lineSequence()
            .firstOrNull { it.startsWith("Override ") }
            ?.substringAfter(": ")
            ?.trim()
            ?: "reset"

    private fun shell(command: String): String =
        ParcelFileDescriptor
            .AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command),
            ).use { it.bufferedReader().readText() }
}
