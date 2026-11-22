# Final test checklist

Run on a real Android device (and, if possible, on an emulator). Watch Logcat
for `FATAL EXCEPTION` the whole time. Expected warnings about missing assets are
fine (tags `Assets`, `SoundEffects`).

## 1. Build

- [ ] Fresh clone: `./gradlew assembleDebug` succeeds with no errors.
- [ ] The debug APK installs and the app icon appears.

## 2. Launch and menu

- [ ] App starts in landscape, fullscreen, no status or navigation bar.
- [ ] Menu shows the title and the PLAY button.
- [ ] Tapping outside the button does nothing.
- [ ] Back button on the menu closes the app.

## 3. Aiming and launching

- [ ] Touching far from the bird does nothing.
- [ ] Dragging the bird stretches the rubber band; the bird never goes beyond 1.8 m
      from the anchor and never below the ground.
- [ ] A very short pull and release returns the bird to the slingshot (no shot).
- [ ] A normal pull and release fires the bird on a natural arc.
- [ ] A fired bird cannot be grabbed again.
- [ ] A second finger during a drag does not break aiming.

## 4. Physics and damage

- [ ] The tower stands still for 10+ seconds without a shot.
- [ ] No damage or score changes during the first second after loading.
- [ ] Glass breaks easily, wood medium, stone hardest.
- [ ] Damaged blocks darken and damaged enemies turn red before they break.
- [ ] Destroyed blocks and enemies disappear; no crash while the tower collapses.
- [ ] Score increases when something is destroyed.

## 5. Game flow

- [ ] After a bird comes to rest, leaves the screen or times out, the next bird
      appears on the slingshot. "Birds" in the HUD counts down on each shot.
- [ ] **Win path:** destroy all enemies, "LEVEL CLEARED!" appears with the score
      including the bonus for unused birds.
- [ ] **Lose path:** miss with all 3 birds, "OUT OF BIRDS" appears about 2 seconds
      after everything stops moving.
- [ ] Retry on the result screen starts a fresh level: full tower, 3 birds, score 0.
- [ ] Play 5 Retries in a row: no slowdown, no crash.
- [ ] Back button during the game returns to the menu; Play starts a clean game.

## 6. Lifecycle

- [ ] Press Home during aiming, then return: the bird is back on the slingshot.
- [ ] Press Home while a bird is flying, then return: the game continues, no crash.
- [ ] Lock and unlock the screen during play: no crash, textures still visible.
- [ ] Open the recent-apps screen and come back: no crash.
- [ ] Rotate the device: the game stays in landscape (it may flip between the
      two landscape directions) and the picture is not stretched.

## 7. Assets and audio

- [ ] With an empty `android/assets/`: the game runs with placeholder graphics and
      no sound; Logcat shows warnings only.
- [ ] Replace `enemy_head.png` with a face image and rebuild: the face changes on
      all enemies with no code change.
- [ ] Put in only some files (for example only `bird.png`): the rest stay placeholders.
- [ ] A corrupt image or sound file does not crash the app (warning in Logcat).
- [ ] With sounds present: launch, hit, win and lose each play; hits are not
      machine-gunned while the tower collapses.
- [ ] Sound stops when the app goes to the background.

## 8. Performance

- [ ] Smooth gameplay (about 60 FPS), no visible stutter during the collapse.
- [ ] Tapping the top-right corner shows and hides the Box2D debug overlay.

## Part 10 additions

- [ ] Fullscreen: no black or blue bars on any side, also on a 20:9 phone and on a 4:3 tablet;
      the slingshot and the tower stay fully visible; nothing important is hidden under the notch.
- [ ] Aiming: dotted arc appears while pulling, follows the pull, disappears on release; a short
      pull (below the minimum) shows no dots. The real shot follows the dots until it hits something.
- [ ] After a shot, white dots mark the flown path and stay on screen while aiming the next shot; when the next bird is
      launched the old dots disappear and only the new shot's trail is shown (also after a blue bird's split).
- [ ] Band: thinner when stretched, snaps back with a small wobble after release; leather pouch under the bird.
- [ ] The next bird jumps from the ground to the slingshot and cannot be grabbed while jumping.
- [ ] Sounds: stretch, launch, hop, hit (wood/stone/glass/pig/ground), break (wood/stone/glass), pig pop,
      win/lose, star (three rising pitches), button click. No crash when the app is paused mid-sound.
- [ ] Blocks show cracks and enemies show bruises as they get damaged; debris and smoke on destruction;
      floating score numbers appear.
