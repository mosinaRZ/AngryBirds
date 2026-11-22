package com.example.slingshot.world

import com.example.slingshot.entities.Entity

/**
 * Collision impacts recorded during the physics step and consumed after it.
 * An entity is null for the ground. Storage grows rarely and is reused, so
 * recording an impact does not allocate in the steady state.
 */
class ImpactQueue {

    private var first = arrayOfNulls<Entity>(INITIAL_CAPACITY)
    private var second = arrayOfNulls<Entity>(INITIAL_CAPACITY)
    private var impulses = FloatArray(INITIAL_CAPACITY)

    var size = 0
        private set

    fun add(a: Entity?, b: Entity?, impulse: Float) {
        if (size == impulses.size) grow()
        first[size] = a
        second[size] = b
        impulses[size] = impulse
        size++
    }

    fun firstAt(index: Int): Entity? = first[index]

    fun secondAt(index: Int): Entity? = second[index]

    fun impulseAt(index: Int): Float = impulses[index]

    /** Drops all impacts and the references they hold. */
    fun clear() {
        for (i in 0 until size) {
            first[i] = null
            second[i] = null
        }
        size = 0
    }

    private fun grow() {
        val newCapacity = impulses.size * 2
        first = first.copyOf(newCapacity)
        second = second.copyOf(newCapacity)
        impulses = impulses.copyOf(newCapacity)
    }

    private companion object {
        const val INITIAL_CAPACITY = 64
    }
}
