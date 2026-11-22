# Assets

Everything lives in `android/assets/`. All files are optional: a missing file is replaced by a
generated placeholder and a warning is written to Logcat (tags `Assets`, `SoundEffects`).

The bundled artwork and sounds are **original, procedurally generated** by the scripts in
`tools/` (see the end of this file). No file comes from any commercial game. To use your own
artwork or sounds, overwrite a file with the same name; nothing in the code has to change
(except the size constants noted below if your sprite has different proportions).

## Images

| File | Purpose | Notes |
|---|---|---|
| `bg.png` | Level background (pastel teal rock mesas, soft hills) | 2048x1024. Drawn to cover the whole screen, bottom aligned, with a little parallax while the camera zooms. |
| `sp_bg.png` | Splash and lobby artwork (supplied by you, 2340x1080) | Drawn to cover the whole screen, bottom aligned, on the splash screen and the lobby (the screen after it). Has the title baked in. To change that background, overwrite this file (the level background is `bg.png`, a different file). |
| `terrain_l1.png` | The rock terrain of level 1 (cliff and mound) | 870x451 = 13.6 x 7.05 m at (9.8, 1.95); must match the polygons in `Levels.kt` (generated together by the tools). |
| `bench.png` | The wooden bench the slingshot stands on | 512x128, drawn 1.97 x 0.53 m. |
| `tnt.png` | TNT crate (not tiled) | 128x128. |
| `pig_icon.png`, `star_glow.png`, `sparkle.png` | HUD pig icon (the pig face in a green ring, made by `tools/make_faces.py`), rays behind earned stars, star sparkles | |
| `ground.png` | Ground strip: lime grass on blue-grey rock with dark leaves | 1024x512, tiled every 4 m, 1.95 m high; must tile horizontally. |
| `bird.png`, `bird_yellow.png`, `bird_blue.png` | Red, yellow (dash) and blue (split) birds with a face each (`img2`, `img3`, `img4`) | 512x512, transparent, made by `tools/make_faces.py`: a round body in the bird's color with the face on it, plus the tail and tuft of the original bird. The body circle is **156/256 = 61 %** of the width and centered (as before). The sprite is drawn at physics diameter x 640/380 (`BIRD_SPRITE_RATIO` in `GameRenderer`); radii are in `BirdType`. The bodies without faces are kept in `tools/source_art/bird*_orig.png`. |
| `enemy_body.png` | Enemy body | Square, transparent. The body circle is **380/512 = 74 %** of the width and centered (ears stick out). Constant: `PIG_BODY_SCALE`. |
| `enemy_head.png` | **The pig face** (`img1`, cut around the face by `tools/make_faces.py`, 512x512) | Optional. Cropped to a circle at load time, drawn at 90 % of the physical enemy size on top of the body. When it is absent the bundled `pig_face.png` is used. |
| `pig_face.png` | Default pig face | Used only when there is no `enemy_head.png`. |
| `enemy_dmg1.png`, `enemy_dmg2.png` | Bruises / bandage overlays | Same square as the face; shown below 70 % and 35 % health. |
| `wood.png`, `stone.png`, `glass.png` | Block textures (orange wood, grey stone, ice-blue glass) | Power-of-two squares (256x256) that **tile**: one repetition covers 0.5 m, grain runs along the long side. A non power-of-two image is stretched instead of tiled. |
| `crack_1.png`, `crack_2.png` | Damage overlays on blocks | Transparent, tileable like the textures. Shown below 67 % and 34 % health. |
| `slingshot.png`, `slingshot_back.png` | Slingshot front (trunk + front prong) and back prong | 256x1024. Drawn 0.8 m wide, height follows the aspect ratio. The fork tips must be where the band is attached (`SlingshotConfig.BACK_BAND_*`, `FRONT_BAND_*`). |
| `panel.png` | Dark glass panel (pause and result screens) | 512x384, stretched. |
| `btn_orange.png`, `btn_blue.png`, `btn_green.png` | Round button bases | 256x256. An icon is drawn on top; buttons never change size, they only darken while pressed. |
| `btn_pill.png` | Wide pill button of the lobby | 512x160. |
| `icon_play.png`, `icon_pause.png`, `icon_restart.png`, `icon_home.png`, `icon_back.png` | White icons for the buttons (no text on buttons) | 128x128. |
| `icon_gear.png`, `icon_music.png`, `icon_sound.png`, `icon_close.png`, `icon_slash.png` | Settings button and panel: gear, music note, speaker, X, and the red slash laid over a muted icon | 128x128. Made by `tools/art_ui.py`. |
| `mark_dash.png`, `mark_split.png` | Badges on the flight trail where a yellow bird dashed / a blue bird split | 128x128. |
| `tower_bg.png` | Sky behind the level tower | 2048x1024. |
| `tower_floor_0..3.png` | The four themed floors of the tower | 1024x320 each. |
| `door_open.png`, `door_gold.png`, `door_locked.png`, `tag_red.png`, `tag_purple.png` | Doors (completed / next / locked) and number banners | 256x384 and 192x120. |
| `dot.png` | Dots of the aiming path and the flight trail | 64x64, white with a soft dark ring. |
| `puff.png` | Smoke when an enemy pops | Soft white circle. |
| `star_on.png`, `star_off.png` | Big detailed result stars (`star_on` is the gold one) | 256x256. |
| `star_bronze.png`, `star_silver.png`, `star_glow_bronze.png`, `star_glow_silver.png` | Bronze and silver stars and their rays | 256x256. Star order: bronze, gold (middle), silver (`Assets.starOnFor`). |
| `font.fnt` + `font.png` | UI font for all text and numbers (BMFont format, white glyphs with a dark outline) | Falls back to libGDX's built-in font when missing. Font: ROGENZ **demo** by Nirfonic (`tools/fonts/ROGENZ-Demo.ttf`): the demo license allows personal, non-commercial use only; buy the full version before any commercial release. The demo lacks ! % + - / : =, which `tools/generate_font.py` builds from the strokes of the font itself. |
| `android/res/mipmap-*/ic_launcher.png` | App icon (from `icon.jpg`) | 48 to 192 px, made by `tools/make_launcher_icon.py`. |

