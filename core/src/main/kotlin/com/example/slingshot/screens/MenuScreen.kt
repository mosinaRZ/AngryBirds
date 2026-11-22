package com.example.slingshot.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.example.slingshot.AngryBirds
import com.example.slingshot.audio.MusicPlayer
import com.example.slingshot.render.Backdrop
import com.example.slingshot.render.Ease
import com.example.slingshot.render.VolumeSlider
import kotlin.math.min

/**
 * The lobby (shown after the splash screen, before the level tower).
 *
 * - The title artwork (sp_bg.png) fills the screen and breathes very slightly; a soft shade at the bottom and top
 *   keeps the controls readable, and a few sparkles drift upwards.
 * - Bottom right: the "Continue campaign" button (it floats gently and glows from time to time).
 * - Bottom left: the best score (only after the first finished level).
 * - Top right: a round settings button. It opens a settings panel with two rows: music and sound effects
 *   (button clicks and all game sounds). Each row has a round icon button that mutes / unmutes it completely
 *   ([AngryBirds.setMusicOn], [AngryBirds.setSoundOn]) and a volume slider with the percent next to it
 *   ([AngryBirds.setMusicVolume], [AngryBirds.setSoundVolume]). The choices are saved.
 *
 * Every button only darkens while pressed and fires when the finger is lifted on it. The Android Back button closes
 * the settings panel, or leaves the game when the panel is closed. Fades in from black; the controls slide in.
 */
class MenuScreen(private val game: AngryBirds) : ScreenAdapter() {

    private enum class Target { NONE, PLAY, GEAR, CLOSE, MUSIC, SOUND, MUSIC_SLIDER, SOUND_SLIDER, OUTSIDE }

    private val viewport = ExtendViewport(UI_WIDTH, UI_HEIGHT)
    private val touch = Vector2()

    // Hit areas, in UI units (set in resize).
    private val playRect = Rectangle()
    private val gearRect = Rectangle()
    private val panelRect = Rectangle()
    private val closeRect = Rectangle()
    private val musicRow = Rectangle()
    private val soundRow = Rectangle()
    private val musicIcon = Rectangle()
    private val soundIcon = Rectangle()
    private val musicSlider = Rectangle()
    private val soundSlider = Rectangle()
    private val bestPill = Rectangle()

    private val playLayout = GlyphLayout()
    private val bestLayout = GlyphLayout()
    private val titleLayout = GlyphLayout()
    private val musicLayout = GlyphLayout()
    private val soundLayout = GlyphLayout()
    private val musicPercentLayout = GlyphLayout()
    private val soundPercentLayout = GlyphLayout()
    private var shownMusicPercent = -1
    private var shownSoundPercent = -1

    // The slider track (the same x range in both rows), in UI units (set in resize).
    private var trackX = 0f
    private var trackWidth = 0f

    private val shadeDark = Color(0f, 0f, 0f, 0.55f)
    private val shadeClear = Color(0f, 0f, 0f, 0f)
    private val shadeTop = Color(0f, 0f, 0f, 0.22f)

    // Sparkles drifting upwards: every value is a fixed random number; the position is computed from the time.
    private val sparkX = FloatArray(SPARKLES) { MathUtils.random() }
    private val sparkY = FloatArray(SPARKLES) { MathUtils.random() }
    private val sparkSpeed = FloatArray(SPARKLES) { MathUtils.random(14f, 34f) }
    private val sparkSize = FloatArray(SPARKLES) { MathUtils.random(14f, 32f) }

    private var time = 0f
    private var pressed = Target.NONE
    private var panelOpen = false
    private var panelT = 0f
    private var hasBest = false

