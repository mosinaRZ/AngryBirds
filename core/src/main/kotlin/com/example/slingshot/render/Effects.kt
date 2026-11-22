package com.example.slingshot.render

import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.example.slingshot.AngryBirds
import com.example.slingshot.entities.Material
import kotlin.math.abs
import kotlin.math.min

/** Easing helpers shared by the animations. */
object Ease {
    /** Fast start, slight overshoot, settles at 1. [t] is 0..1. */
    fun outBack(t: Float): Float {
        val x = t.coerceIn(0f, 1f) - 1f
        return 1f + x * x * (2.70158f * x + 1.70158f)
    }

    fun outCubic(t: Float): Float {
        val x = 1f - t.coerceIn(0f, 1f)
        return 1f - x * x * x
    }

    fun smooth(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}

/**
 * Short-lived visual effects drawn in world space: debris when a block breaks,
 * smoke puffs when an enemy is destroyed, and floating score numbers.
 *
 * Particles live in a fixed pool, so nothing is allocated while playing.
 */
class Effects(private val game: AngryBirds) {

    private class Particle {
        var life = 0f
        var maxLife = 1f
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var rotation = 0f
        var spin = 0f
        var size = 0.1f
        var grow = 0f
        var gravity = 0f
        var r = 1f
        var g = 1f
        var b = 1f
        var a = 1f
        var isPuff = false
        var aspect = 1f
    }

    private class Popup {
        var life = 0f
        var maxLife = 1f
        var x = 0f
        var y = 0f
        var text = ""
        var size = 1f
        var startY = 0f
    }

    private val particles = Array(MAX_PARTICLES) { Particle() }
    private val popups = Array(MAX_POPUPS) { Popup() }
    private val layout = GlyphLayout()

    /** Splinters, stone chips or glass shards flying out of a destroyed block. */
    fun spawnDebris(x: Float, y: Float, material: Material, width: Float, height: Float) {
        val count = (6 + width * height * 12f).toInt().coerceIn(6, 18)
        for (i in 0 until count) {
            val p = obtain() ?: return
            val angle = MathUtils.random(0f, MathUtils.PI2)
            val speed = MathUtils.random(1.2f, 4.2f)
            p.isPuff = false
            p.aspect = 1f
            p.x = x + MathUtils.random(-width / 2f, width / 2f) * 0.8f
            p.y = y + MathUtils.random(-height / 2f, height / 2f) * 0.8f
            p.vx = MathUtils.cos(angle) * speed
            p.vy = MathUtils.sin(angle) * speed + 1.5f
            p.rotation = MathUtils.random(0f, 360f)
            p.spin = MathUtils.random(-540f, 540f)
            p.size = MathUtils.random(0.04f, 0.11f)
            p.grow = 0f
            p.gravity = -9.8f
            p.maxLife = MathUtils.random(0.6f, 1.1f)
            p.life = p.maxLife
            val shade = MathUtils.random(0.8f, 1.1f)
            when (material) {
                Material.WOOD -> setColor(p, 0.72f * shade, 0.48f * shade, 0.24f * shade, 1f)
                Material.STONE -> setColor(p, 0.55f * shade, 0.56f * shade, 0.6f * shade, 1f)
                Material.GLASS -> setColor(p, 0.7f, 0.92f, 1f, 0.85f)
                Material.TNT -> setColor(p, 0.8f * shade, 0.45f * shade, 0.2f * shade, 1f)
            }
        }
    }

    /** A cloud of smoke when an enemy pops. */
    fun spawnPuff(x: Float, y: Float, radius: Float) {
        for (i in 0 until 9) {
            val p = obtain() ?: return
            val angle = MathUtils.random(0f, MathUtils.PI2)
            val speed = MathUtils.random(0.4f, 1.6f)
            p.isPuff = true
            p.aspect = 1f
            p.x = x + MathUtils.cos(angle) * radius * 0.5f
            p.y = y + MathUtils.sin(angle) * radius * 0.5f
            p.vx = MathUtils.cos(angle) * speed
            p.vy = MathUtils.sin(angle) * speed + 0.4f
            p.rotation = MathUtils.random(0f, 360f)
            p.spin = MathUtils.random(-60f, 60f)
            p.size = radius * MathUtils.random(0.9f, 1.4f)
            p.grow = radius * MathUtils.random(1.6f, 2.6f)
            p.gravity = 0f
            p.maxLife = MathUtils.random(0.5f, 0.85f)
            p.life = p.maxLife
            val shade = MathUtils.random(0.88f, 1f)
            setColor(p, shade, shade, shade, 0.9f)
        }
    }

