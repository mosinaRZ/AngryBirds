package com.example.slingshot.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.example.slingshot.AngryBirds
import com.example.slingshot.audio.MusicPlayer
import com.example.slingshot.level.Levels
import com.example.slingshot.render.Ease
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The campaign: a tall tower of themed rooms, three or four arched doors per floor, scrolled with a drag.
 *
 * - A completed level is a blue door with its three star slots; the level to play next is a golden door;
 *   levels that do not exist yet are boarded up with planks and a padlock (they shake with a dull sound when tapped).
 * - Each door carries a numbered banner. Everything is static (no pulsing); only the sky drifts a little while scrolling.
 * - The round back button (bottom left) returns to the lobby.
 *
 * Doors and the button react when the finger is lifted on them; a drag only scrolls.
 */
class LevelSelectScreen(private val game: AngryBirds) : ScreenAdapter() {

    private val viewport = ExtendViewport(UI_WIDTH, UI_HEIGHT)
    private val touch = Vector2()
    private val backRect = Rectangle()
    private val doorRect = Rectangle()

    private val numberLayouts = Array(max(Levels.MENU_SLOTS, DOORS_PER_FLOOR.sum())) { GlyphLayout() }

    private var time = 0f
    private var scroll = 0f
    private var scrollTarget = 0f

    // Touch tracking
    private var pressY = 0f
    private var lastY = 0f
    private var dragVelocity = 0f
    private var dragging = false
    private var pressed = false
    private var backPressed = false

    // "Locked" wobble
    private var shakeIndex = -1
    private var shakeTime = 99f

    private val contentHeight: Float
        get() = BOTTOM_PAD + FLOORS * FLOOR_HEIGHT + TOP_PAD

    private val maxScroll: Float
        get() = max(0f, contentHeight - viewport.worldHeight)

