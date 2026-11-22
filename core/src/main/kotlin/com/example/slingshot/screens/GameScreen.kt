package com.example.slingshot.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.example.slingshot.AngryBirds
import com.example.slingshot.entities.Block
import com.example.slingshot.entities.Enemy
import com.example.slingshot.entities.Entity
import com.example.slingshot.entities.Material
import com.example.slingshot.level.Level
import com.example.slingshot.level.LevelLoader
import com.example.slingshot.level.Levels
import com.example.slingshot.render.BonusFlyers
import com.example.slingshot.render.CheerPose
import com.example.slingshot.render.Ease
import com.example.slingshot.render.Effects
import com.example.slingshot.render.Finale
import com.example.slingshot.render.GameRenderer
import com.example.slingshot.render.Hud
import com.example.slingshot.render.HudButton
import com.example.slingshot.systems.DamageSystem
import com.example.slingshot.audio.MusicPlayer
import com.example.slingshot.systems.AbilityListener
import com.example.slingshot.systems.DestroyListener
import com.example.slingshot.systems.GameState
import com.example.slingshot.systems.GameStateManager
import com.example.slingshot.systems.ScoreManager
import com.example.slingshot.systems.SlingshotConfig
import com.example.slingshot.systems.SlingshotController
import com.example.slingshot.world.BodyFactory
import com.example.slingshot.world.GameContactListener
import com.example.slingshot.world.ImpactQueue
import com.example.slingshot.world.PhysicsWorld
import com.example.slingshot.world.Units
import kotlin.math.max
import kotlin.math.min

/**
 * Wires the physics world, level, damage, game flow, audio events, effects and HUD together.
 * Retry creates a brand new [GameScreen]; the old one is disposed in [hide],
 * so no body or listener of the previous attempt survives.
 *
 * Full screen: both viewports are [ExtendViewport]s, so the picture always fills the whole
 * display. On a screen wider than 16:9 the world simply shows more scenery to the left and
 * right (the slingshot and tower stay where they are); there are no black bars.
 */
