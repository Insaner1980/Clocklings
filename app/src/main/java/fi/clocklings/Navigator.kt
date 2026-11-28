package fi.clocklings

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import fi.clocklings.domain.AccessPolicy

/** Paluupinon muokkaus yhdessä paikassa. Koti on aina pohjalla. */
@Stable
class Navigator(
    private val stack: MutableList<NavKey>,
    private val beforeNavigate: () -> Unit = {},
    private val onDestinationChanged: (Screen) -> Unit = {},
    private val access: () -> AccessPolicy,
) {
    init {
        // Restored routes are checked before any restricted ViewModel is constructed.
        val policy = access()
        val restored =
            buildList<NavKey> {
                stack.forEach { key ->
                    val screen = key as Screen
                    appendDestination(screen, screen.accessibleDestination(policy))
                }
            }
        if (restored != stack) {
            stack.clear()
            stack.addAll(restored)
        }
    }

    /** Ignore callbacks from a destination that has already left the top of the stack. */
    fun ifCurrent(
        screen: Screen,
        action: () -> Unit,
    ) {
        if (stack.last() == screen) action()
    }

    fun push(screen: Screen) =
        navigate {
            stack.appendDestination(screen, screen.accessibleDestination(access()))
        }

    fun pop() =
        navigate {
            if (stack.size > 1) stack.removeAt(stack.lastIndex)
            val screen = stack.last() as Screen
            val target = screen.accessibleDestination(access())
            if (target != screen) {
                stack.removeAt(stack.lastIndex)
                stack.appendDestination(screen, target)
            }
        }

    /** Korvaa päällimmäisen näkymän (esim. peli → tulos), jolloin takaisin vie edelliseen näkymään. */
    fun replace(screen: Screen) =
        navigate {
            val target = screen.accessibleDestination(access())
            if (stack.size > 1) stack.removeAt(stack.lastIndex)
            stack.appendDestination(screen, target)
        }

    /** Palaa pinossa näkymään [screen] tai lisää sen kodin päälle. */
    fun popTo(screen: Screen) =
        navigate {
            val target = screen.accessibleDestination(access())
            if (!stack.popToExisting(target)) {
                while (stack.size > 1) stack.removeAt(stack.lastIndex)
                stack.add(target)
            }
        }

    /** Tason aloitus: ensin oppitunnit, jos niitä on, muuten suoraan peliin. */
    fun startLevel(
        level: Int,
        lessonQueue: List<String>,
        replaceTop: Boolean,
    ) {
        if (!access().level(level)) return
        val allowedQueue = access().lessonQueue(lessonQueue)
        val target =
            if (allowedQueue.isEmpty()) {
                Screen.Game(level, round = System.nanoTime())
            } else {
                Screen.Lesson(allowedQueue, thenLevel = level)
            }
        if (replaceTop) replace(target) else push(target)
    }

    private inline fun navigate(change: () -> Unit) {
        val previous = stack.last()
        val depth = stack.size
        beforeNavigate()
        change()
        val destination = stack.last() as Screen
        if (stack.size != depth || destination != previous) onDestinationChanged(destination)
    }
}

private fun MutableList<NavKey>.appendDestination(
    requested: Screen,
    target: Screen,
) {
    if (lastOrNull() == target && (target == Screen.Levels || target == Screen.LessonList)) return
    // A fallback already in the stack owns its existing entry state.
    if (target == requested || !popToExisting(target)) add(target)
}

private fun MutableList<NavKey>.popToExisting(target: Screen): Boolean {
    val index = indexOfLast { it == target }
    if (index < 0) return false
    while (lastIndex > index) removeAt(lastIndex)
    return true
}

internal fun Screen.accessibleDestination(access: AccessPolicy): Screen =
    when (this) {
        is Screen.Game -> {
            if (access.level(level)) this else Screen.Levels
        }

        is Screen.Lesson -> {
            val allowed = access.lessonQueue(queue)
            if (allowed.isEmpty()) {
                Screen.LessonList
            } else {
                copy(
                    queue = allowed,
                    thenLevel = thenLevel?.takeIf(access::level),
                )
            }
        }

        Screen.Challenge -> {
            if (access.challenge()) this else Screen.Levels
        }

        else -> {
            this
        }
    }
