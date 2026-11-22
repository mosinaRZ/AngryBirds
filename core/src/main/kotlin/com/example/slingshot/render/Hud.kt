package com.example.slingshot.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.example.slingshot.AngryBirds
import com.example.slingshot.systems.ScoreManager
import com.example.slingshot.systems.GameState
import kotlin.math.max

/** The buttons of the in-game HUD. All of them are round icon buttons (no text). */
enum class HudButton { PAUSE, RESUME, RETRY, MENU, MUSIC, SOUND }

/**
 * Everything drawn on top of the world, in a virtual screen that is at least 1280x720
 * (it grows sideways on wider phones; see [resize]).
 *
 * While playing: a round pause button (top left), the score in gold (top right) and below it a bar that counts
 * the defeated pigs (with a "defeated/total" number inside it). Every number counts up to its new value instead of jumping.
 * Paused: a dark panel with three icon buttons: resume (play), restart and menu (home), plus a music row and a sound
 * row (same settings as the lobby): a round icon button that mutes / unmutes completely, and a volume slider with the
 * percent next to it.
 * Result (shown after the level-end sequence): a panel with three big detailed stars that appear one by one,
 * a score and best score that count up, and restart / menu icon buttons.
 *
 * Buttons are round icons that only darken while pressed; nothing pulses or changes size. A button fires when the
 * finger is lifted on the same button ([pressAt] then [releaseAt]).
 *
 * The batch, shape renderer and font belong to the game, so nothing is disposed here. Text layouts are rebuilt only
 * when their text changes. A layout is only valid at the font scale it was made with, so every text is drawn with the
 * same size factor it was laid out with.
 */
class Hud(private val game: AngryBirds) {

    private val pauseRect = Rectangle()
    private val resumeRect = Rectangle()
    private val retryRect = Rectangle()
    private val menuRect = Rectangle()
    private val musicRect = Rectangle()
    private val soundRect = Rectangle()
    private val pauseRetryRect = Rectangle()
    private val pauseMenuRect = Rectangle()
    private val musicSliderRect = Rectangle()
    private val soundSliderRect = Rectangle()

    /** Size of the virtual screen in HUD units; (0,0) is the bottom left corner. */
    var width = BASE_WIDTH
        private set
    var height = BASE_HEIGHT
        private set

    /** Whether the pause button is shown (the screen hides it during the level-end sequence). */
    var pauseVisible = true

    /** True once the result panel has finished its animation; its buttons only react after that. */
    val resultReady: Boolean
        get() = ended && resultTime >= readyAt

    /** True once the pause panel has finished appearing; its buttons only react after that. */
    val pauseReady: Boolean
        get() = pauseTime >= PAUSE_READY_TIME

    /** Center of the score digits in HUD units: the place the bonus numbers fly to. */
    val scoreTargetX: Float
        get() = rightEdge() - scoreLayout.width / 2f
    val scoreTargetY: Float
        get() = height - SCORE_TOP_GAP - scoreLayout.height / 2f

    /** The score swells for a moment (a bonus number has just reached it). */
    fun pulseScore() {
        scorePulseLeft = SCORE_PULSE_TIME
    }

    private val scoreLayout = GlyphLayout()
    private val titleLayout = GlyphLayout()
    private val resultScoreLayout = GlyphLayout()
    private val bestLayout = GlyphLayout()
    private val newBestLayout = GlyphLayout()
    private val pausedLayout = GlyphLayout()
    private val pigCountLayout = GlyphLayout()
    private var pigCountText = ""
    private val musicPercentLayout = GlyphLayout()
    private val soundPercentLayout = GlyphLayout()
    private var shownMusicPercent = -1
    private var shownSoundPercent = -1

    // The slider track (the same x range in both rows), in HUD units (set in resize).
    private var trackX = 0f
    private var trackWidth = 0f

    private val scoreCount = CountUp(rate = 4f, minSpeed = 120f)
    private val pigCount = CountUp(rate = 8f, minSpeed = 4f)
    private val resultScoreCount = CountUp(rate = 3f, minSpeed = 9000f)
    private val bestCount = CountUp(rate = 3f, minSpeed = 4000f)
    private var shownScore = -1
    private var scoreText = "0"
    private var scoreFactor = FACTOR_SCORE
    private var scorePulseLeft = 0f
    private var shownPigsTotal = -1
    private var pigTotalForBar = 1

    /** Time ([fadeTime]) at which the score first reached the star of each rank; negative = not yet. */
    private val starReachedAt = floatArrayOf(-1f, -1f, -1f)
    private var shownResultScore = -1
    private var shownBest = -1

    private var ended = false
    private var countingStarted = false
    private var resultTime = 0f
    private var readyAt = 0f
    private var starsSounded = 0
    private var fadeTime = 0f
    private var pauseTime = 0f
    private var wasPaused = false
    private var bestFinal = 0

    private enum class Mode { PLAYING, PAUSED, RESULT }

    private enum class Slider { MUSIC, SOUND }

    private var mode = Mode.PLAYING
    private var pressed: HudButton? = null

    /** The volume slider the finger is on (it stays set until the finger is lifted). */
    private var dragging: Slider? = null

    // Space taken by display cut-outs, in HUD units.
    private var safeLeft = 0f
    private var safeRight = 0f

    init {
        setText(pausedLayout, "PAUSED", FACTOR_TITLE)
        setText(newBestLayout, "NEW BEST!", FACTOR_NEW_BEST)
    }