Power-of-two images get mip-maps, so sprites stay smooth when drawn much smaller than the file.
Very large face images (e.g. 4000x4000) are decoded fully before cropping; keep them under about 2048 px.

## Sounds

`.ogg` (or `.wav`), short files. Each file is optional; a missing one only disables that sound.

| File | Played when |
|---|---|
| `stretch` | the bird is grabbed and the band is pulled (**your** `angry-birds-slingshot-sound-effect`) |
| `bird_launch` | a bird leaves the slingshot (**your** `angry-birds-chuck`) |
| `win` | the birds win (**your** `angry-birds-plush-yeah-sfx`) |
| `lose` | the pigs win (**your** `angry-birds-level-failed-2`) |
| `big_shot` | a shot that earned 7000+ points ends (**your** `angry-birds-laugh`) |
| `explosion` | a TNT crate explodes |
| `hop` | the next bird jumps onto the slingshot |
| `hit_wood`, `hit_stone`, `hit_glass` | a hard collision involving that material (glass beats stone beats wood) |
| `hit_pig` | a hard collision involving an enemy |
| `thud` | the bird hits the ground |
| `break_wood`, `break_stone`, `break_glass` | a block of that material is destroyed |
| `pig_die` | an enemy is destroyed |
| `star` | a star appears on the result panel (pitch rises for each star) |
| `click` | a button is pressed |
| `locked` | a locked level is tapped |
| `bird_red_idle`, `bird_yellow_idle`, `bird_blue_idle` | the bird on the slingshot has been left alone for 6 to 10 s |
| `yellow_dash`, `blue_split` | the yellow bird dashes / the blue bird splits |

The files marked **your** are the recordings you supplied. The other sounds (collisions, breaking, pig pop,
idle chirps, dash, split, stars, clicks, explosion) are original synthesized sounds; overwrite any file with the
same name to replace it.

## Music

| File | Played in |
|---|---|
| `music_menu.ogg` | the lobby: main menu and level map (**your** theme song, looping, fades in and out) |
| `music_game.ogg` | during play: outdoor ambience (wind, birdsong, a soft pad), quieter while paused |

`music_game.ogg` is an original generated ambience. `.mp3` and `.wav` files of the same name also work. A missing file means
silence for that track (warning in Logcat, tag `MusicPlayer`).

## Regenerating the bundled files

```
pip install pillow numpy scipy
python3 tools/generate_art.py          # all images   (or: bird pig blocks slingshot ground bg ui)
python3 tools/generate_sounds.py       # the synthesized sounds and music_game (needs ffmpeg with libvorbis); never touches your supplied files
python3 tools/generate_level.py        # writes Levels.kt from the pixel layout measured on the reference screenshot
python3 tools/generate_font.py         # font.fnt + font.png (ROGENZ; needs opencv-python-headless too)
python3 tools/make_faces.py            # enemy_head, pig_icon, bird, bird_yellow, bird_blue from tools/source_art/img1..4.jpg (run it after generate_art.py)
python3 tools/make_launcher_icon.py    # android/res/mipmap-*/ic_launcher.png from tools/source_art/icon.jpg
```
