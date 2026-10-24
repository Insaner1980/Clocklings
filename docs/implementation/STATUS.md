# Clocklings implementation

Date: 20 October 2026. This is an internal construction checkpoint, not a release or an agreed final feature inventory.

## Authority and decisions

The owner's current instructions override both copied proposals. In particular, the original application's files must remain in place and unchanged. The older plan's relocation wording does **not** authorize deleting its source copies.

| Decision | Current status |
| --- | --- |
| Workspace | Confirmed: `C:\Dev\Clocklings`. Independent local repository; current work is on `codex/companions-clock-themes`. |
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
