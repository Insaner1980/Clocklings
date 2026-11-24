# Independent 3D artwork version

This note records the initial artwork experiment. The main Android checkout now lives at `C:\Dev\Clocklings`.

The initial copy contained 5,365 files. Every copied file passed the deterministic
secrets scan and matched its source SHA-256 before the changes below. Source,
resources, tests, tools, documents, reference screenshots and reports were copied.
Git metadata, build output, caches, installed tooling dependencies, scanner data,
scratch directories, credential file patterns and machine-specific `local.properties`
were excluded. The Android SDK path was configured locally for this copy.

## Application

The launcher label is **Clocklings**. Its current application ID is `com.finnvek.clocklings` and its Kotlin namespace is `fi.clocklings`. Release signing is unconfigured.
The project's existing debug purchase service already grants full access. The
debug-only `RenderedPreviewApplication` selects the robot and gives 15 preview stars
on its first launch, so all artwork can be tried immediately. A saved initialization
flag preserves subsequent choices and progress. Release uses the original
`ClocklingsApplication` and purchase verification, without this preview setup.

All nine companions now use pre-rendered artwork: robot, cat, dog, dragon,
dinosaur, bunny, unicorn, car and doll.
See [the cat implementation note](artwork-cat.md), [the dog implementation note](artwork-dog.md),
[the dragon implementation note](artwork-dragon.md),
[the dinosaur implementation note](artwork-dinosaur.md),
[the bunny implementation note](artwork-bunny.md),
[the unicorn implementation note](artwork-unicorn.md),
[the car implementation note](artwork-car.md) and
[the doll implementation note](artwork-doll.md) for their
models and verification. The robot uses images rendered from the approved
3D model, with separate head, eyes, arms, body, legs and wind-up key. Compose
applies the existing idle motion, blink, head tilt, wave, jump and outfit transition.
The existing reduced-motion setting still stops animation. Four shell colors and
all existing item IDs remain available. The doll also has modeled hair, dresses,
overalls and shoes. The bunny, unicorn, car and doll now use
[modeled reference accessories](artwork-reference-accessories.md), including
the doll's headwear, wings, bag and roller skates.

The robot's 16 accessories are now modeled in `art/models/robot_accessories.blend`
from the owner's seven robot reference sheets. The flat eye symbols follow sheet 7.
`tools/artwork/render_robot_accessories.py` builds the meshes through the 3D editor integration
and exports the transparent atlas used by the robot's existing animation layers.
The rendered comparison is `reports/robot-reference-accessories/robot-accessories.png`.

The application uses fixed-view layered renders. The editable 3D scenes retain
the three-dimensional accessory geometry.

## Editable model and reproduction

The editable source is `art/models/robot.blend`. The copied original preview images
and animation remain in the local evidence directory for that run.

Run from this repository with 3D 5.3:

Set the `CLOCKLINGS_3D_EDITOR` environment variable to the full path of the executable that opens `.blend` files.

```powershell
& $env:CLOCKLINGS_3D_EDITOR --background --factory-startup --threads 8 --python-exit-code 1 --python tools/artwork/render_robot_layers.py
```

The script uses Cycles on the CPU, renders transparent layers, checks them for empty
or clipped content, and writes four 896 x 640 PNG masters into
`art/models/atlases`. All character and accessory renderers use this master directory.
Publish the Android resources after rendering with:

```powershell
uv run --with pillow==13.0.0 python tools/artwork/optimize_atlases.py
```

The publisher preserves the original dimensions and every RGBA byte, including
invisible RGB values, and verifies the decoded lossless WebP before writing it to
`app/src/main/res/drawable-nodpi`. PNG masters are only included in the test APK,
where `RenderedAssetIntegrityDeviceTest` compares both formats using Android's decoder.
Use `--check` to verify existing WebP resources without rewriting them.

`RenderedAtlas` shares live, full-resolution bitmaps between previews using weak
references. It decodes straight RGBA and lets Android premultiply the pixels, preserving
the PNG decoder's exact alpha rounding. Default outfits skip unused accessory atlases;
outfit celebrations also load the incoming outfit's atlas before the transition.
See the original optimization measurements
for size, memory, frame timing and pixel-equivalence evidence.

Intermediate renders go into
`.scratch/rendered-integration/render`. Atlas coordinates are documented in the
script and `RenderedRobotRig.kt`.

