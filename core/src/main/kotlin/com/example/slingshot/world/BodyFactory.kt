package com.example.slingshot.world

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Shape
import com.badlogic.gdx.physics.box2d.World

/** Physical surface properties used when creating fixtures. */
class Surface(val density: Float, val friction: Float, val restitution: Float) {
    companion object {
        val GROUND = Surface(density = 0f, friction = 0.8f, restitution = 0f)
        val BIRD = Surface(density = 1.5f, friction = 0.6f, restitution = 0.3f)
    }
}

/**
 * Creates Box2D bodies. Must not be used while the world is stepping.
 * Entities attach themselves to their body (`body.userData = this`); the ground
 * has no userData, which the contact listener treats as "no entity".
 */
class BodyFactory(private val world: World) {

    /** A wide static ground whose top surface is at [Units.GROUND_TOP]. */
    fun createGround(): Body {
        val halfHeight = GROUND_THICKNESS / 2f
        return createBody(
            type = BodyDef.BodyType.StaticBody,
            x = Units.WORLD_WIDTH / 2f,
            y = Units.GROUND_TOP - halfHeight,
            surface = Surface.GROUND,
            shape = PolygonShape().apply { setAsBox(GROUND_HALF_WIDTH, halfHeight) }
        )
    }

    /** A static, ground-like convex polygon (terrain). [vertices] is x0, y0, x1, y1, ... in world meters. */
    fun createStaticPolygon(vertices: FloatArray): Body = createBody(
        type = BodyDef.BodyType.StaticBody,
        x = 0f,
        y = 0f,
        surface = Surface.GROUND,
        shape = PolygonShape().apply { set(vertices) }
    )

    fun createBox(
        x: Float,
        y: Float,
        halfWidth: Float,
        halfHeight: Float,
        surface: Surface
    ): Body = createBody(
        type = BodyDef.BodyType.DynamicBody,
        x = x,
        y = y,
        surface = surface,
        shape = PolygonShape().apply { setAsBox(halfWidth, halfHeight) }
    )

    fun createCircle(
        x: Float,
        y: Float,
        radius: Float,
        surface: Surface,
        type: BodyDef.BodyType = BodyDef.BodyType.DynamicBody
    ): Body = createBody(
        type = type,
        x = x,
        y = y,
        surface = surface,
        shape = CircleShape().apply { this.radius = radius }
    )

    /** Builds the body, attaches the fixture and always disposes [shape]. */
    private fun createBody(
        type: BodyDef.BodyType,
        x: Float,
        y: Float,
        surface: Surface,
        shape: Shape
    ): Body {
        check(!world.isLocked) { "Cannot create bodies while the world is stepping" }
        try {
            val bodyDef = BodyDef().apply {
                this.type = type
                position.set(x, y)
            }
            val body = world.createBody(bodyDef)
            val fixtureDef = FixtureDef().apply {
                this.shape = shape
                density = surface.density
                friction = surface.friction
                restitution = surface.restitution
            }
            body.createFixture(fixtureDef)
            return body
        } finally {
            shape.dispose()
        }
    }

    private companion object {
        const val GROUND_THICKNESS = 1f
        const val GROUND_HALF_WIDTH = 30f
    }
}
