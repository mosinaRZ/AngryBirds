# Demo script (about 5 minutes)

Have ready: the app installed on the phone, the project open in Android Studio,
a second face image to swap in, and `docs/TESTING.md` for reference.

## 1. Pitch (30 s)

"This is an Angry Birds-style game in Kotlin with libGDX and Box2D. One level,
three birds, three enemies, and the enemies' heads are a custom image. I focused
on a clean architecture, correct physics handling and tuning, not just getting
something on screen."

## 2. Live gameplay (90 s)

1. Menu, then Play. Point out: landscape, fullscreen, HUD with score and birds.
2. Drag the bird, show the rubber band and the clamped pull, then fire.
3. Hit the tower: blocks darken as they take damage, glass breaks first, stone
   resists, the tower collapses naturally.
4. Finish the level, show "LEVEL CLEARED!" and the bonus for unused birds.
5. Retry, then deliberately miss three times to show the lose path.

## 3. The face swap (30 s)

Replace `android/assets/enemy_head.png` with the second image, rebuild, run.
"Only the file changed. The image is center-cropped to a circle at load time, so
the code never knows what the face is."

## 4. Code tour (2 min)

Open in this order:

1. `GameScreen.kt`: "Wiring only. Each frame: step physics, apply damage,
   update game state, draw."
2. `PhysicsWorld.kt`: fixed 1/60 s timestep with an accumulator, frame time
   clamped to 0.25 s, so physics is the same on every device.
3. `GameContactListener.kt` and `DamageSystem.kt`: "Callbacks run inside the
   step, so they only record impulses in a queue. Bodies are destroyed after the
   step, which is the classic Box2D crash I designed around."
4. `DamageConfig.kt`: "All damage tuning in one place: threshold and factor per
   material."
5. `Assets.kt`: "Every asset is optional. A missing file becomes a generated
   placeholder, so the app never crashes on a missing file."
6. `GameEvents.kt` and `SoundEffects.kt`: "Game logic reports events; audio is
   one listener. Adding vibration or particles would not touch the game logic."

## 5. Decisions and trade-offs (30 s)

- Meters in the physics world, a viewport for scaling, a separate virtual screen
  for the HUD.
- The bird is static while aimed: no joints needed.
- Retry creates a new screen, so nothing leaks between attempts.
- Not done on purpose (optional in the brief): trajectory preview, follow camera,
  particles, leaderboard. One level, hardcoded data.

## Likely questions

- **Why a fixed timestep?** Variable steps make Box2D unstable and results
  device-dependent. The accumulator keeps physics deterministic while rendering
  runs at the display rate.
- **How is damage computed?** From the normal impulse in `postSolve`, minus a
  per-material threshold, times a factor. Resting contacts produce small impulses
  and fall under the threshold.
- **What happens on pause?** The slingshot drag is cancelled, sounds are stopped,
  textures are rebuilt after a GL context loss (placeholders are managed pixmaps).
- **What would you do next?** Trajectory preview, a second level loaded from JSON,
  unit tests for `DamageProfile` and `ScoreManager`, and a leaderboard.