    /** A fireball and smoke for a TNT explosion. */
    fun spawnExplosion(x: Float, y: Float) {
        for (i in 0 until 16) {
            val p = obtain() ?: return
            val angle = MathUtils.random(0f, MathUtils.PI2)
            val speed = MathUtils.random(1.5f, 5.5f)
            p.isPuff = true
            p.aspect = 1f
            p.x = x + MathUtils.cos(angle) * 0.2f
            p.y = y + MathUtils.sin(angle) * 0.2f
            p.vx = MathUtils.cos(angle) * speed
            p.vy = MathUtils.sin(angle) * speed
            p.rotation = MathUtils.random(0f, 360f)
            p.spin = MathUtils.random(-120f, 120f)
            p.size = MathUtils.random(0.6f, 1.1f)
            p.grow = MathUtils.random(1.8f, 3.2f)
            p.gravity = 0f
            p.maxLife = MathUtils.random(0.5f, 1.0f)
            p.life = p.maxLife
            if (i < 8) setColor(p, 1f, MathUtils.random(0.55f, 0.85f), 0.2f, 0.95f)
            else setColor(p, 0.45f, 0.42f, 0.4f, 0.85f)
        }
        spawnDebris(x, y, Material.TNT, 1.0f, 1.0f)
        spawnDebris(x, y, Material.WOOD, 1.0f, 1.0f)
    }

    /** Colorful paper pieces that fall slowly (level-end celebration). */
    fun spawnConfetti(x: Float, y: Float, count: Int, spread: Float) {
        for (i in 0 until count) {
            val p = obtain() ?: return
            p.isPuff = false
            p.aspect = 0.55f
            p.x = x + MathUtils.random(-spread, spread)
            p.y = y + MathUtils.random(-0.4f, 0.4f)
            p.vx = MathUtils.random(-1.5f, 1.5f)
            p.vy = MathUtils.random(0.5f, 4.5f)
            p.rotation = MathUtils.random(0f, 360f)
            p.spin = MathUtils.random(-420f, 420f)
            p.size = MathUtils.random(0.09f, 0.17f)
            p.grow = 0f
            p.gravity = -2.4f
            p.maxLife = MathUtils.random(2.2f, 3.6f)
            p.life = p.maxLife
            when (MathUtils.random(5)) {
                0 -> setColor(p, 1f, 0.35f, 0.35f, 1f)
                1 -> setColor(p, 1f, 0.85f, 0.2f, 1f)
                2 -> setColor(p, 0.35f, 0.85f, 0.4f, 1f)
                3 -> setColor(p, 0.35f, 0.65f, 1f, 1f)
                4 -> setColor(p, 1f, 0.55f, 0.85f, 1f)
                else -> setColor(p, 1f, 1f, 1f, 1f)
            }
        }
    }

    /**
     * A small score number that fades in, drifts up a little and fades out. [size] 1 is the normal size
     * (above 1 it is drawn larger and in a warmer color). A number that would overlap a fresh one is
     * stacked above it, so several pieces breaking at once (a TNT blast) stay readable.
     */
    fun spawnPopup(x: Float, y: Float, text: String, size: Float) {
        var py = y
        for (attempt in 0 until POPUP_STACK_TRIES) {
            var blocked = false
            for (i in popups.indices) {
                val o = popups[i]
                if (o.life <= 0f || o.maxLife - o.life > POPUP_STACK_AGE) continue
                if (abs(o.x - x) < POPUP_STACK_DX && abs(o.startY - py) < POPUP_STACK_DY) {
                    blocked = true
                    break
                }
            }
            if (!blocked) break
            py += POPUP_STACK_DY
        }
        for (i in popups.indices) {
            val p = popups[i]
            if (p.life > 0f) continue
            p.x = x
            p.y = py
            p.startY = py
            p.text = text
            p.size = size
            p.maxLife = POPUP_LIFE
            p.life = POPUP_LIFE
            return
        }
    }

