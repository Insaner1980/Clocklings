package fi.clocklings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import fi.clocklings.audio.Sound
import fi.clocklings.domain.Lessons
import fi.clocklings.ui.AppViewModel
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.appearance.LocalPersonalization
import fi.clocklings.ui.ask.AskScreen
import fi.clocklings.ui.challenge.ChallengeRoute
import fi.clocklings.ui.companion.WardrobeScreen
import fi.clocklings.ui.components.ConfettiHost
import fi.clocklings.ui.freeclock.FreeClockScreen
import fi.clocklings.ui.game.GameRoute
import fi.clocklings.ui.game.ResultScreen
import fi.clocklings.ui.home.HomeDestination
import fi.clocklings.ui.home.HomeScreen
import fi.clocklings.ui.intro.IntroOverlay
import fi.clocklings.ui.intro.coveredByIntro
import fi.clocklings.ui.lesson.LessonListScreen
import fi.clocklings.ui.lesson.LessonRoute
import fi.clocklings.ui.levels.LevelsScreen
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.dotBackground

@Composable
// Keep all Navigation 3 route bindings visible in one declarative graph.
@Suppress("LongMethod")
fun ClocklingsApp(
    modifier: Modifier = Modifier,
    container: AppServices = (LocalContext.current.applicationContext as ClocklingsApplication).container,
    app: AppViewModel = viewModel(factory = viewModelFactory { initializer { AppViewModel(container) } }),
) {
    ClocklingsTheme {
        Box(modifier.fillMaxSize().dotBackground()) {
            val progress by app.progress.collectAsStateWithLifecycle()
            val settings by app.settings.collectAsStateWithLifecycle()
            // Tallennus luetaan muutamassa millisekunnissa; siihen asti näytetään pelkkä tausta.
            val p = progress ?: return@Box
            val s = settings ?: return@Box
            val speech by app.speechAvailable.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppViewModel provides app, LocalPersonalization provides s.personalization) {
                ConfettiHost {
                    // Aloitusnäkymä jokaisella käynnistyksellä; ei palaa näkyviin, kun näkymä luodaan uudelleen.
                    var introDone by rememberSaveable { mutableStateOf(false) }
                    val backStack = rememberNavBackStack(ScreenSavedState, Screen.Home)
                    DisposableEffect(backStack.last()) {
                        onDispose { app.stopSpeech() }
                    }
                    val nav = remember(backStack) { Navigator(backStack) }
                    val go = { screen: Screen ->
                        app.play(Sound.POP)
                        nav.push(screen)
                    }
                    val back = {
                        app.play(Sound.POP)
                        nav.pop()
                    }
                    val playLevel = { level: Int, replaceTop: Boolean ->
                        app.play(Sound.POP)
                        nav.startLevel(level, app.startLevel(level), replaceTop)
                    }
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.coveredByIntro(!introDone),
                        onBack = { nav.pop() },
                        entryDecorators =
                            listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                        entryProvider =
                            entryProvider {
                                entry<Screen.Home> {
                                    HomeScreen(p, s, onOpen = { destination ->
                                        go(
                                            when (destination) {
                                                HomeDestination.KOULU -> Screen.LessonList
                                                HomeDestination.VAPAA -> Screen.FreeClock
                                                HomeDestination.PELAA -> Screen.Levels
                                                HomeDestination.KYSY -> Screen.Ask
                                                HomeDestination.NUKKE -> Screen.Personalize()
                                                HomeDestination.CLOCK_STYLES -> Screen.Personalize(clocks = true)
                                            },
                                        )
                                    })
                                }
                                entry<Screen.FreeClock> { FreeClockScreen(s.h24, s.help, speech, back) }
                                entry<Screen.Ask> { AskScreen(s.h24, s.help, speech, back) }
                                entry<Screen.Wardrobe> {
                                    val kind = s.personalization.companion
                                    WardrobeScreen(kind, p, s.personalization.styleFor(kind).color, back)
                                }
                                entry<Screen.Personalize> { key ->
                                    AppearanceScreen(s.personalization, p, key.clocks, { go(Screen.Wardrobe) }, back)
                                }
                                entry<Screen.LessonList> {
                                    LessonListScreen(
                                        s.h24,
                                        p.lessonsDone,
                                        onOpen = { go(Screen.Lesson(listOf(it))) },
                                        onBack = back,
                                    )
                                }
                                entry<Screen.Lesson> { key ->
                                    LessonRoute(
                                        key = key,
                                        speechAvailable = speech,
                                        onLeave = { nav.pop() },
                                        onStartGame = { level ->
                                            nav.replace(Screen.Game(level, round = System.nanoTime()))
                                        },
                                    )
                                }
                                entry<Screen.Levels> {
                                    LevelsScreen(
                                        progress = p,
                                        onPlay = { playLevel(it, false) },
                                        onLesson = { go(Screen.Lesson(Lessons.levelLessons[it])) },
                                        onChallenge = { go(Screen.Challenge) },
                                        onBack = back,
                                    )
                                }
                                entry<Screen.Game> { key ->
                                    GameRoute(key, s.h24, s.help, onBack = back, onFinish = { nav.replace(it) })
                                }
                                entry<Screen.Result> { key ->
                                    ResultScreen(
                                        result = key,
                                        outfit = p.outfitFor(s.personalization.companion),
                                        onPlayAgain = {
                                            app.play(Sound.POP)
                                            nav.replace(Screen.Game(key.level, round = System.nanoTime()))
                                        },
                                        onNextLevel = { playLevel(key.level + 1, true) },
                                        onDress = {
                                            app.play(Sound.POP)
                                            nav.replace(Screen.Wardrobe)
                                        },
                                        onLevels = {
                                            app.play(Sound.POP)
                                            nav.popTo(Screen.Levels)
                                        },
                                    )
                                }
                                entry<Screen.Challenge> { ChallengeRoute(s.h24, s.help, back) }
                            },
                    )
                    if (!introDone) {
                        IntroOverlay(
                            p.outfitFor(s.personalization.companion),
                            onTick = { app.play(Sound.TICK) },
                            onDone = { introDone = true },
                        )
                    }
                }
            }
        }
    }
}