class GameScreen(
    private val game: AngryBirds,
    private val levelIndex: Int = 0
) : ScreenAdapter() {

    private val worldViewport = ExtendViewport(Units.WORLD_WIDTH, Units.WORLD_HEIGHT)
    private val hudViewport = ExtendViewport(Hud.BASE_WIDTH, Hud.BASE_HEIGHT)

    private val physics = PhysicsWorld()
    private val bodies = BodyFactory(physics.world)
    private val effects = Effects(game)
    private val renderer = GameRenderer(game.batch, game.shapes, game.assets, physics.world, effects)
    private val controller = SlingshotController(worldViewport, game.sounds)
    private val impacts = ImpactQueue()
    private val score = ScoreManager()
    private val hud = Hud(game)
    private val flyers = BonusFlyers(game, worldViewport, hudViewport)
    private val touch = Vector2()

    private lateinit var level: Level
    private lateinit var damage: DamageSystem
    private lateinit var stateManager: GameStateManager
    private var released = false
    private var paused = false
    private var resultRecorded = false
    private var newBest = false
    private var previousBest = 0
    private var resultStars = 0
    private var totalPigs = 0

    // The level-end sequence: camera zoom + celebration, then (if the birds won) every unused bird jumps in turn and
    // a "+10000" flies out of it into the score, and only when the last one has arrived the result panel appears.
    private val finale = Finale()
    private var finalePrepared = false
    private var resultPhase = false
    private var bonusLanded = 0
    private var lastLandAt = 0f

    // Level-end camera: zoom onto the winners (the birds if the level was cleared, the pigs if it was lost).
    private var endTime = 0f
    private var focusReady = false
    private var focusX = 0f
    private var focusY = 0f
    private var focusZoom = 1f
    private var confettiTimer = 0f

    override fun show() {
        bodies.createGround()
        level = LevelLoader(bodies).load(Levels.get(levelIndex))
        totalPigs = level.enemies.size
        physics.world.setContactListener(GameContactListener(impacts))
        stateManager = GameStateManager(physics.world, bodies, level, controller, score, game.sounds)
        damage = DamageSystem(physics.world, level, impacts, game.sounds)
        damage.destroyListener = DestroyListener { entity -> onEntityDestroyed(entity) }
        stateManager.abilityListener = AbilityListener { _, x, y -> effects.spawnPuff(x, y, ABILITY_PUFF_RADIUS) }
        physics.beforeStep = { savePreviousStates() }
        flyers.landListener = BonusFlyers.LandListener { onBonusLanded() }
        game.music.play(MusicPlayer.Track.GAME)

        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        val screenInput = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode != Input.Keys.BACK && keycode != Input.Keys.ESCAPE) return false
                when {
                    !stateManager.isPlaying -> if (resultPhase) openLevelSelect()
                    paused -> resumeGame()
                    else -> pauseGame()
                }
                return true
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                hudViewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))

                // HUD buttons act when the finger is lifted on the same button (see touchUp).
                if (hud.pressAt(touch.x, touch.y)) return true
                // While paused or while the level ends, taps never reach the slingshot.
                if (paused || !stateManager.isPlaying) return true

                // Tapping the top-right corner toggles the Box2D debug overlay.
                val inCorner = screenX > Gdx.graphics.width * DEBUG_CORNER_X &&
                    screenY < Gdx.graphics.height * DEBUG_CORNER_Y
                if (inCorner) {
                    renderer.debugEnabled = !renderer.debugEnabled
                    return true
                }
                stateManager.resetIdle()
                return false
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                // A finger on a volume slider of the pause panel moves the slider.
                hudViewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
                return hud.dragAt(touch.x)
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                hudViewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
                when (hud.releaseAt(touch.x, touch.y)) {
                    HudButton.PAUSE -> pauseGame()
                    HudButton.RESUME -> resumeGame()
                    HudButton.RETRY -> restart()
                    HudButton.MENU -> openLevelSelect()
                    HudButton.MUSIC -> {
                        game.sounds.onClick()
                        game.setMusicOn(!game.settings.musicOn)
                    }
                    HudButton.SOUND -> {
                        // Turning the sound off is silent (the click would be cut off); turning it on plays the click.
                        val on = !game.settings.soundOn
                        game.setSoundOn(on)
                        if (on) game.sounds.onClick()
                    }
                    null -> return paused || !stateManager.isPlaying
                }
                return true
            }
        }
        Gdx.input.inputProcessor = InputMultiplexer(screenInput, controller)
    }

    override fun hide() {
        game.music.setDucked(false)
        Gdx.input.setCatchKey(Input.Keys.BACK, false)
        Gdx.input.inputProcessor = null
        releaseResources()
    }

    override fun pause() {
        // A finger may be lifted while the app is in the background; also pause the game itself.
        controller.cancelDrag()
        if (::stateManager.isInitialized && stateManager.isPlaying) paused = true
    }

    private fun pauseGame() {
        if (paused) return
        paused = true
        controller.cancelDrag()
        game.sounds.stopAll()
        game.sounds.onClick()
        game.music.setDucked(true)
    }

    private fun resumeGame() {
        paused = false
        game.sounds.onClick()
        game.music.setDucked(false)
        stateManager.resetIdle()
    }

    private fun restart() {
        game.sounds.stopAll()
        game.sounds.onClick()
        game.setScreen(GameScreen(game, levelIndex))
    }

    private fun openLevelSelect() {
        game.sounds.stopAll()
        game.sounds.onClick()
        game.setScreen(LevelSelectScreen(game))
    }

    override fun resize(width: Int, height: Int) {
        // The minimum area (Units.WORLD_WIDTH x WORLD_HEIGHT) stays centered with the ground at the bottom:
        // extra width appears equally on both sides, extra height only above.
        worldViewport.update(width, height, false)
        worldViewport.camera.position.set(Units.WORLD_WIDTH / 2f, worldViewport.worldHeight / 2f, 0f)
        worldViewport.camera.update()

        hudViewport.update(width, height, true)
        hud.resize(hudViewport.worldWidth, hudViewport.worldHeight)
    }

    override fun render(delta: Float) {
        if (!paused) {
            physics.update(delta)
            // The world is not stepping from here on, so bodies may be created and destroyed.
            damage.update(delta)
            stateManager.update(delta)
        }
        updateRenderStates(physics.alpha)

        // Animations use a clamped frame time so a hiccup does not make them jump.
        val frame = min(delta, MAX_ANIMATION_DELTA)
        // While paused the scene is frozen: no animation time passes.
        val sceneDelta = if (paused) 0f else frame

        ScreenUtils.clear(0.53f, 0.81f, 0.92f, 1f)

        if (!stateManager.isPlaying && !finalePrepared) prepareFinale()
        updateCamera(frame)
        updateFinale(frame)
        worldViewport.apply()
        renderer.render(
            worldViewport, sceneDelta, stateManager.bird, stateManager.splitBirds, level,
            stateManager.spawnedCount, finale
        )

        hud.pauseVisible = stateManager.isPlaying
        hudViewport.apply()
        hud.render(
            hudViewport.camera, frame, score.score,
            if (resultPhase) stateManager.state else GameState.PLAYING,
            resultStars, game.progress.bestScore, previousBest, newBest, paused,
            level.enemies.size, totalPigs
        )
        // The bonus numbers fly over the HUD, into the score.
        flyers.draw(hudViewport.camera)
    }

    /**
     * Fixes everything the level-end sequence needs, once, in the first frame after the attempt ended: which birds
     * were never fired and where each of them stands. They cheer exactly there, so nothing has to move or jump
     * to a "stage" when the level ends.
     */
    private fun prepareFinale() {
        finalePrepared = true
        finale.state = stateManager.state
        finale.bonusBirds = min(stateManager.bonusBirds, Finale.MAX_BIRDS)
        finale.firstBonusIndex = level.birds.size - stateManager.bonusBirds
        for (k in 0 until finale.bonusBirds) {
            val index = finale.firstBonusIndex + k
            if (index < stateManager.spawnedCount) {
                // The unused bird that already sits on the slingshot.
                finale.homeX[k] = SlingshotConfig.ANCHOR_X
                finale.homeY[k] = SlingshotConfig.ANCHOR_Y
            } else {
                finale.homeX[k] = SlingshotConfig.waitingX(index)
                finale.homeY[k] = SlingshotConfig.waitingBaseY(index) + level.birds[index].radius
            }
        }
    }

    /**
     * Runs the level-end timeline. When the birds won, every bird that was never fired jumps in turn (with a
     * sound); at the top of its jump a "+10000" bursts out of it and flies to the score, which grows when the
     * number arrives. The result panel waits for the last number. When the pigs won there is just the zoom and
     * the celebration.
     */
    private fun updateFinale(delta: Float) {
        if (stateManager.isPlaying) return
        finale.time = endTime

        var resultAt = if (finale.state == GameState.WON) WON_RESULT_MIN_TIME else LOST_RESULT_TIME
        if (finale.state == GameState.WON) {
            while (finale.bonusCounted < finale.bonusBirds &&
                endTime >= BONUS_START + finale.bonusCounted * BONUS_INTERVAL
            ) {
                startBonusBird(finale.bonusCounted)
                finale.bonusCounted++
            }
            while (finale.numbersLaunched < finale.bonusCounted &&
                endTime >= finale.countedAt[finale.numbersLaunched] + CheerPose.APEX_TIME
            ) {
                launchBonusNumber(finale.numbersLaunched)
                finale.numbersLaunched++
            }
            flyers.update(delta, hud.scoreTargetX, hud.scoreTargetY)
            if (finale.bonusBirds > 0) {
                resultAt = if (bonusLanded >= finale.bonusBirds) {
                    max(resultAt, lastLandAt + BONUS_RESULT_PAUSE)
                } else {
                    Float.MAX_VALUE
                }
            }
        }
        if (!resultPhase && endTime >= resultAt) {
            resultPhase = true
            resultStars = ScoreManager.starsFor(score.score, finale.state == GameState.WON)
            recordResultOnce()
        }
    }

    /** The [k]-th unused bird is counted: it starts its jump now. */
    private fun startBonusBird(k: Int) {
        finale.countedAt[k] = endTime
        game.sounds.onHop()
    }

    /** At the top of its jump the bird lets its "+10000" burst out; the number then flies to the score. */
    private fun launchBonusNumber(k: Int) {
        val x = finale.homeX[k]
        val y = finale.homeY[k] + CheerPose.JUMP_HEIGHT
        flyers.launch(x, y, "+${ScoreManager.BIRD_BONUS}")
        effects.spawnPuff(x, y, BONUS_PUFF_RADIUS)
        effects.spawnConfetti(x, y, 10, 0.5f)
    }

    /** A "+10000" has reached the score: only now do the points count. */
    private fun onBonusLanded() {
        score.addBirdBonus()
        hud.pulseScore()
        game.sounds.onBonus(bonusLanded)
        bonusLanded++
        lastLandAt = endTime
    }

    /**
     * While playing the camera shows the whole level. When the attempt ends it eases onto the winners
     * (after a short pause) and confetti falls over them.
     */
    private fun updateCamera(delta: Float) {
        val camera = worldViewport.camera as? OrthographicCamera ?: return
        val viewWidth = worldViewport.worldWidth
        val viewHeight = worldViewport.worldHeight
        val baseX = Units.WORLD_WIDTH / 2f
        val baseY = viewHeight / 2f
        if (stateManager.isPlaying) {
            camera.zoom = 1f
            camera.position.set(baseX, baseY, 0f)
            camera.update()
            return
        }

        endTime += delta
        if (!focusReady) computeFocus(viewWidth, viewHeight)
        val t = Ease.smooth((endTime - ZOOM_DELAY) / ZOOM_TIME)
        val zoom = MathUtils.lerp(1f, focusZoom, t)
        val x = MathUtils.lerp(baseX, focusX, t)
        // Never show below the ground.
        val y = max(MathUtils.lerp(baseY, focusY, t), viewHeight * zoom / 2f)
        camera.zoom = zoom
        camera.position.set(x, y, 0f)
        camera.update()

        if (endTime > ZOOM_DELAY) {
            confettiTimer -= delta
            if (confettiTimer <= 0f) {
                confettiTimer = CONFETTI_INTERVAL
                effects.spawnConfetti(x, y + viewHeight * zoom / 2f, 3, viewWidth * zoom / 2f)
            }
        }
    }

    /**
     * The camera frames the unused birds where they stand, including the top of their jump and the number that
     * bursts out of them; with no unused bird it shows the bench.
     */
    private fun computeBirdsFocus(viewWidth: Float, viewHeight: Float) {
        val count = finale.bonusBirds
        if (count == 0) {
            focusX = BIRDS_FOCUS_X
            focusY = BIRDS_FOCUS_Y
            focusZoom = BIRDS_FOCUS_ZOOM
            return
        }
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (k in 0 until count) {
            val radius = level.birds[finale.firstBonusIndex + k].radius
            minX = min(minX, finale.homeX[k] - radius)
            maxX = max(maxX, finale.homeX[k] + radius)
            minY = min(minY, finale.homeY[k] - radius)
            maxY = max(maxY, finale.homeY[k] + radius + CheerPose.JUMP_HEIGHT + BIRDS_TOP_ROOM)
        }
        focusX = (minX + maxX) / 2f
        focusY = (minY + maxY) / 2f
        focusZoom = max((maxX - minX + BIRDS_MARGIN_X) / viewWidth, (maxY - minY + BIRDS_MARGIN_Y) / viewHeight)
            .coerceIn(BIRDS_FOCUS_ZOOM, PIGS_ZOOM_MAX)
    }

    /** Chooses where the camera ends up: the unused birds (or the bench), or the pigs that are left. */
    private fun computeFocus(viewWidth: Float, viewHeight: Float) {
        focusReady = true
        if (stateManager.state == GameState.WON || level.enemies.isEmpty()) {
            computeBirdsFocus(viewWidth, viewHeight)
            return
        }
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        val enemies = level.enemies
        for (i in enemies.indices) {
            val p = enemies[i].body.position
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        focusX = (minX + maxX) / 2f
        focusY = (minY + maxY) / 2f + 0.2f
        val spanX = maxX - minX + PIGS_MARGIN
        val spanY = maxY - minY + PIGS_MARGIN
        focusZoom = max(spanX / viewWidth, spanY / viewHeight).coerceIn(PIGS_ZOOM_MIN, PIGS_ZOOM_MAX)
    }

    /** Effects, score popups and points for something that was just destroyed. */
    private fun onEntityDestroyed(entity: Entity) {
        val position = entity.body.position
        when (entity) {
            is Block ->
                if (entity.material == Material.TNT) {
                    effects.spawnExplosion(position.x, position.y)
                } else {
                    effects.spawnDebris(position.x, position.y, entity.material, entity.width, entity.height)
                }
            is Enemy -> effects.spawnPuff(position.x, position.y, entity.radius)
            else -> Unit
        }
        // Points count while the round runs (also while the last blocks collapse after the final pig).
        if (stateManager.isPlaying) {
            val points = score.onDestroyed(entity)
            if (points > 0) {
                effects.spawnPopup(
                    position.x, position.y + POPUP_LIFT, "+$points",
                    if (entity is Enemy) 1.3f else 1f
                )
            }
        }
    }

    /** Saves the result once, the moment the attempt ends. */
    private fun recordResultOnce() {
        if (resultRecorded || stateManager.isPlaying) return
        resultRecorded = true
        previousBest = game.progress.bestScore
        newBest = game.progress.record(score.score, resultStars)
    }

    /** Remembers every body's state before a physics step (for interpolation). */
    private fun savePreviousStates() {
        val blocks = level.blocks
        for (i in blocks.indices) blocks[i].savePreviousState()
        val enemies = level.enemies
        for (i in enemies.indices) enemies[i].savePreviousState()
        stateManager.bird?.savePreviousState()
        val extras = stateManager.splitBirds
        for (i in extras.indices) extras[i].savePreviousState()
    }

    /** Computes where everything is drawn between the last two physics steps. */
    private fun updateRenderStates(alpha: Float) {
        val blocks = level.blocks
        for (i in blocks.indices) blocks[i].updateRenderState(alpha)
        val enemies = level.enemies
        for (i in enemies.indices) enemies[i].updateRenderState(alpha)
        stateManager.bird?.updateRenderState(alpha)
        val extras = stateManager.splitBirds
        for (i in extras.indices) extras[i].updateRenderState(alpha)
    }

    private fun releaseResources() {
        if (released) return
        released = true
        renderer.dispose()
        physics.dispose()
    }

    private companion object {
        const val DEBUG_CORNER_X = 0.9f
        const val DEBUG_CORNER_Y = 0.15f
        const val MAX_ANIMATION_DELTA = 0.05f
        const val POPUP_LIFT = 0.45f
        const val ABILITY_PUFF_RADIUS = 0.3f

        // Level-end camera
        const val ZOOM_DELAY = 0.4f
        const val ZOOM_TIME = 1.3f
        const val CONFETTI_INTERVAL = 0.05f

        // Bird bonus: the first bird is counted at BONUS_START, then one every BONUS_INTERVAL seconds. The result
        // panel appears BONUS_RESULT_PAUSE after the last number has reached the score.
        const val BONUS_START = 1.3f
        const val BONUS_INTERVAL = 0.85f
        const val BONUS_RESULT_PAUSE = 1.0f
        const val BONUS_PUFF_RADIUS = 0.2f
        const val WON_RESULT_MIN_TIME = 2.6f
        const val LOST_RESULT_TIME = 2.6f

        // Camera on the unused birds: the smallest zoom, the room around them, and the room above the highest jump
        // for the number that bursts out. Without unused birds the camera shows the bench.
        const val BIRDS_FOCUS_X = 4.1f
        const val BIRDS_FOCUS_Y = 3.3f
        const val BIRDS_FOCUS_ZOOM = 0.42f
        const val BIRDS_MARGIN_X = 3.0f
        const val BIRDS_MARGIN_Y = 1.2f
        const val BIRDS_TOP_ROOM = 0.6f
        const val PIGS_MARGIN = 3.2f
        const val PIGS_ZOOM_MIN = 0.4f
        const val PIGS_ZOOM_MAX = 0.85f
    }
}
