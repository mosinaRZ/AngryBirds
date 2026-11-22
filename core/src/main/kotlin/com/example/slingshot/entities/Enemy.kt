package com.example.slingshot.entities

import com.badlogic.gdx.physics.box2d.Body
import com.example.slingshot.world.BodyFactory
import com.example.slingshot.world.Surface

/** A circular enemy ("pig"). The renderer draws its body and the swappable head image. */
class Enemy private constructor(
    body: Body,
    val radius: Float
) : Entity(body, MAX_HP) {

    override val damageProfile: DamageProfile
        get() = DamageConfig.ENEMY

    init {
        body.userData = this
        body.angularDamping = ANGULAR_DAMPING
    }

    companion object {
        const val DEFAULT_RADIUS = 0.33f

        private const val MAX_HP = 44f
        private const val ANGULAR_DAMPING = 0.5f
        private val SURFACE = Surface(density = 1f, friction = 0.6f, restitution = 0.1f)

        /** Creates an enemy whose center is at ([x], [y]). */
        fun create(bodies: BodyFactory, x: Float, y: Float, radius: Float = DEFAULT_RADIUS): Enemy {
            val body = bodies.createCircle(x = x, y = y, radius = radius, surface = SURFACE)
            return Enemy(body, radius)
        }
    }
}
