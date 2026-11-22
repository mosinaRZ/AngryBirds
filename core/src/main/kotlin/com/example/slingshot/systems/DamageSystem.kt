package com.example.slingshot.systems

import com.badlogic.gdx.physics.box2d.World
import com.example.slingshot.entities.Block
import com.example.slingshot.entities.DamageConfig
import com.example.slingshot.entities.Material
import com.example.slingshot.entities.Entity
import com.example.slingshot.level.Level
import com.example.slingshot.world.ImpactQueue
import kotlin.math.max
import kotlin.math.sqrt

/** Notified when an entity is destroyed, just before its body is removed. */
fun interface DestroyListener {
    fun onDestroyed(entity: Entity)
}

/**
 * Turns recorded impacts into damage and removes destroyed entities.
 * [update] must be called after the physics step, never from a contact callback.
 */
class DamageSystem(
    private val world: World,
    private val level: Level,
    private val impacts: ImpactQueue,
    private val events: GameEvents
) {

    /** Optional hook, used for scoring. */
    var destroyListener: DestroyListener? = null

    private var graceLeft = DamageConfig.SETTLE_GRACE_SECONDS

    fun update(delta: Float) {
        check(!world.isLocked) { "DamageSystem must run outside of the physics step" }

        tickShields(delta)

        if (graceLeft > 0f) {
            // The tower is still settling after loading: ignore its small bumps.
            graceLeft -= delta
            impacts.clear()
            return
        }

        applyImpacts()
        removeDestroyed()
    }

    private fun applyImpacts() {
        for (i in 0 until impacts.size) {
            val impulse = impacts.impulseAt(i)
            val first = impacts.firstAt(i)
            val second = impacts.secondAt(i)
            events.onImpact(impulse, first, second)
            applyTo(first, impulse)
            applyTo(second, impulse)
        }
        impacts.clear()
    }

    private fun applyTo(entity: Entity?, impulse: Float) {
        if (entity == null) return
        val profile = entity.damageProfile ?: return
        if (entity.damageShield > 0f) return
        entity.hp -= profile.damageFor(impulse)
    }

    private fun tickShields(delta: Float) {
        val blocks = level.blocks
        for (i in blocks.indices) {
            val block = blocks[i]
            if (block.damageShield > 0f) block.damageShield = max(0f, block.damageShield - delta)
        }
    }

    /**
     * A TNT crate blew up at ([x], [y]). Blocks close to it are shattered and pushed. Blocks farther away
     * survive: they are thrown away from the explosion (see [throwAway]). Enemies are pushed and damaged
     * by distance as before. Things that die from the blast are removed on the next update (this also lets
     * TNT crates set each other off).
     */
    private fun explode(x: Float, y: Float) {
        val blocks = level.blocks
        for (i in blocks.indices) blastBlock(blocks[i], x, y)
        val enemies = level.enemies
        for (i in enemies.indices) blast(enemies[i], x, y)
    }

    private fun blastBlock(block: Block, x: Float, y: Float) {
        val dx = block.body.position.x - x
        val dy = block.body.position.y - y
        val distance = max(0.15f, sqrt(dx * dx + dy * dy))
        when {
            distance <= DamageConfig.BLAST_KILL_RADIUS -> blast(block, x, y)
            distance < DamageConfig.BLAST_THROW_RADIUS -> throwAway(block, dx, dy, distance)
        }
    }

    /** Pushes a far block away from the blast center (opposite to the explosion) without hurting it. */
    private fun throwAway(block: Block, dx: Float, dy: Float, distance: Float) {
        val body = block.body
        val t = ((distance - DamageConfig.BLAST_KILL_RADIUS) /
            (DamageConfig.BLAST_THROW_RADIUS - DamageConfig.BLAST_KILL_RADIUS)).coerceIn(0f, 1f)
        val speed = DamageConfig.BLAST_THROW_SPEED * (1f - 0.5f * t)
        val push = speed * body.mass
        val lift = DamageConfig.BLAST_THROW_LIFT
        body.applyLinearImpulse(dx / distance * push, (dy / distance + lift) * push, body.worldCenter.x, body.worldCenter.y, true)
        block.damageShield = DamageConfig.BLAST_SHIELD_SECONDS
    }

    private fun blast(entity: Entity, x: Float, y: Float) {
        val body = entity.body
        val dx = body.position.x - x
        val dy = body.position.y - y
        val distance = max(0.15f, sqrt(dx * dx + dy * dy))
        val falloff = 1f - distance / DamageConfig.BLAST_RADIUS
        if (falloff <= 0f) return
        val push = DamageConfig.BLAST_SPEED * falloff * body.mass
        // A little extra upward push makes the blast look more explosive.
        body.applyLinearImpulse(dx / distance * push, dy / distance * push + 0.25f * push, body.worldCenter.x, body.worldCenter.y, true)
        val tntShare = if ((entity as? Block)?.material == Material.TNT) DamageConfig.TNT_BLAST_FACTOR else 1f
        entity.hp -= DamageConfig.BLAST_DAMAGE * falloff * tntShare
    }

    /** Index loops from the end: no iterator allocation and safe removal. */
    private fun removeDestroyed() {
        val blocks = level.blocks
        var i = blocks.size - 1
        while (i >= 0) {
            val block = blocks[i]
            if (block.isDestroyed) {
                blocks.removeAt(i)
                events.onDestroyed(block)
                destroyListener?.onDestroyed(block)
                if (block.material == Material.TNT) explode(block.body.position.x, block.body.position.y)
                world.destroyBody(block.body)
            }
            i--
        }

        val enemies = level.enemies
        i = enemies.size - 1
        while (i >= 0) {
            val enemy = enemies[i]
            if (enemy.isDestroyed) {
                enemies.removeAt(i)
                events.onDestroyed(enemy)
                destroyListener?.onDestroyed(enemy)
                world.destroyBody(enemy.body)
            }
            i--
        }
    }
}
