package com.example.slingshot.systems

import com.example.slingshot.world.Units

/** Slingshot geometry and launch tuning. All values are in meters. */
object SlingshotConfig {
    /** Top of the wooden bench the slingshot stands on. */
    const val TABLE_TOP = 2.48f

    /** Width of the slingshot picture; its height follows the texture's aspect ratio. */
    const val SLINGSHOT_WIDTH = 0.9f

    /** Rest position of the bird (between the two fork tips). */
    const val ANCHOR_X = 5.85f
    const val ANCHOR_Y = TABLE_TOP + 1.62f

    /** Longest allowed pull distance from the anchor. */
    const val MAX_PULL = 1.9f

    /** Pulls shorter than this do not fire; the bird returns to the anchor. */
    const val MIN_PULL = 0.3f

    /** Launch impulse = pull vector * -POWER. With the bird mass of ~0.29 kg, max pull gives ~12.6 m/s. */
    const val POWER = 1.95f

    /** A touch this close to the bird starts a drag (generous for fingers). */
    const val GRAB_RADIUS = 0.9f

    /** Where the two rubber band halves are attached to the fork tips. */
    const val BACK_BAND_X = ANCHOR_X - 0.225f
    const val BACK_BAND_Y = TABLE_TOP + 1.93f
    const val FRONT_BAND_X = ANCHOR_X + 0.225f
    const val FRONT_BAND_Y = TABLE_TOP + 1.82f

    /** Bench (picture and spot for waiting birds): left and right edge, and its height above the ground. */
    const val BENCH_LEFT = 4.03f
    const val BENCH_RIGHT = 6.0f

    // Waiting places of the birds that have not been fired yet, in the order they hop onto the slingshot:
    // two on the bench, two on the ground in front of it.
    private val WAIT_X = floatArrayOf(5.05f, 4.45f, 3.65f, 2.95f)
    private val WAIT_BASE_Y = floatArrayOf(TABLE_TOP, TABLE_TOP, Units.GROUND_TOP, Units.GROUND_TOP)

    /** X of the waiting place of the bird with the given index (index 0 starts on the slingshot, 1 is the next one). */
    fun waitingX(index: Int): Float = WAIT_X[(index - 1).coerceIn(0, WAIT_X.size - 1)]

    /** Height of the surface the waiting bird with the given index stands on. */
    fun waitingBaseY(index: Int): Float = WAIT_BASE_Y[(index - 1).coerceIn(0, WAIT_BASE_Y.size - 1)]
}