    fun update(delta: Float) {
        for (i in particles.indices) {
            val p = particles[i]
            if (p.life <= 0f) continue
            p.life -= delta
            p.vy += p.gravity * delta
            p.x += p.vx * delta
            p.y += p.vy * delta
            p.rotation += p.spin * delta
            p.size += p.grow * delta
            if (p.isPuff) {
                // Smoke slows down as it spreads.
                p.vx *= 1f - min(1f, 2.5f * delta)
                p.vy *= 1f - min(1f, 2.5f * delta)
            }
        }
        for (i in popups.indices) {
            val p = popups[i]
            if (p.life <= 0f) continue
            p.life -= delta
            p.y = p.startY + POPUP_RISE * Ease.outCubic((p.maxLife - p.life) / p.maxLife)
        }
    }

    /** Draws debris and smoke. The batch must be started and use the world projection. */
    fun drawParticles(batch: SpriteBatch) {
        val pixel = game.assets.pixel
        val puff = game.assets.puff
        for (i in particles.indices) {
            val p = particles[i]
            if (p.life <= 0f) continue
            val fade = min(1f, p.life / (p.maxLife * 0.4f))
            batch.setColor(p.r, p.g, p.b, p.a * fade)
            val s = p.size
            val sh = s * p.aspect
            val texture = if (p.isPuff) puff else pixel
            batch.draw(
                texture,
                p.x - s / 2f, p.y - sh / 2f, s / 2f, sh / 2f, s, sh, 1f, 1f, p.rotation,
                0, 0, texture.width, texture.height, false, false
            )
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /** Draws the floating score numbers. The batch must be started and use the world projection. */
    fun drawPopups(batch: SpriteBatch) {
        val font = game.font
        // Glyph height of the font file in pixels, so popups have the same size in meters for any font.
        val lineHeightPx = font.data.lineHeight / font.data.scaleY
        var drewAny = false
        // Text is drawn in meters here (glyphs are a few centimeters), so positions must not be rounded to whole
        // units: with the default rounding the glyphs collapse into one spot and the number is invisible.
        val wasInteger = font.usesIntegerPositions()
        font.setUseIntegerPositions(false)
        for (i in popups.indices) {
            val p = popups[i]
            if (p.life <= 0f) continue
            drewAny = true
            val age = p.maxLife - p.life
            // Appear: quick fade-in and a soft grow. Leave: fade out over the last part of the life.
            val appear = Ease.smooth(age / POPUP_IN_TIME)
            val alpha = appear * Ease.smooth(p.life / POPUP_OUT_TIME)
            if (alpha <= 0.01f) continue
            val scale = (0.8f + 0.2f * Ease.outCubic(age / POPUP_GROW_TIME)) * p.size
            font.data.setScale(POPUP_LINE_METERS / lineHeightPx * scale)
            layout.setText(font, p.text)
            val tx = p.x - layout.width / 2f
            val ty = p.y + layout.height / 2f
            // Soft shadow keeps the number readable on any background.
            font.setColor(0f, 0f, 0f, alpha * POPUP_SHADOW_ALPHA)
            font.draw(batch, layout, tx + POPUP_SHADOW_OFFSET, ty - POPUP_SHADOW_OFFSET)
            if (p.size > 1f) font.setColor(1f, 0.9f, 0.55f, alpha) else font.setColor(1f, 1f, 1f, alpha)
            font.draw(batch, layout, tx, ty)
        }
        font.setUseIntegerPositions(wasInteger)
        if (drewAny) game.resetFontScale()
    }

    private fun obtain(): Particle? {
        for (i in particles.indices) {
            if (particles[i].life <= 0f) return particles[i]
        }
        return null
    }

    private fun setColor(p: Particle, r: Float, g: Float, b: Float, a: Float) {
        p.r = r
        p.g = g
        p.b = b
        p.a = a
    }

    private companion object {
        const val MAX_PARTICLES = 320
        const val MAX_POPUPS = 32
        const val POPUP_LIFE = 0.95f
        const val POPUP_IN_TIME = 0.12f
        const val POPUP_GROW_TIME = 0.22f
        const val POPUP_OUT_TIME = 0.35f

        /** How far (m) a number drifts up during its life. */
        const val POPUP_RISE = 0.5f
        const val POPUP_SHADOW_ALPHA = 0.5f
        const val POPUP_SHADOW_OFFSET = 0.025f

        // Stacking of numbers that appear close together.
        const val POPUP_STACK_TRIES = 6
        const val POPUP_STACK_AGE = 0.45f
        const val POPUP_STACK_DX = 0.7f
        const val POPUP_STACK_DY = 0.3f

        /** Height of one text line of a popup, in meters. */
        const val POPUP_LINE_METERS = 0.3f
    }
}
