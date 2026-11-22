# Changelog

History of the build, part by part (newest at the bottom).

## Part 1

NEW:      core/.../world/Units.kt
NEW:      core/.../world/PhysicsWorld.kt
NEW:      core/.../world/BodyFactory.kt
MODIFIED: core/.../AngryBirds.kt   (Box2D.init())
MODIFIED: core/.../screens/GameScreen.kt (physics sandbox)
UNCHANGED: core/.../screens/MenuScreen.kt

REQUIRED Gradle change (Box2D extension was missing from Part 0 setup):

core/build.gradle (dependencies):
    api "com.badlogicgames.gdx:gdx-box2d:$gdxVersion"

android/build.gradle (dependencies), next to the existing gdx-platform natives:
    natives "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a"
    natives "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a"
    natives "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86"
    natives "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64"

Then: Sync Gradle, Build > Clean Project, run.

## Part 2

NEW:      core/.../entities/Entity.kt
NEW:      core/.../entities/Bird.kt
NEW:      core/.../systems/SlingshotConfig.kt
NEW:      core/.../systems/SlingshotController.kt
NEW:      core/.../render/GameRenderer.kt
MODIFIED: core/.../world/BodyFactory.kt   (Surface.BIRD, optional body type in createCircle)
MODIFIED: core/.../screens/GameScreen.kt  (slingshot sandbox, replaces tap-to-spawn balls)
UNCHANGED: AngryBirds.kt, MenuScreen.kt, PhysicsWorld.kt, Units.kt

No Gradle changes in Part 2.

## Part 3

NEW:      core/.../entities/Block.kt      (enum Material, Block)
NEW:      core/.../entities/Enemy.kt
NEW:      core/.../level/LevelData.kt
NEW:      core/.../level/Levels.kt        (LEVEL_1)
NEW:      core/.../level/LevelLoader.kt   (Level, LevelLoader)
MODIFIED: core/.../entities/Entity.kt     (maxHp, hp, isDestroyed)
MODIFIED: core/.../render/GameRenderer.kt (draws blocks and enemies; render takes Level)
MODIFIED: core/.../screens/GameScreen.kt  (loads LEVEL_1 instead of the test pyramid)
UNCHANGED: AngryBirds.kt, MenuScreen.kt, Bird.kt, SlingshotController.kt,
           SlingshotConfig.kt, PhysicsWorld.kt, BodyFactory.kt, Units.kt

No Gradle changes in Part 3.

## Part 4

NEW:      core/.../entities/DamageConfig.kt   (DamageProfile, all damage tuning)
NEW:      core/.../world/ImpactQueue.kt       (impacts recorded during the step)
NEW:      core/.../world/GameContactListener.kt (postSolve -> queue)
NEW:      core/.../systems/DamageSystem.kt    (apply damage, remove destroyed after the step)
MODIFIED: core/.../entities/Entity.kt         (damageProfile, healthFraction)
MODIFIED: core/.../entities/Block.kt          (Material.damage, damageProfile override)
MODIFIED: core/.../entities/Enemy.kt          (damageProfile override)
MODIFIED: core/.../level/LevelLoader.kt       (Level lists are MutableList)
MODIFIED: core/.../render/GameRenderer.kt     (damaged blocks darken, enemies redden)
MODIFIED: core/.../screens/GameScreen.kt      (contact listener + DamageSystem wiring)
UNCHANGED: AngryBirds.kt, MenuScreen.kt, Bird.kt, SlingshotController.kt,
           SlingshotConfig.kt, PhysicsWorld.kt, BodyFactory.kt, Units.kt, Levels.kt, LevelData.kt

No Gradle changes in Part 4.

## Part 5

NEW:      core/.../systems/GameStateManager.kt (GameState, bird queue, win/lose)
NEW:      core/.../systems/ScoreManager.kt
NEW:      core/.../render/Hud.kt               (score, birds left, result overlay, Retry)
MODIFIED: core/.../systems/DamageSystem.kt     (DestroyListener hook)
MODIFIED: core/.../screens/GameScreen.kt       (wiring only; sandbox bird reload removed)
UNCHANGED: everything else (GameRenderer, Bird, SlingshotController, entities, level, world, MenuScreen, AngryBirds)

