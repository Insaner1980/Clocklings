# 3D dog

The third 3D companion follows the owner's caramel puppy reference sheet
and existing dog screenshot: long chocolate floppy ears, a right-eye patch,
cream muzzle and chest, glossy eyes, pink tongue, seated paws and an upturned tail.
The original robot and cat models, eight atlases, cat rig and both rendering
scripts have unchanged SHA-256 hashes. The shared rig selector adds only DOG;
robot drawing code, cat drawing code, other companions and preview setup remain
unchanged. No dependencies or toolchain versions changed.

## Deliverables and reproduction

- `art/models/dog.blend`: normal 3D 5.3 file, one `Clocklings puppy` scene,
  49 named objects, editable meshes/curves, materials, camera and studio lights.
- `tools/artwork/render_dog_layers.py`: model generation and rendering.
- `app/src/main/res/drawable-nodpi/dog_rendered_*.png`: four 1112 x 880 RGBA atlases.
- `RenderedDogRig.kt`: six layers on the existing 240 x 300 design grid.
- `app/build/outputs/apk/debug/app-debug.apk`: installable debug application.
- the local evidence directory for that run: seven inspected screenshots, including all 16 items.

Run from this repository:

Set the `CLOCKLINGS_3D_EDITOR` environment variable to the full path of the executable that opens `.blend` files.

```powershell
& $env:CLOCKLINGS_3D_EDITOR --background --factory-startup --threads 8 --python-exit-code 1 --python tools/artwork/render_dog_layers.py
```

This renders the saved model. Add `-- --rebuild` to replace it from the script,
or `-- --rebuild --preview` for one complete preview. Intermediate renders use
`.scratch/rendered-dog/render`.

The body, head, eyes, tail and two ears are separate layers. Compose retains head
tilt, blinking, right-ear movement, tail wagging, breathing, sway, jump and outfit
transitions. Reduced motion uses the existing system setting. Four colors and
all 16 existing vector accessories retain their IDs and wardrobe crops. The full
frame adds 64 design units above the model to keep tall hats below the star badge
at narrow widths and large font sizes. This is fixed-view layered artwork.

## Verification on 12 November 2026

1. Full JVM suite: 503 passed, no failures, errors or skips. This includes all
   companion colors/items, independent dog head/ear/tail/blink movement, existing
   robot/cat rendering, shared motion and debug preview state.
2. `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:ktlintCheck`,
   `:app:detekt` and `:app:lintDebug` passed. Lint has the same six existing
   warnings: four dependency suggestions, SVG text concatenation and the debug
   preview SharedPreferences call.
3. Seven device test executions passed on dedicated API 26 AVD
   `ClocklingsRenderedDog26`, serial `emulator-5592`. Wardrobe flow passed at
   411 dp / 100%, 320 dp / 100%, 411 dp / 200% and 320 dp / 200% font scale
   (1080/840 pixels at 420 dpi). Each changes all colors, equips party hat and
   frisbee, recreates the activity and checks retained color/frisbee selection.
   All four final wardrobe images were inspected; controls remain scrollable.
4. The 16-item/four-color gallery and animation strip were inspected. Normal
   animation changes across spin, jump and idle; animator duration scale 0 keeps
   identical pixels. Hats, boots, backpack, cape and mouth-held frisbee fit.
5. Normal debug launch showed the unchanged robot preview and 15 stars. The dog
   was then selected through the real picker; `dog-in-app.png` records the result.
   APK identity is the prototype package, launcher label `Clocklings`.
6. All 24 layer bounds/opacity assertions passed. The saved model reopened in
   3D with its single puppy scene. All 13 recorded robot/cat file hashes
   matched their pre-task values.

APK SHA-256:
`7EB0AE944876F0522E3E0CDABF291F85DA4C20A4E5C49ED8268DB345F2A1C39C`.

Physical devices, API 36, TalkBack, real Play purchases, release builds and
owner-run `lc`/`sc` were not tested. No commit, push or pull request was made.

## Historical cleanup

The original verification used a temporary emulator and local render files. Its cleanup record predates the project move.
The verification recorded a blocked cleanup attempt at the time.
