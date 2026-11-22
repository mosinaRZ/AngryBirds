package com.example.slingshot.render

import kotlin.math.abs
import kotlin.math.max

/**
 * A number that counts up (or down) to its target instead of jumping: it covers the distance quickly at first
 * and settles smoothly, never overshooting. Used for every number the player watches change.
 *
 * [rate] is the share of the remaining distance covered per second; [minSpeed] (units per second) keeps small
 * differences from crawling.
 */
class CountUp(private val rate: Float = 5f, private val minSpeed: Float = 40f) {

    private var value = 0f

    var target = 0
        private set

    /** The whole number to display right now. */
    val shown: Int
        get() = (value + 0.5f).toInt()

    /** The exact (fractional) value being displayed, for smooth bars. */
    val current: Float
        get() = value

    /** True while the displayed number has not reached the target yet. */
    val isCounting: Boolean
        get() = shown != target

    /** Jumps to [number] without counting. */
    fun snap(number: Int) {
        value = number.toFloat()
        target = number
    }

    /** Starts counting towards [number] from wherever the display is now. */
    fun countTo(number: Int) {
        target = number
    }

    fun update(delta: Float) {
        val diff = target - value
        if (diff == 0f) return
        val step = max(abs(diff) * rate, minSpeed) * delta
        value = if (abs(diff) <= step) target.toFloat() else value + (if (diff > 0f) step else -step)
    }
}