    override fun show() {
        for (i in numberLayouts.indices) {
            game.font.data.setScale(game.uiFontScale * FACTOR_NUMBER)
            numberLayouts[i].setText(game.font, (i + 1).toString())
        }
        game.resetFontScale()

        game.music.play(MusicPlayer.Track.MENU)
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                    goBack()
                    return true
                }
                return false
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (pointer != 0) return false
                pressed = true
                dragging = false
                pressY = screenY.toFloat()
                lastY = pressY
                dragVelocity = 0f
                viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
                backPressed = backRect.contains(touch)
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (!pressed || pointer != 0) return false
                if (abs(screenY - pressY) > DRAG_THRESHOLD_PIXELS) dragging = true
                if (dragging) {
                    backPressed = false
                    val worldPerPixel = viewport.worldHeight / max(1, Gdx.graphics.height)
                    // Dragging down reveals the higher floors (scroll shrinks the content's height on screen);
                    // dragging up reveals the lower ones.
                    val step = (screenY - lastY) * worldPerPixel
                    scrollTarget = (scrollTarget + step).coerceIn(0f, maxScroll)
                    dragVelocity = dragVelocity * 0.6f + step / max(0.001f, Gdx.graphics.deltaTime) * 0.4f
                }
                lastY = screenY.toFloat()
                return true
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (!pressed || pointer != 0) return false
                pressed = false
                if (dragging) {
                    // A little momentum after a flick.
                    scrollTarget = (scrollTarget + dragVelocity * FLICK_SECONDS).coerceIn(0f, maxScroll)
                } else {
                    onTap(screenX, screenY)
                }
                backPressed = false
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
        val safeLeft = Gdx.graphics.safeInsetLeft * viewport.worldWidth / max(1, Gdx.graphics.width)
        backRect.set(MARGIN + safeLeft, MARGIN, BACK_SIZE, BACK_SIZE)
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll)
        scroll = scroll.coerceIn(0f, maxScroll)
    }

    override fun render(delta: Float) {
        val dt = min(delta, 0.05f)
        time += dt
        shakeTime += dt
        scroll += (scrollTarget - scroll) * min(1f, dt * 12f)

        ScreenUtils.clear(0.58f, 0.84f, 0.94f, 1f)
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        val projection = viewport.camera.combined
        val batch = game.batch

        batch.projectionMatrix = projection
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        drawSky(w, h)
        for (floor in 0 until FLOORS) drawFloor(floor, w, h)
        drawBackButton()
        batch.end()

        val fade = 1f - Ease.smooth(time / FADE_TIME)
        if (fade > 0.002f) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            val shapes = game.shapes
            shapes.projectionMatrix = projection
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.setColor(0f, 0f, 0f, fade)
            shapes.rect(0f, 0f, w, h)
            shapes.end()
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }
    }

    // ------------------------------------------------------------------ drawing

    /** The sky drifts slowly while the tower scrolls (a little parallax). */
    private fun drawSky(w: Float, h: Float) {
        val tex = game.assets.towerBg
        val scale = max(w / tex.width, h / tex.height) * SKY_OVERSCAN
        val bw = tex.width * scale
        val bh = tex.height * scale
        val t = if (maxScroll > 0f) scroll / maxScroll else 0f
        game.batch.draw(tex, (w - bw) / 2f, -t * (bh - h), bw, bh)
    }

    private fun floorBottom(floor: Int): Float = BOTTOM_PAD + floor * FLOOR_HEIGHT - scroll

    /** First level number (0-based) on a floor, and how many doors the floor has. */
    private fun floorFirst(floor: Int): Int {
        var first = 0
        for (f in 0 until floor) first += DOORS_PER_FLOOR[f]
        return first
    }

    private fun drawFloor(floor: Int, w: Float, h: Float) {
        val bottom = floorBottom(floor)
        if (bottom > h || bottom + FLOOR_HEIGHT < 0f) return
        val assets = game.assets
        val batch = game.batch
        val left = (w - FLOOR_WIDTH) / 2f
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(assets.towerFloors[floor % assets.towerFloors.size], left, bottom, FLOOR_WIDTH, FLOOR_HEIGHT)

        val first = floorFirst(floor)
        val count = DOORS_PER_FLOOR[floor]
        val spacing = if (count == 3) SPACING_THREE else SPACING_FOUR
        for (k in 0 until count) {
            val index = first + k
            val cx = w / 2f + (k - (count - 1) / 2f) * spacing
            drawDoor(index, cx, bottom + DOOR_BASE)
        }
    }

    private fun drawDoor(index: Int, cx: Float, baseY: Float) {
        val assets = game.assets
        val batch = game.batch
        val open = Levels.isUnlocked(index)
        val completed = index == 0 && game.progress.bestStars > 0
        var x = cx
        if (index == shakeIndex) x += sin(shakeTime * 48f) * 10f * exp(-shakeTime * 6f)

        val texture = when {
            !open -> assets.doorLocked
            completed -> assets.doorOpen
            else -> assets.doorGold
        }
        batch.setColor(1f, 1f, 1f, 1f)
        batch.draw(texture, x - DOOR_WIDTH / 2f, baseY, DOOR_WIDTH, DOOR_HEIGHT)

        if (completed) {
            val stars = game.progress.bestStars
            for (s in 0 until 3) {
                val tex = if (s < stars) assets.starOnFor(s) else assets.starOff
                val sx = x + (s - 1) * STAR_DX
                val sy = baseY + DOOR_HEIGHT * 0.46f + (if (s == 1) STAR_MID_LIFT else 0f)
                batch.draw(tex, sx - STAR_SIZE / 2f, sy - STAR_SIZE / 2f, STAR_SIZE, STAR_SIZE)
            }
        }

        // Numbered banner on top of the arch.
        val tag = if (index == Levels.MENU_SLOTS - 1) assets.tagPurple else assets.tagRed
        val tagY = baseY + DOOR_HEIGHT - TAG_HEIGHT * 0.42f
        batch.draw(tag, x - TAG_WIDTH / 2f, tagY, TAG_WIDTH, TAG_HEIGHT)
        val layout = numberLayouts[index]
        drawText(layout, FACTOR_NUMBER, x - layout.width / 2f, tagY + TAG_HEIGHT * 0.5f + layout.height / 2f + 2f)

    }

    private fun drawBackButton() {
        val assets = game.assets
        val batch = game.batch
        val shade = if (backPressed) 0.72f else 1f
        val dy = if (backPressed) -3f else 0f
        batch.setColor(shade, shade, shade, 1f)
        batch.draw(assets.btnOrange, backRect.x, backRect.y + dy, backRect.width, backRect.height)
        val icon = backRect.width * ICON_SHARE
        batch.draw(
            assets.iconBack,
            backRect.x + (backRect.width - icon) / 2f, backRect.y + (backRect.height - icon) / 2f + dy + 2f, icon, icon
        )
        batch.setColor(1f, 1f, 1f, 1f)
    }

    // ------------------------------------------------------------------ input

    private fun onTap(screenX: Int, screenY: Int) {
        viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
        if (backRect.contains(touch)) {
            goBack()
            return
        }
        // Test every door by recomputing its rectangle (cheap: at most 14).
        val w = viewport.worldWidth
        for (floor in 0 until FLOORS) {
            val bottom = floorBottom(floor)
            val first = floorFirst(floor)
            val count = DOORS_PER_FLOOR[floor]
            val spacing = if (count == 3) SPACING_THREE else SPACING_FOUR
            for (k in 0 until count) {
                val cx = w / 2f + (k - (count - 1) / 2f) * spacing
                doorRect.set(cx - DOOR_WIDTH / 2f, bottom + DOOR_BASE, DOOR_WIDTH, DOOR_HEIGHT + TAG_HEIGHT * 0.5f)
                if (!doorRect.contains(touch)) continue
                val index = first + k
                if (Levels.isUnlocked(index)) {
                    game.sounds.onClick()
                    game.setScreen(GameScreen(game, index))
                } else {
                    shakeIndex = index
                    shakeTime = 0f
                    game.sounds.onLocked()
                }
                return
            }
        }
    }

    private fun goBack() {
        game.sounds.onClick()
        game.setScreen(MenuScreen(game))
    }

    // ------------------------------------------------------------------ text

    /** Draws a number layout made with FACTOR_NUMBER; [y] is the top of the text. */
    private fun drawText(layout: GlyphLayout, factor: Float, x: Float, y: Float) {
        val font = game.font
        font.data.setScale(game.uiFontScale * factor)
        font.setColor(1f, 1f, 1f, 1f)
        font.draw(game.batch, layout, x, y)
        game.resetFontScale()
    }

    private companion object {
        const val UI_WIDTH = 1280f
        const val UI_HEIGHT = 720f

        /** Floors from the bottom, with the number of doors on each (3 + 3 + 4 + 4 = 14 levels). */
        val DOORS_PER_FLOOR = intArrayOf(3, 3, 4, 4)
        const val FLOORS = 4

        const val FLOOR_WIDTH = 1000f
        const val FLOOR_HEIGHT = 313f
        const val BOTTOM_PAD = 30f
        const val TOP_PAD = 60f
        const val SKY_OVERSCAN = 1.25f

        const val DOOR_BASE = 43f
        const val DOOR_WIDTH = 126f
        const val DOOR_HEIGHT = 190f
        const val SPACING_THREE = 280f
        const val SPACING_FOUR = 215f

        const val TAG_WIDTH = 84f
        const val TAG_HEIGHT = 52f
        const val STAR_SIZE = 44f
        const val STAR_DX = 38f
        const val STAR_MID_LIFT = 20f

        const val MARGIN = 30f
        const val BACK_SIZE = 104f
        const val ICON_SHARE = 0.62f
        const val DRAG_THRESHOLD_PIXELS = 14f
        const val FLICK_SECONDS = 0.25f

        const val FACTOR_NUMBER = 0.62f
        const val FADE_TIME = 0.4f
    }
}
