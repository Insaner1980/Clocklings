# Clocklings implementation

Updated: 25 October 2026. Earlier dated checkpoints remain below. This is an internal construction record, not a release or an agreed final feature inventory.

## Authority and decisions

The owner's current instructions override both copied proposals. In particular, the original application's files must remain in place and unchanged. The older plan's relocation wording does **not** authorize deleting its source copies.

| Decision | Current status |
| --- | --- |
| Workspace | Confirmed: `C:\Dev\Clocklings`. Independent local repository; current work is on `codex/localization-settings`. |
| Identity | Confirmed: app **Clocklings**, Play title **Clocklings: Learn to Tell Time**, application ID and namespace `fi.clocklings`. |
| Languages | Confirmed: English first, then Finnish and one shared Swedish version. All three completed before the single complete public release. |
| English expressions | Confirmed: shared past/to style, including “quarter past three”, “half past three”, and “quarter to four”; numeric time alongside. No separate British/American modes. |
| Together | Confirmed: set a time, ask, reveal, and swap turns. No assumed family roles. Cooperative stories, split-screen racing, and other variants remain proposals. |
| Speech | Confirmed: on-demand speech when an installed offline voice in the content language is available; learning still works without it. Regional English/Swedish voice tags do not create separate content variants. |
| Companions | Confirmed by the owner on 21 October: human, cat, dog, robot, dragon, dinosaur, and no-companion mode. |
| Clock appearances | Confirmed by the owner on 21 October: classic, rainbow, space, forest, ocean, garden, workshop, and a constrained custom clock. |
| Exclusions | No curriculum mapping, Teacher Approved work, or partial/MVP publication. |
| Publication | No commits, pushes, remote creation, signing-key setup, uploads, or pull requests authorized. |

Before their dependent implementation, resolve the final activity inventory, profiles, rewards/session rules, everyday timer/routines/widget, commercial model, data transfer, adult controls, and final audience declaration. Existing copied wardrobe/reward behavior remains a development baseline for broader reward decisions. The companion and clock inventory above is now explicitly approved.

## Source import and preservation

- Source: `C:\Dev\Sateenkaarikello-Android`, branch `feature/splash-intro`, commit `cfed3a417047255d3930cc005f1785616f6233d8`.
- The source had no tracked modifications. Its three untracked files were `PROJECT.md` and the two product plans.
- [source-snapshot.json](source-snapshot.json) records SHA-256 for all 153 tracked/untracked input files, the source branch/status, and `.git/HEAD`, `.git/index`, and `.git/config`.
- Copied 142 selected working-tree files: application sources, tests, assets/font licenses, Gradle wrapper and pinned dependency configuration, checking configuration/wrappers, and Dependabot configuration. Every copied file was hash-checked before adapting the destination.
- Both plans were separately copied from their untracked working-tree contents and hash-checked. The [primary plan](../superpowers/plans/2026-10-20-public-clock-learning-app.md) remains intact; the [comparison plan](../play-store-app-plan-claude.md) is historical reference.
- Did not copy the original `.git`, machine-specific `local.properties`, keys/credentials, caches, builds, reports, `.deepsec` installation, or stale source-specific `PROJECT.md`/`CLAUDE.md`.
- The destination's pre-existing empty `.sonar` directory was preserved.
- New package/source/test paths, application and theme names, DataStore name, shrinker/stability names, and local checking project IDs use Clocklings.
- Release signing references only `CLOCKLINGS_*` properties; no release key was configured or copied. Debug builds use the normal local Android debug signing setup.
- Removed the inherited project's scanner exception: its historical approval is not an approval for this project.
- Sonar configuration has a distinct Clocklings project key. No remote service was registered or contacted for analysis. Local `lc`/`sc` remain owner-run.

## Initial implementation

