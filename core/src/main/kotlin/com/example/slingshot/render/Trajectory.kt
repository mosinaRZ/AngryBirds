package com.example.slingshot.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import com.example.slingshot.assets.Assets
import com.example.slingshot.entities.Bird
import com.example.slingshot.systems.SlingshotConfig
import com.example.slingshot.world.Units

/**
 * The dotted prediction shown while the player is aiming.
 *
 * It integrates the flight with the same rules Box2D uses (gravity, then linear
 * damping, then movement, one fixed step at a time) so the dots follow the real
 * path of the bird. The dots slowly flow forward along the arc and fade out with distance.
 * Obstacles are ignored: the path stops where the bird would touch the ground.
 */
class AimPreview {

    private val xs = FloatArray(MAX_DOTS)
    private val ys = FloatArray(MAX_DOTS)
    private var count = 0

    /** Hides the dots (no bird is being aimed). */
    fun clear() {
        count = 0
    }

    /** Predicts the path of a bird released at ([birdX], [birdY]). [time] drives the flowing animation. */
    fun update(birdX: Float, birdY: Float, time: Float) {
        count = 0
        val pullX = birdX - SlingshotConfig.ANCHOR_X
        val pullY = birdY - SlingshotConfig.ANCHOR_Y
        if (pullX * pullX + pullY * pullY < SlingshotConfig.MIN_PULL * SlingshotConfig.MIN_PULL) return

        // Launch speed = impulse / mass, the same maths as SlingshotController + Box2D.
        var vx = -pullX * SlingshotConfig.POWER / Bird.REFERENCE_MASS
        var vy = -pullY * SlingshotConfig.POWER / Bird.REFERENCE_MASS
        var x = birdX
        var y = birdY

        val h = Units.STEP
        val damping = (1f - h * Bird.LINEAR_DAMPING).coerceAtLeast(0f)
        val phase = (time * FLOW_SPEED) % 1f
        var elapsed = 0f
        var nextDot = phase * DOT_INTERVAL
        while (count < MAX_DOTS && elapsed < MAX_TIME) {
            vy += Units.GRAVITY * h
            vx *= damping
            vy *= damping
            x += vx * h
            y += vy * h
            elapsed += h
            if (y < Units.GROUND_TOP + Bird.RADIUS) break
            if (elapsed >= nextDot) {
                xs[count] = x
                ys[count] = y
                count++
                nextDot += DOT_INTERVAL
            }
        }
    }

