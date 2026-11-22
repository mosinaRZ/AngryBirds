package com.example.slingshot.entities

import kotlin.math.max

/**
 * How an entity reacts to a collision impulse.
 * damage = max(0, impulse - [threshold]) * [factor]
 */
class DamageProfile(val threshold: Float, val factor: Float) {
    fun damageFor(impulse: Float): Float = max(0f, impulse - threshold) * factor
}

/** All damage tuning in one place. Impulses are in N*s as reported by Box2D per step. */
object DamageConfig {

    /** Contacts with a weaker impulse are not even queued (resting contacts, jitter). */
    const val MIN_RECORDED_IMPULSE = 0.1f

    /** Right after loading, the structure settles; impacts in this time are ignored. */
    const val SETTLE_GRACE_SECONDS = 2.5f

    val WOOD = DamageProfile(threshold = 0.2f, factor = 90f)
    val STONE = DamageProfile(threshold = 0.5f, factor = 100f)
    val GLASS = DamageProfile(threshold = 0.2f, factor = 90f)
    val TNT = DamageProfile(threshold = 0.2f, factor = 160f)
    val ENEMY = DamageProfile(threshold = 0.2f, factor = 110f)

    /** TNT explosion: reach (m), impulse per kilogram at the center, and damage at the center. */
    const val BLAST_RADIUS = 3.8f
    const val BLAST_SPEED = 6f
    const val BLAST_DAMAGE = 280f

    /** A TNT crate caught in a blast takes only this share of the damage (so chain reactions need a close blast). */
    const val TNT_BLAST_FACTOR = 0.45f

    /**
     * Blocks closer to the blast than this (m) are shattered as before. Blocks farther away (up to
     * [BLAST_THROW_RADIUS]) are not damaged by the blast: they are thrown away from the explosion instead.
     */
    const val BLAST_KILL_RADIUS = 1.6f

    /** Reach (m) of the throw. Speed (m/s) of a thrown block is [BLAST_THROW_SPEED] at the kill radius and fades to half at the edge. */
    const val BLAST_THROW_RADIUS = 5.6f
    const val BLAST_THROW_SPEED = 2.4f

    /** Extra upward share of the throw, so pieces fly in an arc instead of sliding. */
    const val BLAST_THROW_LIFT = 0.2f

    /** A thrown block takes no collision damage for this long (s), so it flies instead of shattering on its first hit. */
    const val BLAST_SHIELD_SECONDS = 0.6f
}
