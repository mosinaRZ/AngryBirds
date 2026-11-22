package com.example.slingshot.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.MathUtils
import kotlin.math.cos
import kotlin.math.sin

/** Creates a fully transparent pixmap (new pixmaps are not guaranteed to be cleared). */
internal fun blankPixmap(width: Int, height: Int): Pixmap {
    val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
    pixmap.setColor(0f, 0f, 0f, 0f)
    pixmap.fill()
    return pixmap
}

/**
 * Simple generated images used when a real asset file is missing, so the game
 * always runs. Pixmap y = 0 is the top row.
 */
internal object PlaceholderArt {

    fun background(): Pixmap {
        val pixmap = blankPixmap(2, 256)
        for (y in 0 until 256) {
            val t = y / 255f
            pixmap.setColor(
                MathUtils.lerp(0.53f, 0.80f, t),
                MathUtils.lerp(0.81f, 0.92f, t),
                MathUtils.lerp(0.92f, 0.97f, t),
                1f
            )
            pixmap.fillRectangle(0, y, 2, 1)
        }
        return pixmap
    }

    fun ground(): Pixmap {
        val pixmap = blankPixmap(128, 128)
        pixmap.setColor(0.45f, 0.30f, 0.15f, 1f)
        pixmap.fill()
        pixmap.setColor(0.30f, 0.55f, 0.20f, 1f)
        pixmap.fillRectangle(0, 0, 128, 36)
        pixmap.setColor(0.22f, 0.42f, 0.14f, 1f)
        pixmap.fillRectangle(0, 36, 128, 6)
        return pixmap
    }

    /** Red bird; the eye and beak are on the right so its rotation is visible. */
    fun bird(): Pixmap {
        val pixmap = blankPixmap(128, 128)
        pixmap.setColor(0.85f, 0.15f, 0.12f, 1f)
        pixmap.fillCircle(64, 64, 60)
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.fillCircle(84, 48, 14)
        pixmap.setColor(0.05f, 0.05f, 0.05f, 1f)
        pixmap.fillCircle(88, 48, 6)
        pixmap.setColor(0.95f, 0.75f, 0.10f, 1f)
        pixmap.fillTriangle(104, 58, 104, 74, 124, 66)
        return pixmap
    }

    fun enemyBody(): Pixmap {
        val pixmap = blankPixmap(128, 128)
        pixmap.setColor(0.45f, 0.75f, 0.30f, 1f)
        pixmap.fillCircle(64, 64, 62)
        pixmap.setColor(0.25f, 0.50f, 0.15f, 1f)
        pixmap.drawCircle(64, 64, 62)
        pixmap.drawCircle(64, 64, 61)
        return pixmap
    }

    /** A smiley, shown until the real enemy_head.png is provided. */
    fun enemyHead(): Pixmap {
        val pixmap = blankPixmap(128, 128)
        pixmap.setColor(1f, 0.85f, 0.15f, 1f)
        pixmap.fillCircle(64, 64, 62)
        pixmap.setColor(0.05f, 0.05f, 0.05f, 1f)
        pixmap.fillCircle(44, 50, 8)
        pixmap.fillCircle(84, 50, 8)
        for (degrees in 25..155 step 5) {
            val radians = degrees * MathUtils.degreesToRadians
            pixmap.fillCircle(
                (64f + 34f * cos(radians)).toInt(),
                (74f + 24f * sin(radians)).toInt(),
                3
            )
        }
        return pixmap
    }

    /** A flat colored block with a darker border and one plank line. */
    fun block(r: Float, g: Float, b: Float): Pixmap {
        val pixmap = blankPixmap(64, 64)
        pixmap.setColor(r * 0.6f, g * 0.6f, b * 0.6f, 1f)
        pixmap.fill()
        pixmap.setColor(r, g, b, 1f)
        pixmap.fillRectangle(4, 4, 56, 56)
        pixmap.setColor(r * 0.8f, g * 0.8f, b * 0.8f, 1f)
        pixmap.drawLine(4, 32, 59, 32)
        return pixmap
    }

    /** A brown Y-shaped fork; the two prong tips sit where the rubber band attaches. */
    fun slingshot(): Pixmap {
        val pixmap = blankPixmap(128, 256)
        pixmap.setColor(0.42f, 0.26f, 0.12f, 1f)
        pixmap.fillRectangle(56, 110, 16, 146)
        for (dx in -6..6) {
            pixmap.drawLine(64 + dx, 115, 24 + dx, 20)
            pixmap.drawLine(64 + dx, 115, 104 + dx, 36)
        }
        return pixmap
    }

    /** A single transparent pixel, used where an optional overlay image is missing. */
    fun empty(): Pixmap = blankPixmap(4, 4)

    /** One white pixel; tinted and stretched to draw plain rectangles with the sprite batch. */
    fun pixel(): Pixmap {
        val pixmap = Pixmap(4, 4, Pixmap.Format.RGBA8888)
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.fill()
        return pixmap
    }

    fun dot(): Pixmap {
        val pixmap = blankPixmap(32, 32)
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.fillCircle(16, 16, 13)
        return pixmap
    }

    fun puff(): Pixmap {
        val pixmap = blankPixmap(64, 64)
        pixmap.setColor(1f, 1f, 1f, 0.7f)
        pixmap.fillCircle(32, 32, 28)
        return pixmap
    }

    fun star(on: Boolean): Pixmap {
        val pixmap = blankPixmap(64, 64)
        if (on) pixmap.setColor(1f, 0.82f, 0.15f, 1f) else pixmap.setColor(0.3f, 0.33f, 0.4f, 0.9f)
        pixmap.fillCircle(32, 32, 28)
        return pixmap
    }

    fun panel(): Pixmap {
        val pixmap = blankPixmap(64, 48)
        pixmap.setColor(0.15f, 0.22f, 0.38f, 0.95f)
        pixmap.fill()
        return pixmap
    }

    fun button(r: Float, g: Float, b: Float): Pixmap {
        val pixmap = blankPixmap(64, 24)
        pixmap.setColor(r * 0.6f, g * 0.6f, b * 0.6f, 1f)
        pixmap.fill()
        pixmap.setColor(r, g, b, 1f)
        pixmap.fillRectangle(2, 2, 60, 20)
        return pixmap
    }
}