    /** Draws the dots; the batch must be started. */
    fun draw(batch: SpriteBatch, dot: Texture) {
        for (i in 0 until count) {
            val fraction = i / MAX_DOTS.toFloat()
            val alpha = 1f - fraction * 0.85f
            val size = MathUtils.lerp(DOT_SIZE_NEAR, DOT_SIZE_FAR, fraction)
            batch.setColor(1f, 1f, 1f, alpha)
            batch.draw(dot, xs[i] - size / 2f, ys[i] - size / 2f, size, size)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private companion object {
        const val MAX_DOTS = 6
        const val MAX_TIME = 1f
        const val DOT_INTERVAL = 0.07f
        const val FLOW_SPEED = 1.4f
        const val DOT_SIZE_NEAR = 0.17f
        const val DOT_SIZE_FAR = 0.10f
    }
}

/**
 * The path the last fired bird has actually flown, left behind as white dots (like the trail in the original game).
 *
 * - Up to [MAX_STREAMS] birds can leave a trail at the same time (the main bird and the two extra birds of a
 *   blue bird's split), each with its own spacing, so all three paths of a split are drawn.
 * - All dots are white, whatever the bird is.
 * - The place where an ability was used gets a small white cloud instead of an icon: a cluster of soft puffs,
 *   a little bigger than a dot, that pops onto the path once and stays there.
 * Only the last shot is kept: [clear] wipes the dots and clouds when the next bird is launched, and the trail stays on
 * screen (also at the end of the attempt) until then.
 */
class FlightTrail {

    private val xs = FloatArray(MAX_DOTS)
    private val ys = FloatArray(MAX_DOTS)
    private val born = FloatArray(MAX_DOTS)
    private var count = 0

    private val recording = BooleanArray(MAX_STREAMS)
    private val touched = BooleanArray(MAX_STREAMS)
    private val lastX = FloatArray(MAX_STREAMS)
    private val lastY = FloatArray(MAX_STREAMS)

    private val cloudX = FloatArray(MAX_CLOUDS)
    private val cloudY = FloatArray(MAX_CLOUDS)
    private val cloudBorn = FloatArray(MAX_CLOUDS)
    private var cloudCount = 0

    /** Forgets every dot and cloud: a new shot starts a new trail. */
    fun clear() {
        count = 0
        cloudCount = 0
        for (i in recording.indices) recording[i] = false
    }

    /** Call once at the start of every frame's tracking, before the [record] calls. */
    fun beginFrame() {
        for (i in touched.indices) touched[i] = false
    }

    /** Call after the [record] calls: streams that were not recorded this frame start fresh next time. */
    fun endFrame() {
        for (i in recording.indices) if (!touched[i]) recording[i] = false
    }

    /** Records one bird's position; [stream] is 0 for the main bird, 1 and 2 for the extra birds. */
    fun record(stream: Int, x: Float, y: Float, time: Float) {
        if (stream !in 0 until MAX_STREAMS) return
        touched[stream] = true
        if (!recording[stream]) {
            recording[stream] = true
            lastX[stream] = x
            lastY[stream] = y
            add(x, y, time)
            return
        }
        val dx = x - lastX[stream]
        val dy = y - lastY[stream]
        if (dx * dx + dy * dy >= SPACING * SPACING) {
            lastX[stream] = x
            lastY[stream] = y
            add(x, y, time)
        }
    }

    /** Puts a cloud on the path where an ability was used. */
    fun addCloud(x: Float, y: Float, time: Float) {
        if (cloudCount >= MAX_CLOUDS) return
        cloudX[cloudCount] = x
        cloudY[cloudCount] = y
        cloudBorn[cloudCount] = time
        cloudCount++
    }

    /** Draws all dots and clouds; the batch must be started. */
    fun draw(batch: SpriteBatch, assets: Assets, time: Float) {
        val dot = assets.dot
        batch.setColor(1f, 1f, 1f, TRAIL_ALPHA)
        for (i in 0 until count) {
            val pop = Ease.outBack((time - born[i]) / POP_TIME).coerceAtLeast(0f)
            val size = DOT_SIZE * pop
            batch.draw(dot, xs[i] - size / 2f, ys[i] - size / 2f, size, size)
        }
        for (i in 0 until cloudCount) drawCloud(batch, assets.puff, i, time)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /** A cloud is a cluster of overlapping soft puffs; it grows in once, with a slight overshoot. */
    private fun drawCloud(batch: SpriteBatch, puff: Texture, index: Int, time: Float) {
        val pop = Ease.outBack((time - cloudBorn[index]) / CLOUD_POP_TIME).coerceAtLeast(0f)
        if (pop <= 0f) return
        val size = CLOUD_SIZE * pop
        val x = cloudX[index]
        val y = cloudY[index]
        batch.setColor(1f, 1f, 1f, TRAIL_ALPHA)
        var i = 0
        while (i < CLOUD_PARTS.size) {
            val part = CLOUD_PARTS[i + 2] * size
            batch.draw(
                puff,
                x + CLOUD_PARTS[i] * size - part / 2f,
                y + CLOUD_PARTS[i + 1] * size - part / 2f,
                part,
                part
            )
            i += 3
        }
    }

    private fun add(x: Float, y: Float, time: Float) {
        if (count >= MAX_DOTS) return
        xs[count] = x
        ys[count] = y
        born[count] = time
        count++
    }

    companion object {
        private const val MAX_STREAMS = 3
        private const val MAX_CLOUDS = 8
        private const val MAX_DOTS = 900
        private const val SPACING = 0.4f
        private const val DOT_SIZE = 0.15f
        private const val POP_TIME = 0.18f
        private const val TRAIL_ALPHA = 0.95f

        /** Overall width of a cloud in meters: a little bigger than the dots. */
        private const val CLOUD_SIZE = 0.36f
        private const val CLOUD_POP_TIME = 0.3f

        /** Puffs of the cloud as (x, y, size), relative to [CLOUD_SIZE]. */
        private val CLOUD_PARTS = floatArrayOf(
            0f, 0.04f, 0.62f,
            -0.24f, -0.06f, 0.46f,
            0.24f, -0.06f, 0.46f,
            -0.12f, -0.14f, 0.42f,
            0.12f, -0.14f, 0.42f
        )
    }
}
