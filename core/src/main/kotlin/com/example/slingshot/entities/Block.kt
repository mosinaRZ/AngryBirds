package com.example.slingshot.entities

import com.badlogic.gdx.physics.box2d.Body
import com.example.slingshot.world.BodyFactory
import com.example.slingshot.world.Surface

/** Block materials with their physical properties and health. */
enum class Material(val surface: Surface, val maxHp: Float, val damage: DamageProfile) {
    WOOD(Surface(density = 0.7f, friction = 0.6f, restitution = 0.1f), maxHp = 100f, damage = DamageConfig.WOOD),
    STONE(Surface(density = 2.4f, friction = 0.7f, restitution = 0.05f), maxHp = 300f, damage = DamageConfig.STONE),
    GLASS(Surface(density = 0.5f, friction = 0.3f, restitution = 0.1f), maxHp = 40f, damage = DamageConfig.GLASS),

    /** A crate that explodes when it is destroyed (see DamageSystem). */
    TNT(Surface(density = 0.8f, friction = 0.5f, restitution = 0.1f), maxHp = 20f, damage = DamageConfig.TNT)
}

/** A rectangular piece of the structure. [width] and [height] are in meters. */
class Block private constructor(
    body: Body,
    val material: Material,
    val width: Float,
    val height: Float
) : Entity(body, material.maxHp) {

    override val damageProfile: DamageProfile
        get() = material.damage

    init {
        body.userData = this
    }

    companion object {
        /** Creates a block whose center is at ([x], [y]). */
        fun create(
            bodies: BodyFactory,
            material: Material,
            x: Float,
            y: Float,
            width: Float,
            height: Float
        ): Block {
            val body = bodies.createBox(
                x = x,
                y = y,
                halfWidth = width / 2f,
                halfHeight = height / 2f,
                surface = material.surface
            )
            return Block(body, material, width, height)
        }
    }
}
