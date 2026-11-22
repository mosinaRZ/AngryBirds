package com.example.slingshot.render

import com.badlogic.gdx.math.MathUtils

/**
 * The cheering motion of an unused bird when its bonus is counted: a short crouch, one high jump (the bird
 * stretches in the air and squashes when it lands), then small happy hops that grow in gently.
 *
 * Every value is a continuous function of the time since the bird was counted, so the bird never snaps
 * from one pose to another, whatever it was doing before. One instance is reused for every bird.
 */
class CheerPose {

    /** Height above the resting place, in meters. */
    var lift = 0f
        private set

    /** Squash and stretch around the point where the bird touches the ground (1 = normal). */
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set

    /** Rotation in radians. */
    var tilt = 0f
        private set

    /** Computes the pose [age] seconds after the bird was counted; a negative age is the neutral pose. */
    fun update(age: Float) {
        lift = 0f
        scaleX = 1f
        scaleY = 1f
        tilt = 0f
        if (age <= 0f) return

        val stretch = stretchAt(age)
        scaleY = 1f + stretch
        scaleX = 1f - stretch * WIDTH_FOLLOW

        val air = age - TAKE_OFF
        if (air < 0f) return
        if (air < AIR_TIME) {
            // One parabola: fast at the start, slow at the top, fast again on the way down.
            val u = air / AIR_TIME
            lift = 4f * JUMP_HEIGHT * u * (1f - u)
            tilt = JUMP_TILT * MathUtils.sin(u * MathUtils.PI2)
            return
        }

        val hop = air - AIR_TIME - SETTLE_TIME
        if (hop <= 0f) return
        val strength = Ease.smooth(hop / HOP_RAMP)
        val phase = (hop / HOP_PERIOD) % 1f
        lift = HOP_HEIGHT * strength * 4f * phase * (1f - phase)
        tilt = HOP_TILT * strength * MathUtils.sin(phase * MathUtils.PI2)
    }

    /** Positive: tall and thin; negative: low and wide. Eased between the keyframes below. */
    private fun stretchAt(age: Float): Float {
        if (age >= KEY_TIME[KEY_TIME.size - 1]) return 0f
        var i = 1
        while (age > KEY_TIME[i]) i++
        val t = (age - KEY_TIME[i - 1]) / (KEY_TIME[i] - KEY_TIME[i - 1])
        return KEY_STRETCH[i - 1] + (KEY_STRETCH[i] - KEY_STRETCH[i - 1]) * Ease.smooth(t)
    }

    companion object {
        /** Seconds of crouching before the bird leaves the ground. */
        const val TAKE_OFF = 0.12f

        /** Seconds the big jump lasts, and how high it goes (meters). */
        const val AIR_TIME = 0.55f
        const val JUMP_HEIGHT = 0.95f

        /** Seconds after the bird was counted when it is at the top of its jump. */
        const val APEX_TIME = TAKE_OFF + AIR_TIME / 2f

        /** Seconds after landing before the small hops begin (the landing squash settles meanwhile). */
        private const val SETTLE_TIME = 0.38f

        private const val JUMP_TILT = 0.2f
        private const val WIDTH_FOLLOW = 0.7f

        private const val HOP_HEIGHT = 0.16f
        private const val HOP_PERIOD = 0.46f
        private const val HOP_RAMP = 0.3f
        private const val HOP_TILT = 0.08f

        // Squash and stretch keyframes (seconds after counting, amount):
        // crouch, take-off stretch, back to normal in the air, a little stretch before landing,
        // landing squash, rebound, settle.
        private val KEY_TIME = floatArrayOf(0f, 0.12f, 0.20f, 0.42f, 0.62f, 0.67f, 0.72f, 0.80f, 0.92f, 1.05f)
        private val KEY_STRETCH = floatArrayOf(0f, -0.16f, 0.14f, 0f, 0.05f, 0.06f, -0.20f, 0.05f, -0.02f, 0f)
    }
}