Retry = new GameScreen; hide() disposes the old World, so nothing leaks.
Camera follow (optional) is not implemented. No Gradle changes.

## Part 6

NEW:      core/.../assets/Assets.kt          (texture loading + placeholder fallback + circular face crop)
NEW:      core/.../assets/PlaceholderArt.kt  (generated placeholder images)
NEW:      ASSETS.md                          (expected files)
MODIFIED: core/.../render/GameRenderer.kt    (sprites via SpriteBatch; bands still ShapeRenderer; debug overlay off by default)
MODIFIED: core/.../AngryBirds.kt          (owns and disposes Assets)
MODIFIED: core/.../screens/GameScreen.kt     (new GameRenderer constructor)
MODIFIED: core/.../screens/MenuScreen.kt     (background image)
UNCHANGED: everything else (Hud, systems, entities, level, world)

Not done (optional in the guide): trajectory dots, parallax, particles.
No Gradle changes.

## Part 7

NEW:      core/.../audio/SoundEffects.kt   (launch/hit/win/lose, optional files, throttled hits)
NEW:      core/.../systems/GameEvents.kt   (launch/impact/win/lose notifications)
MODIFIED: core/.../systems/GameStateManager.kt (events param; onLaunch/onWin/onLose)
MODIFIED: core/.../systems/DamageSystem.kt     (events param; onImpact)
MODIFIED: core/.../AngryBirds.kt            (owns SoundEffects; pause() stops sounds; disposes)
MODIFIED: core/.../screens/GameScreen.kt       (passes game.sounds)
MODIFIED: ASSETS.md                            (sound files)
UNCHANGED: everything else

Not done: vibration (needs the VIBRATE permission in AndroidManifest.xml, which is not in this project zip).
No Gradle changes.

## Part 9 (final QA and delivery)

NEW:      README.md, docs/CHANGELOG.md, docs/TESTING.md, docs/DEMO_SCRIPT.md
NEW:      .gitignore, .editorconfig
NEW:      Gradle and Android module files (reference scaffold, see README "Build files")
MOVED:    CHANGES_PART1..7.md merged into this file
MODIFIED: screens/GameScreen.kt   (compile fix: Input.setCatchKey instead of isCatchKey(int, boolean); stale comment)
MODIFIED: world/PhysicsWorld.kt   (removed unused deferred-action queue)
MODIFIED: world/BodyFactory.kt    (removed unused Surface.CRATE/BALL and the unused userData parameters)
MODIFIED: entities/Enemy.kt       (stale comment)
UNCHANGED: everything else

## Part 10 (visual and feel polish)

REQUESTED: fullscreen without side bars, sound effects, better character images, smoother animation,
dotted aiming path, dotted flight trail, star rating.

