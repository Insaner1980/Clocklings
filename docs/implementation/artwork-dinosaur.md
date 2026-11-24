# 3D dinosaur

The fifth 3D companion uses the owner's green dinosaur reference sheets,
ice-cream and duck swim-ring references, and existing dinosaur app screenshot.
It retains the three-quarter pose, rounded long muzzle, dark back plates and
spots, cream belly and claws, peach cheek and glossy brown eyes.

The robot, cat, dog and dragon models, sixteen atlases, cat/dog/dragon rigs and
four render scripts retain their pre-task SHA-256 hashes. The shared rig selector
adds only DINOSAUR; the robot drawing function is otherwise byte-for-byte
unchanged. Other companions, app identity, debug preview initialization,
dependencies and toolchain are unchanged.

## Deliverables and reproduction

- `art/models/dinosaur.blend`: normal 3D 5.3 file with one
  `Clocklings dinosaur` scene and 48 named objects, editable meshes, curves,
  materials, camera and studio lights. The alternate holding fist is hidden in
  the saved rest pose.
- `tools/artwork/render_dinosaur_layers.py`: model generation and rendering.
- `app/src/main/res/drawable-nodpi/dinosaur_rendered_*.png`: four 1284 x 832 RGBA atlases.
- `RenderedDinosaurRig.kt`: eight layers on the existing 240 x 300 design grid.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- the local evidence directory for that run: device screenshots and accessory/motion galleries.

Run from this repository:

Set the `CLOCKLINGS_3D_EDITOR` environment variable to the full path of the executable that opens `.blend` files.

```powershell
& $env:CLOCKLINGS_3D_EDITOR --background --factory-startup --threads 8 --python-exit-code 1 --python tools/artwork/render_dinosaur_layers.py
```

This renders the saved model. Add `-- --rebuild` to replace it from the script,
or `-- --rebuild --preview` for a full preview. Disposable intermediate renders
use `.scratch/rendered-dinosaur/render`.

Separate head, near/far eyes, body, tail, near/far arms and holding fist retain
head tilt, blinking, tail motion, breathing, sway, jump and outfit transitions.
The ice-cream pose hides the far arm, places the fist over the cone, and redraws
only the scoops above the muzzle. The duck ring retains its back/front layers.
The four supported colors (original green, blue, purple, orange) and all 16
existing vector accessories keep their IDs and wardrobe crops. The full frame
includes 80 design units above the character for tall accessories below the
star badge. This uses fixed-view layered artwork rather than a real-time 3D engine.

Reference images inspected: `Friendly green dinosaur reference sheet-7.png`,
`Front-Facing Green Dinosaur Reference Sheet-10.png`,
`Dinosaur holding a two-scoop cone-5.png`,
`Dinosaur with golden duck swim ring-4.png`, and the original `06-dinosaur.png`.

## Initial verification on 12 November 2026

These checks preceded the muzzle correction described below.

1. Full JVM suite: 506 passed, zero failures, errors or skips. This includes all
   companion colors/items, independent dinosaur head/tail/blink motion, previous
   3D companions, shared animation and debug preview state.
2. Debug app/test assembly, `ktlintCheck`, `detekt` and `lintDebug` passed.
   An initial missing blank line was corrected before the final run. Lint retains
   six existing warnings: four dependency suggestions, SVG text concatenation,
   and the unchanged preview SharedPreferences call.
3. Seven device test executions passed on the dedicated API 26 AVD
   `ClocklingsRenderedDinosaur26`, serial `emulator-5592`: wardrobe flow at
   411 dp / 100%, 320 dp / 100%, 411 dp / 200% and 320 dp / 200% font scale
   (1080/840 pixels at 420 dpi). Each changes all four colors, equips the party
   hat and ice cream, recreates the activity, and checks retained color and
   ice-cream selection. All four final screenshots were inspected. Large-font
   controls remain reachable by scrolling.
4. The 16-item/four-color gallery and animation strip were inspected.
   Hats, head flower, held items, necklaces, backpack, duck ring and skateboard
   remain visible. Normal spin, jump and idle produce different frames; animator
   duration scale 0 keeps identical pixels.
5. Normal debug launch retained the robot preview and 15 stars. The dinosaur
   was selected through the real companion picker. APK metadata identifies
   the prototype package, launcher label `Clocklings`.
6. All 32 layer bounds/opacity assertions passed. The saved model opened as one
   dinosaur scene with 48 objects. All 27 recorded robot/cat/dog/dragon file
   hashes matched their pre-task values. The robot source also matched after
   excluding the single new DINOSAUR selector line.

## Muzzle correction on 12 November 2026

The owner identified the muzzle as deformed. The overlapping skull/jaw/muzzle
ellipsoids left concave seams despite voxel smoothing. The head now uses one
continuous mesh with a smoothly extended rounded muzzle. Cheek, nostrils, smile
and eyes project onto that surface. The body, limbs, wardrobe anchors, Kotlin
drawing code and animation logic are unchanged.

The rebuilt model and four atlases passed all 32 layer bounds/opacity checks.
Four focused JVM tests passed (all companion colors/items and dinosaur
head/tail/blink movement), as did debug app/test assembly and lint. The full
506-test suite was not repeated for this artwork-only correction. Lint retained
the six existing warnings listed above.

All seven API 26 device test executions were repeated successfully, including
411/320 dp, 100%/200% text and their combination, activity recreation, the
16-accessory gallery, normal movement and reduced motion. Updated screenshots
were inspected, including the ice-cream overlap and all four colors. The 27
protected robot/cat/dog/dragon file hashes still match the original baseline.

Corrected APK SHA-256:
`70CA3DDB9A27B34AC828C6D9155DFE6A17EF69C63A4C52761852C38B373B3B18`.

Physical devices, API 36, TalkBack, real Play purchases, release builds and
owner-run `lc`/`sc` were not tested. No commit, push or pull request was made.

## Historical cleanup

The original verification used a temporary emulator and local render files. Its cleanup record predates the project move.
The temporary directory and emulator registration were later confirmed absent.
