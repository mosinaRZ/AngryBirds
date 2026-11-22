package com.example.slingshot.entities

import com.badlogic.gdx.physics.box2d.Body

/**
 * Base class of everything that owns a Box2D [Body].
 * Subclasses must set `body.userData = this` in their own init block.
 *
 * [hp] is the remaining health. Entities that cannot be damaged (the bird)
 * keep the default of infinite health and have no [damageProfile].
 *
 * Rendering never reads the body directly: it uses [renderX], [renderY] and
 * [renderAngle], which are interpolated between the last two physics steps
 * (see PhysicsWorld) so motion stays smooth at any refresh rate.
 */
abstract class Entity(
    val body: Body,
    val maxHp: Float = Float.POSITIVE_INFINITY
) {
    var hp: Float = maxHp

    val isDestroyed: Boolean
        get() = hp <= 0f

    /** Remaining health from 0 to 1 (always 1 for entities with infinite health). */
    val healthFraction: Float
        get() = if (maxHp.isInfinite()) 1f else (hp / maxHp).coerceIn(0f, 1f)

    /** While above zero, collisions do not damage this entity (set when a blast throws it; counted down by DamageSystem). */
    var damageShield: Float = 0f

    /** Null means the entity takes no damage. */
    open val damageProfile: DamageProfile?
        get() = null

    private var prevX: Float = body.position.x
    private var prevY: Float = body.position.y
    private var prevAngle: Float = body.angle

    /** Position and rotation to draw (meters, radians). */
    var renderX: Float = body.position.x
        private set
    var renderY: Float = body.position.y
        private set
    var renderAngle: Float = body.angle
        private set

    /** Remembers the current state; called before every physics step. */
    fun savePreviousState() {
        val p = body.position
        prevX = p.x
        prevY = p.y
        prevAngle = body.angle
    }

    /** Computes the draw state between the previous and the current step. [alpha] is 0..1. */
    open fun updateRenderState(alpha: Float) {
        val p = body.position
        renderX = prevX + (p.x - prevX) * alpha
        renderY = prevY + (p.y - prevY) * alpha
        renderAngle = prevAngle + (body.angle - prevAngle) * alpha
    }

    /** Draw exactly where the body is, with no interpolation. */
    protected fun snapRenderState() {
        val p = body.position
        prevX = p.x
        prevY = p.y
        prevAngle = body.angle
        renderX = p.x
        renderY = p.y
        renderAngle = body.angle
    }

    /** Draw at a custom place (used for scripted animation such as the bird's hop). */
    protected fun setRenderState(x: Float, y: Float, angle: Float) {
        renderX = x
        renderY = y
        renderAngle = angle
    }
}