NEW:      core/.../render/Effects.kt          (debris, smoke, score popups, Ease)
NEW:      core/.../render/Trajectory.kt       (AimPreview, FlightTrail)
NEW:      core/.../systems/Progress.kt        (best score / stars on the device)
NEW:      tools/generate_art.py, generate_sounds.py, generate_font.py
NEW:      android/assets/*                    (images, sounds, font; see ASSETS.md)
MODIFIED: core/.../AngryBirds.kt           (outlined font, Progress)
MODIFIED: core/.../screens/GameScreen.kt, MenuScreen.kt   (ExtendViewport, effects, fade-in, buttons)
MODIFIED: core/.../render/GameRenderer.kt, Hud.kt
MODIFIED: core/.../assets/Assets.kt, PlaceholderArt.kt    (new textures, mip-maps, tiling, pig_face.png fallback)
MODIFIED: core/.../audio/SoundEffects.kt      (16 sounds; hit.ogg replaced by hit_wood/stone/glass/pig)
MODIFIED: core/.../systems/GameEvents.kt, DamageSystem.kt, GameStateManager.kt, ScoreManager.kt,
          SlingshotController.kt, SlingshotConfig.kt
MODIFIED: core/.../entities/Entity.kt, Bird.kt, world/PhysicsWorld.kt   (interpolation, bird hop)
MODIFIED: android/.../AndroidLauncher.kt, res/values/styles.xml, res/values-v28/styles.xml
NO Gradle change.

## Part 11 (levels, pause, bird types, audio)

REQUESTED: Duolingo-style level map (level 1 only), pause with restart/menu, red + yellow + blue birds,
shorter aim dots, harder level, bird voices, better background and underground, background music.

NEW:      entities/BirdType.kt; screens/LevelSelectScreen.kt; audio/MusicPlayer.kt
NEW:      android/assets: bird_yellow/blue, pause_icon, node_open/locked, lock, bird voices, yellow_dash,
          blue_split, locked, music_menu, music_game (generated by tools/)
MODIFIED: tools/generate_art.py (new background and ground, new birds/icons),
          tools/generate_sounds.py (voices, ability sounds, two music tracks)
MODIFIED: entities/Bird.kt (type, per-bird radius and mass, abilities, split birds)
MODIFIED: systems/GameStateManager.kt, SlingshotController.kt, GameEvents.kt, ScoreManager.kt (thresholds 20000/30000/38000)
MODIFIED: level/LevelData.kt, LevelLoader.kt, Levels.kt (birds list, harder level 1, four pigs)
MODIFIED: render/GameRenderer.kt, Hud.kt (pause button + panel), Trajectory.kt (6 dots)
MODIFIED: screens/GameScreen.kt (pause, level index, ability effect), MenuScreen.kt (Play opens the level map)
MODIFIED: audio/SoundEffects.kt, AngryBirds.kt, assets/Assets.kt
NO Gradle change.

## Part 12 (reference level, splash, finale, supplied audio)

REQUESTED: zoom onto the winners who celebrate; bigger detailed stars; splash screen from sp_bg; redesign in the
theme of the reference screenshot; level 1 identical to the screenshot (background, ground, underground, theme);
supplied sounds and the theme song.

NEW:      screens/SplashScreen.kt; tools/generate_level.py, tools/art_scenery.py
NEW:      android/assets: sp_bg, terrain_l1, bench, tnt, pig_icon, star_glow, sparkle, explosion;
          your sounds as stretch, bird_launch, win, lose, big_shot, music_menu
MODIFIED: world/Units.kt (world 23.4 x 10.8 m, ground 1.95 m), systems/SlingshotConfig.kt (slingshot on a bench)
MODIFIED: level/Levels.kt (generated: 108 blocks, 5 pigs, terrain polygons, birds blue/yellow/blue/red/red),
          LevelData.kt, LevelLoader.kt, world/BodyFactory.kt (static terrain polygons)
MODIFIED: entities/Block.kt (TNT), DamageConfig.kt (TNT, blast, longer settle grace), Enemy.kt (pig radius 0.33), Bird.kt
MODIFIED: systems/DamageSystem.kt (TNT explosion), GameStateManager.kt (waiting slots, big-shot event), ScoreManager.kt,
          GameEvents.kt
MODIFIED: render/GameRenderer.kt (terrain, bench, parallax zoom, celebrations), Hud.kt (original-game style HUD, big stars),
          Effects.kt (confetti, explosion)
MODIFIED: screens/GameScreen.kt (end-of-level camera), MenuScreen.kt (lobby on the title artwork), AngryBirds.kt
MODIFIED: audio/SoundEffects.kt, assets/Assets.kt; tools/generate_art.py, generate_sounds.py
NO Gradle change.

## Part 13 (feel, finale, professional UI, level tower)

REQUESTED: slower launch, abilities only before the first hit, ability badges and three blue trails on the aiming path,
counting numbers, no multi-window, bird bonus counted one by one at the end, professional icon UI without pulsing,
a tower-style level list, easier pigs and TNT, delayed level end after the last pig, a single "continue campaign" button
in the lobby.

NEW:      tools/art_ui.py; render/CountUp.kt, render/Finale.kt
NEW:      android/assets: btn_*, icon_*, mark_*, tower_bg, tower_floor_0..3, door_*, tag_*; new panel.png
REMOVED:  button_green/blue, pause_icon, node_open/locked, lock (replaced)
MODIFIED: world/Units.kt (gravity), systems/SlingshotConfig.kt (power), systems/GameStateManager.kt (win delay, bonus),
          world/GameContactListener.kt + entities/Bird.kt (abilities end at the first hit), entities/DamageConfig.kt,
          Enemy.kt, Block.kt (easier), ScoreManager.kt, GameEvents.kt, audio/SoundEffects.kt
MODIFIED: render/Trajectory.kt (multi-path trail + ability badges), GameRenderer.kt, Hud.kt (icon buttons, count-up)
MODIFIED: screens/GameScreen.kt (level-end timeline, press/release buttons), LevelSelectScreen.kt (tower),
          MenuScreen.kt (one button), assets/Assets.kt, android manifest and AndroidLauncher.kt
NO Gradle change.


## Part 13 fixes (balance, trail, level map, bonus count)

MODIFIED: core/.../entities/Enemy.kt (pig HP 40 -> 44, about 10% tougher)
MODIFIED: core/.../entities/DamageConfig.kt (TNT impact threshold 0.1 -> 0.2; TNT_BLAST_FACTOR 0.45 for crates caught in a blast)
MODIFIED: core/.../entities/Block.kt (TNT crate HP 15 -> 20)
MODIFIED: core/.../systems/DamageSystem.kt (blast damage to TNT crates scaled by TNT_BLAST_FACTOR: chain reactions need a close blast)
MODIFIED: core/.../render/Trajectory.kt (FlightTrail: all dots white; ability marks are a white cloud made of puffs instead of badges)
MODIFIED: core/.../render/GameRenderer.kt (trail calls without tints; only the unfired birds cheer, on compact spots; no glow on counted birds)
MODIFIED: core/.../render/Effects.kt (spawnPopup(burst = true): the number pops out of the bird with an overshoot)
MODIFIED: core/.../screens/GameScreen.kt (bonus popup spawns at the bird, \"+10000\" in order)
MODIFIED: core/.../screens/LevelSelectScreen.kt (drag direction reversed: drag down for higher floors, up for lower)
NO Gradle change.

## Part 13 fixes v3 (smooth bird bonus, numbers flying into the score, one trail)

REQUESTED: the unused birds at the end of a won level jumped up suddenly from wherever they were and moved roughly;
count them so that a "10000" comes out of each bird and is added to the total score; keep only the dotted trail of the
last shot.

CAUSE:    the renderer drew the unused birds on fixed "cheering spots" (so they teleported away from their waiting places
          and from the slingshot) and their jump height changed from 0.12 m to 1 m in one frame at the moment of counting
          (|sin| motion with a corner at the ground); the score was added at once and the popup only floated up.

NEW:      core/.../render/CheerPose.kt (crouch, one parabola jump with stretch, landing squash, happy hops: continuous in time)
NEW:      core/.../render/BonusFlyers.kt ("+10000" pops out of the bird at the top of its jump, flies to the score; the score
          is added when the number arrives)
MODIFIED: core/.../render/Finale.kt (home place and count time of every unused bird; the fixed spots are gone)
MODIFIED: core/.../render/GameRenderer.kt (unused birds stay where they wait, also the one on the slingshot; band goes slack
          when it jumps; FlightTrail is cleared when the next bird is launched)
MODIFIED: core/.../render/Trajectory.kt (FlightTrail.clear(); only the last shot's trail is kept)
MODIFIED: core/.../render/Hud.kt (scoreTargetX/Y for the flying numbers, pulseScore(): the score swells smoothly)
MODIFIED: core/.../render/Effects.kt (the burst mode of spawnPopup is removed, it is not used any more)
MODIFIED: core/.../screens/GameScreen.kt (prepareFinale(), bonus timeline: jump -> number -> landing -> result panel;
          the camera frames the birds where they stand)
MODIFIED: core/.../systems/GameEvents.kt (comment of onBonus)
NO Gradle change. NO asset change.

## Part 14 (TNT throws far pieces away, elegant score popups)

REQUESTED: a TNT blast must not destroy every piece; the farther pieces should be thrown away from the explosion.
Every destroyed piece shows its points briefly in a minimal, professional way (appearing animation).

MODIFIED: core/.../entities/DamageConfig.kt (BLAST_KILL_RADIUS, BLAST_THROW_RADIUS, BLAST_THROW_SPEED, BLAST_THROW_LIFT, BLAST_SHIELD_SECONDS)
MODIFIED: core/.../entities/Entity.kt (damageShield: no collision damage for a short time after being thrown)
MODIFIED: core/.../systems/DamageSystem.kt (blocks inside the kill radius are shattered as before; blocks between the kill radius
          and the throw radius take no blast damage and are thrown away from the explosion; enemies behave as before)
MODIFIED: core/.../render/Effects.kt (popups: fade in + soft grow, small drift up, fade out, soft shadow, stacking of numbers that appear together)
MODIFIED: core/.../screens/GameScreen.kt (popup text is "+points")
NO Gradle change. NO asset change.

## Part 14 fix (popups were invisible, TNT throw too strong)

CAUSE:    the shared BitmapFont rounds glyph positions to whole units; popups are drawn in meters at a tiny scale, so all
          glyphs collapsed into one spot. Effects.drawPopups now switches the rounding off while it draws.
MODIFIED: core/.../render/Effects.kt (setUseIntegerPositions(false) during drawPopups, restored after)
MODIFIED: core/.../entities/DamageConfig.kt (BLAST_THROW_SPEED 7.5 -> 2.4, BLAST_THROW_LIFT 0.35 -> 0.2, BLAST_SHIELD_SECONDS 0.8 -> 0.6)

## Part 15 (pig bar, score bar with stars, bronze / silver / gold stars)

REQUESTED: the pig counter ("1/5") becomes a bar that fills as pigs are defeated; a second bar shows how many stars the
current score earns; the stars are bronze, silver, gold in order.

NEW:      android/assets/star_bronze.png, star_silver.png, star_glow_bronze.png, star_glow_silver.png (made by tools/recolor_stars.py)
NEW:      tools/recolor_stars.py
MODIFIED: core/.../assets/Assets.kt (starOnFor(index), starGlowFor(index))
MODIFIED: core/.../render/CountUp.kt (current: fractional value for smooth bars)
MODIFIED: core/.../render/Hud.kt (pig bar with one slice per pig, no "x/5" text; score bar under it filled by score / BAR_MAX,
          its color follows the rank reached, three stars at the star thresholds light up with a pop; result stars use the new colors)
MODIFIED: core/.../screens/LevelSelectScreen.kt (earned stars on the doors use the same colors)
NO Gradle change.

## Part 16 (lobby redesign, sound settings, gold middle star, pig counter)

REQUESTED: update the README; redesign the lobby (the screen after the splash) and add sound settings (music and touch/sound
effects); make the middle star gold; show the defeated-pig counter with the pig bar.

NEW:      core/.../systems/Settings.kt (music on/off, sound on/off, saved in Preferences "slingshot_settings")
NEW:      android/assets/icon_gear.png, icon_music.png, icon_sound.png, icon_close.png, icon_slash.png (tools/art_ui.py)
MODIFIED: core/.../screens/MenuScreen.kt (new lobby: shade, drifting sparkles, floating Continue button with a glow,
          best-score pill, round settings button, settings panel with two animated switches, entrance animations,
          Back closes the panel or leaves the game)
MODIFIED: core/.../audio/MusicPlayer.kt (`enabled`: master switch, fades out when off and back in when on)
MODIFIED: core/.../audio/SoundEffects.kt (`enabled`: master switch for every effect including clicks; stops playing sounds)
MODIFIED: core/.../AngryBirds.kt (owns Settings; setMusicOn / setSoundOn; settings applied at start-up)
MODIFIED: core/.../assets/Assets.kt (new icons; star order is now bronze, gold, silver, so the middle star is gold)
MODIFIED: core/.../render/Hud.kt (defeated/total counter inside the pig bar, gold when all pigs are down; score bar tier
          colors follow the new star order)
MODIFIED: tools/art_ui.py, tools/generate_art.py (the new icons)
MODIFIED: README.md (rewritten to match the project), ASSETS.md, docs/TESTING.md
NO Gradle change.


## Part 16 addendum (settings in the pause panel)

MODIFIED: core/.../render/Hud.kt (HudButton.MUSIC / SOUND; two round switches left and right of the resume button)
MODIFIED: core/.../screens/GameScreen.kt (handles the two switches like the lobby does)

## Part 17 (volume sliders, score bar star order, README)

REQUESTED: let the player set the volume of the music and the sound effects in the settings (lobby) and in the game
(pause panel), and mute / unmute each one by tapping its icon; on the in-game score bar the second star must be silver
and the third gold (the result panel keeps the gold star in the middle); update the README to match the project and add
a Persian version. Nothing else changes.

NEW:      core/.../render/VolumeSlider.kt (slider drawing and touch -> value, shared by the lobby and the pause panel)
NEW:      README.fa.md (the README in Persian)
MODIFIED: core/.../systems/Settings.kt (musicVolume, soundVolume, saved in Preferences; default 100%)
MODIFIED: core/.../audio/MusicPlayer.kt (volumeScale multiplies the track volume)
MODIFIED: core/.../audio/SoundEffects.kt (volumeScale multiplies every effect, clicks included)
MODIFIED: core/.../AngryBirds.kt (setMusicVolume / setSoundVolume; volumes applied at start-up; unmuting at 0% restores 50%,
          moving a slider while muted unmutes)
MODIFIED: core/.../screens/MenuScreen.kt (settings panel rows: round mute button + label + percent + slider instead of the switches)
MODIFIED: core/.../render/Hud.kt (pause panel: music row and sound row with slider and percent, restart / menu below;
          score bar stars bronze, silver, gold via SCORE_BAR_STAR_STYLE and matching bar colors)
MODIFIED: core/.../screens/GameScreen.kt (forwards finger drags to the pause panel sliders)
MODIFIED: README.md (updated to the current project, link to the Persian version), docs/TESTING.md
NO Gradle change. NO asset change.


## Part 17 addendum (result panel star order)

MODIFIED: core/.../render/Hud.kt (result panel: the stars keep their places, gold in the middle, but appear in the order
          bronze, silver, gold; the star sounds follow that order)
MODIFIED: README.md, README.fa.md, docs/TESTING.md

## Part 17 addendum 2 (bigger final score, star light)

MODIFIED: core/.../render/Hud.kt (result panel: bigger score, panel 680 high with the layout shifted, "NEW BEST!" moved beside
          the BEST line; star light: soft halo, landing flash, two counter-rotating ray layers, twinkles, drawn in a separate
          additive pass so a glow never covers a neighbouring star)
MODIFIED: README.md, README.fa.md, docs/TESTING.md
NO asset change.

## Part 18 (font, backgrounds, icon, faces)

REQUESTED: use the supplied font (ROGENZ) for all text and numbers; the supplied bg.png as the background of the splash
screen and the lobby; the supplied icon.jpg as the app icon; the four supplied photos as faces (img1 the pig, img2 to
img4 the three birds). Nothing else changes.

NEW:      tools/make_faces.py, tools/make_launcher_icon.py, tools/fonts/ROGENZ-Demo.ttf, tools/source_art/
          (img1..4.jpg, icon.jpg, bird*_orig.png, the OpenCV face detector file)
MODIFIED: tools/generate_font.py (ROGENZ instead of Archivo Black; ! % + - / : = built from the font's strokes because
          the demo font does not have them), tools/generate_art.py (bird bodies go to tools/source_art, pig_icon is
          no longer overwritten)
MODIFIED: android/assets/font.fnt, font.png (same metrics: 64 px glyphs, base 62, line height 81)
MODIFIED: android/assets/sp_bg.png (the supplied bg.png; the level background bg.png is NOT touched)
MODIFIED: android/assets/enemy_head.png, pig_icon.png, bird.png, bird_yellow.png, bird_blue.png (faces; the bird
          sprites are now 512 px, same proportions, so GameRenderer is unchanged)
MODIFIED: android/res/mipmap-*/ic_launcher.png
MODIFIED: ASSETS.md, README.md, README.fa.md, docs/TESTING.md, PROJECT_GUIDE.md
NO Kotlin change. NO Gradle change.