    /** Call from the screen's resize with the HUD viewport's world size. */
    fun resize(worldWidth: Float, worldHeight: Float) {
        width = worldWidth
        height = worldHeight
        val pixelWidth = max(1, Gdx.graphics.width)
        safeLeft = Gdx.graphics.safeInsetLeft * worldWidth / pixelWidth
        safeRight = Gdx.graphics.safeInsetRight * worldWidth / pixelWidth

        val cx = width / 2f
        val cy = height / 2f
        pauseRect.set(MARGIN + safeLeft, height - MARGIN - PAUSE_SIZE, PAUSE_SIZE, PAUSE_SIZE)
        // Pause panel (top to bottom): the big resume button, the music row, the sound row, restart and menu.
        resumeRect.set(cx - RESUME_SIZE / 2f, cy + 50f, RESUME_SIZE, RESUME_SIZE)
        // Result panel (and the lower pair of the pause panel).
        retryRect.set(cx - SMALL_BUTTON - 20f, cy - PANEL_HEIGHT / 2f + 40f, SMALL_BUTTON, SMALL_BUTTON)
        menuRect.set(cx + 20f, cy - PANEL_HEIGHT / 2f + 40f, SMALL_BUTTON, SMALL_BUTTON)
        pauseRetryRect.set(cx - PAUSE_SMALL_BUTTON - 20f, cy - PAUSE_PANEL_HEIGHT / 2f + 44f, PAUSE_SMALL_BUTTON, PAUSE_SMALL_BUTTON)
        pauseMenuRect.set(cx + 20f, cy - PAUSE_PANEL_HEIGHT / 2f + 44f, PAUSE_SMALL_BUTTON, PAUSE_SMALL_BUTTON)
        // Each volume row: [round icon button] [slider] [percent].
        val rowX = cx - VOLUME_ROW_WIDTH / 2f
        val musicRowY = cy - 54f
        val soundRowY = musicRowY - VOLUME_ROW_HEIGHT - VOLUME_ROW_GAP
        musicRect.set(rowX, musicRowY + (VOLUME_ROW_HEIGHT - TOGGLE_SIZE) / 2f, TOGGLE_SIZE, TOGGLE_SIZE)
        soundRect.set(rowX, soundRowY + (VOLUME_ROW_HEIGHT - TOGGLE_SIZE) / 2f, TOGGLE_SIZE, TOGGLE_SIZE)
        trackX = rowX + TOGGLE_SIZE + VOLUME_ICON_GAP
        trackWidth = VOLUME_ROW_WIDTH - TOGGLE_SIZE - VOLUME_ICON_GAP - VOLUME_PERCENT_WIDTH
        musicSliderRect.set(
            trackX - VolumeSlider.KNOB_RADIUS, musicRowY, trackWidth + 2f * VolumeSlider.KNOB_RADIUS, VOLUME_ROW_HEIGHT
        )
        soundSliderRect.set(
            trackX - VolumeSlider.KNOB_RADIUS, soundRowY, trackWidth + 2f * VolumeSlider.KNOB_RADIUS, VOLUME_ROW_HEIGHT
        )
    }

    // ------------------------------------------------------------------ touch

    /** A finger went down at ([x], [y]) in HUD units. Returns true when it hit one of the active buttons. */
    fun pressAt(x: Float, y: Float): Boolean {
        if (mode == Mode.PAUSED && pauseReady) {
            val slider = sliderAt(x, y)
            if (slider != null) {
                pressed = null
                dragging = slider
                setVolume(slider, x)
                return true
            }
        }
        pressed = hit(x, y)
        return pressed != null
    }

    /** The finger moved to [x] while it is on a volume slider. Returns true when it was (the slider follows it). */
    fun dragAt(x: Float): Boolean {
        val slider = dragging ?: return false
        if (mode != Mode.PAUSED) {
            dragging = null
            return false
        }
        setVolume(slider, x)
        return true
    }

    /** The finger was lifted at ([x], [y]). Returns the button to activate (the same one that was pressed), or null. */
    fun releaseAt(x: Float, y: Float): HudButton? {
        val slider = dragging
        if (slider != null) {
            dragging = null
            // A click at the new sound volume lets the player hear the level.
            if (slider == Slider.SOUND) game.sounds.onClick()
            return null
        }
        val was = pressed
        pressed = null
        return if (was != null && hit(x, y) == was) was else null
    }

    private fun hit(x: Float, y: Float): HudButton? = when (mode) {
        Mode.PLAYING -> if (pauseVisible && pauseRect.contains(x, y)) HudButton.PAUSE else null
        Mode.PAUSED -> when {
            !pauseReady -> null
            resumeRect.contains(x, y) -> HudButton.RESUME
            pauseRetryRect.contains(x, y) -> HudButton.RETRY
            pauseMenuRect.contains(x, y) -> HudButton.MENU
            musicRect.contains(x, y) -> HudButton.MUSIC
            soundRect.contains(x, y) -> HudButton.SOUND
            else -> null
        }
        Mode.RESULT -> when {
            !resultReady -> null
            retryRect.contains(x, y) -> HudButton.RETRY
            menuRect.contains(x, y) -> HudButton.MENU
            else -> null
        }
    }

    private fun sliderAt(x: Float, y: Float): Slider? = when {
        musicSliderRect.contains(x, y) -> Slider.MUSIC
        soundSliderRect.contains(x, y) -> Slider.SOUND
        else -> null
    }