    override fun show() {
        game.font.data.setScale(game.uiFontScale * LABEL_FACTOR)
        playLayout.setText(game.font, "CONTINUE CAMPAIGN")
        game.font.data.setScale(game.uiFontScale * SMALL_FACTOR)
        musicLayout.setText(game.font, "MUSIC")
        soundLayout.setText(game.font, "SOUND FX")
        val best = game.progress.bestScore
        hasBest = best > 0
        bestLayout.setText(game.font, "BEST  $best")
        game.font.data.setScale(game.uiFontScale * TITLE_FACTOR)
        titleLayout.setText(game.font, "SETTINGS")
        game.resetFontScale()

        game.music.play(MusicPlayer.Track.MENU)
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode != Input.Keys.BACK && keycode != Input.Keys.ESCAPE) return false
                if (panelOpen) {
                    panelOpen = false
                } else {
                    Gdx.app.exit()
                }
                return true
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (pointer != 0) return false
                viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
                pressed = hit(touch.x, touch.y)
                if (pressed == Target.MUSIC_SLIDER || pressed == Target.SOUND_SLIDER) setVolume(pressed, touch.x)
                return pressed != Target.NONE
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (pointer != 0) return false
                if (pressed != Target.MUSIC_SLIDER && pressed != Target.SOUND_SLIDER) return false
                viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
                setVolume(pressed, touch.x)
                return true
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (pointer != 0 || pressed == Target.NONE) return false
                val was = pressed
                pressed = Target.NONE
                if (was == Target.MUSIC_SLIDER || was == Target.SOUND_SLIDER) {
                    // The volume is already set. A click at the new sound volume lets the player hear the level.
                    if (was == Target.SOUND_SLIDER) game.sounds.onClick()
                    return true
                }
                viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
                if (hit(touch.x, touch.y) == was) activate(was)
                return true
            }
        }
    }

    override fun hide() {
        Gdx.input.setCatchKey(Input.Keys.BACK, false)
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        val pixelW = maxOf(1, Gdx.graphics.width)
        val pixelH = maxOf(1, Gdx.graphics.height)
        val safeLeft = Gdx.graphics.safeInsetLeft * w / pixelW
        val safeRight = Gdx.graphics.safeInsetRight * w / pixelW
        val safeTop = Gdx.graphics.safeInsetTop * h / pixelH

        playRect.set(w - PLAY_WIDTH - 70f - safeRight, 52f, PLAY_WIDTH, PLAY_HEIGHT)
        gearRect.set(w - GEAR_SIZE - MARGIN - safeRight, h - GEAR_SIZE - MARGIN - safeTop, GEAR_SIZE, GEAR_SIZE)

        val cx = w / 2f
        val cy = h / 2f
        panelRect.set(cx - PANEL_WIDTH / 2f, cy - PANEL_HEIGHT / 2f, PANEL_WIDTH, PANEL_HEIGHT)
        closeRect.set(
            panelRect.x + panelRect.width - CLOSE_SIZE + 14f, panelRect.y + panelRect.height - CLOSE_SIZE + 14f,
            CLOSE_SIZE, CLOSE_SIZE
        )
        musicRow.set(cx - ROW_WIDTH / 2f, cy + 30f - ROW_HEIGHT / 2f, ROW_WIDTH, ROW_HEIGHT)
        soundRow.set(cx - ROW_WIDTH / 2f, cy - 95f - ROW_HEIGHT / 2f, ROW_WIDTH, ROW_HEIGHT)
        // Each row: [icon button] then, to its right, the label and percent on top and the slider below them.
        trackX = musicRow.x + CONTENT_LEFT
        trackWidth = ROW_WIDTH - CONTENT_LEFT - CONTENT_RIGHT
        for (row in arrayOf(musicRow, soundRow)) {
            val icon = if (row === musicRow) musicIcon else soundIcon
            val slider = if (row === musicRow) musicSlider else soundSlider
            icon.set(row.x + 18f, row.y + (row.height - ROW_ICON) / 2f, ROW_ICON, ROW_ICON)
            slider.set(
                trackX - VolumeSlider.KNOB_RADIUS, row.y + SLIDER_Y - SLIDER_TOUCH_HEIGHT / 2f,
                trackWidth + 2f * VolumeSlider.KNOB_RADIUS, SLIDER_TOUCH_HEIGHT
            )
        }

        val pillWidth = 26f + BEST_STAR + 14f + bestLayout.width + 40f
        bestPill.set(70f + safeLeft, 52f + (PLAY_HEIGHT - BEST_HEIGHT) / 2f, pillWidth, BEST_HEIGHT)
    }

    // ------------------------------------------------------------------ touch

    private fun hit(x: Float, y: Float): Target {
        if (panelOpen) {
            if (panelT < 0.5f) return Target.NONE
            return when {
                closeRect.contains(x, y) -> Target.CLOSE
                musicIcon.contains(x, y) -> Target.MUSIC
                soundIcon.contains(x, y) -> Target.SOUND
                musicSlider.contains(x, y) -> Target.MUSIC_SLIDER
                soundSlider.contains(x, y) -> Target.SOUND_SLIDER
                panelRect.contains(x, y) -> Target.NONE
                else -> Target.OUTSIDE
            }
        }
        return when {
            gearRect.contains(x, y) -> Target.GEAR
            playRect.contains(x, y) -> Target.PLAY
            else -> Target.NONE
        }
    }

    private fun activate(target: Target) {
        when (target) {
            Target.PLAY -> {
                game.sounds.onClick()
                game.setScreen(LevelSelectScreen(game))
            }
            Target.GEAR -> {
                game.sounds.onClick()
                panelOpen = true
            }
            Target.CLOSE, Target.OUTSIDE -> {
                game.sounds.onClick()
                panelOpen = false
            }
            Target.MUSIC -> {
                game.sounds.onClick()
                game.setMusicOn(!game.settings.musicOn)
            }
            Target.SOUND -> {
                // Turning the sound off is silent (the click would be cut off); turning it on plays the click.
                val on = !game.settings.soundOn
                game.setSoundOn(on)
                if (on) game.sounds.onClick()
            }
            Target.MUSIC_SLIDER, Target.SOUND_SLIDER, Target.NONE -> Unit // sliders act while the finger moves
        }
    }

    /** Sets the volume of the slider [target] from the touch position [x] (UI units). */
    private fun setVolume(target: Target, x: Float) {
        val volume = VolumeSlider.valueAt(x, trackX, trackWidth)
        if (target == Target.MUSIC_SLIDER) game.setMusicVolume(volume) else game.setSoundVolume(volume)
    }

    // ------------------------------------------------------------------ frame

    override fun render(delta: Float) {
        val dt = min(delta, 0.05f)
        time += dt
        val panelGoal = if (panelOpen) 1f else 0f
        panelT += MathUtils.clamp(panelGoal - panelT, -dt / PANEL_TIME, dt / PANEL_TIME)

        ScreenUtils.clear(0.53f, 0.81f, 0.92f, 1f)
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        val assets = game.assets
        val batch = game.batch
        val shapes = game.shapes
        val projection = viewport.camera.combined

        // 1) artwork (breathes very slightly)
        batch.projectionMatrix = projection
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        Backdrop.draw(batch, assets.splash, w / 2f, w, h, 1f + 0.012f * MathUtils.sin(time * 0.8f))
        batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // 2) shade at the bottom and top, and the best-score pill
        val bestIn = Ease.smooth((time - 0.55f) / 0.5f)
        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.rect(0f, 0f, w, h * 0.42f, shadeDark, shadeDark, shadeClear, shadeClear)
        shapes.rect(0f, h * 0.8f, w, h * 0.2f, shadeClear, shadeClear, shadeTop, shadeTop)
        if (hasBest) {
            shapes.setColor(1f, 1f, 1f, 0.9f * bestIn)
            pill(shapes, bestPill.x - 3f, bestPill.y - 3f, bestPill.width + 6f, bestPill.height + 6f)
            shapes.setColor(0.10f, 0.13f, 0.27f, 0.92f * bestIn)
            pill(shapes, bestPill.x, bestPill.y, bestPill.width, bestPill.height)
        }
        shapes.end()

        // 3) sparkles, best score, play button, settings button
        batch.begin()
        drawSparkles(w, h)
        if (hasBest) drawBest(bestIn)
        drawPlayButton()
        drawGear()
        batch.end()

        // 4) settings panel
        if (panelT > 0.002f) drawSettings(w, h)

        // 5) fade in from black
        val fade = 1f - Ease.smooth(time / FADE_TIME)
        if (fade > 0.002f) {
            shapes.projectionMatrix = projection
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.setColor(0f, 0f, 0f, fade)
            shapes.rect(0f, 0f, w, h)
            shapes.end()
        }
        Gdx.gl.glDisable(GL20.GL_BLEND)
        batch.setColor(1f, 1f, 1f, 1f)
        game.resetFontScale()
    }

    private fun drawSparkles(w: Float, h: Float) {
        val batch = game.batch
        val span = h + 80f
        val appear = Ease.smooth((time - 0.3f) / 0.8f)
        for (i in 0 until SPARKLES) {
            val x = sparkX[i] * w + MathUtils.sin(time * 0.5f + i) * 16f
            val y = (sparkY[i] * span + time * sparkSpeed[i]) % span - 40f
            val twinkle = MathUtils.sin(time * 1.6f + i * 1.7f)
            val alpha = (0.12f + 0.42f * twinkle * twinkle) * appear
            val size = sparkSize[i]
            batch.setColor(1f, 1f, 1f, alpha)
            batch.draw(game.assets.sparkle, x - size / 2f, y - size / 2f, size, size)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawBest(alpha: Float) {
        val batch = game.batch
        val cy = bestPill.y + bestPill.height / 2f
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(game.assets.starOn, bestPill.x + 26f, cy - BEST_STAR / 2f, BEST_STAR, BEST_STAR)
        batch.setColor(1f, 1f, 1f, 1f)
        drawText(
            bestLayout, SMALL_FACTOR, bestPill.x + 26f + BEST_STAR + 14f, cy + bestLayout.height / 2f,
            1f, 0.84f, 0.26f, alpha
        )
    }

    private fun drawPlayButton() {
        val batch = game.batch
        val enter = Ease.outCubic((time - 0.35f) / 0.6f)
        val alpha = Ease.smooth((time - 0.35f) / 0.4f)
        val bob = MathUtils.sin(time * 1.9f) * 3f
        val isPressed = pressed == Target.PLAY
        val shade = if (isPressed) 0.75f else 1f
        val dy = (if (isPressed) -3f else 0f) + bob - (1f - enter) * 150f
        val r = playRect

        batch.setColor(shade, shade, shade, alpha)
        batch.draw(game.assets.btnPill, r.x, r.y + dy, r.width, r.height)

        // A soft glow that swells and fades once per period.
        val cycle = (time % GLOW_PERIOD) / GLOW_PERIOD
        val glow = if (cycle < GLOW_SHARE) MathUtils.sin(MathUtils.PI * cycle / GLOW_SHARE) else 0f
        if (glow > 0.01f && !isPressed) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            batch.setColor(1f, 1f, 1f, 0.28f * glow * alpha)
            batch.draw(game.assets.btnPill, r.x, r.y + dy, r.width, r.height)
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }

        batch.setColor(shade, shade, shade, alpha)
        val icon = r.height * 0.62f
        batch.draw(game.assets.iconPlay, r.x + 38f, r.y + (r.height - icon) / 2f + dy + 4f, icon, icon)
        batch.setColor(1f, 1f, 1f, 1f)
        drawText(
            playLayout, LABEL_FACTOR, r.x + 38f + icon + 20f, r.y + (r.height + playLayout.height) / 2f + dy + 6f,
            1f, 1f, 1f, alpha
        )
    }

    private fun drawGear() {
        val enter = Ease.outBack((time - 0.6f) / 0.5f).coerceAtLeast(0f)
        val shade = if (pressed == Target.GEAR) 0.72f else 1f
        val r = gearRect
        val batch = game.batch
        batch.setColor(shade, shade, shade, 1f)
        drawCentered(game.assets.btnBlue, r.x + r.width / 2f, r.y + r.height / 2f, r.width, r.height, enter, 0f)
        val icon = r.width * ICON_SHARE
        // The gear makes a quarter turn while the panel opens.
        drawCentered(
            game.assets.iconGear, r.x + r.width / 2f, r.y + r.height / 2f + 2f, icon, icon, enter,
            -Ease.smooth(panelT) * 90f
        )
        batch.setColor(1f, 1f, 1f, 1f)
    }

    // ------------------------------------------------------------------ settings panel

    private fun drawSettings(w: Float, h: Float) {
        val batch = game.batch
        val shapes = game.shapes
        val assets = game.assets
        val projection = viewport.camera.combined
        val t = Ease.smooth(panelT)
        val slide = (1f - Ease.outCubic(panelT)) * 36f
        val p = panelRect

        // dim
        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, 0.6f * t)
        shapes.rect(0f, 0f, w, h)
        shapes.end()

        // panel and title
        batch.begin()
        batch.setColor(1f, 1f, 1f, t)
        batch.draw(assets.panel, p.x, p.y - slide, p.width, p.height)
        batch.setColor(1f, 1f, 1f, 1f)
        drawText(
            titleLayout, TITLE_FACTOR, p.x + (p.width - titleLayout.width) / 2f, p.y + p.height - 34f - slide,
            1f, 1f, 1f, t
        )
        batch.end()

        // rows and sliders
        updatePercentLayouts()
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawRowBack(shapes, musicRow, slide, t)
        drawRowBack(shapes, soundRow, slide, t)
        drawSlider(shapes, musicRow, slide, t, game.settings.musicVolume, game.settings.musicOn, pressed == Target.MUSIC_SLIDER)
        drawSlider(shapes, soundRow, slide, t, game.settings.soundVolume, game.settings.soundOn, pressed == Target.SOUND_SLIDER)
        shapes.end()

        // icons, labels, percents and the close button
        batch.begin()
        drawRowContent(
            musicRow, musicIcon, slide, t, assets.iconMusic, musicLayout, musicPercentLayout,
            game.settings.musicOn, game.settings.musicVolume, pressed == Target.MUSIC
        )
        drawRowContent(
            soundRow, soundIcon, slide, t, assets.iconSound, soundLayout, soundPercentLayout,
            game.settings.soundOn, game.settings.soundVolume, pressed == Target.SOUND
        )
        val shade = if (pressed == Target.CLOSE) 0.72f else 1f
        val c = closeRect
        batch.setColor(shade, shade, shade, t)
        batch.draw(assets.btnOrange, c.x, c.y - slide, c.width, c.height)
        val icon = c.width * ICON_SHARE
        batch.draw(assets.iconClose, c.x + (c.width - icon) / 2f, c.y + (c.height - icon) / 2f - slide + 2f, icon, icon)
        batch.setColor(1f, 1f, 1f, 1f)
        batch.end()
    }

    /** Rebuilds the "50%" texts when a volume changed. */
    private fun updatePercentLayouts() {
        val music = VolumeSlider.percent(game.settings.musicVolume)
        val sound = VolumeSlider.percent(game.settings.soundVolume)
        if (music == shownMusicPercent && sound == shownSoundPercent) return
        game.font.data.setScale(game.uiFontScale * SMALL_FACTOR)
        if (music != shownMusicPercent) musicPercentLayout.setText(game.font, "$music%")
        if (sound != shownSoundPercent) soundPercentLayout.setText(game.font, "$sound%")
        game.resetFontScale()
        shownMusicPercent = music
        shownSoundPercent = sound
    }

    private fun drawRowBack(shapes: ShapeRenderer, row: Rectangle, slide: Float, alpha: Float) {
        shapes.setColor(1f, 1f, 1f, 0.08f * alpha)
        pill(shapes, row.x, row.y - slide, row.width, row.height)
    }

    /** The volume slider of [row]; it is drawn grey while the row is muted. */
    private fun drawSlider(
        shapes: ShapeRenderer, row: Rectangle, slide: Float, alpha: Float, volume: Float, on: Boolean, isPressed: Boolean
    ) {
        VolumeSlider.draw(shapes, trackX, row.y + SLIDER_Y - slide, trackWidth, volume, on, isPressed, alpha)
    }

    /**
     * The round icon button of a row (dimmed with a red slash while muted or at volume 0), the label and the percent.
     * The icon button mutes / unmutes the row completely.
     */
    private fun drawRowContent(
        row: Rectangle, iconRect: Rectangle, slide: Float, alpha: Float, icon: Texture, label: GlyphLayout,
        percent: GlyphLayout, on: Boolean, volume: Float, isPressed: Boolean
    ) {
        val batch = game.batch
        val audible = on && volume > 0f
        val cy = iconRect.y + iconRect.height / 2f - slide
        val size = iconRect.width
        val x = iconRect.x
        val tint = (if (audible) 1f else 0.55f) * (if (isPressed) 0.72f else 1f)
        batch.setColor(tint, tint, tint, alpha)
        batch.draw(game.assets.btnBlue, x, cy - size / 2f, size, size)
        val glyph = size * ICON_SHARE
        batch.draw(icon, x + (size - glyph) / 2f, cy - glyph / 2f + 2f, glyph, glyph)
        if (!audible) {
            batch.setColor(1f, 1f, 1f, alpha)
            val slash = size * 0.78f
            batch.draw(game.assets.iconSlash, x + (size - slash) / 2f, cy - slash / 2f, slash, slash)
        }
        batch.setColor(1f, 1f, 1f, 1f)
        val textAlpha = alpha * (if (on) 1f else 0.7f)
        val textTop = row.y + row.height - LABEL_TOP_GAP - slide
        drawText(label, SMALL_FACTOR, trackX, textTop, 1f, 1f, 1f, textAlpha)
        drawText(percent, SMALL_FACTOR, trackX + trackWidth - percent.width, textTop, 1f, 0.84f, 0.26f, textAlpha)
    }

    // ------------------------------------------------------------------ helpers

    /** A rounded bar (rectangle with half-circles at both ends). */
    private fun pill(shapes: ShapeRenderer, x: Float, y: Float, w: Float, h: Float) {
        val r = h / 2f
        shapes.rect(x + r, y, w - 2f * r, h)
        shapes.circle(x + r, y + r, r, 24)
        shapes.circle(x + w - r, y + r, r, 24)
    }

    private fun drawCentered(texture: Texture, x: Float, y: Float, w: Float, h: Float, scale: Float, degrees: Float) {
        game.batch.draw(
            texture, x - w / 2f, y - h / 2f, w / 2f, h / 2f, w, h, scale, scale, degrees,
            0, 0, texture.width, texture.height, false, false
        )
    }

    /** Draws a layout made at [factor] times the normal UI size. [y] is the top of the text. */
    private fun drawText(
        layout: GlyphLayout, factor: Float, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float
    ) {
        val font = game.font
        font.data.setScale(game.uiFontScale * factor)
        font.setColor(r, g, b, a)
        font.draw(game.batch, layout, x, y)
        game.resetFontScale()
    }

    private companion object {
        const val UI_WIDTH = 1280f
        const val UI_HEIGHT = 720f
        const val MARGIN = 34f

        const val PLAY_WIDTH = 560f
        const val PLAY_HEIGHT = 130f
        const val GEAR_SIZE = 96f
        const val ICON_SHARE = 0.62f

        const val LABEL_FACTOR = 0.62f
        const val SMALL_FACTOR = 0.55f
        const val TITLE_FACTOR = 0.8f
        const val FADE_TIME = 0.45f
        const val GLOW_PERIOD = 4.2f
        const val GLOW_SHARE = 0.4f
        const val SPARKLES = 16

        const val BEST_HEIGHT = 76f
        const val BEST_STAR = 54f

        const val PANEL_WIDTH = 700f
        const val PANEL_HEIGHT = 420f
        const val PANEL_TIME = 0.25f
        const val CLOSE_SIZE = 84f
        const val ROW_WIDTH = 580f
        const val ROW_HEIGHT = 104f
        const val ROW_ICON = 80f

        /** Where the label / slider area of a row starts (right of the icon) and how far it stays from the right edge. */
        const val CONTENT_LEFT = 122f
        const val CONTENT_RIGHT = 36f

        /** Height of the slider's center above the row's bottom, the touch area around it, and the label's gap to the row top. */
        const val SLIDER_Y = 34f
        const val SLIDER_TOUCH_HEIGHT = 64f
        const val LABEL_TOP_GAP = 12f
    }
}
