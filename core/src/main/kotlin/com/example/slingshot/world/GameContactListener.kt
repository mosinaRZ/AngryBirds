package com.example.slingshot.world

import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import com.example.slingshot.entities.Bird
import com.example.slingshot.entities.DamageConfig
import com.example.slingshot.entities.Entity

/**
 * Records collision impulses into an [ImpactQueue].
 *
 * Callbacks run while the world is stepping, so this class only reads data and
 * enqueues; it never changes health and never creates or destroys bodies.
 * A body without an [Entity] in its userData (the ground) is recorded as null.
 */
class GameContactListener(private val impacts: ImpactQueue) : ContactListener {

    /** A bird that touches anything but another bird loses its ability (see Bird.canUseAbility). */
    override fun beginContact(contact: Contact) {
        val a = contact.fixtureA.body.userData
        val b = contact.fixtureB.body.userData
        if (a is Bird && b !is Bird) a.markCollided()
        if (b is Bird && a !is Bird) b.markCollided()
    }

    override fun endContact(contact: Contact) = Unit

    override fun preSolve(contact: Contact, oldManifold: Manifold) = Unit

    override fun postSolve(contact: Contact, impulse: ContactImpulse) {
        val strongest = strongestNormalImpulse(impulse)
        if (strongest < DamageConfig.MIN_RECORDED_IMPULSE) return

        val a = contact.fixtureA.body.userData as? Entity
        val b = contact.fixtureB.body.userData as? Entity
        if (a == null && b == null) return

        impacts.add(a, b, strongest)
    }

    private fun strongestNormalImpulse(impulse: ContactImpulse): Float {
        val normals = impulse.normalImpulses
        var strongest = 0f
        for (i in 0 until impulse.count) {
            if (normals[i] > strongest) strongest = normals[i]
        }
        return strongest
    }
}
