package com.example.slingshot.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.example.slingshot.AngryBirds
import com.example.slingshot.render.Backdrop
import com.example.slingshot.render.Ease
import kotlin.math.min

/**
 * The splash screen: the title artwork (sp_bg.png) slowly pushes in while a loading bar fills with a
 * little bird rolling along its end; then it fades to black and opens the lobby (the menu).
 * A tap skips the wait after a moment. The artwork is drawn to cover the whole display.
 */
class SplashScreen(private val game: AngryBirds) : ScreenAdapter() {

    private val viewport = ExtendViewport(UI_WIDTH, UI_HEIGHT)
    private val loadingLayout = GlyphLayout()
    private var time = 0f
    private var leaveTime = -1f

    override fun show() {
        game.font.data.setScale(game.uiFontScale * LABEL_FACTOR)
        loadingLayout.setText(game.font, "LOADING...")
        game.resetFontScale()

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (time > SKIP_AFTER) startLeaving()
                return true
            }
        }
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        time += min(delta, 0.05f)
        if (leaveTime < 0f && time >= LOAD_TIME) startLeaving()
        if (leaveTime >= 0f) {
            leaveTime += min(delta, 0.05f)
            if (leaveTime >= LEAVE_TIME) {
                game.setScreen(MenuScreen(game))
                return
            }
        }

        ScreenUtils.clear(0.53f, 0.81f, 0.92f, 1f)
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        val projection = viewport.camera.combined
        val batch = game.batch
        val shapes = game.shapes
        val assets = game.assets
        val progress = Ease.smooth(time / LOAD_TIME)

        // 1) artwork with a slow push-in
        batch.projectionMatrix = projection
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        Backdrop.draw(batch, assets.splash, w / 2f, w, h, 1f + 0.07f * min(1f, time / (LOAD_TIME + LEAVE_TIME)))
        batch.end()

        // 2) loading bar
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        val barX = (w - BAR_WIDTH) / 2f
        val barY = BAR_BOTTOM
        shapes.setColor(0f, 0f, 0f, 0.3f)
        pill(shapes, barX - 4f, barY - 8f, BAR_WIDTH + 8f, BAR_HEIGHT + 8f)
        shapes.setColor(1f, 1f, 1f, 1f)
        pill(shapes, barX - 5f, barY - 5f, BAR_WIDTH + 10f, BAR_HEIGHT + 10f)
        shapes.setColor(0.13f, 0.17f, 0.34f, 1f)
        pill(shapes, barX, barY, BAR_WIDTH, BAR_HEIGHT)
        val fillWidth = max(BAR_HEIGHT, BAR_WIDTH * progress)
        shapes.setColor(0.36f, 0.76f, 0.16f, 1f)
        pill(shapes, barX + 3f, barY + 3f, fillWidth - 6f, BAR_HEIGHT - 6f)
        shapes.setColor(0.7f, 0.95f, 0.4f, 0.85f)
        pill(shapes, barX + 8f, barY + BAR_HEIGHT * 0.52f, max(2f, fillWidth - 16f), BAR_HEIGHT * 0.26f)
        shapes.end()

        // 3) bird rolling along the end of the bar, and the label
        batch.begin()
        val bx = barX + fillWidth - 6f
        val by = barY + BAR_HEIGHT / 2f + 4f + MathUtils.sin(time * 9f) * 3f
        batch.draw(
            assets.bird,
            bx - BIRD_SIZE / 2f, by - BIRD_SIZE / 2f, BIRD_SIZE / 2f, BIRD_SIZE / 2f, BIRD_SIZE, BIRD_SIZE,
            1f, 1f, -progress * 900f, 0, 0, assets.bird.width, assets.bird.height, false, false
        )
        val font = game.font
        font.data.setScale(game.uiFontScale * LABEL_FACTOR)
        font.setColor(1f, 1f, 1f, 1f)
        font.draw(batch, loadingLayout, (w - loadingLayout.width) / 2f, barY + BAR_HEIGHT + 48f)
        game.resetFontScale()
        batch.end()

        // 4) fade from black at the start, to black at the end
        val fadeIn = 1f - Ease.smooth(time / FADE_IN_TIME)
        val fadeOut = if (leaveTime >= 0f) Ease.smooth(leaveTime / LEAVE_TIME) else 0f
        val black = max(fadeIn, fadeOut)
        if (black > 0.002f) {
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.setColor(0f, 0f, 0f, black)
            shapes.rect(0f, 0f, w, h)
            shapes.end()
        }
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun startLeaving() {
        if (leaveTime < 0f) leaveTime = 0f
    }

    /** A rounded bar (rectangle with half-circles at both ends). */
    private fun pill(shapes: ShapeRenderer, x: Float, y: Float, w: Float, h: Float) {
        val r = h / 2f
        shapes.rect(x + r, y, w - 2f * r, h)
        shapes.circle(x + r, y + r, r, 24)
        shapes.circle(x + w - r, y + r, r, 24)
    }

    private fun max(a: Float, b: Float): Float = if (a > b) a else b

    private companion object {
        const val UI_WIDTH = 1280f
        const val UI_HEIGHT = 720f

        const val LOAD_TIME = 3.2f
        const val SKIP_AFTER = 1.2f
        const val FADE_IN_TIME = 0.6f
        const val LEAVE_TIME = 0.45f

        const val BAR_WIDTH = 560f
        const val BAR_HEIGHT = 34f
        const val BAR_BOTTOM = 52f
        const val BIRD_SIZE = 76f
        const val LABEL_FACTOR = 0.5f
    }
}
