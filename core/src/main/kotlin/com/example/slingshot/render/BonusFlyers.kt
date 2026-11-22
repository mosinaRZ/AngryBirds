package com.example.slingshot.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import com.example.slingshot.AngryBirds
import kotlin.math.min

/**
 * The "+10000" numbers of the bird bonus. A number bursts out of a cheering bird (it pops up with a small
 * overshoot and rises a little), waits for a moment, then flies in a smooth arc to the score at the top right
 * of the screen. Only when it arrives is the score increased ([LandListener]), so the player sees exactly
 * which bird pays for which points.
 *
 * Numbers are drawn in HUD space (on top of the HUD); while one is bursting out it is glued to its world position,
 * so it follows the bird even while the camera is still zooming. The pool is fixed, nothing is allocated per frame.
 */
class BonusFlyers(
    private val game: AngryBirds,
    private val worldViewport: Viewport,
    private val hudViewport: Viewport
) {

    /** Told when a number has reached the score. */
    fun interface LandListener {
        fun onLand()
    }

    var landListener: LandListener? = null

    private class Flyer {
        var active = false
        var age = 0f
        var text = ""
        var worldX = 0f
        var worldY = 0f

        /** Where the flight starts (HUD units); follows the burst until the flight begins. */
        var startX = 0f
        var startY = 0f

        /** Where the number is drawn right now (HUD units), its size factor and its opacity. */
        var x = 0f
        var y = 0f
        var scale = 0f
        var alpha = 1f
    }

    private val flyers = Array(MAX_FLYERS) { Flyer() }
    private val layout = GlyphLayout()
    private val point = Vector2()

    /** True while at least one number is on its way. */
    val isBusy: Boolean
        get() {
            for (i in flyers.indices) if (flyers[i].active) return true
            return false
        }

    /** Lets [text] burst out at the world position ([worldX], [worldY]). */
    fun launch(worldX: Float, worldY: Float, text: String) {
        for (i in flyers.indices) {
            val f = flyers[i]
            if (f.active) continue
            f.active = true
            f.age = 0f
            f.text = text
            f.worldX = worldX
            f.worldY = worldY
            f.scale = 0f
            f.alpha = 1f
            toHud(worldX, worldY)
            f.x = point.x
            f.y = point.y
            f.startX = point.x
            f.startY = point.y
            return
        }
        // No free slot (cannot happen with the real levels): pay at once rather than lose the points.
        landListener?.onLand()
    }

    /** Moves every number; ([targetX], [targetY]) is the center of the score in HUD units. */
    fun update(delta: Float, targetX: Float, targetY: Float) {
        for (i in flyers.indices) {
            val f = flyers[i]
            if (!f.active) continue
            f.age += delta

            if (f.age < EMERGE_TIME + HOLD_TIME) {
                // Bursting out of the bird: pop up and rise a little, staying glued to the world.
                val emerge = min(1f, f.age / EMERGE_TIME)
                toHud(f.worldX, f.worldY + RISE_METERS * Ease.outCubic(emerge))
                f.x = point.x
                f.y = point.y
                f.startX = point.x
                f.startY = point.y
                f.scale = Ease.outBack(emerge)
                f.alpha = 1f
                continue
            }

            val u = (f.age - EMERGE_TIME - HOLD_TIME) / FLY_TIME
            if (u >= 1f) {
                f.active = false
                landListener?.onLand()
                continue
            }
            // A curve that first climbs, then sweeps to the right into the score; eased at both ends.
            val e = Ease.smooth(u)
            val inv = 1f - e
            f.x = f.startX + (targetX - f.startX) * e * e
            f.y = targetY + (f.startY - targetY) * inv * inv
            f.scale = MathUtils.lerp(1f, ARRIVE_SCALE, e)
            f.alpha = 1f - Ease.smooth((u - FADE_START) / (1f - FADE_START))
        }
    }

    /** Draws the numbers on top of the HUD. [camera] is the HUD camera (its viewport must be applied). */
    fun draw(camera: Camera) {
        if (!isBusy) return
        val batch = game.batch
        val font = game.font
        batch.projectionMatrix = camera.combined
        batch.begin()
        for (i in flyers.indices) {
            val f = flyers[i]
            if (!f.active || f.scale < MIN_SCALE) continue
            font.data.setScale(game.uiFontScale * FONT_FACTOR * f.scale)
            layout.setText(font, f.text)
            font.setColor(1f, 0.84f, 0.25f, f.alpha)
            font.draw(batch, layout, f.x - layout.width / 2f, f.y + layout.height / 2f)
        }
        batch.end()
        game.resetFontScale()
    }

    /** Converts a world position to HUD units; the result is left in [point]. */
    private fun toHud(worldX: Float, worldY: Float) {
        point.set(worldX, worldY)
        worldViewport.project(point)
        point.x = (point.x - hudViewport.screenX) * hudViewport.worldWidth / hudViewport.screenWidth
        point.y = (point.y - hudViewport.screenY) * hudViewport.worldHeight / hudViewport.screenHeight
    }

    companion object {
        private const val MAX_FLYERS = 8

        /** Seconds a number takes to pop out, how long it then waits, and how long the flight to the score lasts. */
        const val EMERGE_TIME = 0.3f
        const val HOLD_TIME = 0.25f
        const val FLY_TIME = 0.7f

        /** How far (meters) the number rises above the bird while it pops out. */
        private const val RISE_METERS = 0.7f

        /** Size of the number relative to the score's size at the start and at the end of the flight. */
        private const val FONT_FACTOR = 1.15f
        private const val ARRIVE_SCALE = 0.8f

        /** Share of the flight after which the number fades out while it merges into the score. */
        private const val FADE_START = 0.8f

        private const val MIN_SCALE = 0.02f
    }
}
