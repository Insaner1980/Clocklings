package fi.clocklings

import androidx.compose.animation.SharedTransitionLayout
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import fi.clocklings.ui.LocalSharedTransitionScope
import fi.clocklings.ui.appearance.AppearanceScreen
import fi.clocklings.ui.appearance.LocalCompanionOutfit
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
import fi.clocklings.ui.locale.AppLocaleProvider
import fi.clocklings.ui.locale.LocalAppLanguage
import fi.clocklings.ui.navigationTransition
import fi.clocklings.ui.rememberReduceMotion
import fi.clocklings.ui.settings.LanguageScreen
import fi.clocklings.ui.settings.SettingsScreen
import fi.clocklings.ui.theme.ClocklingsTheme

@Composable
// Keep all Navigation 3 route bindings visible in one declarative graph.
@Suppress("LongMethod")
fun ClocklingsApp(
    modifier: Modifier = Modifier,
    container: AppServices = (LocalContext.current.applicationContext as ClocklingsApplication).container,
    app: AppViewModel = viewModel(factory = viewModelFactory { initializer { AppViewModel(container) } }),
) {
    AppLocaleProvider(container.repository) {
        val language = LocalAppLanguage.current
        DisposableEffect(language) {
            app.services.setSpeechLanguage(language)
            onDispose { app.stopSpeech() }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { app.services.refreshSpeech() }
        ClocklingsTheme {
            Box(modifier.fillMaxSize()) {
                val progress by app.progress.collectAsStateWithLifecycle()
                val settings by app.settings.collectAsStateWithLifecycle()
                // Tallennus luetaan muutamassa millisekunnissa; siihen asti näytetään pelkkä tausta.
                val p = progress ?: return@Box
                val s = settings ?: return@Box
                val speech by app.speechAvailable.collectAsStateWithLifecycle()
                CompositionLocalProvider(
                    LocalAppViewModel provides app,
                    LocalPersonalization provides s.personalization,
                    LocalCompanionOutfit provides p.outfitFor(s.personalization.companion),
                ) {
                    ConfettiHost {
                        // Aloitusnäkymä jokaisella käynnistyksellä; ei palaa näkyviin, kun näkymä luodaan uudelleen.
                        var introDone by rememberSaveable { mutableStateOf(false) }
                        val backStack = rememberNavBackStack(ScreenSavedState, Screen.Home)
                        DisposableEffect(backStack.last()) {
                            onDispose { app.stopSpeech() }
                        }
                        val nav = remember(backStack) { Navigator(backStack) }
                        // Ignore repeated or delayed callbacks from a destination that has already left.
                        val go = { from: Screen, screen: Screen ->
                            if (backStack.last() == from) {
                                app.play(Sound.POP)
                                nav.push(screen)
                            }
                        }
                        val back = { from: Screen ->
                            if (backStack.last() == from) {
                                app.play(Sound.POP)
                                nav.pop()
                            }
                        }
                        val playLevel = { from: Screen, level: Int, replaceTop: Boolean ->
                            if (backStack.last() == from) {
                                app.play(Sound.POP)
                                nav.startLevel(level, app.startLevel(level), replaceTop)
                            }
                        }
                        val reducedMotion = rememberReduceMotion()
                        SharedTransitionLayout {
                            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                                NavDisplay(
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    transitionSpec = { navigationTransition(reducedMotion) },
                                    popTransitionSpec = { navigationTransition(reducedMotion, back = true) },
                                    predictivePopTransitionSpec = { edge ->
                                        navigationTransition(reducedMotion, back = true, predictiveEdge = edge)
                                    },
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
                                                        Screen.Home,
                                                        when (destination) {
                                                            HomeDestination.KOULU -> {
                                                                Screen.LessonList
                                                            }

                                                            HomeDestination.VAPAA -> {
                                                                Screen.FreeClock
                                                            }

                                                            HomeDestination.PELAA -> {
                                                                Screen.Levels
                                                            }

                                                            HomeDestination.KYSY -> {
                                                                Screen.Ask
                                                            }

                                                            HomeDestination.NUKKE -> {
                                                                Screen.Personalize
                                                            }

                                                            HomeDestination.SETTINGS -> {
                                                                Screen.Settings
                                                            }
                                                        },
                                                    )
                                                })
                                            }
                                            entry<Screen.Settings> {
                                                SettingsScreen(
                                                    s,
                                                    speech,
                                                    { go(Screen.Settings, Screen.Language) },
                                                    { back(Screen.Settings) },
                                                )
                                            }
                                            entry<Screen.Language> { LanguageScreen({ back(Screen.Language) }) }
                                            entry<Screen.FreeClock> {
                                                FreeClockScreen(s.h24, s.help, speech, { back(Screen.FreeClock) })
                                            }
                                            entry<Screen.Ask> { AskScreen(s.h24, s.help, speech, { back(Screen.Ask) }) }
                                            entry<Screen.Wardrobe> {
                                                val kind = s.personalization.companion
                                                WardrobeScreen(kind, p, s.personalization.styleFor(kind).color, {
                                                    back(Screen.Wardrobe)
                                                })
                                            }
                                            entry<Screen.Personalize> {
                                                AppearanceScreen(
                                                    s.personalization,
                                                    p,
                                                    { go(Screen.Personalize, Screen.Wardrobe) },
                                                    { back(Screen.Personalize) },
                                                )
                                            }
                                            entry<Screen.LessonList> {
                                                LessonListScreen(
                                                    s.h24,
                                                    p,
                                                    onPlayLevel = { playLevel(Screen.LessonList, it, false) },
                                                    onOpen = { go(Screen.LessonList, Screen.Lesson(listOf(it))) },
                                                    onBack = { back(Screen.LessonList) },
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
                                                    onPlay = { playLevel(Screen.Levels, it, false) },
                                                    onLesson = {
                                                        go(Screen.Levels, Screen.Lesson(Lessons.levelLessons[it]))
                                                    },
                                                    onChallenge = { go(Screen.Levels, Screen.Challenge) },
                                                    onBack = { back(Screen.Levels) },
                                                )
                                            }
                                            entry<Screen.Game> { key ->
                                                GameRoute(
                                                    key,
                                                    s.h24,
                                                    s.help,
                                                    onBack = { back(key) },
                                                    onFinish = { nav.replace(it) },
                                                )
                                            }
                                            entry<Screen.Result> { key ->
                                                ResultScreen(
                                                    result = key,
                                                    outfit = p.outfitFor(s.personalization.companion),
                                                    onPlayAgain = {
                                                        app.play(Sound.POP)
                                                        nav.replace(Screen.Game(key.level, round = System.nanoTime()))
                                                    },
                                                    onNextLevel = { playLevel(key, key.level + 1, true) },
                                                    onDress = {
                                                        app.play(Sound.POP)
                                                        nav.replace(Screen.Wardrobe)
                                                    },
                                                    onLevels = {
                                                        app.play(Sound.POP)
                                                        nav.popTo(Screen.Levels)
                                                    },
                                                    onSound = app::play,
                                                )
                                            }
                                            entry<Screen.Challenge> {
                                                ChallengeRoute(s.h24, s.help, { back(Screen.Challenge) })
                                            }
                                        },
                                )
                            }
                        }
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
}
