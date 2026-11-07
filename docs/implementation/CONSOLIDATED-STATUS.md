# Clocklings — consolidated implementation and verification status

Updated: 7 November 2026.

This is the single current summary of implemented functionality, the Android/Google audit follow-ups, adaptive-layout work, actual validation and remaining limits. It supersedes older workload counts, desktop-scope deferrals, rejected short-window screenshots and outstanding-cleanup statements in the historical reports. Those reports remain evidence, not separate current task lists.

The 6 November consolidation reviewed retained evidence and current file identities without rerunning validation. The PR #13 follow-up below records the additional 7 November checks. Historical test results retain their original version and environment boundaries.

## Current position

- The scoped application corrections and adaptive implementation are complete and have the validation described below. The reviewed final records identify no outstanding reproduced application defect requiring another input audit or layout correction.
- Supported product scope includes small and large Android phones, tablets, Chromebooks and Googlebook devices. The later owner instruction supersedes the audit's earlier desktop/laptop deferral. Selected product scope is not a claim of verified compatibility on every device/runtime.
- The owner has no equipment available for the additional hardware/accessibility checks discussed at handoff. These are recorded as unavailable evidence, not required owner tasks, purchasing requirements or reasons to reopen completed work.
- Additional emulator-based checks can be considered separately. An old physical Android phone is not inherently required for API 26 validation: API 26 tests already ran on a dedicated emulator before the adaptive changes.
- Publication, signing, licensing/provenance and privacy/store work remain deliberately deferred. This document does not declare release readiness.
- The audit/adaptive work was committed and pushed as `49e1c16` on `codex/adaptive-layouts`. The owner opened [PR #13](https://github.com/Insaner1980/Clocklings/pull/13); its review follow-up is recorded below.

## PR #13 review follow-up, 7 November

CodeRabbit and Greptile's 17 inline findings at `49e1c16` were checked against the implementation. Eleven prompted corrections: API-compatible IME/cutout reads, preservation of emulator rotation/display overrides, safe fixture-parent logging, non-masking split-screen teardown, optional page-size expectations, rejected SoundPool sample handling, removal of a diagnostic print, independent startup-loading coverage, a one-way restored-scroll inset readiness latch, and the toolchain/local-evidence clarifications in this document. Focused regressions cover rejected audio samples and retained scaffold content during later inset changes.

Six findings did not warrant changes:

- `ActivityRetentionDeviceTest`: all six `ActivityScenario.onActivity` calls must complete; unavailable activities or failed recreation throw rather than silently skipping a reference. See the [ActivityScenario contract](https://developer.android.com/reference/androidx/test/core/app/ActivityScenario).
- `AdaptiveWindowDeviceTest`: explicit expected layout thresholds provide an independent test oracle; production thresholds were not changed.
- `AuditInputMethodService`: Android creates the IME window before `onCreateInputView`; the potentially absent root insets are already checked. See the [framework lifecycle implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/inputmethodservice/InputMethodService.java).
- `AuditSpeechService`: synthesis requests run serially on the synthesis thread, and the callback blocks until completion. The claimed concurrent pending-latch replacement does not occur under the [TextToSpeechService contract](https://developer.android.com/reference/android/speech/tts/TextToSpeechService).
- `Sfx`: the local `loaded` name is a naming preference without a behavior defect.
- `resumedActivities`: the inspected lifecycle and audio-setting callers run on the main thread, including the delayed lifecycle coroutine; no cross-thread access was found.

Validation with AGP 9.5.1: 20 targeted JVM cases passed (`SfxFailureTest`, `SpeechTest`, `StartupStateTest` on API 26/33, and `ScaffoldInsetsTest`). Debug app/test APK builds, ktlint, Detekt, Android lint, and debug/release stability checks passed. Eight instrumented cases passed on the dedicated API 36 `ClocklingsReview20261030` emulator: native-library loading with an unspecified page-size expectation, multi-window audio ownership, live resizing/rotation/recreation, four real-IME runs at 411/320 dp and 100%/200% font scale (each covering English, Finnish, Swedish and German), and native composition/replacement/commit/cancellation. The fixture-IME case initially timed out before the keyboard appeared; enabling hardware-keyboard IME display and reselecting the fixture made the unchanged test pass. Emulator settings were restored afterwards.

The final review patch used targeted validation, not another complete 407-case JVM run. API 26 instrumented execution, 16 KB runtime execution, physical devices, and an online policy review were not repeated in this follow-up.

## Implemented application

| Area | Implemented behavior |
| --- | --- |
| Identity and architecture | Independent Clocklings application (`fi.clocklings`) with its own launcher/splash branding, Kotlin, Jetpack Compose, Navigation 3, manual AppServices and Preferences DataStore. The original private application's source and data remain separate. |
| Learning | Eight standard lessons, optional 24-hour lesson, demonstrations, quizzes and a learning path connected to the existing five practice levels. Completion/progress and retry behavior are implemented. |
| Practice and challenge | Read/set clock practice, feedback, results, best stars and the optional one-minute challenge with an absolute monotonic deadline, best score and replay/retry actions. |
| Explore and Play Together | Interactive clock exploration; set, ask, reveal and swap turns in Play Together. Hidden answers stay hidden from accessibility until revelation. |
| Personalization | Nine companions, no-companion mode, companion names/colors/outfits, recorded companion voices, eight clock appearances including constrained custom styling, and local saved choices. Stars unlock thresholds rather than being spent. Bunny, car and unicorn were completed after the earlier pilot. |
| Language | English, Finnish, shared Swedish, German, French, Spain Spanish and Brazilian Portuguese throughout content, controls and accessibility text. Device `pt` and `pt-PT` deliberately use Brazilian content. Language-specific clock mathematics/wording, half-hour conventions and 24-hour explanations have independent fixtures. |
| Audio | Recorded effects and on-demand speech using an eligible installed offline voice in the content language. Brazilian speech requires a Brazilian voice. Learning works without an eligible voice; absent effects are silent rather than queued for later playback. |
| Settings and data | Language, sound/speech, motion and existing learning/display preferences; progress reset; local persistence; visible failure/retry paths. No new account, analytics, backend or data-transfer feature was added by this work. |
| Accessibility and input | Accessible names/states, answer feedback, visible keyboard focus, clock arrow-key controls, pointer interactions, enlarged-text layouts, IME/inset handling and corrected edit-history routing. Human and automated evidence have different scopes, recorded below. |

The current toolchain uses AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Material 3 1.4.0, Navigation 3 1.2.0, Lifecycle 2.11.0 and DataStore 1.3.0; minSdk 26, target/compileSdk 37. The historical audit/adaptive checks below used AGP 9.5.0. PR #12 updated AGP separately on 7 November; the subsequent 407-test suite, debug app/test builds, ktlint, Detekt, lint and stability checks passed with 9.5.1 before commit `49e1c16`. Adaptive work introduced no dependency upgrade, SDK change, navigation redesign, text-field migration or new experimental API. Earlier audit code already had a narrowly scoped BringIntoViewSpec opt-in; this is not presented as a new adaptive migration.

## Application corrections completed during the audit

| Area | Completed correction or verified contract |
| --- | --- |
| Startup and storage reads | Added visible loading while initial data is pending. Locale-read failure and Retry respect system insets. Initial I/O fallback does not overwrite stored progress; later failures retain known data and recovery remains possible. |
| Failed edits and retries | Failed companion-name drafts retain the error and Retry through restoration. Name, color and item operations keep their own failure state; an unrelated save cannot clear or retry the wrong operation. Preview/tab-switch and item-slot retry regressions were corrected in preceding reviews. |
| Pending saves and reset | Lesson completion praise waits for successful persistence while correct-answer feedback remains immediate. After a destructive reset is submitted, dismissal/cancellation is blocked until completion rather than implying the committed operation can be cancelled. Pending-write exit, failure, reopen and retry behavior was exercised. |
| Persistence boundaries | Real owned DataStore files were reopened and deliberately corrupted for recovery tests. A staged write was interrupted after serialization and before fsync/rename; old-value recovery and later successful commits were checked. These observations are not a universal power-loss guarantee. |
| Activity and process state | Audio ownership counts resumed Activities, so pausing one does not mute another resumed instance. Navigation/task/draft/selection/scroll state is retained across the tested resize, rotation and Activity recreation cases. Actual process-death observations follow the separately accepted unfinished-session restart policy; they do not promise seamless durable resume of every unfinished session. |
| Keyboard and focus | Shared controls and interactive clocks show visible focus. Clock arrows use existing nudge mathematics. Focus-driven scrolling keeps focused labels out of fades/system bars. Modal Cancel, Back and Escape and complete task journeys were verified. |
| Semantics | Correct/Incorrect answer state and a polite live region are exposed after feedback. Name-field errors are associated with the field. Inactive reserved Settings speech text is excluded from semantics. |
| Scroll and initial insets | Restored Settings scroll no longer clamps against a temporary zero-inset viewport or a shorter speech-availability message. Exact offset restoration is tested; no arbitrary delay or relaxed tolerance was substituted. |
| Audio and animation resources | Unsuccessful AudioTrack loads release resources; failed recorded-clip loads do not prevent later clips. Expired confetti stops unnecessary catch-up work. Finite speech bursts and native stall/background cancellation/recovery were tested. |
| Bounded work | Challenge answer-layout cache retains at most 16 configuration keys. Consecutive I/O retries back off from 1 to 30 seconds and reset after successful reading, reducing repeated reads/logging. |
| Large graphics | Oversized clock-style previews and compact Home artwork were constrained after rendered failures were reproduced. Later adaptive work extends the layout treatment across existing screens. |
| Gboard history | Exact Ctrl+Z / Ctrl+Shift+Z routing now reaches Compose's normal focused-view dispatch before the IME, preserving two undo and two redo operations. No custom undo store, production reflection or text-field API migration was added. |
| Test infrastructure | Replaced a fixed practice-progression sleep with an indexed-task wait. Controlled native IME/TTS fixtures observe current input requests and actual lifecycle events. The test receiver registration that blocked lint now uses `ContextCompat.registerReceiver` with the explicit exported flag needed by the separate test APK. |
| Localization lint | Investigated 27 plural-category warnings and two German repeated-word warnings. Proven unreachable count categories and grammatical false positives received element-specific documented exceptions with regression coverage; global rules were not disabled. Current lint's four warnings concern dependency updates, not those old translation warnings. |
| Dependency and notice evidence | Reconciled resolved dependencies, maintainer/source/advisory facts and 123 license-metadata rows; checked packaged font notices, corrected missing Protobuf attribution and reconciled Okio evidence. This does not establish owner artwork/audio/code rights or final-distribution licensing. Earlier DeepSec dependency/test corrections and their checks remain recorded in STATUS.md; no fresh vulnerability scan or remote-alert status is claimed here. |

## Adaptive implementation and final visual correction

Layout decisions use actual available width, height and font scale, including portrait, landscape, narrow, wide, short and resized windows. The Activity is resizable and no longer portrait-locked; touchscreen hardware is optional. Content/clock widths are bounded, and instructions remain readable rather than stretching indefinitely.

Home pairs its scene with its task menu when space permits. Explore, lessons, practice, challenge, Play Together and personalization pair visuals with controls where useful. Lists, results and settings retain their appropriate sequential layout. Narrow windows and enlarged text reflow and scroll. No navigation rail or unnecessary multi-pane navigation was added.

The owner correctly rejected the first polished short-window lesson/challenge/together images: visible primary buttons did not prove that playback/setup controls were unobscured. The final correction uses a dedicated compact arrangement below 480 dp height when width and font scale permit it:

- The clock uses the left column's height instead of being squeezed above a full-window footer.
- Instructions occupy the right pane; a separate action row reserves its own height and cannot cover them.
- Compact lesson playback controls sit beside Previous/Next. Compact Play Together places Random time beside Pass to partner and uses the existing instruction text without the decorative companion.
- Normal-height/narrow layouts retain the existing scaffold and tested challenge clock sizing, animation and reduced-motion behavior.
- Independent composition slots and explicit action-focus restoration preserve button identity, the selected primary action and scroll across layout thresholds. Play Together retains focus through phase changes.

The existing palette, theme tokens, seven languages, touch targets and Gboard correction were preserved. No decorative colored edge stripes or middle-dot separators were added. At enlarged text sizes, long instructions may require scrolling; all text is not promised to fit simultaneously.

## Validation actually performed

### Evidence rules

The original discovery mapped **877 checks across 60 sections**. Mapping is coverage of inspection, not 877 passing tests. Subsequent execution counts are snapshots from different versions and sometimes repeated configurations, so they must not be added into a single current test total. Superseded failures remain recorded. Automated semantics are not human TalkBack listening, and resized Android windows are not Chromebook/Googlebook runtime evidence.

### Build, local tests and static analysis

| Checkpoint | Actual result and version boundary |
| --- | --- |
| Earlier application/localization | Foundation, companions, artwork, settings, localization, persistence and review regressions were validated in their dated implementation checkpoints. Seven-language resource parity and checker self-tests passed. Historical gallery coverage includes new companions and their wardrobes; earlier pilot “still to do” notes were superseded. |
| Audit local corrections | Full JVM suites progressed through 395 and then 401 passing tests. Localization lint verification covered Android resource selection on API 26, 33 and 37. These are retained historical results, not a rerun of the final adaptive version. |
| Audit completion | 31 focused JVM tests passed, plus debug/test builds, ktlint, Detekt and both Compose stability checks. |
| Final input correction | Full JVM suite: **402 passed**, zero failures/errors/skips. Builds, ktlint and Detekt passed. |
| Initial adaptive implementation | Full JVM suite: **404 passed**, followed by two focused saved-state tests. Builds, ktlint, Detekt and stability checks passed. Lint subsequently passed after the receiver correction. |
| Adaptive polish intermediate version | Full JVM suite: **406 passed**. That invocation still failed style checks, which were subsequently corrected; later focused runs of 23 and then 12 tests and static checks passed. It is not described as a wholly successful initial Gradle invocation. |
| Final compact-layout version | **24 focused JVM tests passed**, zero failures/errors/skips: ChallengeInteractionTest 8, AdaptiveContentTest 4, ClockTaskScaffoldTest 1, StartupInsetsTest 1 and ScreenshotReviewUiTest 10. Debug app/test builds, ktlint, Detekt, lint and debug/release stability checks passed. |
| Current lint | **0 errors, 4 existing dependency-update warnings**. No upgrade was introduced merely to silence them. |
| Baselines and assertions | Compose stability signatures were inspected before accepting scoped baseline updates. Screenshot baselines were not regenerated to accept failures. Final visibility assertions remain strict; the action-focus test checks the second of two enabled buttons, node identity and saved scroll offset across layout thresholds. |

The last complete JVM suite ran before the final compact-layout refinement; the final version has the focused regression coverage above. Owner-run `lc`/`sc` and fresh CI were not run for the final audit/adaptive handoff. Earlier CI or security-check results do not establish a current remote result.

### Actual Android runtime coverage retained from the audit

| Environment / subject | Executed evidence | Limit |
| --- | --- | --- |
| API 26 and API 32 dedicated x86_64 emulators | Minimum and final legacy-locale endpoints; native composition/history, startup read fallback/recovery, language changes, Settings scroll/recreation, native libraries and TTS stall/cancellation/recovery. Normal, 320 dp, 200% text and combined cases ran. | Predates adaptive changes. API 27–31 were not individually executed. An old physical phone was not required. |
| API 36 x86_64 with actual 16 KB pages | Both guest and in-process page-size checks returned 16384; both packaged native libraries loaded. Runtime/input matrix and later corrected-input regression passed. | Emulator execution, not ARM64 hardware or final signed/optimized distribution. |
| Runtime/provider continuation | **56 distinct successful method/configuration observations across retained runs**, followed by explicitly identified final-fixture groups. | Not one clean 56-test invocation; earlier fixture and observation failures remain separate. |
| Final corrected-input APK | Eight successful Gboard/composing-history observations across normal/320 dp/200%/combined; four regression passes on API32 and four on actual 16 KB API36. | Earlier APK than the final adaptive build. Gboard version and hashes are pinned in the historical audit. |
| API 36 core/state/error work | Real owned storage corruption/reopen and staged-commit interruption, 40 pending-write/exit observations, process-death policy, draft/error restoration, two simultaneously resumed Activities and real split-screen audio ownership. Final audit completion recorded **36/36** selected observations. | Scoped failure contracts and observed configurations, not universal durability or every lifecycle event. |
| API 37 phone evidence | Historical startup, locale, name-field/inset and provider observations on the identified phone. | Limited earlier debug APK and observed configuration; no current equipment access is assumed. |
| Retention/performance | Repeated navigation/recreation, Activity collection controls and bounded memory trends; earlier debug graphics/frame observations. | A ComposeTestRule retention diagnostic failed and was isolated with an independently passing ActivityScenario control. No general no-leak guarantee or release benchmark is claimed. |

### Adaptive window, input and state coverage

All adaptive device work used the dedicated **ClocklingsReview20261030 / emulator-5590, API 36**. Android key/mouse events were injected through actual platform paths. This was not a physical keyboard/trackpad test. Host QEMU crashes and interrupted invocations were not counted as passes.

| Window / text size | Completed coverage |
| --- | --- |
| 320 x 800 dp / 100% | Complete screen-family journey and narrow layout checks. |
| 420 x 900 dp / 200% | Complete screen-family journey with enlarged text. |
| 320 x 640 dp / 200% | Combined narrow/enlarged-text journey, strict action visibility and real IME editing. Earlier adaptive runs also covered 320 x 800 / 200%. |
| 1000 x 360 dp / 100% | Final screen-family journey; lesson, challenge and Together introductions in all seven languages; raw unclipped clickable-control bounds and clock bounds of at least 200 dp. |
| 1000 x 360 dp / 200% | Screen-family journey rerun after the final compact lesson playback-row adjustment. Final screenshot shows playback/navigation visible. |
| 1000 x 900 dp / 100% | Tablet-sized screen-family journey and state/input coverage. |
| 1280 x 720 dp / 100% | Laptop-sized screen-family journey. |
| Live transitions | Multiple widths/heights, portrait/landscape, 360/480 dp height threshold crossings, rotation and Activity recreation; active lesson/practice, selected time, drafts/selections and scroll retained in the relevant tests. Real Android split-screen was separately exercised. |
| Keyboard and pointer | Complete lesson/practice/challenge/personalization/Together journeys across the adaptive stages; final compact action focus and phase changes; native mouse click, drag and wheel scrolling. |
| IME/insets | Real multilingual editing, cursor/selection/paste, field visibility, IME-driven layout transitions, three-button/cutout evidence and retained Gboard history checks. The final compact correction did not change text-field routing. |

Initial adaptive work recorded 47 successful selected executions; the subsequent polish recorded 20. The final compact report records its own matrix, five state/pointer tests, three keyboard journeys and a combined-size real IME test. These are overlapping observations, not counts of distinct features.

After the **last** compact lesson playback change, the final APK passed four checks at 1000 x 360 / 100%: the complete screen-family journey, all seven languages for the three compact introductions, lesson/practice threshold/recreation state and the whole keyboard-only lesson. The 1000 x 360 / 200% screen-family journey passed again. Unaffected phone/tablet/laptop and other-flow evidence was retained from immediately before that compact-only adjustment, rather than falsely attributed to a new execution.

### Human audio/accessibility and language evidence

The audit records an earlier owner-assisted Finnish TalkBack session covering Home, Settings/Language reading order and state, lesson incorrect/correct feedback, challenge timer behavior, Explore alternative clock actions, Together, wardrobe/name controls, error announcement/Retry and retained draft. Modal focus evidence was recorded separately. Those observations used the earlier installed APK; they do **not** verify the final compact pane arrangement's spoken order.

The same historical audit records disconnected/offline listening to effects and eligible Finnish, English, Swedish, German, French and Brazilian Portuguese voices, including speech stopping on backgrounding. Spanish had no eligible installed voice and was not counted as a spoken pass. Controlled native TTS stall tests additionally verified `started -> stopped -> released -> completed`; silent fixture output is not human listening evidence.

Automated language checks cover all seven resource sets, clock-language boundary fixtures, lesson answers, formatting/plurals and layout. They are not professional native-speaker certification. An additional linguistic review is optional quality evidence, not an invented mandatory release gate.

## Current visuals and deliverable

The final compact evidence directory contains **15 screenshots**. The nine final English/Finnish/German lesson/challenge/Together images and the enlarged-text lesson were visually inspected after the last build; the remaining five phone/tablet/laptop captures retain applicable unchanged-layout evidence.

| View | Current evidence |
| --- | --- |
| Finnish short lesson | [fi-lesson.png](../../reports/adaptive-layouts/compact/screenshots/fi-lesson.png) |
| Finnish short challenge | [fi-challenge.png](../../reports/adaptive-layouts/compact/screenshots/fi-challenge.png) |
| Finnish short Play Together | [fi-together.png](../../reports/adaptive-layouts/compact/screenshots/fi-together.png) |
| Short window, 200% text | [short200-lesson.png](../../reports/adaptive-layouts/compact/screenshots/short200-lesson.png) |
| 320 dp / 100%, 200%, combined | [320 dp](../../reports/adaptive-layouts/compact/screenshots/phone320-lesson.png), [200%](../../reports/adaptive-layouts/compact/screenshots/phone200-lesson.png), [320 dp + 200%](../../reports/adaptive-layouts/compact/screenshots/phone320200-lesson.png) |
| Tablet and laptop windows | [tablet](../../reports/adaptive-layouts/compact/screenshots/tablet-lesson.png), [laptop](../../reports/adaptive-layouts/compact/screenshots/laptop-lesson.png) |

The rejected `polish/screenshots/short-lesson.png`, `short-challenge.png` and `short-together.png` are historical failure evidence, not accepted final layouts.

Latest debug APK: `C:\Dev\Clocklings\app\build\outputs\apk\debug\app-debug.apk`.

SHA-256, rechecked during this consolidation: `5E4DDD8089484917D0B2689E404FA0B559FD5BD99581065C117162ED7DA8AB6C`.

This is a debug artifact, not a signed publication deliverable. Older report hashes identify historical builds.

## Reconciled audit follow-ups

The input audit ended at **14 RESOLVED / 0 OPEN / 0 BLOCKED / 5 DEFERRED**. The later adaptive scope decision changes how the window row is described below; repeating that old aggregate as today's complete backlog would be misleading.

| Original follow-up | Current disposition |
| --- | --- |
| FU01 — artifact/release | Debug and historical unsigned optimized package evidence completed. Final signing, distributed artifact identity and optimized distribution runtime remain deferred with publication. |
| FU03 — runtime verification | Original scoped minimum/legacy/16 KB gaps resolved. Later adaptive validation and its explicit unavailable evidence are recorded above. |
| FU04 — Console/store | Deferred owner/account/listing/audience/distribution decisions. No current Console or remote track claim. |
| FU05 — official sources/dependencies | Dated source/dependency/advisory reconciliation completed. Recheck applicable requirements when actual publication is authorized; historical research is not perpetual compliance evidence. |
| FU06 — audio | Scoped installed-voice/effect/offline/background observations completed. Missing Spanish voice and provider/device limits remain explicit. |
| FU-A02-STATE | Startup/read fallback/recovery and relevant legacy/current runtime checks resolved. |
| FU-A02-WRITES | Identified pending-write, failure, interruption, exit/reopen and retry contracts resolved. |
| FU-A05-LICENSE | Local dependency/notice corrections completed. Owner provenance/grants and final-distribution obligations remain deferred. |
| FU-A08-ASYNC | Scoped resource, cancellation, Activity retention and controlled native-provider stall/recovery criteria resolved. |
| FU-A09-RESTORE | Accepted restart policy and scoped process/Activity/draft/scroll/multi-resume criteria resolved. Adaptive state preservation additionally verified. |
| FU-A11-FOCUS | Scoped control/journey/visible-focus/modal criteria resolved; compact action-focus regressions added. |
| FU-A12-INSETS | Original legacy/current phone/IME criteria resolved; adaptive resizing/IME checks added. Desktop caption-runtime evidence remains unavailable. |
| FU-A13-WINDOWS | Phones/tablets/laptops are selected scope and adaptive implementation/emulator validation are completed. Actual Chromebook/Googlebook runtime evidence is unavailable. Specialized fold-hinge/external-display behavior is unselected, not an automatic unfinished feature. |
| FU-A14-INPUT | Current scoped application input requirements resolved, including verified Gboard undo/redo routing. Original Ka/null event attribution is historically unknown, with no reproduced current composition defect. No repeat broad input audit is required. |
| FU-A15-SEMANTICS | Original finite semantics/human scenarios resolved. Human review of the final adaptive arrangement has not been performed and is unavailable under the current owner constraint. |
| FU-A20-CACHE | Bounded retained configuration cache resolved and tested. |
| FU-A31-DISCLOSURE | Local data/provider inventory completed; owner/contact/privacy/retention/audience/store reconciliation remains deferred. |
| FU-A33-RETRY | Measured retry/log-rate issue resolved and regression tested. |
| FU-A41-SYNC | Fixed-sleep progression replaced and the full instrumented journey verified. |

The earlier discovery-only actions FU-A01.02-01, FU02 and FU07 are also complete. The original Ka/null traces cannot be reconstructed from missing historical events. Source recovery and passing replays do not turn that unknown into a proven cause; equally, the unknown is not a current application bug or new owner task.

## What may still be done

### Unavailable evidence, accepted as a current limitation

| Item | Current treatment |
| --- | --- |
| Actual Chromebook and Googlebook runtimes | No suitable equipment/runtime was available. These are distinct targets; neither is certified by phone-AVD window resizing. No equipment acquisition is requested. |
| Desktop caption controls and physical trackpad gestures | Not observed on actual target equipment. Android injected mouse/wheel and safe-inset evidence remains valid within its scope. |
| Human TalkBack review of the final adaptive layouts | Unavailable under the owner's current constraint. Historical human checks and current automated semantics/focus tests remain evidence; neither is relabeled as a new listening session. |
| Latest adaptive APK on minimum-API physical hardware | Not available and not required as a new owner task. Earlier API26/API32 emulator execution is retained; the final adaptive APK has not had that runtime rerun. |

There is no requirement to buy, borrow or obtain these devices to complete the authorized implementation. These limitations prevent unsupported compatibility/accessibility claims; they do not by themselves identify a source defect.

### Optional additional checks that do not inherently require owning those devices

- A focused **latest-APK API 26 emulator** rerun could provide additional regression evidence for the final adaptive changes. Earlier API26/API32/16 KB environments were successfully used; this document did not recreate them or run new tests.
- An **emulator-hosted accessibility session** could be assessed if a usable TalkBack setup and observation method are available. That setup was not verified in this consolidation, and automated accessibility nodes alone would still not prove spoken behavior.
- A suitable dedicated desktop emulator, if available and separately used, could add caption/window evidence. It would remain emulator evidence, not physical Chromebook/Googlebook/trackpad certification.
- Owner-run `lc`/`sc` and later branch CI may provide current check reports. None is claimed for this final snapshot. The four dependency-update lint warnings do not authorize unrelated upgrades.

These are options for more evidence, not a mandatory new queue or permission to repeat completed matrices. No unresolved product decision is needed to close the current implementation work.

### Deliberately deferred until publication work is requested

1. Production signing and final distribution artifact/certificate identity, optimized distributed runtime and release validation.
2. Owner artwork, audio, font and imported-code provenance/grants, plus final packaged license/NOTICE reconciliation.
3. Publishing identity/contact, privacy/provider/retention/deletion wording and required public/in-app/store disclosures.
4. Play Console/account/track/listing/audience/country/commercial/distribution declarations and current applicable publication requirements.

Old proposals such as additional profiles, everyday timers/routines/widgets, adult controls or specialized fold/external-display features are not automatically approved implementation debt. They require a separate product decision if pursued. No curriculum mapping, Teacher Approved work or partial/MVP publication was added.

## Cleanup and preservation

This consolidation checked **25 exact previously named cleanup paths**. All are absent: seven audit temporary directories; `reports/adaptive-layouts/.scratch`; the nine polish disposable files; the seven compact disposable files; and `app/build/compact-runtime`. Their old blocked-deletion instructions are historical and do not represent current outstanding cleanup. This task verified absence; it did not perform or claim the earlier deletions.

The final APK and 15 compact screenshots remain present. Prior task receipts record shutdown of owned emulators/helpers/temporary Gradle processes. This documentation task started no emulator, server or watcher and created no disposable scratch artifacts. No unrelated files, application data, processes, commits or Git history were changed.

## Evidence references

This document is sufficient for the current status and remaining-work decisions. The following records retain detailed commands, logs, historical failures and chronology:

Evidence under `reports/`, including screenshots, logs and APKs, is local-only and Git-ignored. Those relative links require the original working directory and do not resolve in a fresh GitHub checkout. They are historical local evidence, not downloadable repository artifacts.

| Record | Purpose |
| --- | --- |
| [Implementation history](STATUS.md) | Foundation, product decisions, localization, companions, branding, review fixes and dated validation. Older headings/branch names remain historical. |
| [Android/Google audit](../../reports/android-google-audit-discovery.md) | 877-item discovery, corrective work, runtime/provider/physical observations, dependency/notice research and final input closeout. Read final dispositions rather than early OPEN counts. |
| [Adaptive implementation](../../reports/adaptive-layouts/REPORT.md) | Whole-application scope, official runtime-source links, initial 404-test and 47-execution evidence. |
| [Adaptive polish history](../../reports/adaptive-layouts/polish/REPORT.md) | Intermediate 406-test suite, phase-focus fix and retained input/state evidence. Its three rejected short-window captures are superseded. |
| [Final compact correction](../../reports/adaptive-layouts/compact/REPORT.md) | Final 24-test result, actual window matrix, strict bounds/focus assertions and current screenshots. Its cleanup section is historical; the paths were rechecked absent above. |
| Final compact receipts | `reports/adaptive-layouts/compact/validation.log`, `jvm-results.json`, `stability.log`, `lesson-final.log`, `short200.log`, and the named matrix/input logs. |

Official Chromebook/Googlebook/desktop guidance was checked during adaptive implementation and linked in its report. It describes distinct runtime/input/window requirements, not equivalent runtimes or certification. Those sources were not fetched again for this documentation-only consolidation.
