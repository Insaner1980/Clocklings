package fi.clocklings

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.ui.companion.RenderedAtlas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Compare Android's actual PNG and WebP decoders, including transparent edge pixels. */
@RunWith(AndroidJUnit4::class)
class RenderedAssetIntegrityDeviceTest {
    @Test
    fun everyPackagedAtlasMatchesItsOriginalPixels() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val context = instrumentation.targetContext
        val originals =
            assets.list("").orEmpty().asList() +
                assets.list("transformations").orEmpty().map { "transformations/$it" }
        val atlases = originals.filter { "rendered" in it && it.endsWith(".png") }
        // 42 base/wardrobe atlases, nine transformed originals and three dyes for eight companions.
        assertEquals(
            "Every character, accessory and transformation atlas must be checked",
            42 + 9 + 3 * 8,
            atlases.size,
        )
        for (path in atlases) {
            val name = path.substringAfterLast('/')
            val resource = context.resources.getIdentifier(name.removeSuffix(".png"), "drawable", context.packageName)
            assertTrue("Missing Android resource for $name", resource != 0)
            val original = assets.open(path).use { checkNotNull(BitmapFactory.decodeStream(it)) }
            val atlas = RenderedAtlas.load(context.resources, resource)
            val packaged = atlas.asAndroidBitmap()
            try {
                assertEquals("Width changed: $name", original.width, packaged.width)
                assertEquals("Height changed: $name", original.height, packaged.height)
                assertTrue("Android decoded pixels changed: $name", original.sameAs(packaged))
                assertSame(
                    "Visible previews must share their atlas: $name",
                    atlas,
                    RenderedAtlas.load(context.resources, resource),
                )
            } finally {
                original.recycle()
            }
        }
    }
}
