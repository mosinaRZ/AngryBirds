package com.example.slingshot.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.example.slingshot.entities.Bird
import com.example.slingshot.entities.BirdState
import com.example.slingshot.entities.BirdType
import com.example.slingshot.level.Level
import com.example.slingshot.world.BodyFactory
import com.example.slingshot.world.Units
import kotlin.math.sqrt

enum class GameState { PLAYING, WON, LOST }

/** Told when a bird uses its ability, with the place where it happened (for visual effects). */
fun interface AbilityListener {
    fun onAbility(type: BirdType, x: Float, y: Float)
}

/**
 * Game flow: the bird queue, bird abilities, win and lose detection.
 *
 * - [birdsLeft] counts birds that have not been fired yet, including the one on the slingshot.
 * - The birds come in the order given by the level ([Level.birds]).
 * - A tap while a yellow bird flies makes it dash; a tap while a blue bird flies splits it in three
 *   (the two extra birds are in [splitBirds]).
 * - A fired bird (and its split copies) is removed when everything has come to rest, left the level
 *   or timed out; then the next bird is placed on the slingshot.
 * - WON as soon as no enemy is left (leftover birds give a bonus).
 * - LOST when no bird is left and everything has been still for [LOSE_SETTLE_SECONDS].
 *
 * [update] must be called after the physics step, outside of contact callbacks.
 */