    /** Sets the volume of [slider] from the touch position [x] (HUD units). */
    private fun setVolume(slider: Slider, x: Float) {
        val volume = VolumeSlider.valueAt(x, trackX, trackWidth)
        if (slider == Slider.MUSIC) game.setMusicVolume(volume) else game.setSoundVolume(volume)
    }

    // ------------------------------------------------------------------ frame

    /**
     * Draws one frame. [state] must stay PLAYING during the level-end sequence (the score keeps counting and the
     * HUD stays visible) and change to WON / LOST when the result panel should appear.
     */
    fun render(
        camera: Camera,
        delta: Float,
        score: Int,
        state: GameState,
        stars: Int,
        bestScore: Int,
        previousBest: Int,
        newBest: Boolean,
        paused: Boolean,
        pigsLeft: Int,
        pigsTotal: Int
    ) {
        val isOver = state != GameState.PLAYING
        if (paused) {
            if (!wasPaused) pauseTime = 0f
            pauseTime += delta
        } else {
            pauseTime = 0f
        }
        wasPaused = paused
        if (isOver && !ended) startResult(state, stars, bestScore, previousBest)
        if (!isOver) {
            ended = false
            resultTime = 0f
        } else {
            resultTime += delta
        }
        fadeTime += delta
        mode = if (isOver) Mode.RESULT else if (paused) Mode.PAUSED else Mode.PLAYING

        updateNumbers(delta, score, pigsLeft, pigsTotal, isOver)

        val batch = game.batch
        val shapes = game.shapes
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (isOver) {
            shapes.setColor(0f, 0f, 0f, Ease.smooth(resultTime / DIM_TIME) * 0.55f)
            shapes.rect(0f, 0f, width, height)
        } else {
            drawCounterBar(shapes)
            if (paused) {
                shapes.setColor(0f, 0f, 0f, Ease.smooth(pauseTime / 0.25f) * 0.6f)
                shapes.rect(0f, 0f, width, height)
            }
        }
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        if (isOver) {
            drawResult(stars, newBest)
        } else {
            drawPlayingHud(paused)
            if (paused) drawPausePanel()
        }
        batch.end()

        if (paused && !isOver) {
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            drawVolumeSliders(shapes)
            shapes.end()
        }

        val fade = 1f - Ease.smooth(fadeTime / FADE_TIME)
        if (fade > 0.002f) {
            shapes.projectionMatrix = camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.setColor(0f, 0f, 0f, fade)
            shapes.rect(0f, 0f, width, height)
            shapes.end()
        }
        Gdx.gl.glDisable(GL20.GL_BLEND)
        game.resetFontScale()
    }

    /** Makes every displayed number count towards its real value. */
    private fun updateNumbers(delta: Float, score: Int, pigsLeft: Int, pigsTotal: Int, isOver: Boolean) {
        if (scoreCount.target != score) scoreCount.countTo(score)
        scoreCount.update(delta)
        // The pulse is a smooth swell (up and back down), so the digits never jump in size.
        scorePulseLeft = max(0f, scorePulseLeft - delta)
        val pulse = 1f - scorePulseLeft / SCORE_PULSE_TIME
        val factor =
            if (scorePulseLeft > 0f) FACTOR_SCORE * (1f + SCORE_PULSE_GROW * MathUtils.sin(MathUtils.PI * pulse))
            else FACTOR_SCORE
        var relayout = false
        if (scoreCount.shown != shownScore) {
            shownScore = scoreCount.shown
            scoreText = shownScore.toString()
            relayout = true
        }
        if (factor != scoreFactor) {
            scoreFactor = factor
            relayout = true
        }
        if (relayout) setText(scoreLayout, scoreText, scoreFactor)

        val defeated = pigsTotal - pigsLeft
        if (pigsTotal != shownPigsTotal) {
            shownPigsTotal = pigsTotal
            pigCount.snap(defeated)
        }
        pigTotalForBar = max(1, pigsTotal)
        if (pigCount.target != defeated) pigCount.countTo(defeated)
        pigCount.update(delta)
        // "2/5" inside the pig bar (the number counts up together with the bar).
        val pigText = "${pigCount.shown.coerceIn(0, pigTotalForBar)}/$pigTotalForBar"
        if (pigText != pigCountText) {
            pigCountText = pigText
            setText(pigCountLayout, pigText, FACTOR_PIG_COUNT)
        }

        // A star lights up (and pops) the moment the shown score reaches its threshold.
        for (i in 0 until 3) {
            if (starReachedAt[i] < 0f && shownScore >= STAR_THRESHOLDS[i]) starReachedAt[i] = fadeTime
        }

        if (isOver) {
            if (!countingStarted && resultTime >= COUNT_START) {
                countingStarted = true
                resultScoreCount.countTo(score)
                bestCount.countTo(bestFinal)
            }
            resultScoreCount.update(delta)
            bestCount.update(delta)
            if (resultScoreCount.shown != shownResultScore) {
                shownResultScore = resultScoreCount.shown
                setText(resultScoreLayout, shownResultScore.toString(), FACTOR_RESULT_SCORE)
            }
            if (bestCount.shown != shownBest) {
                shownBest = bestCount.shown
                setText(bestLayout, "BEST $shownBest", FACTOR_BEST)
            }
        }
    }

    // ------------------------------------------------------------------ playing

    private fun rightEdge() = width - MARGIN - safeRight

    private fun barY() = height - 92f - BAR_HEIGHT

    private fun scoreBarY() = barY() - SCORE_BAR_GAP - SCORE_BAR_HEIGHT

