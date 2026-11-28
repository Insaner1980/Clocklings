package fi.clocklings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.transformationIds
import fi.clocklings.ui.itemName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Exercise the real tile click, persisted outfit and both actual wardrobe render sizes. */
@RunWith(Parameterized::class)
class TransformationWardrobeDeviceTest(
    private val kind: CompanionKind,
) : RenderedWardrobeDeviceTest(kind) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun companions() = transformationIds.keys.toList()
    }

    @Test
    fun selectEachModelAndReturnHome() {
        openWardrobe(captureScreens = false)
        val args = InstrumentationRegistry.getArguments()
        args.getString("bodyColor")?.let { if (kind != CompanionKind.HUMAN) selectColor(it) }
        args.getString("widthDp")?.let { expected ->
            val config = compose.activity.resources.configuration
            org.junit.Assert.assertEquals(expected.toInt(), config.screenWidthDp)
            org.junit.Assert.assertEquals(args.getString("fontScale")!!.toFloat(), config.fontScale, .001f)
        }
        val fullGallery = args.getString("case") == "all"
        if (fullGallery) capture(companionPreview(), "${kind.name}_base")
        val items = transformationIds.getValue(kind)
        for (id in if (fullGallery) items else items.take(1)) {
            val label = compose.activity.getString(itemName(id))
            selectItem(label)
            val tile = compose.onNodeWithTag("item_art_$id", useUnmergedTree = true)
            tile.scrollToIfScrollable().assertIsDisplayed()
            capture(tile, id + "_tile")
            val stage = companionPreview()
            // The scroll fade overlays a preview reached by scrolling only to its upper edge.
            val page =
                compose.onNode(
                    hasScrollAction() and hasAnyDescendant(hasTestTag("companion_${kind.name}")),
                    useUnmergedTree = true,
                )
            val offset = page.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            page.performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(0f, -offset) }
            compose.waitForIdle()
            stage.assertIsDisplayed()
            capture(stage, id + "_equipped")
        }
        repeat(2) {
            compose.activityRule.scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
        }
        compose.onNodeWithTag("home_stage", useUnmergedTree = true).assertExists()
        capture(compose.onNodeWithTag("companion_${kind.name}", useUnmergedTree = true), kind.name + "_home")
    }

    private fun capture(
        node: SemanticsNodeInteraction,
        name: String,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val case = InstrumentationRegistry.getArguments().getString("case", "default")
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "outfit-review/$case")
        dir.mkdirs()
        val bitmap = node.captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