- [ ] Stars: bar in the HUD lights star markers; the result panel shows 0 stars when lost, 1 to 3 when won;
      one bird used = 3 stars, all three birds used = 1 star (approximately, see ScoreManager).
- [ ] Best score / stars survive an app restart and are shown in the menu.
- [ ] Smoothness: no visible jitter when the tower collapses, also on a 90/120 Hz display.

## Part 11 additions

- [ ] Menu -> Play opens the level map; only level 1 is colored and has START and its stars; tapping levels 2 to 8
      wobbles them with a dull sound; dragging scrolls the map; Back returns to the menu.
- [ ] The pause button (top left) freezes everything (birds, blocks, effects); Resume continues exactly where it was;
      Restart starts a fresh attempt; Menu opens the level map. The Android back key toggles pause. Pressing Home
      during play pauses the game.
- [ ] Order of birds: red, yellow, blue. Tapping while the red bird flies does nothing.
- [ ] Yellow: tap in flight -> a clear speed-up and a dash sound. Blue: tap in flight -> three birds fan out; the next bird
      only comes when all of them have stopped.
- [ ] Aiming shows only a short run of dots (about six).
- [ ] The level needs real aiming: four pigs, a stone slab and stone pillars protect the left pigs, the right tower has two
      pigs inside and one on the roof. It must still be winnable with three birds (try it).
- [ ] Idle chirp: leave the bird on the slingshot for about ten seconds; the voices differ per bird.
- [ ] Music: menu track in the menu and on the map, ambience during play (quieter while paused); no restart when moving
      between the menu and the map; silence (no crash) if a music file is removed.

## Part 12 additions

- [ ] Splash: the artwork fades in and slowly pushes in, the loading bar fills with a rolling bird, then it fades to the lobby
      (a tap after about a second skips). The lobby plays the theme song; PLAY opens the level map.
- [ ] Level 1 looks like the reference: three glass towers, the big structure with a TNT crate inside and one on the ground,
      the stone-cage tower on the tall rock with orange wooden stilts, five pigs, the slingshot on a wooden bench with
      the waiting birds on the bench and the ground.
- [ ] The structures stand still after loading (nothing collapses by itself in the first seconds). If a tower falls apart
      by itself, report which one.
- [ ] A TNT crate that is destroyed explodes: nearby blocks and pigs are pushed away and damaged; the explosion sound plays.
- [ ] Sounds: pulling the band plays your slingshot sound; releasing plays the chuck call; a shot that earns 7000+ points
      plays the laugh when it ends; all pigs gone plays "plush yeah"; out of birds plays "level failed". Leaving the
      screen cuts these sounds off.
- [ ] Level end, birds win: the camera eases onto the unused birds, which stay exactly where they were waiting (no bird
      teleports), confetti falls, then the result panel.
- [ ] Level end, pigs win: the camera eases onto the remaining pigs, they hop and wobble, confetti, then the result panel.
- [ ] Result stars are big, detailed, with rays and sparkles, appear one by one with a rising sound.
- [ ] HUD: round pause button top left, gold score top right, pig counter bar "0/5" below it; the counter grows as pigs die.
- [ ] Reaching the top pig (on the cliff) is possible with a strong pull; the aiming dots stay short.

## Part 13 additions

- [ ] Birds fly slower and are easier to follow; the top pig on the cliff is still reachable with a strong pull.
- [ ] Yellow/blue: tapping works only before the bird touches anything (blocks, terrain, ground); after the first hit a tap does nothing.
- [ ] The trail shows a small white cloud where the ability was used, and three paths after a split.
- [ ] Score and pig counter count up instead of jumping; the result score and best score count up from 0 / the old best.
- [ ] After the last pig dies the level keeps going until about 2.4 s without new points (collapsing blocks still score), then it ends.
- [ ] Win with unused birds: they jump one after another; each jump is smooth (crouch, parabola, landing squash, then small
      hops; no sudden jump from the ground). At the top of the jump a "+10000" pops out of the bird, waits a moment, flies
      in an arc to the score, and only then does the score count up (and swell a little) with a rising sound. The result
      panel appears about a second after the last number; the stars reflect the final score.
- [ ] Win while the next bird already sits on the slingshot: that bird jumps on the slingshot (the band goes slack),
      it does not move to another place.
- [ ] Win with no unused bird: no numbers, the camera shows the bench, the result panel appears after about 2.6 s.
- [ ] No button pulses or changes size; round icon buttons darken while pressed and fire on release; no words on buttons.
- [ ] Lobby shows only the "Continue campaign" button; the level tower scrolls, level 1 is the golden door (blue with stars
      after completing it), the other doors are boarded with padlocks and shake when tapped; back (round orange) returns.