    /** Share (0..1) of the score bar that is filled for the score shown right now. */
    private fun scoreFraction() = (scoreCount.current / ScoreManager.BAR_MAX).coerceIn(0f, 1f)

    /** Rank (0 = none yet, 1..3) of the star the shown score has reached. */
    private fun starRank(): Int {
        var rank = 0
        for (i in 0 until 3) if (shownScore >= STAR_THRESHOLDS[i]) rank = i + 1
        return rank
    }

    /** A dark rounded frame with a white rim. */
    private fun frame(shapes: ShapeRenderer, x: Float, y: Float, w: Float, h: Float) {
        shapes.setColor(0f, 0f, 0f, 0.28f)
        pill(shapes, x - 2f, y - 5f, w + 4f, h + 4f)
        shapes.setColor(1f, 1f, 1f, 1f)
        pill(shapes, x - 4f, y - 4f, w + 8f, h + 8f)
        shapes.setColor(0.13f, 0.14f, 0.24f, 1f)
        pill(shapes, x, y, w, h)
    }

    /** A filled bar inside a dark track: [fraction] of the track is colored. */
    private fun fillBar(
        shapes: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, fraction: Float,
        r: Float, g: Float, b: Float
    ) {
        shapes.setColor(0.06f, 0.07f, 0.14f, 1f)
        pill(shapes, x, y, w, h)
        if (fraction <= 0.001f) return
        val fillW = max(h, w * fraction)
        shapes.setColor(r, g, b, 1f)
        pill(shapes, x, y, fillW, h)
        // A soft light stripe on the upper half gives the fill some depth.
        shapes.setColor(1f, 1f, 1f, 0.22f)
        pill(shapes, x + h * 0.25f, y + h * 0.5f, max(h * 0.5f, fillW - h * 0.5f), h * 0.38f)
    }

    /** The pig bar (fills as pigs are defeated) and the score bar (fills with the score; its stars light up). */
    private fun drawCounterBar(shapes: ShapeRenderer) {
        val x = rightEdge() - BAR_WIDTH

        // Pig bar: one slice per pig.
        val y = barY()
        frame(shapes, x, y, BAR_WIDTH, BAR_HEIGHT)
        val trackX = x + PIG_TRACK_LEFT
        val trackW = BAR_WIDTH - PIG_TRACK_LEFT - PIG_TRACK_RIGHT
        val trackH = BAR_HEIGHT - 2f * PIG_TRACK_PAD
        val fraction = (pigCount.current / pigTotalForBar).coerceIn(0f, 1f)
        fillBar(shapes, trackX, y + PIG_TRACK_PAD, trackW, trackH, fraction, 0.46f, 0.84f, 0.28f)
        shapes.setColor(0.13f, 0.14f, 0.24f, 1f)
        for (k in 1 until pigTotalForBar) {
            shapes.rect(trackX + trackW * k / pigTotalForBar - 1.5f, y + PIG_TRACK_PAD, 3f, trackH)
        }

        // Score bar: the color follows the star rank reached (bronze, silver, gold).
        val sy = scoreBarY()
        frame(shapes, x, sy, BAR_WIDTH, SCORE_BAR_HEIGHT)
        val rank = starRank()
        val tier = if (rank == 0) TIER_NONE else TIER_COLORS[rank - 1]
        fillBar(
            shapes, x + SCORE_TRACK_PAD, sy + SCORE_TRACK_PAD, BAR_WIDTH - 2f * SCORE_TRACK_PAD,
            SCORE_BAR_HEIGHT - 2f * SCORE_TRACK_PAD, scoreFraction(), tier[0], tier[1], tier[2]
        )
    }

    /** A rounded bar (rectangle with half-circles at both ends). */
    private fun pill(shapes: ShapeRenderer, x: Float, y: Float, w: Float, h: Float) {
        val r = h / 2f
        shapes.rect(x + r, y, w - 2f * r, h)
        shapes.circle(x + r, y + r, r, 24)
        shapes.circle(x + w - r, y + r, r, 24)
    }

    private fun drawPlayingHud(paused: Boolean) {
        val assets = game.assets
        if (pauseVisible && !paused) drawIconButton(assets.btnBlue, assets.iconPause, pauseRect, pressed == HudButton.PAUSE, 1f)
        val right = rightEdge()
        // Score: big gold digits.
        drawText(scoreLayout, scoreFactor, right - scoreLayout.width, height - SCORE_TOP_GAP, 1f, 0.72f, 0.08f, 1f)
        // Pig bar: the pig icon sits on its left end.
        val barX = right - BAR_WIDTH
        val y = barY()
        drawCentered(assets.pigIcon, barX + 6f, y + BAR_HEIGHT / 2f, PIG_ICON_SIZE, PIG_ICON_SIZE, 1f)
        // Defeated / total, centered on the bar's track; it turns gold when every pig is down.
        val trackLeft = barX + PIG_TRACK_LEFT
        val trackWidth = BAR_WIDTH - PIG_TRACK_LEFT - PIG_TRACK_RIGHT
        val allDown = pigCount.shown >= pigTotalForBar
        val textX = trackLeft + (trackWidth - pigCountLayout.width) / 2f
        val textY = y + (BAR_HEIGHT + pigCountLayout.height) / 2f
        if (allDown) drawText(pigCountLayout, FACTOR_PIG_COUNT, textX, textY, 1f, 0.82f, 0.2f, 1f)
        else drawText(pigCountLayout, FACTOR_PIG_COUNT, textX, textY, 1f, 1f, 1f, 1f)

        // Score bar: three stars at the scores where they are earned.
        val trackX = barX + SCORE_TRACK_PAD
        val trackW = BAR_WIDTH - 2f * SCORE_TRACK_PAD
        val cy = scoreBarY() + SCORE_BAR_HEIGHT / 2f
        for (i in 0 until 3) {
            val sx = trackX + trackW * (STAR_THRESHOLDS[i].toFloat() / ScoreManager.BAR_MAX)
            val reached = starReachedAt[i]
            if (reached < 0f) {
                drawCentered(assets.starOff, sx, cy, SCORE_STAR_SIZE, SCORE_STAR_SIZE, 1f)
            } else {
                val pop = Ease.outBack((fadeTime - reached) / SCORE_STAR_POP_TIME)
                drawCentered(assets.starOnFor(SCORE_BAR_STAR_STYLE[i]), sx, cy, SCORE_STAR_SIZE, SCORE_STAR_SIZE, max(0f, pop))
            }
        }
    }

