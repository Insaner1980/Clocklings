# 3D unicorn

The current model has a four-beat walk in place. Play the timeline in
`art/models/unicorn.blend` to see the forelegs flex at the carpus and the
hind legs at the hock. The app poses the four textured legs independently and
attaches each sparkle boot to its own hoof. Reduced motion and wardrobe
thumbnails use the standing pose.

To reproduce the current Android artwork, render both edited scenes from the
repository root, then encode the PNG masters.

Set the `CLOCKLINGS_3D_EDITOR` environment variable to the full path of the executable that opens `.blend` files.

```powershell
$editor = $env:CLOCKLINGS_3D_EDITOR
& $editor -b art/models/unicorn.blend --python-exit-code 1 -P tools/artwork/export_wardrobe.py
& $editor -b art/models/unicorn_accessories.blend --python-exit-code 1 -P tools/artwork/export_wardrobe.py
python tools/artwork/optimize_atlases.py
```

## Original prototype and its verification

The seventh 3D companion follows the owner's
`White unicorn character reference sheet-8.png` and existing `08-unicorn.png`
app screenshot. It has a pearl-white coat, a cream muzzle, a smiling face,
glossy violet eyes, pink cheeks, pointed ears, a golden spiral horn, three pastel
mane and tail strands, four legs and violet hooves.

The body, neck and four legs are joined and voxel-remeshed into one smooth coat.
The head, eyes and tail remain independent animation layers. The existing
240 x 300 design grid, wardrobe anchors, item IDs and all 16 vector accessories
are reused. Horn rings align to the golden horn, boots cover the four original
hoof rectangles, and the cape, rainbow and wings retain their front/rear layers.
Head tilt, blinking, tail sway, breathing, jump and outfit spin use the existing
Compose motion. This is fixed-view layered artwork, not a real-time 3D engine.

## Deliverables and reproduction

- `art/models/unicorn.blend`: normal editable 3D 5.3 file, one
  `Clocklings unicorn` scene, 45 named objects, meshes, curves, materials,
  orthographic camera and studio lights.
- `tools/artwork/render_unicorn_layers.py`: model generation and layer rendering.
- `app/src/main/res/drawable-nodpi/unicorn_rendered_*.png`: four RGBA atlases,
  845 x 866 pixels, in original white, blue, green and purple.
- `RenderedUnicornRig.kt`: four atlas layers and existing wardrobe rendering.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- the local evidence directory for that run: device screenshots and accessory/motion galleries.

Run from this repository:

```powershell
& $env:CLOCKLINGS_3D_EDITOR --background --factory-startup --threads 8 --python-exit-code 1 --python tools/artwork/render_unicorn_layers.py
```

This renders the saved model. Add `-- --rebuild` to recreate the model from the
script, or `-- --rebuild --preview` to create a full-character preview. Temporary
render files use `.scratch/rendered-unicorn/render`.

Only UNICORN is added to the shared rig selector. The six earlier characters,
their render scripts, models and atlases are preserved, as are the original
unicorn vector rig and accessories. The Kotlin/Compose architecture, toolchain,
dependencies, application identity and debug preview initialization are unchanged.

## Verification on 2026-11-12

The final Gradle invocation passed in 4 minutes 29 seconds with `--no-daemon`:

```text
:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
:app:ktlintCheck :app:detekt :app:lintDebug
```

All 510 JVM tests passed with zero failures, errors or skips. The two new tests
verify head tilt, tail sway and blinking independently of resting hooves, return
to the resting pose, and sparkle boots covering all four hooves. Lint retained
the six existing warnings: AndroidGradlePluginVersion, two GradleDependency,
NewerVersionAvailable, TextConcatSpace and UseKtx.

Seven instrumentation test executions passed on dedicated API 26 AVD
`ClocklingsRenderedUnicorn26` (`emulator-5594`). The wardrobe flow passed at
411 dp / 100%, 320 dp / 100%, 411 dp / 200% and 320 dp / 200% font scale.
It changes colors, equips horn rings and sparkle boots, recreates the activity
and checks retained color and boot selection. The remaining executions cover
all 16 accessories, normal outfit spin/jump/idle motion and disabled system
animations. The four wardrobe screenshots and accessory/motion galleries were
visually inspected, including horn-ring alignment and all four boots.

Normal launcher startup retained the robot preview and 15 stars. Selecting
Unicorn displayed the new white model; `unicorn-in-app.png` records that view.

All 16 atlas regions passed opacity and crop checks after expanding the initial
tail crop to include its lilac crest. 3D reopened the saved file and
reported one scene and 45 objects. All 41 protected files for the robot, cat,
dog, dragon, corrected dinosaur and bunny retained their baseline SHA-256 hashes.
Removing the single new UNICORN selector line reproduces the previous shared
`RenderedRobotRig.kt` source after newline normalization.

APK metadata confirms the prototype package and launcher label
`Clocklings`. APK SHA-256:
`9D344063283CAA144BDC4638E5A6BC17E7B25535938172854481A63ED0BA9866`.
Model SHA-256:
`9FB248BCB847887978DDFCFE4F6839D754FDD640BF629136A9CFD6ACC52C4A31`.

No API 36 device, physical device, TalkBack, real Play purchase or release signing
was tested. The owner's `lc` and `sc` checks were not run. The original projects
and their installed application data were not modified. No commit or push was made.

## Historical cleanup

The original verification used a temporary emulator and local render files. Its cleanup record predates the project move.
The verification recorded a blocked cleanup attempt at the time.