The renderer uses Compose's existing bitmap drawing API:
[Display layered images on a canvas](https://developer.android.com/develop/ui/compose/quick-guides/content/display-layered-image).
The renderer adds no runtime Android dependency. Startup benchmarking uses the
test-only AndroidX dependencies described below; toolchain versions are unchanged.

## Startup optimization

The `benchmark` test module generates an app-specific Baseline Profile
for the cold-launch journey through the existing intro to the usable home screen.
These profiles contain code paths for Android's compiler, not user data. Regular
builds consume the checked-in profiles without starting an emulator. Regenerate
them after changes to startup code, using an explicitly selected dedicated English
test emulator:

```powershell
$env:ANDROID_SERIAL = '<dedicated-emulator-serial>'
./gradlew.bat :app:generateReleaseBaselineProfile --no-daemon '-Pandroid.testInstrumentationRunnerArguments.class=fi.clocklings.benchmark.StartupProfileGenerator'
```

`benchmarkRelease` inherits release R8/resource optimization and uses the standard
debug key only for local measurements. `nonMinifiedRelease` is the unobfuscated
profile-generation variant. Production release signing remains unconfigured.
`StartupBenchmark` compares no precompilation with the APK's packaged profiles.
`ReportDrawnWhen` records full startup only once saved state has loaded and the
intro has finished.

The startup greeting now takes 1250 ms instead of 3150 ms, or 400 ms instead of
1200 ms with reduced motion. The existing logo animation and lettering run on a
shorter, overlapping timeline. The jingle and narrator can finish on Home without
holding the overlay open. Tap-to-skip and readiness checks remain in place.
Character/accessory artwork, image resolution and in-app companion motion are
unchanged.

The generator deliberately excludes a separate Startup Profile. An experiment
with startup DEX reordering did not improve usable startup on the dedicated
emulator and increased the uncompiled first-display median. The Baseline Profile
still permits ahead-of-time compilation of the captured startup code.

See the [shorter-intro measurements](../../reports/fast-startup/RESULTS.md) for
the current comparison and validation. The earlier
[profile measurements](../../reports/startup-optimization/RESULTS.md) retain
the original 3150 ms intro and describe a separate experiment.

## Validation

Completed on 12 November 2026:

1. Full JVM run: 499 tests passed, no failures, errors or skips, after the robot
   layer-order correction. The final focused run passed six tests covering all
   companion colors/items, independent robot part motion, first-launch preview
   setup and preservation of later user changes.
2. `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:ktlintCheck` and
   `:app:detekt` passed. `:app:lintDebug` passed with five warnings in unchanged
   code/configuration: one SVG path string-concatenation warning and four dependency
   update suggestions. No dependency upgrades were made.
3. Localization parity passed for all seven languages.
4. `RenderedRobotDeviceTest` passed on the dedicated temporary Android 8 / API 26
   emulator `ClocklingsRenderedRobot26`, using 420 dpi and 1080/840 pixel widths:
   411 dp / 100%, 320 dp / 100%, 411 dp / 200%, and 320 dp / 200%. The test opens
   the robot's wardrobe, changes its color, equips the golden key, recreates the
   activity, and checks the preserved selection. The resulting screenshots were
   visually inspected. Test entitlement/progress fixtures are separate from release.
5. A normal launch after clearing only this new app's data on the owned test
   emulator showed the robot and 15 stars. The saved preview initialization flag
   was confirmed. This was the regular debug application, without instrumentation.
6. The merged debug manifest uses `RenderedPreviewApplication`; the merged release
   manifest uses the original `ClocklingsApplication`. The APK identifies itself
   as the prototype package, with launcher label `Clocklings`.
7. Four transparent atlases were checked for clipped or empty rendered parts.
   Every original robot item remains visibly distinct in the JVM rendering tests.

The existing API 36 emulator repeatedly crashed in its Windows QEMU process
(`0xc0000005`), including with software graphics. API 36 device validation therefore
remains unverified; successful device evidence is API 26. An initial strict pixel
assertion failed once, then passed in isolation, the subsequent full run, and the
final focused run; the first failure's cause was not established. No physical
device, real Play purchase, TalkBack, owner-run `lc`/`sc`, or release APK validation
was performed.

Screenshots are in the local evidence directory for that run. The installable preview is
`app/build/outputs/apk/debug/app-debug.apk`. The original project's branch remains
`codex/full-unlock` at `8c71d6729175f0eb1714310d5eb80bfe9c6884fa`, with its pre-existing
untracked attachments preserved. No commit or push was made.

## Historical cleanup

The original verification used a temporary emulator and local render files. Its cleanup record predates the project move.
The temporary directory and emulator registration were later confirmed absent.