    // ------------------------------------------------------------------ pause panel

    private fun drawPausePanel() {
        val assets = game.assets
        val cx = width / 2f
        val cy = height / 2f
        val t = Ease.outCubic(pauseTime / 0.25f)
        val alpha = t
        val slide = (1f - t) * 30f
        game.batch.setColor(1f, 1f, 1f, alpha)
        game.batch.draw(assets.panel, cx - PAUSE_PANEL_WIDTH / 2f, cy - PAUSE_PANEL_HEIGHT / 2f - slide, PAUSE_PANEL_WIDTH, PAUSE_PANEL_HEIGHT)
        game.batch.setColor(1f, 1f, 1f, 1f)
        drawText(
            pausedLayout, FACTOR_TITLE,
            cx - pausedLayout.width / 2f, cy + PAUSE_PANEL_HEIGHT / 2f - 34f - slide, 1f, 1f, 1f, alpha
        )
        drawIconButton(assets.btnGreen, assets.iconPlay, resumeRect, pressed == HudButton.RESUME, alpha)
        drawIconButton(assets.btnBlue, assets.iconRestart, pauseRetryRect, pressed == HudButton.RETRY, alpha)
        drawIconButton(assets.btnOrange, assets.iconHome, pauseMenuRect, pressed == HudButton.MENU, alpha)
        val settings = game.settings
        drawToggle(
            assets.iconMusic, musicRect, settings.musicOn && settings.musicVolume > 0f, pressed == HudButton.MUSIC, alpha
        )
        drawToggle(
            assets.iconSound, soundRect, settings.soundOn && settings.soundVolume > 0f, pressed == HudButton.SOUND, alpha
        )
        updatePercentLayouts()
        drawPercent(musicPercentLayout, musicRect, settings.musicOn, alpha)
        drawPercent(soundPercentLayout, soundRect, settings.soundOn, alpha)
    }

    /** The two volume sliders of the pause panel (a shape pass: the panel and the buttons are already drawn). */
    private fun drawVolumeSliders(shapes: ShapeRenderer) {
        val alpha = Ease.outCubic(pauseTime / 0.25f)
        val settings = game.settings
        VolumeSlider.draw(
            shapes, trackX, musicRect.y + musicRect.height / 2f, trackWidth, settings.musicVolume,
            settings.musicOn, dragging == Slider.MUSIC, alpha
        )
        VolumeSlider.draw(
            shapes, trackX, soundRect.y + soundRect.height / 2f, trackWidth, settings.soundVolume,
            settings.soundOn, dragging == Slider.SOUND, alpha
        )
    }

    /** Rebuilds the "50%" texts when a volume changed. */
    private fun updatePercentLayouts() {
        val music = VolumeSlider.percent(game.settings.musicVolume)
        val sound = VolumeSlider.percent(game.settings.soundVolume)
        if (music != shownMusicPercent) {
            shownMusicPercent = music
            setText(musicPercentLayout, "$music%", FACTOR_PERCENT)
        }
        if (sound != shownSoundPercent) {
            shownSoundPercent = sound
            setText(soundPercentLayout, "$sound%", FACTOR_PERCENT)
        }
    }

    /** The percent at the right end of the volume row of [icon], vertically centered on it. */
    private fun drawPercent(layout: GlyphLayout, icon: Rectangle, on: Boolean, alpha: Float) {
        val right = trackX + trackWidth + VOLUME_PERCENT_WIDTH
        drawText(
            layout, FACTOR_PERCENT, right - layout.width, icon.y + (icon.height + layout.height) / 2f,
            1f, 0.84f, 0.26f, alpha * (if (on) 1f else 0.6f)
        )
    }

