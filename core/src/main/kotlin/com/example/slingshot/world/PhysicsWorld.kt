package com.example.slingshot.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import kotlin.math.min

/**
 * Owns the Box2D [World] and advances it with a fixed timestep.
 *
 * Rule: bodies and joints must never be created or destroyed while the world
 * is stepping (for example inside contact callbacks). Callbacks only record
 * data (see [ImpactQueue]); the game systems apply the changes after [update]
 * has returned, still on the same frame.
 *
 * Smooth rendering: the physics runs at a fixed 60 Hz while the screen may run at
 * 90 or 120 Hz. [beforeStep] lets entities remember where they were before each step,
 * and [alpha] tells the renderer how far between the last two steps this frame is,
 * so sprites are drawn at interpolated positions instead of stuttering.
 */
class PhysicsWorld {

    val world = World(Vector2(0f, Units.GRAVITY), true)

    /** Called right before every fixed step (used to store the previous state for interpolation). */
    var beforeStep: (() -> Unit)? = null

    private var accumulator = 0f

    /** 0..1: how far the current frame is between the previous and the latest physics step. */
    val alpha: Float
        get() = (accumulator / Units.STEP).coerceIn(0f, 1f)

    /** Advances the simulation by [delta] seconds using fixed steps. */
    fun update(delta: Float) {
        accumulator += min(delta, Units.MAX_FRAME_TIME)
        while (accumulator >= Units.STEP) {
            beforeStep?.invoke()
            world.step(Units.STEP, Units.VELOCITY_ITERATIONS, Units.POSITION_ITERATIONS)
            accumulator -= Units.STEP
        }
    }

    fun dispose() {
        world.dispose()
    }
}
