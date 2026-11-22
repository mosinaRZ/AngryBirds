package com.example.slingshot.world

/**
 * Central constants for units and physics stepping.
 * 1 world unit = 1 meter. Never mix pixels and meters.
 */
object Units {
    /** The smallest area that is always visible (meters). A wider or taller screen shows more scenery. */
    const val WORLD_WIDTH = 23.4f
    const val WORLD_HEIGHT = 10.8f

    /** Y coordinate of the top surface of the ground. */
    const val GROUND_TOP = 1.95f

    /**
     * Lower than real gravity on purpose: together with the gentler launch speed (SlingshotConfig.POWER) the
     * birds fly the same arcs more slowly, which is easier to follow and to aim.
     */
    const val GRAVITY = -5.4f

    /** Fixed physics step (seconds). */
    const val STEP = 1f / 60f

    /** Longest frame time we are willing to simulate (avoids the spiral of death). */
    const val MAX_FRAME_TIME = 0.25f

    const val VELOCITY_ITERATIONS = 8
    const val POSITION_ITERATIONS = 3
}
