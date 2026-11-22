package com.example.slingshot.render

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

/**
 * A horizontal volume slider (a rounded track that fills up to a round knob). Used by the lobby settings panel and
 * the pause panel so both look and behave the same. It has no state: the caller owns the value (0..1), the
 * geometry and the touch handling, and uses [valueAt] to turn a touch position into a value.
 *
 * [draw] must be called between `shapes.begin(Filled)` and `shapes.end()`.
 */
object VolumeSlider {

    const val TRACK_HEIGHT = 16f
    const val KNOB_RADIUS = 20f

    /**
     * The volume for a touch at [x], for a track that starts at [trackX] and is [trackWidth] long, rounded to a whole
     * percent so the number shown next to the slider always matches the stored value.
     */
    fun valueAt(x: Float, trackX: Float, trackWidth: Float): Float {
        val raw = ((x - trackX) / trackWidth).coerceIn(0f, 1f)
        return MathUtils.round(raw * 100f) / 100f
    }

    /** The volume as a whole percent (0..100). */
    fun percent(value: Float): Int = MathUtils.round(value.coerceIn(0f, 1f) * 100f)

    /**
     * Draws the slider with the track starting at [trackX], centered vertically on [centerY].
     * [active] false (muted) draws it grey; the knob keeps showing the stored volume.
     */
    fun draw(
        shapes: ShapeRenderer, trackX: Float, centerY: Float, trackWidth: Float,
        value: Float, active: Boolean, pressed: Boolean, alpha: Float
    ) {
        val v = value.coerceIn(0f, 1f)
        val y = centerY - TRACK_HEIGHT / 2f

        // White rim and dark track.
        shapes.setColor(1f, 1f, 1f, alpha)
        pill(shapes, trackX - 3f, y - 3f, trackWidth + 6f, TRACK_HEIGHT + 6f)
        shapes.setColor(0.06f, 0.07f, 0.14f, alpha)
        pill(shapes, trackX, y, trackWidth, TRACK_HEIGHT)

        // Fill up to the knob (green while on, grey while muted) with a soft light stripe.
        val fillWidth = maxOf(TRACK_HEIGHT, trackWidth * v)
        if (active) shapes.setColor(0.36f, 0.76f, 0.16f, alpha) else shapes.setColor(0.45f, 0.48f, 0.58f, alpha)
        pill(shapes, trackX, y, fillWidth, TRACK_HEIGHT)
        shapes.setColor(1f, 1f, 1f, 0.22f * alpha)
        pill(shapes, trackX + 4f, y + TRACK_HEIGHT * 0.52f, maxOf(TRACK_HEIGHT * 0.5f, fillWidth - 8f), TRACK_HEIGHT * 0.3f)

        // Knob.
        val kx = trackX + trackWidth * v
        val radius = if (pressed) KNOB_RADIUS + 2f else KNOB_RADIUS
        shapes.setColor(0f, 0f, 0f, 0.28f * alpha)
        shapes.circle(kx, centerY - 3f, radius, 28)
        shapes.setColor(1f, 1f, 1f, alpha)
        shapes.circle(kx, centerY, radius, 28)
        if (active) shapes.setColor(0.36f, 0.76f, 0.16f, alpha) else shapes.setColor(0.45f, 0.48f, 0.58f, alpha)
        shapes.circle(kx, centerY, radius * 0.45f, 20)
    }

    /** A rounded bar (rectangle with half-circles at both ends). */
    private fun pill(shapes: ShapeRenderer, x: Float, y: Float, w: Float, h: Float) {
        val r = h / 2f
        shapes.rect(x + r, y, w - 2f * r, h)
        shapes.circle(x + r, y + r, r, 24)
        shapes.circle(x + w - r, y + r, r, 24)
    }
}
