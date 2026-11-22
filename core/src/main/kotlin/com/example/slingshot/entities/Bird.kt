package com.example.slingshot.entities

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.example.slingshot.systems.SlingshotConfig
import com.example.slingshot.world.BodyFactory
import com.example.slingshot.world.Surface
import kotlin.math.min

/** Life cycle of a bird: waiting on the slingshot, being pulled, flying. */
enum class BirdState { READY, DRAGGING, FLYING }

/**
 * The projectile. While [BirdState.READY] or [BirdState.DRAGGING] its body is static, so it is
 * moved by hand and ignores gravity and collisions. [launch] turns it into a normal dynamic body.
 *
 * A bird that is not the first one hops from its waiting place on the ground to the
 * slingshot; during that [isHopping] animation it cannot be grabbed.
 *
 * Yellow and blue birds have a one-time ability while flying ([canUseAbility]); the game flow
 * (GameStateManager) carries it out. Birds created by the blue bird's split are born flying
 * with the ability already used.
 */
class Bird private constructor(
    body: Body,
    val type: BirdType,
    hopFromIndex: Int?,
    startsFlying: Boolean
) : Entity(body) {

    var state = if (startsFlying) BirdState.FLYING else BirdState.READY
        private set

    /** Radius of this bird's physics circle. */
    val radius: Float
        get() = type.radius

    /** Mass of this bird (a static body reports 0, so it is computed from the surface). */
    val mass: Float = Surface.BIRD.density * MathUtils.PI * type.radius * type.radius

    var abilityUsed = false
        private set

    /** True once this bird has touched anything (blocks, pigs, terrain, ground) in flight. */
    var hasCollided = false
        private set

    /** Seconds this bird has been (almost) at rest; maintained by the game flow. */
    var restTime = 0f

    /** True while this bird is in flight, has not hit anything yet and still has an ability to use. */
    val canUseAbility: Boolean
        get() = state == BirdState.FLYING && !abilityUsed && !hasCollided && type != BirdType.RED

    private val hopStartX: Float =
        if (hopFromIndex == null) SlingshotConfig.ANCHOR_X else SlingshotConfig.waitingX(hopFromIndex)
    private val hopStartY: Float =
        if (hopFromIndex == null) SlingshotConfig.ANCHOR_Y else SlingshotConfig.waitingBaseY(hopFromIndex) + type.radius
    private var hopTime: Float = if (hopFromIndex == null) HOP_DURATION else 0f

    /** True while the bird is still jumping onto the slingshot. */
    val isHopping: Boolean
        get() = hopTime < HOP_DURATION

    init {
        body.userData = this
        body.linearDamping = LINEAR_DAMPING
        body.angularDamping = ANGULAR_DAMPING
    }

    fun markAbilityUsed() {
        abilityUsed = true
    }

    /** Called by the contact listener the first time this bird touches something other than a bird. */
    fun markCollided() {
        hasCollided = true
    }

    /** Advances the hop animation. */
    fun updateHop(delta: Float) {
        if (hopTime < HOP_DURATION) hopTime = min(HOP_DURATION, hopTime + delta)
    }

    override fun updateRenderState(alpha: Float) {
        when {
            state == BirdState.FLYING -> super.updateRenderState(alpha)
            isHopping -> {
                // Jump along a parabola with one forward roll.
                val u = hopTime / HOP_DURATION
                val x = hopStartX + (SlingshotConfig.ANCHOR_X - hopStartX) * u
                val y = hopStartY + (SlingshotConfig.ANCHOR_Y - hopStartY) * u + HOP_HEIGHT * 4f * u * (1f - u)
                setRenderState(x, y, -MathUtils.PI2 * u)
            }
            else -> snapRenderState()
        }
    }

    fun beginDrag() {
        check(state == BirdState.READY) { "Bird can only be dragged from READY, was $state" }
        state = BirdState.DRAGGING
    }

    /** Moves the held bird. Only valid while it has not been launched. */
    fun holdAt(x: Float, y: Float) {
        check(state != BirdState.FLYING) { "Cannot move a flying bird by hand" }
        body.setTransform(x, y, 0f)
    }

    /** Puts the bird back on the slingshot anchor without launching it. */
    fun returnToSlingshot() {
        holdAt(SlingshotConfig.ANCHOR_X, SlingshotConfig.ANCHOR_Y)
        state = BirdState.READY
    }

    /** Releases the bird with the given linear [impulse]. */
    fun launch(impulse: Vector2) {
        check(state == BirdState.DRAGGING) { "Bird can only be launched from DRAGGING, was $state" }
        state = BirdState.FLYING
        body.type = BodyDef.BodyType.DynamicBody
        body.isBullet = true
        body.applyLinearImpulse(impulse, body.worldCenter, true)
    }

    companion object {
        /** Radius of the standard bird; the slingshot geometry and the aiming preview are based on it. */
        const val RADIUS = 0.25f
        const val LINEAR_DAMPING = 0.05f
        private const val ANGULAR_DAMPING = 0.8f

        /**
         * Mass of the standard bird. The pull is converted to a launch speed with this mass, and
         * lighter or heavier birds get a proportional impulse, so every bird leaves at the same speed.
         */
        val REFERENCE_MASS: Float = Surface.BIRD.density * MathUtils.PI * RADIUS * RADIUS

        private const val HOP_DURATION = 0.55f
        private const val HOP_HEIGHT = 0.9f

        /**
         * Creates a bird sitting on the slingshot anchor. With [hopFromIndex] the bird first jumps
         * from its waiting place (see SlingshotConfig.waitingX) to the slingshot; with null it is simply there.
         */
        fun create(bodies: BodyFactory, type: BirdType, hopFromIndex: Int? = null): Bird {
            val body = bodies.createCircle(
                x = SlingshotConfig.ANCHOR_X,
                y = SlingshotConfig.ANCHOR_Y,
                radius = type.radius,
                surface = Surface.BIRD,
                type = BodyDef.BodyType.StaticBody
            )
            return Bird(body, type, hopFromIndex, startsFlying = false)
        }

        /** Creates a bird that is already flying (used by the blue bird's split). */
        fun createFlying(bodies: BodyFactory, type: BirdType, x: Float, y: Float, vx: Float, vy: Float): Bird {
            val body = bodies.createCircle(
                x = x,
                y = y,
                radius = type.radius,
                surface = Surface.BIRD,
                type = BodyDef.BodyType.DynamicBody
            )
            body.isBullet = true
            body.setLinearVelocity(vx, vy)
            val bird = Bird(body, type, null, startsFlying = true)
            bird.markAbilityUsed()
            return bird
        }
    }
}