    /** A round on/off button: dimmed with a red slash over the icon while it is off. */
    private fun drawToggle(icon: Texture, rect: Rectangle, on: Boolean, isPressed: Boolean, alpha: Float) {
        val batch = game.batch
        val tint = (if (on) 1f else 0.55f) * (if (isPressed) 0.72f else 1f)
        val dy = if (isPressed) -3f else 0f
        batch.setColor(tint, tint, tint, alpha)
        batch.draw(game.assets.btnBlue, rect.x, rect.y + dy, rect.width, rect.height)
        val glyph = rect.width * ICON_SHARE
        batch.draw(icon, rect.x + (rect.width - glyph) / 2f, rect.y + (rect.height - glyph) / 2f + dy + 2f, glyph, glyph)
        if (!on) {
            val slash = rect.width * 0.78f
            batch.setColor(1f, 1f, 1f, alpha)
            batch.draw(game.assets.iconSlash, rect.x + (rect.width - slash) / 2f, rect.y + (rect.height - slash) / 2f + dy, slash, slash)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    // ------------------------------------------------------------------ result panel

    private fun startResult(state: GameState, stars: Int, bestScore: Int, previousBest: Int) {
        ended = true
        resultTime = 0f
        starsSounded = 0
        countingStarted = false
        shownResultScore = -1
        shownBest = -1
        resultScoreCount.snap(0)
        bestCount.snap(previousBest)
        bestFinal = bestScore
        setText(titleLayout, if (state == GameState.WON) "LEVEL CLEARED!" else "LEVEL FAILED", FACTOR_TITLE)
        setText(bestLayout, "BEST $previousBest", FACTOR_BEST)
        // Buttons work after the last star has appeared.
        readyAt = if (stars > 0) STARS_START + stars * STAR_GAP + 0.4f else PANEL_START + 0.9f
    }

    private fun drawResult(stars: Int, newBest: Boolean) {
        if (resultTime < PANEL_START) return
        val assets = game.assets
        val batch = game.batch
        val cx = width / 2f
        val cy = height / 2f

        val t = Ease.outCubic((resultTime - PANEL_START) / PANEL_TIME)
        val slide = (1f - t) * 40f
        batch.setColor(1f, 1f, 1f, t)
        batch.draw(assets.panel, cx - PANEL_WIDTH / 2f, cy - PANEL_HEIGHT / 2f - slide, PANEL_WIDTH, PANEL_HEIGHT)
        batch.setColor(1f, 1f, 1f, 1f)

        val contentAlpha = Ease.smooth((resultTime - PANEL_START - 0.1f) / 0.3f)
        val panelTop = cy + PANEL_HEIGHT / 2f
        drawText(titleLayout, FACTOR_TITLE, cx - titleLayout.width / 2f, panelTop - 36f - slide, 1f, 1f, 1f, contentAlpha)

        // Big stars. Three passes so a glow never covers a neighbouring star: the dark stars, then the glows of the
        // earned ones (additive light), then the earned stars themselves with their sparkles. They sit as bronze (left),
        // gold (middle, bigger), silver (right) but appear in the order bronze, silver, gold.
        while (starsSounded < stars && resultTime >= STARS_START + starsSounded * STAR_GAP) {
            game.sounds.onStar(starsSounded)
            starsSounded++
        }
        for (i in 0 until 3) {
            val size = starSize(i)
            batch.setColor(1f, 1f, 1f, contentAlpha)
            drawCentered(assets.starOff, starX(cx, i), starY(cy, i), size, size, 1f, starTilt(i))
        }
        batch.setColor(1f, 1f, 1f, 1f)

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        for (i in 0 until 3) {
            val rank = RESULT_STAR_RANK[i]
            if (rank >= stars) continue
            val age = resultTime - (STARS_START + rank * STAR_GAP)
            if (age >= 0f) drawStarLight(i, starX(cx, i), starY(cy, i), starSize(i), age)
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        batch.setColor(1f, 1f, 1f, 1f)

        for (i in 0 until 3) {
            val rank = RESULT_STAR_RANK[i]
            if (rank >= stars) continue
            val age = resultTime - (STARS_START + rank * STAR_GAP)
            if (age < 0f) continue
            val x = starX(cx, i)
            val y = starY(cy, i)
            val size = starSize(i)
            val p = age / STAR_POP_TIME
            // The star itself: overshoots in size and spins a little while settling.
            drawCentered(
                assets.starOnFor(i), x, y, size, size, max(0f, Ease.outBack(p)),
                starTilt(i) + (1f - Ease.outCubic(p)) * -40f
            )
            drawSparkles(x, y, size, age, i)
            drawTwinkles(x, y, size, age, i)
        }

        // Score (big) and best score count up while the stars appear.
        val scoreY = cy + SCORE_OFFSET_Y
        val bestY = cy + BEST_OFFSET_Y
        drawText(resultScoreLayout, FACTOR_RESULT_SCORE, cx - resultScoreLayout.width / 2f, scoreY, 1f, 0.82f, 0.2f, contentAlpha)
        drawText(bestLayout, FACTOR_BEST, cx - bestLayout.width / 2f, bestY, 0.9f, 0.96f, 1f, contentAlpha)
        if (newBest && !resultScoreCount.isCounting && countingStarted) {
            // The label blinks (alpha) instead of scaling: a layout is only valid at the scale it was made with.
            val blink = 0.7f + 0.3f * MathUtils.sin(resultTime * 7f)
            drawText(newBestLayout, FACTOR_NEW_BEST, cx + bestLayout.width / 2f + 28f, bestY + 4f, 1f, 0.9f, 0.2f, contentAlpha * blink)
        }

        drawIconButton(assets.btnBlue, assets.iconRestart, retryRect, pressed == HudButton.RETRY, contentAlpha)
        drawIconButton(assets.btnOrange, assets.iconHome, menuRect, pressed == HudButton.MENU, contentAlpha)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun starSize(i: Int) = if (i == 1) STAR_SIZE * 1.3f else STAR_SIZE

    private fun starX(cx: Float, i: Int) = cx + (i - 1) * STAR_SPACING

    private fun starY(cy: Float, i: Int) = cy + STAR_OFFSET_Y + (if (i == 1) 46f else 0f)

    private fun starTilt(i: Int) = (i - 1) * -9f

    /**
     * The light behind an earned star, [age] seconds after it landed. Must be called with additive blending:
     * a soft colored halo that breathes, a flash that expands once when the star lands, and two layers of rays
     * turning in opposite directions (so the rays cross and shimmer instead of just spinning).
     */
    private fun drawStarLight(i: Int, x: Float, y: Float, size: Float, age: Float) {
        val batch = game.batch
        val assets = game.assets
        val fadeIn = Ease.smooth(age / 0.35f)
        val breath = 0.5f + 0.5f * MathUtils.sin(resultTime * 3f + i * 1.3f)
        val tint = STAR_TINT[i]

        // Soft halo.
        val halo = size * (2.5f + 0.2f * breath)
        batch.setColor(tint[0], tint[1], tint[2], (0.42f + 0.2f * breath) * fadeIn)
        batch.draw(assets.puff, x - halo / 2f, y - halo / 2f, halo, halo)

        // Landing flash.
        if (age < FLASH_TIME) {
            val t = age / FLASH_TIME
            val flash = size * (1.1f + 1.9f * Ease.outCubic(t))
            batch.setColor(1f, 0.97f, 0.85f, 0.85f * (1f - t) * (1f - t))
            batch.draw(assets.puff, x - flash / 2f, y - flash / 2f, flash, flash)
        }

        // Two layers of rays turning in opposite directions.
        val rays = assets.starGlowFor(i)
        val big = size * 2.45f
        batch.setColor(1f, 1f, 1f, 0.8f * fadeIn)
        drawCentered(rays, x, y, big, big, 1f, resultTime * 20f)
        val small = size * 1.75f
        batch.setColor(1f, 1f, 1f, (0.38f + 0.14f * breath) * fadeIn)
        drawCentered(rays, x, y, small, small, 1f, 15f - resultTime * 34f)

        // A tight bright core right behind the star.
        val core = size * 1.15f
        batch.setColor(tint[0], tint[1], tint[2], 0.5f * fadeIn)
        batch.draw(assets.puff, x - core / 2f, y - core / 2f, core, core)
    }

    /** A few little stars that twinkle around a landed star for as long as the panel is shown. */
    private fun drawTwinkles(x: Float, y: Float, size: Float, age: Float, starIndex: Int) {
        if (age < SPARKLE_TIME) return
        val batch = game.batch
        val fadeIn = Ease.smooth((age - SPARKLE_TIME) / 0.5f)
        for (k in 0 until TWINKLE_COUNT) {
            val angle = resultTime * 0.7f + k * (MathUtils.PI2 / TWINKLE_COUNT) + starIndex * 1.1f
            val radius = size * (0.62f + 0.08f * MathUtils.sin(resultTime * 1.3f + k))
            val phase = MathUtils.sin(resultTime * 4.2f + k * 1.9f + starIndex * 2.3f)
            val strength = max(0f, phase)
            if (strength <= 0.02f) continue
            val s = TWINKLE_SIZE * (0.5f + 0.5f * strength)
            batch.setColor(1f, 1f, 1f, strength * fadeIn)
            batch.draw(
                game.assets.sparkle,
                x + MathUtils.cos(angle) * radius - s / 2f, y + MathUtils.sin(angle) * radius * 0.9f - s / 2f, s, s
            )
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /** A burst of little stars flying outwards when a big star lands. */
    private fun drawSparkles(x: Float, y: Float, size: Float, age: Float, starIndex: Int) {
        if (age >= SPARKLE_TIME) return
        val t = age / SPARKLE_TIME
        val batch = game.batch
        for (k in 0 until SPARKLE_COUNT) {
            val angle = MathUtils.degreesToRadians * (k * (360f / SPARKLE_COUNT) + starIndex * 17f)
            val distance = size * 0.30f + size * 0.85f * Ease.outCubic(t)
            val s = SPARKLE_SIZE * (1f - 0.5f * t) * (0.7f + 0.3f * MathUtils.sin(k * 2.3f + 1f))
            batch.setColor(1f, 1f, 1f, 1f - t)
            batch.draw(
                game.assets.sparkle,
                x + MathUtils.cos(angle) * distance - s / 2f, y + MathUtils.sin(angle) * distance - s / 2f, s, s
            )
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    // ------------------------------------------------------------------ helpers

    /** A round button: [base] with [icon] on it. It only darkens while [isPressed]; it never changes size. */
    private fun drawIconButton(base: Texture, icon: Texture, rect: Rectangle, isPressed: Boolean, alpha: Float) {
        val batch = game.batch
        val shade = if (isPressed) 0.72f else 1f
        val dy = if (isPressed) -3f else 0f
        batch.setColor(shade, shade, shade, alpha)
        batch.draw(base, rect.x, rect.y + dy, rect.width, rect.height)
        val iconSize = rect.width * ICON_SHARE
        batch.draw(
            icon,
            rect.x + (rect.width - iconSize) / 2f, rect.y + (rect.height - iconSize) / 2f + dy + rect.height * 0.02f,
            iconSize, iconSize
        )
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /** Draws [texture] centered at ([x], [y]) at [scale] times the given size, rotated by [degrees]. */
    private fun drawCentered(
        texture: Texture, x: Float, y: Float, w: Float, h: Float, scale: Float, degrees: Float = 0f
    ) {
        game.batch.draw(
            texture,
            x - w / 2f, y - h / 2f, w / 2f, h / 2f, w, h, scale, scale, degrees,
            0, 0, texture.width, texture.height, false, false
        )
    }

    /** Lays out [text] with the UI font at [factor] times the normal UI size. */
    private fun setText(layout: GlyphLayout, text: String, factor: Float) {
        game.font.data.setScale(game.uiFontScale * factor)
        layout.setText(game.font, text)
        game.resetFontScale()
    }

    /** Draws a layout made with [setText] using the same [factor]. [y] is the top of the text. */
    private fun drawText(
        layout: GlyphLayout, factor: Float, x: Float, y: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val font = game.font
        font.data.setScale(game.uiFontScale * factor)
        font.setColor(r, g, b, a)
        font.draw(game.batch, layout, x, y)
        game.resetFontScale()
    }

    companion object {
        const val BASE_WIDTH = 1280f
        const val BASE_HEIGHT = 720f

        private const val FACTOR_SCORE = 1.45f
        private const val SCORE_TOP_GAP = 20f
        private const val SCORE_PULSE_TIME = 0.3f
        private const val SCORE_PULSE_GROW = 0.18f
        private const val FACTOR_TITLE = 0.9f
        private const val FACTOR_RESULT_SCORE = 1.7f
        private const val FACTOR_BEST = 0.55f
        private const val FACTOR_NEW_BEST = 0.6f
        private const val FACTOR_PIG_COUNT = 0.62f
        private const val FACTOR_PERCENT = 0.5f

        private const val MARGIN = 34f
        private const val PAUSE_SIZE = 96f
        private const val BAR_WIDTH = 270f
        private const val BAR_HEIGHT = 46f
        private const val PIG_ICON_SIZE = 78f
        private const val PIG_TRACK_LEFT = 50f
        private const val PIG_TRACK_RIGHT = 12f
        private const val PIG_TRACK_PAD = 8f

        private const val SCORE_BAR_HEIGHT = 24f
        private const val SCORE_BAR_GAP = 40f
        private const val SCORE_TRACK_PAD = 6f
        private const val SCORE_STAR_SIZE = 46f
        private const val SCORE_STAR_POP_TIME = 0.45f

        /** Scores at which the three stars are earned (the bar is full at [ScoreManager.BAR_MAX]). */
        private val STAR_THRESHOLDS = intArrayOf(ScoreManager.STAR_ONE, ScoreManager.STAR_TWO, ScoreManager.STAR_THREE)

        /**
         * The star style ([Assets.starOnFor] index: 0 bronze, 1 gold, 2 silver) of each star on the score bar, in order:
         * bronze, silver, gold. Only the score bar uses this order.
         */
        private val SCORE_BAR_STAR_STYLE = intArrayOf(0, 2, 1)

        /**
         * The rank (0 = first earned, 1 = second, 2 = third) of the result panel star at each position, left to right:
         * bronze is first, silver second, gold third, while gold stays in the middle. A star appears when its rank is
         * below the stars earned, one rank after the other.
         */
        private val RESULT_STAR_RANK = intArrayOf(0, 2, 1)

        /** Fill colors of the score bar by star reached: bronze, silver, gold; and before the first. */
        private val TIER_COLORS = arrayOf(
            floatArrayOf(0.80f, 0.48f, 0.22f),
            floatArrayOf(0.78f, 0.82f, 0.90f),
            floatArrayOf(1f, 0.78f, 0.16f)
        )
        private val TIER_NONE = floatArrayOf(0.38f, 0.62f, 0.95f)

        private const val ICON_SHARE = 0.62f

        private const val PANEL_WIDTH = 820f
        private const val PANEL_HEIGHT = 680f
        private const val PAUSE_PANEL_WIDTH = 700f
        private const val PAUSE_PANEL_HEIGHT = 680f
        private const val RESUME_SIZE = 170f
        private const val SMALL_BUTTON = 130f
        private const val PAUSE_SMALL_BUTTON = 120f
        private const val TOGGLE_SIZE = 84f
        private const val VOLUME_ROW_WIDTH = 580f
        private const val VOLUME_ROW_HEIGHT = 88f
        private const val VOLUME_ROW_GAP = 12f
        private const val VOLUME_ICON_GAP = 40f
        private const val VOLUME_PERCENT_WIDTH = 110f
        private const val STAR_SIZE = 200f
        private const val STAR_SPACING = 235f

        // Result panel layout, in HUD units relative to the screen center (the top of the score and best texts).
        private const val STAR_OFFSET_Y = 72f
        private const val SCORE_OFFSET_Y = -48f
        private const val BEST_OFFSET_Y = -124f

        /** Light colors of the result stars by position: bronze, gold, silver. */
        private val STAR_TINT = arrayOf(
            floatArrayOf(1f, 0.62f, 0.30f),
            floatArrayOf(1f, 0.84f, 0.28f),
            floatArrayOf(0.80f, 0.90f, 1f)
        )
        private const val FLASH_TIME = 0.45f
        private const val TWINKLE_COUNT = 4
        private const val TWINKLE_SIZE = 34f

        // Result timeline (seconds after the result phase began; the camera zoom and the bird bonus happen before it).
        private const val DIM_TIME = 0.5f
        private const val PANEL_START = 0.35f
        private const val PANEL_TIME = 0.4f
        private const val STARS_START = PANEL_START + 0.8f
        private const val STAR_GAP = 0.6f
        private const val STAR_POP_TIME = 0.5f
        private const val COUNT_START = PANEL_START + 0.3f

        private const val SPARKLE_TIME = 0.9f
        private const val SPARKLE_COUNT = 10
        private const val SPARKLE_SIZE = 44f

        private const val FADE_TIME = 0.45f
        private const val PAUSE_READY_TIME = 0.3f
    }
}