- Reused the working clock, Navigation 3 graph, nine short lessons, five read/set practice levels, optional minute challenge, local persistence, and baseline companion rendering.
- Added `EnglishTime` with clock phrases, numeric display/readings, explicit noon/midnight and day context, and greetings. The original Finnish formatter and its tests remain available for the later Finnish implementation.
- Adapted all 294 inherited resource entries into English and added the hidden-answer accessibility description. Half-hour and five-minute lessons use English hour references, not translated Finnish sentence structure.
- Shared hints now carry a time value instead of Finnish day-part wording. The current English UI resolves the wording.
- Together keeps its phase and player across activity recreation, swaps roles after revelation, hides the spoken accessibility answer before revelation, and records no learning reward.
- Speech selects installed offline voices of the content language, rejects network-only/missing-data/wrong-language candidates, and stops on navigation, lesson page changes/replay, role swap, and backgrounding. Actual pronunciation and real-engine offline operation still need device evidence.
- Narrow layouts give navigation titles their own row. Home cards give their text a full row when space is limited; narrow widths and large fonts use one column for home activities. This fixes observed word splitting in English time phrases, companion labels, and the Together title.
- Preserved the original dependency versions and architecture. No backend, profile migration, generic language engine, or new dependency was added.

## Language and platform evidence

Accessed 20 October 2026:

