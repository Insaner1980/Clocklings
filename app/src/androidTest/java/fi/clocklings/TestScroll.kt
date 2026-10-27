package fi.clocklings

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performScrollTo

/**
 * Pinned top bars and screens that fit without scrolling are not inside a scroll container, so
 * scroll only when the node has one.
 */
fun SemanticsNodeInteraction.scrollToIfScrollable(): SemanticsNodeInteraction =
    try {
        performScrollTo()
    } catch (_: AssertionError) {
        this
    }