- [ ] Split screen / floating window cannot be used (the app is not resizable).

## Part 16 additions

- [ ] Lobby: the artwork fills the screen, sparkles drift upwards, the Continue button slides in, floats and glows
      now and then; the gear button (top right) pops in. After a finished level the best score pill shows at the
      bottom left. Nothing is hidden under a notch.
- [ ] Gear button: opens the settings panel (the gear turns a quarter); the X button, a tap outside the panel and the
      Android Back button close it. Back with the panel closed leaves the game.
- [ ] Music switch: off fades the lobby music out; on fades it back in; the level tower and the level stay silent
      while it is off. The state survives leaving the lobby and restarting the app.
- [ ] Sound FX switch: off silences clicks, bird voices, hits and breaks (also in a level); on plays a click.
      The state survives restarting the app. Music and sound FX are independent of each other.
- [ ] The middle star on the tower doors is gold; the first is bronze and the third silver (the result panel changed in Part 17).
- [ ] The pig bar shows `defeated/total` inside the bar, counting up with the bar; it turns gold at `5/5`.

- [ ] Pause panel: the music and sound buttons next to Resume toggle the same settings as the lobby (dimmed with a red
      slash when off); music fades out/in while paused; the state matches in the lobby afterwards.

## Part 17 additions

- [ ] Lobby settings panel: each row (music, sound FX) has a round icon button, a label, a percent and a volume slider.
      Dragging the music slider to 50% plays the music at half its level; 0% is silent, 100% is the full level.
- [ ] Tapping a row's icon mutes it completely (icon dimmed with a red slash, slider grey, percent kept); tapping again
      brings it back at the same volume. Music fades out / in; sound FX stop / return.
- [ ] Moving a slider while the row is muted unmutes it. Unmuting with the icon while the volume is 0% sets it to 50%.
- [ ] Releasing the sound slider plays a click at the new volume (it is silent at 0%).
- [ ] The slider follows the finger while dragging, also when the finger leaves the slider vertically; a tap on the track
      jumps there; the percent next to it always matches the knob.
- [ ] Both volumes and both mute states survive leaving the lobby and restarting the app.
- [ ] Pause panel (in a level): Resume on top, then the music row and the sound row (icon button + slider + percent),
      then Restart and Menu. Everything fits the screen and nothing overlaps. Sliders and icons behave exactly like in the
      lobby, and the lobby shows the same values afterwards. A finger on a slider never fires the slingshot.
- [ ] Changing the music volume while paused changes the music at once (it stays ducked while paused, the new volume
      applies on top of it).
- [ ] Score bar (in a level): the three stars are bronze (first), silver (second) and gold (third) and the bar color
      follows the last star reached (bronze, silver, gold).
- [ ] Result panel: positions are bronze (left), gold (middle, bigger), silver (right). After the score is counted the
      stars appear one by one in the order bronze, silver, gold (with 1 star only bronze, with 2 bronze and silver); the
      rays behind each match its color. Tower doors are unchanged (bronze, gold, silver).
- [ ] Result panel: the final score is clearly bigger than before and fits (no overlap with the stars, the BEST line or
      the buttons); "NEW BEST!" blinks to the right of the BEST line.
- [ ] Each earned star lands with a flash, then has a colored halo (bronze, gold, silver), two crossing layers of rays
      turning in opposite directions and twinkling little stars. A glow never covers a neighbouring star.

## Part 18 additions

- [ ] App icon: the launcher shows the red bird on the blue rays (not the old icon) at every size.
- [ ] Splash screen and the lobby behind it show the supplied artwork (the "Angry Birds" scene) with no stretching; the
      level background inside a level is unchanged.
- [ ] Every text and number is in the ROGENZ brush font: splash "LOADING...", lobby buttons and settings (50%, 100%),
      level numbers on the tower doors, HUD score and "2/5", "+10000" popups, pause panel, result panel
      ("LEVEL CLEARED!", "NEW BEST!", BEST line). Nothing is cut off, overlapping or missing (no empty gaps for % ! + /).
- [ ] The pigs show the face of img1 (also the small pig icon of the HUD); the damage bruises still show on top.
- [ ] The three birds show faces: red = img2, yellow = img3 (dash still works), blue = img4 (split still works, the two
      copies show the same face). The faces turn with the bird in flight and look right waiting on the bench, on the
      slingshot and when cheering after a win. The small bird rolling on the splash loading bar shows the red face.