- [Cambridge, How to say the time](https://dictionaryblog.cambridge.org/2018/02/14/what-time-is-it-how-to-say-the-time/): past/to, reference to the preceding hour for half past, numeric alternatives, and contextual day periods. The precise per-minute template is the owner's selected app style, not a claim that other expressions are wrong. The linked British Council worksheet could not be fetched in this session and is not counted as reviewed evidence.
- [Android Voice](https://developer.android.com/reference/android/speech/tts/Voice) and [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech): voice locale, network requirement, missing-data feature, explicit voice selection, and stop behavior. Metadata tests do not prove pronunciation or offline engine behavior.

Boundary fixtures cover minute 0/1/2/14/15/16/29/30/31/44/45/46/58/59, hour wrap, contextual noon/midnight, numeric readings, all 720 analog values, and all 1,440 day values. Generated choices are checked for distinct phrase and numeric labels. Instrumentation checks the actual English lesson choices against independent expected times.

## Validation

- Initial renamed baseline: `:app:testDebugUnitTest :app:assembleDebug` passed; 124 JVM tests. Log: `reports/foundation/baseline-build.log`.
- `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:ktlintCheck :app:stabilityCheck` passed, including 130 JVM tests with zero failures/errors. Log: `reports/foundation/final-checks.log`.
- Compose stability baselines changed only for the added Together setter parameter and clock hidden-answer parameter; both remain stable/skippable. Debug and release stability checks passed.
- The first device runs exposed an inherited Challenge test's outdated numeric expectation. It now asserts the English 24-hour display `03:00`/`04:00`.
- Visual checks then exposed word splitting missed by the existing interaction tests. After fixing the layouts, `:app:assembleDebug :app:assembleDebugAndroidTest :app:ktlintCheck :app:stabilityCheck` passed again (`reports/foundation/layout-checks.log`). A focused UI regression checks actual rendered line bounds and word boundaries. It does not use paragraph allocation width as a proxy for visible text overflow.
- Final instrumentation results on the final debug APK: 20/20 tests at 360 dp / 100% font; 9/9 relevant UI tests at each of 320 dp / 100%, 360 dp / 200%, and 320 dp / 200%. This is 47 passing executions, not 47 distinct tests. Logs: `reports/foundation/device-*.log`.
- The UI subset covers actual home text line bounds, Together phases/role swapping/recreation/hidden answers, lessons, a complete practice round, customization, all level entries, results, clock dragging, and challenge controls. It is not a full TalkBack or release accessibility audit.
- Visually inspected result screenshots in all four display cells, the corrected home/time/companion cards at 320 dp / 200%, and the corrected Together title/clock at 320 dp / 200%. Evidence: `reports/foundation/result-*.png`, `home-layout-current.png`, `companion-layout-current.png`, and `together-320dp-200percent.png`. Scrolling remains necessary on small screens.
- Dedicated test AVD: `ClocklingsFoundation20261020`, serial `emulator-5590`, API 37. Only synthetic test data is used. The unrelated existing emulator `emulator-5586` is untouched.
- The isolated original package uses the already-built source debug APK, not a new build in the source checkout. This does not establish freshness of that historical artifact.
- Installation isolation: both packages coexist with different Android UIDs. A synthetic file in the test original's private storage retained the same SHA-256 after Clocklings updates, UI tests, uninstall, and reinstall. Evidence: `reports/foundation/install-isolation.json`. No existing user installation/data was used.
- Final source recheck: all 153 input-file hashes, all three recorded Git-file hashes, branch, commit, and dirty status match the initial snapshot. The new repository has no commits or remotes. Evidence: `reports/foundation/verification-summary.json`.
- Reset the dedicated emulator's display/font overrides and stopped it. Its recorded emulator processes no longer exist; this task's temporary build processes have also exited. Unrelated processes were left alone.
- Reviewable development APK: `app/build/outputs/apk/debug/app-debug.apk` (`fi.clocklings`, label `Clocklings`, version 1.0 / code 1). This is locally debug-signed, not a release artifact.
- Full release readiness is not claimed: Finnish/Swedish completion, final selected features/assets, full accessibility review including TalkBack, real-device speech, release/bundle delivery, owner-run `lc`/`sc`, and store preparation remain outstanding.

## Companion and clock selection — 21 October 2026

The owner explicitly selected all six companions, seven authored clock themes, the custom clock, and no-companion mode. This implements U04; the rest of the product proposal is not implicitly selected.

- `My Companion` opens the companion picker and clock-style tab. `Clock styles` on Home opens the clock tab directly. All base choices are available without stars.
- Human retains its existing outfit, name and earned wardrobe. Cat, dog, robot, dragon and dinosaur have distinct original vector artwork, idle movement, greeting/success movement, and static rendering when system animations are disabled. Each has four color choices and an anatomy-appropriate accessory (scarf, bandana, antenna, pendant or backpack), remembered independently.
- `Just the clock` removes companion artwork from Home, the intro and results. Learning access and saved progress are unchanged. Human wardrobe reward pictures are displayed only for the human companion; the underlying earned progress is retained when switching.
- Classic, rainbow, space, forest, ocean, garden and workshop use the same clock geometry and teaching overlays. Their rims have distinct colors and authored motifs. The custom clock offers four frame colors, three frame patterns, two numeral styles and two hand shapes. Numerals remain 1–12, and the short red/long blue hand roles remain consistent with the lessons.
- The selected appearance reaches Home, intro, lessons, Explore, games, Together and the minute challenge through one root visual-preference provider. Theme changes do not change time values, answers, dragging or accessible time controls.
- Existing Preferences DataStore stores the selections and custom options with defaults for old installations and unknown identifiers. Learning reset retains these visual settings. No dependency or toolchain change was needed.
- New artwork was authored directly in code: companions in `CompanionArt.kt` (replaced on 22–23 October by the per-companion rigs in `ui/companion/`, see below) and clocks in `ClockThemeArt.kt`; existing human art was reused. No external character assets, private source files or signing material were imported.

Validation evidence for this change is under `reports/personalization/`. The release-wide exclusions and outstanding work above still apply.

### Verified implementation evidence

- `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` passed: 133 JVM tests, no failures, errors or skipped tests (`build-ready.log`).
- `:app:ktlintCheck`, `:app:detekt` and both debug/release Compose stability checks passed (`quality.log`, `ui-final-build.log`, `final-quality.log`). Updated stability baselines reflect the actual generated metadata; the new UI composables and clock remain skippable.
- The existing device regression suite passed 18 tests (`regression-normal.log`). The new suite passed 7 tests at 360 dp / 100% font, including real DataStore close/reopen persistence. Six relevant UI tests passed in each remaining cell: 320 dp / 100%, 360 dp / 200%, and 320 dp / 200% (`device-*.log`). These are 43 passing executions, not 43 distinct tests.
- Device checks cover every companion and theme, no-companion learning/results, independent companion styles, custom clock controls, activity recreation, persistence, shared clock appearance in learning modes, and readable controls at narrow widths and large fonts.
- Visually inspected all six base companions, their accessory variants, and all eight clock choices. Also inspected the actual picker/custom controls at normal and large font settings, plus the dragon picker, custom controls and no-companion Explore screen at 320 dp / 200%. Screenshots are in the display-specific subdirectories. Small screens still require scrolling.
- Used only the dedicated `ClocklingsFoundation20261020` AVD on `emulator-5590` (API 37). The physical device and unrelated running emulator were not targeted. Restored the original density and font settings before shutting down the dedicated emulator.
- `git diff --check` and a source-snapshot whitespace comparison passed. Existing unrelated files were preserved. No commit, push, release signing or original-app changes were made. Owner-run `lc` and `sc` were not run.
- Updated debug APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `9A0B7976E732A2781816E4271460A077C811ADAC8C77FDC98FD91B40EBB6348D`. This verifies this feature scope, not complete public-release readiness.

## Companion wardrobes and vinyl artwork — 22–23 October 2026

Spec: [companion wardrobes design](../superpowers/specs/2026-10-22-companion-wardrobes-design.md); plan: `docs/superpowers/plans/2026-10-22-companion-wardrobes.md`. This replaces the single on/off accessory of the 21 October selection with a full wardrobe for every companion.

### Owner decisions

- Style: all six companions are drawn as shaded vinyl toys (radial-gradient volume, specular highlights, contact and ground shadows) after the owner's reference image.
- Technique: artwork stays code-drawn on Compose `Canvas`. No raster art, 3D engine or new dependency.
- Wardrobes: cat, dog, robot, dragon and dinosaur each have one free item and 15 unlockable items, one per total star 1 to 15. The doll keeps its 18 items, unlock rules and geometry.
- Colors stay free and form the first wardrobe section; all four colors are available immediately.
- Checkpoint: the owner approved the cat (artwork, items, wardrobe screen) on 22 October before the other companions were drawn.

### Structure

- `ui/art/VinylShading.kt`: shared shading toolkit (vinyl tones, cached unit-space brushes, specular, contact and ground shadows, glossy eyes, path helpers). `ui/art/ArtTones.kt`: shared art tones built once (cream, eye ink, cheeks, item materials, metal, screen).
- `ui/companion/`: `CompanionRig` interface with `rigFor(kind)` dispatch and the shared item-art registry; `CatRig`, `DogRig`, `RobotRig`, `DragonRig`, `DinosaurRig` with matching `<Kind>Items.kt`; `ItemArtKit.kt` for shared item shapes (scarf, bow tie, flower lei and others); `CompanionPose.kt` for idle, greeting, celebration and the item-reveal spin; `CompanionView.kt` as the one entry point for every companion; `WardrobeScreen.kt` (generalized from the doll screen) with color, item sections, wearing and lock state, star cost and companion name.
- `ui/doll/DollRig.kt` and `DollParts.kt`: the doll with the same shading, geometry unchanged. The earlier `ui/appearance/CompanionArt.kt` and its legacy art are deleted.
- `domain/CompanionItems.kt` holds the 80 animal items (ids `<kind>-<slot>-<name>`); `domain/Wardrobe.kt` covers every companion. English item names are in `res/values/companion_items.xml`.
- Persistence: each companion has its own outfit and name. The doll keeps its earlier keys (`outfit_<slot>`, `doll_name`); animals use `outfit_<kind>_<slot>` and `companion_name_<kind>`. Colors stay in personalization. The old `companion_<KIND>_accessory` flag is read once (only when the kind has no saved outfit) to put on its free item, and is never written again. Learning reset clears outfits and keeps colors.
- Results show newly unlocked items for every companion; the companion spins the new items on.

### Rulings that changed plan details

- R9: wardrobe tile crops were adjusted per companion so no stray tail or wing fragment or sliced mouth shows and every item of the slot stays visible. Final HEAD/NECK crops (240 x 300 grid): cat (46,10,194,151)/(55,150,185,236), dog (20,0,220,174)/(66,158,174,238), robot (24,0,216,172)/(67,148,173,238), dragon (24,0,216,151)/(60,156,180,240), dinosaur (34,0,226,146)/(66,138,192,230).
- R11: where a color option looked the same as a kind's original, per-kind override tokens in `PersonalizationColors.companion()` replace the generic swatch: dragon purple `8D68C8`, robot blue `5B95DA`, dinosaur green `4E9C86`.
- R13: the word "accessory" remains only in `ProgressRepository` (`legacyAccessoryKey`) and its tests, for the one-time legacy read.
- R15: the doll got the breathing squash so every companion idles. Head tilt was not added; it would need the back hair and front group to rotate together, and is left as an owner decision.

### Checks run on 23 October (final code)

- `:app:testDebugUnitTest :app:assembleDebug --no-daemon`: passed. The unit tests were re-executed with `--rerun`: 158 tests in 23 classes, 0 failures (`reports/companions/final/unit-assemble-rerun.log`).
- `:app:connectedDebugAndroidTest --no-daemon` on `emulator-5590` (AVD `ClocklingsFoundation20261020`, API 37), all classes: 46 tests, 0 failures. AppFlowTest 6, ChallengeScreenTest 1, CompanionArtCoverageTest 7, ComposeStabilityRegressionTest 1, EnglishContentTest 2, GalleryTest 13, IntroOverlayTest 3, LintRegressionTest 3, PersonalizationFlowTest 3, PersonalizationGalleryTest 3, PersonalizationStorageTest 1, OfflineVoiceTest 3 (`connected.log`).
- `CompanionArtCoverageTest` re-run alone: 7/7 passed (`coverage-rerun.log`). An earlier 1/7 failure during concurrent edits did not recur.
- Device screenshots in `reports/companions/final/`: Home, picker (top and scrolled), each companion's wardrobe (top and scrolled to items) and a dog result with three unlocks, at default (411 dp / 100%), 320 dp, 200% font and 320 dp / 200%. Inspected visually: no clipping, split words, overlap, middle-dot or bar separators, or colored edge stripes. Long labels wrap at whole words (for example "Bone tag collar", "Flower behind the ear", "Companion name"); small screens need scrolling.
- Color row: four tiles in one row from 392 dp width (font 100%); "Original" and "Selected" fit on one line there. At 360 and 380 dp the grid width is below the 360 dp breakpoint after the gutters, so the colors use the 2 x 2 grid (`reports/companions/final/colorrow/`).
- Dog result spin captured on device in the real app (no Compose test rule): `result-dog-spin-frames.png` shows the turn and the cap, tennis ball and bone tag collar appearing.
- Frame time, Home idle 10 s after `dumpsys gfxinfo` reset, debug build on the emulator: doll in the star dress 612 frames, 1.80% janky, p50 30 ms, p90 36 ms, p95 44 ms; dinosaur with a hat and neck item 618 frames, 0.32% janky, p50 26 ms, p90 31 ms, p95 32 ms (`gfxinfo-*.txt`). Emulator numbers, not device numbers.
- Cold start, `am start -W` after force-stop, debug build on the emulator: TotalTime 3754, 2846 and 2777 ms (`cold-start.txt`).
- Display overrides were restored afterwards (`1080x2400`, density 420, font scale 1.0). The physical phone and the other emulator were not used. Temporary screenshot test code was deleted.

### Remaining gaps

- Finnish and Swedish item names (with the rest of the localization pass).
- Owner-run `lc` and `sc` have not been run for this work. Gradle `ktlintCheck` and `detekt` passed during the final review fix wave.
- `app/stability/app-debug.stability` baseline is stale and should be re-dumped (owner decision).
- Art polish raised as owner decisions: cat fish backpack and sparkle cape drawn behind without straps or clasp, while the dog's backpack and cape are front items with straps and clasp; cat pink inner ears mostly hidden and cream tail tip shading grey-taupe; dog neck-band ends overhang in neck tiles, hero cape and raincoat read weakly, frisbee covers the tongue; robot original teal greyer than the reference, jetpack rockets read as flanking, necklace halos flat, neck tiles keep half shoulder discs; dragon wings sit lower than the reference (for the head crop) and the royal cape tile shows a thin sliver; doll cheeks, eye size, item outlines and head tilt. Item and pigment colors are implementer picks.
- No TalkBack audit; TalkBack reads the selected state twice (pre-existing).

## Localization and Settings — 25 October 2026

Implemented from the owner's request to execute the [localization/settings plan](../superpowers/plans/2026-10-24-localization-and-settings.md). The recommended Home entry, device-default language option, shared Swedish style and explicit reset dialog are implemented. This section supersedes the older Finnish/Swedish and stale-stability-baseline gaps above.

### Implemented

- Complete English, Finnish and shared Swedish resources: 438 base keys, 437 translatable keys per language after removing five obsolete Home keys, including companions/items and the new Settings copy. The [language review](../localization/language-review.md) records wording choices, independent time/lesson fixtures, sources and linguistic limits.
- One root locale boundary keeps Compose resources, generated clock wording, Canvas labels and accessibility text aligned. Android 13+ uses `LocaleManager`; API 26–32 stores the choice in the existing DataStore. Device language picks the first supported locale, otherwise English. Regional Swedish uses shared content. Legacy preferences migrate once without overriding an existing system choice.
- Home opens Settings and Language. Existing 12/24-hour, clock-guide and sound preferences are reused. Reset requires confirmation and preserves settings/language while clearing the existing learning/reward/name/outfit progress. Failed writes retain committed state and expose retry feedback.
- `TimeLanguage` dispatches English/Finnish/Swedish arithmetic-independent wording. Finnish/Swedish half-hour lessons reference the upcoming hour; all nine lesson IDs, numeric targets and answer identities remain stable. Noon/midnight narration uses complete language-specific sentences.
- Language changes retain active practice/quiz/Together values, hidden answers and progress. Challenge retains its deadline; saved celebration consumption and Explore values survive recreation. Speech stops/reconfigures without replaying stale requests and only becomes available for an eligible offline voice.
- Device tests exposed real word splitting in lesson navigation and long answer rows. Their `FlowRow` layouts now wrap complete buttons/numeric badges without shrinking text. Finnish speech guidance and Swedish reset copy use shorter equivalent wording at large font sizes. Visual inspection also found overlapping custom dialog buttons; both actions now share a spaced `FlowRow`, with tests for complete action bounds and scrolling the body to its last line.
- Language splits are disabled so all three languages ship together. No toolchain/dependency upgrade, AppCompat migration, release signing, commit, push or PR was performed.

### Automated and device evidence

Build logs are under `.superpowers/sdd/2026-10-24-localization-and-settings/`; device/bundle evidence is under `reports/localization/`. These local evidence directories are ignored by Git.

- `final-layout-checks.log`: `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `ktlintCheck`, `detekt`, `stabilityCheck`, `lintDebug` and `bundleDebug` passed with `--no-daemon`. JVM result: **184 tests in 28 suites, zero failures/errors/skips**. `final-copy-checks.log` verifies the copy adjustment; `dialog-checks.log` verifies the final APK/test APK/bundle, detekt, stability and lint after the dialog fix; `final-ktlint.log` passes the final formatting check. Lint has zero errors and six warnings (one existing version notice and five obsolete Home strings retained for key continuity).
- Both generated Compose stability baselines were inspected before updating. Existing UI functions did not lose skippability; the locale provider and new Settings UI are skippable. New non-Unit text helpers are non-restartable as expected. No baseline/check was disabled.
- `resource-completeness.log` passes exact key/type/plural/array/placeholder parity; `resource-selftest.log` exercises the checker's negative fixtures. This proves structural completeness, not native-speaker approval.
- Dedicated API 37 AVD `ClocklingsFoundation20261020`, serial `emulator-5590`: `modern-final-core.log` passed 26 functional/content/audio/state tests; two screenshot-directory failures in layout tests were corrected in the test helper and rerun in the display matrix. All three `ActiveLocaleStateTest` cases passed, including actual Activity replacement during practice, an unfinished lesson quiz, a result, and a running real-time minute challenge. Initial existing regressions passed 15 cases; their one new layout failure led to the verified layout fixes.
- Dedicated API 33 AVD `ClocklingsLocaleApi33`, serial `emulator-5594`: `api33-settings.log` passed **6/6** real Activity/Settings tests. These write/read Android's `LocaleManager` and check agreement with the in-app picker; they do not claim manual navigation of Android Settings itself.
- Dedicated API 26 AVD `ClocklingsLocaleApi26`, serial `emulator-5592`: legacy storage/resources, recreation, reset scope and speech tests passed; the large-font copy failures were reproduced and fixed. `legacy-settings-final.log` passed **6/6 Settings tests at 320 dp / 200%**, including all three languages, full dialog action bounds and body scrolling. `legacy-final-regression.log` passed **6/6 AppFlow tests at 360 dp / 100%**. Earlier `legacy-verified.log` passed the other 22 content/functional/audio cases; its obsolete copy failure is superseded by the final Settings run.

The API 37 display matrix is complete. Every layout test loops English, Finnish and Swedish, covering **12 language/display combinations**. `matrix-*.log` contains the clock/lesson checks; `final-settings-*.log` contains the final dialog/Settings checks. The six-test normal Settings run includes the layout case and five functional cases; the other cells run the one layout case.

| Cell | Width / font | Clock and lesson layout | Final Settings, Language, Home entry and reset |
| --- | --- | --- | --- |
| A | 360 dp / 100% | PASS, all 3 languages | PASS, 6/6 tests |
| B | 320 dp / 100% | PASS, all 3 languages | PASS, 1/1 layout test |
| C | 360 dp / 200% | PASS, all 3 languages | PASS, 1/1 layout test |
| D | 320 dp / 200% | PASS, all 3 languages | PASS, 1/1 layout test |

Settings/navigation tests use the real Activity. Clock layout tests render the actual Game/Lesson composables with explicit representative states, including long exact-minute choices, half/five-minute lessons, footer actions and day-period Canvas text; active-state preservation is separately covered by the real Activity tests. Assertions inspect rendered text width, word boundaries, ellipsis, action bounds and scroll limits. Centered paragraph allocation width was not mistaken for visible text overflow. PNG evidence is in `screenshots/localization/` and `legacy-screenshots/localization/`; representative normal/large-font images were visually inspected, including the corrected Finnish answer row and Swedish reset dialog with its body scrolled to the end.

### Offline bundle and cleanup

- Built `app/build/outputs/bundle/debug/app-debug.aab` and generated/installed its device-specific APK set with official bundletool **1.18.3**, using only the ordinary debug key. The final Settings matrix ran on this installation with airplane mode **on** and Wi-Fi **off**. All three languages switched and retained their content without downloading language resources.
- `bundle-config.json` disables the LANGUAGE split; the APK set contains `base-master.apk`, `base-x86_64.apk` and `base-xxhdpi.apk`. The final master resource table contains Finnish and Swedish plus default English. See `bundle-resource-configurations.txt`, `bundle-apk-entries.txt`, `bundle-installed-paths.txt` and `bundle-install-final.log`.
- Final SHA-256: debug APK `DF77E0982C992DA1316DBCD378991019D2A1943129A44CDEEAD7DA96DE653E63`; debug AAB `0439244B537994AD526049BDF8CD1E02D859163E7466D23D6F666F681B80934C`. The test APK identity is retained alongside these in `artifact-sha256.json`.
- Dedicated display/font overrides were restored (API 37: 1080×2400, density 420, font 1.0; API 26/33: default density and previously absent font override). API 37 airplane/Wi-Fi and accessibility settings were restored. Temporary emulators and this task's adb server were stopped; no physical or unrelated emulator was used.

### Evidence limits

- The 8 fake-engine speech tests and 3 offline-voice metadata tests prove reconfiguration, last-request-wins initialization, errors/recovery, language/network filtering and no stale replay. They do not prove engine pronunciation or audible offline playback. API 37 has `com.google.android.tts`, version `googletts.google-speech-apk_20260928.02_p0.989914902`; Settings showed English available and Swedish unavailable in the captured normal-size run. No audio was listened to; the test emulators were launched with `-no-audio`.
- Real Android accessibility roles, selected state, headings, hidden answers and touch targets are tested. A bounded API 37 TalkBack smoke check bound the installed service and visually observed focus on Home's Settings entry and Back, and opening Settings (`talkback-*.png`, `talkback-service.txt`). Complete focus traversal and spoken output in all three languages were not established; a complete spoken TalkBack review and native-speaker approval remain unverified. The source review's uncommon Swedish exact-minute formulations remain an explicit editorial choice.
- Owner-run `lc`/`sc`, physical devices, signed release/Play delivery and publication are outside this evidence. Existing art/product decisions remain open. Spanish/French are not exposed.

### Lint followup — 25 October 2026

- Removed unused `mode_label`, `sound_on`, `sound_off`, `reset` and `reset_confirm` resources from English, Finnish and Swedish. Settings already uses its own copy; rendered UI is unchanged.
- Updated the exact `NewerVersionAvailable` exception from Compose Rules 0.4.23 to the installed 0.4.28. The reviewed 0.6.8 upgrade requires Detekt 2; Detekt remains 1.23.8. [Upstream compatibility](https://mrmans0n.github.io/compose-rules/detekt/). Other update notices remain enabled.
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon` passed: 184 JVM tests in 28 suites, zero failures/errors/skips; Android lint has zero errors and zero warnings. `tools/check-localization.ps1` passed. Evidence: `reports/lint-fix-20261025/` (`gradle-checks.log`, `lint-before.xml`, `lint-after.xml`, `unit-test-summary.json`, `localization-check.log`). This supersedes the six-warning note above.
- No UI/device matrix rerun was needed for removal of unreferenced resources. Owner-run `lc`/`sc` were not rerun. No new Java process remained after validation; no commit or push was made.

### Sonar command repair — 25 October 2026

- The owner's 15:09 run timed out after 3,600 seconds before upload. Instrumentation started 44 tests and finished 43; `LanguageSettingsTest.externalLocaleChangePreservesExploreAndTogetherHiddenAnswer` remained unfinished. The previous upload receipt was from 24 October. Evidence is preserved under `reports/sonar-repair-20261025/`.
- `tools/sonar.ps1` now streams both Gradle output pipes to the console and report, emits a running-status message every 30 seconds, prints failures/timeouts and success, and terminates its own process tree on timeout/interruption. It no longer depends on the external Android-check process wrapper. The existing dedicated-emulator selection and cleanup remain in place.
- The command waits for the server's Quality Gate using `sonar.qualitygate.wait=true` and requires a fresh matching `report-task.txt` before reporting success. `LanguageSettingsTest` has a 120-second outer JUnit timeout covering setup, Compose idle waits, and cleanup; assertions and coverage were not removed.
- The old wrapper failed the new live-output regression check. All four wrapper scenarios (live success, nonzero exit, timeout with child cleanup, stale/missing receipt) then passed in both PowerShell 7 and Windows PowerShell 5.1 (`tools/test-sonar.ps1`, `script-tests-*.log`). Android test-source ktlint and Detekt passed (`test-source-checks.log`).
- The original stall did not recur: the isolated test, its six-test class, and the unchanged 72-test suite passed. The final real `sonar` command rebuilt the changed instrumentation tests, passed all 72 tests without skips, uploaded the combined coverage, and exited 0 after a nine-minute Gradle run (`final-sonar.txt`, `sonar-suite.xml`). Debug assembly and JVM test tasks were up-to-date in this run, not freshly executed JVM tests.
- Server task `AaF0LXE5J8I9a074RZ-H` completed successfully on 25 October at 19:48 Helsinki time; analysis `5eb12e9b-61b2-48dd-8d0a-e89d38c4fe99` has Quality Gate `OK`, confirmed independently through the authenticated API (`server-task.json`, `gate-after.json`). All six gate conditions passed. The wrapper printed completion at 19:49 and stopped its dedicated emulator.
- No production UI changed, so the 320 dp / 200% layout matrix was not rerun. The original intermittent test stall's internal cause remains unproven; the successful reruns and bounded test wait do not establish a root-cause fix for that stall. Existing uncommitted work was preserved. No commit, push, physical-device testing, owner `lc`/`sc`, or release work was performed.
