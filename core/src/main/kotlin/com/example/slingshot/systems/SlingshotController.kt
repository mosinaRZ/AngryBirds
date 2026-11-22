package com.example.slingshot.systems

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import com.example.slingshot.entities.Bird
import com.example.slingshot.entities.BirdState
import com.example.slingshot.world.Units

/**
 * Turns touch input into aiming and launching of the current [bird].
 * Only the first finger that grabs the bird is tracked.
 */
class SlingshotController(
    private val viewport: Viewport,
    private val events: GameEvents
) : InputAdapter() {

    /** The bird that can currently be aimed, or null. */
    var bird: Bird? = null

    private var activePointer = NO_POINTER
    private var abilityRequested = false

    // Reused to avoid allocations while input events arrive.
    private val touch = Vector2()
    private val pull = Vector2()
    private val impulse = Vector2()

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val current = bird ?: return false
        // A tap while a yellow or blue bird is flying triggers its ability.
        if (current.state == BirdState.FLYING) {
            if (!current.canUseAbility) return false
            abilityRequested = true
            return true
        }
        if (activePointer != NO_POINTER || current.state != BirdState.READY || current.isHopping) return false

        toWorld(screenX, screenY)
        if (touch.dst(current.body.position) > SlingshotConfig.GRAB_RADIUS) return false

        activePointer = pointer
        current.beginDrag()
        updatePull(current)
        events.onStretch()
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val current = bird ?: return false
        if (pointer != activePointer || current.state != BirdState.DRAGGING) return false

        toWorld(screenX, screenY)
        updatePull(current)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val current = bird ?: return false
        if (pointer != activePointer) return false

        activePointer = NO_POINTER
        if (current.state != BirdState.DRAGGING) return true

        if (pull.len() >= SlingshotConfig.MIN_PULL) {
            // Lighter birds get a smaller impulse, so every bird leaves at the same speed.
            impulse.set(pull).scl(-SlingshotConfig.POWER * current.mass / Bird.REFERENCE_MASS)
            current.launch(impulse)
        } else {
            current.returnToSlingshot()
        }
        return true
    }

    /** True once if the player tapped for an ability since the last call. */
    fun consumeAbilityRequest(): Boolean {
        val requested = abilityRequested
        abilityRequested = false
        return requested
    }

    /** Aborts a drag in progress (for example when the app is paused). */
    fun cancelDrag() {
        activePointer = NO_POINTER
        val current = bird ?: return
        if (current.state == BirdState.DRAGGING) current.returnToSlingshot()
    }

    private fun toWorld(screenX: Int, screenY: Int) {
        viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
    }

    /** Computes the clamped pull vector from [touch] and moves the bird there. */
    private fun updatePull(current: Bird) {
        pull.set(touch).sub(SlingshotConfig.ANCHOR_X, SlingshotConfig.ANCHOR_Y)
        if (pull.len() > SlingshotConfig.MAX_PULL) pull.setLength(SlingshotConfig.MAX_PULL)

        // Keep the bird above the ground while it is being pulled.
        val minPullY = Units.GROUND_TOP + current.radius - SlingshotConfig.ANCHOR_Y
        if (pull.y < minPullY) pull.y = minPullY

        current.holdAt(
            SlingshotConfig.ANCHOR_X + pull.x,
            SlingshotConfig.ANCHOR_Y + pull.y
        )
    }

    private companion object {
        const val NO_POINTER = -1
    }
}