class GameStateManager(
    private val world: World,
    private val bodies: BodyFactory,
    private val level: Level,
    private val controller: SlingshotController,
    private val score: ScoreManager,
    private val events: GameEvents
) {

    var state = GameState.PLAYING
        private set

    var birdsLeft = level.birds.size
        private set

    /** How many birds have been put on the slingshot so far (the rest still wait on the ground). */
    var spawnedCount = 0
        private set

    /** Birds that were never fired when the level was won; each is worth BIRD_BONUS (counted by the screen). */
    var bonusBirds = 0
        private set

    /** The bird currently on the slingshot or in flight, or null between birds. */
    var bird: Bird? = null
        private set

    /** The two extra birds created by a blue bird's split (empty most of the time). */
    val splitBirds: List<Bird>
        get() = extraBirds

    /** Set by the screen to show effects when an ability is used. */
    var abilityListener: AbilityListener? = null

    val isPlaying: Boolean
        get() = state == GameState.PLAYING

    private val extraBirds = ArrayList<Bird>(4)
    private var launchCounted = false
    private var scoreAtLaunch = 0

    // After the last pig is gone the level only ends once nothing scores for a while.
    private var allPigsDown = false
    private var winWaitTime = 0f
    private var winQuietTime = 0f
    private var winLastScore = 0
    private var flightTime = 0f
    private var settledTime = 0f
    private var idleTime = 0f
    private var nextIdleAt = MathUtils.random(IDLE_MIN_SECONDS, IDLE_MAX_SECONDS)

    init {
        spawnBird()
    }

    /** Call when the player touches the screen: the bird's idle chirp timer starts again. */
    fun resetIdle() {
        idleTime = 0f
    }

    fun update(delta: Float) {
        bird?.updateHop(delta)
        if (state != GameState.PLAYING) return

        if (level.enemies.isEmpty()) {
            updateWinWait(delta)
            return
        }
        updateIdle(delta)
        updateBird(delta)
        if (birdsLeft == 0) updateLoseCheck(delta)
    }

    /**
     * All pigs are gone, but blocks may still be collapsing for points: keep playing (no new bird can be
     * fired) and win when the score has not changed for [WIN_QUIET_SECONDS] (at most [WIN_MAX_WAIT_SECONDS]).
     */
    private fun updateWinWait(delta: Float) {
        if (!allPigsDown) {
            allPigsDown = true
            winLastScore = score.score
            controller.cancelDrag()
            controller.bird = null
        }
        updateBird(delta)
        winWaitTime += delta
        if (score.score != winLastScore) {
            winLastScore = score.score
            winQuietTime = 0f
        } else {
            winQuietTime += delta
        }
        if (winQuietTime >= WIN_QUIET_SECONDS || winWaitTime >= WIN_MAX_WAIT_SECONDS) finish(GameState.WON)
    }

    /** The waiting bird chirps now and then when the player does nothing. */
    private fun updateIdle(delta: Float) {
        val current = bird
        if (current != null && current.state == BirdState.READY && !current.isHopping) {
            idleTime += delta
            if (idleTime >= nextIdleAt) {
                events.onBirdIdle(current.type)
                idleTime = 0f
                nextIdleAt = MathUtils.random(IDLE_MIN_SECONDS, IDLE_MAX_SECONDS)
            }
        } else {
            idleTime = 0f
        }
    }

    private fun updateBird(delta: Float) {
        val current = bird ?: return
        if (current.state != BirdState.FLYING) {
            controller.consumeAbilityRequest()
            return
        }

        if (!launchCounted) {
            launchCounted = true
            scoreAtLaunch = score.score
            birdsLeft--
            events.onLaunch(current.type)
        }
        flightTime += delta
        if (controller.consumeAbilityRequest()) useAbility(current)

        // Every flying bird must be finished before the next one comes.
        var allDone = isDone(current, delta)
        for (i in extraBirds.indices) {
            if (!isDone(extraBirds[i], delta)) allDone = false
        }
        if (flightTime >= MAX_FLIGHT_TIME || allDone) retireBirds(current)
    }

    /** True when [b] has been at rest long enough or has left the level. */
    private fun isDone(b: Bird, delta: Float): Boolean {
        b.restTime = if (isSlow(b.body, BIRD_REST_SPEED)) b.restTime + delta else 0f
        return b.restTime >= BIRD_REST_SECONDS || isOutsideLevel(b)
    }

    private fun useAbility(current: Bird) {
        if (!current.canUseAbility) return
        current.markAbilityUsed()

        val position = current.body.position
        val px = position.x
        val py = position.y
        val velocity = current.body.linearVelocity
        val vx = velocity.x
        val vy = velocity.y

        when (current.type) {
            BirdType.YELLOW -> current.body.setLinearVelocity(vx * YELLOW_BOOST, vy * YELLOW_BOOST)
            BirdType.BLUE -> splitBlue(px, py, vx, vy)
            BirdType.RED -> Unit
        }
        events.onAbility(current.type)
        abilityListener?.onAbility(current.type, px, py)
    }

    /** Creates two more blue birds that fan out to both sides of the original's path. */
    private fun splitBlue(px: Float, py: Float, vx: Float, vy: Float) {
        val speed = sqrt(vx * vx + vy * vy)
        if (speed < 0.01f) return
        val normalX = -vy / speed
        val normalY = vx / speed
        for (side in SPLIT_SIDES) {
            val angle = side * SPLIT_ANGLE
            val c = MathUtils.cos(angle)
            val s = MathUtils.sin(angle)
            extraBirds.add(
                Bird.createFlying(
                    bodies, BirdType.BLUE,
                    px + normalX * side * SPLIT_OFFSET, py + normalY * side * SPLIT_OFFSET,
                    vx * c - vy * s, vx * s + vy * c
                )
            )
        }
    }

    private fun retireBirds(current: Bird) {
        check(!world.isLocked) { "Birds must be removed outside of the physics step" }
        for (i in extraBirds.indices) world.destroyBody(extraBirds[i].body)
        extraBirds.clear()
        world.destroyBody(current.body)
        // A shot that scored a lot gets a laugh (not when it just won the level: the win sound plays then).
        if (score.score - scoreAtLaunch >= BIG_SHOT_POINTS && level.enemies.isNotEmpty()) events.onBigShot()
        bird = null
        controller.bird = null
        // Once every pig is down no new bird is offered: the unused ones become the bonus.
        if (birdsLeft > 0 && level.enemies.isNotEmpty()) spawnBird()
    }

    private fun spawnBird() {
        val index = spawnedCount
        val type = level.birds[index]
        // The first bird is simply on the slingshot; the others jump there from the ground.
        val newBird = Bird.create(bodies, type, if (index == 0) null else index)
        spawnedCount++
        if (index > 0) events.onHop()
        bird = newBird
        controller.bird = newBird
        controller.consumeAbilityRequest()
        launchCounted = false
        flightTime = 0f
        idleTime = 0f
    }

    private fun updateLoseCheck(delta: Float) {
        settledTime = if (isWorldSettled()) settledTime + delta else 0f
        if (settledTime >= LOSE_SETTLE_SECONDS) finish(GameState.LOST)
    }

    private fun finish(result: GameState) {
        if (result == GameState.WON) bonusBirds = birdsLeft
        state = result
        if (result == GameState.WON) events.onWin() else events.onLose()
        // Put a bird that is being dragged back and stop all further aiming.
        controller.cancelDrag()
        controller.bird = null
    }

    private fun isWorldSettled(): Boolean {
        val current = bird
        if (current != null && !isSlow(current.body, SETTLE_SPEED)) return false
        for (i in extraBirds.indices) if (!isSlow(extraBirds[i].body, SETTLE_SPEED)) return false

        val blocks = level.blocks
        for (i in blocks.indices) if (isMoving(blocks[i].body)) return false
        val enemies = level.enemies
        for (i in enemies.indices) if (isMoving(enemies[i].body)) return false
        return true
    }

    /** Bodies that fell out of the level never come to rest and are ignored. */
    private fun isMoving(body: Body): Boolean =
        body.position.y > FALLEN_BELOW_Y && !isSlow(body, SETTLE_SPEED)

    private fun isSlow(body: Body, maxSpeed: Float): Boolean =
        body.linearVelocity.len2() < maxSpeed * maxSpeed

    private fun isOutsideLevel(current: Bird): Boolean {
        val p = current.body.position
        return p.x < -OUT_MARGIN || p.x > Units.WORLD_WIDTH + OUT_MARGIN || p.y < -OUT_MARGIN
    }

    private companion object {
        /** A fired bird that stays slower than this for [BIRD_REST_SECONDS] is finished. */
        const val BIRD_REST_SPEED = 0.3f
        const val BIRD_REST_SECONDS = 1f
        const val MAX_FLIGHT_TIME = 7f
        const val OUT_MARGIN = 6f

        /** Nothing may move faster than this for [LOSE_SETTLE_SECONDS] before the game is lost. */
        const val SETTLE_SPEED = 0.15f
        const val LOSE_SETTLE_SECONDS = 2f
        const val FALLEN_BELOW_Y = -1f

        /** The yellow bird's dash multiplies its speed by this. */
        const val YELLOW_BOOST = 1.7f

        /** Blue split: angle (radians) between the three paths, and the sideways offset of the new birds. */
        const val SPLIT_ANGLE = 0.3f
        const val SPLIT_OFFSET = 0.3f
        val SPLIT_SIDES = floatArrayOf(-1f, 1f)

        /** After the last pig: how long the score must stay unchanged before the level ends, and the longest wait. */
        const val WIN_QUIET_SECONDS = 2.4f
        const val WIN_MAX_WAIT_SECONDS = 9f

        /** A shot that earns at least this many points is a "big shot". */
        const val BIG_SHOT_POINTS = 7000

        const val IDLE_MIN_SECONDS = 6f
        const val IDLE_MAX_SECONDS = 10f
    }
}
