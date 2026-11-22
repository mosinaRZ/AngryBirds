package com.example.slingshot.systems

import com.example.slingshot.entities.BirdType
import com.example.slingshot.entities.Entity

/**
 * Things that happen during play which the game may react to (sound today,
 * maybe vibration later). Every method has an empty default, so a listener
 * only overrides what it needs. Called on the render thread, outside the physics step.
 */
interface GameEvents {

    /** The player grabbed the bird and starts pulling the band. */
    fun onStretch() {}

    /** A bird of the given [type] has just left the slingshot. */
    fun onLaunch(type: BirdType) {}

    /** A flying yellow or blue bird used its ability (dash / split). */
    fun onAbility(type: BirdType) {}

    /** The bird on the slingshot has been left alone for a while. */
    fun onBirdIdle(type: BirdType) {}

    /** The shot that just ended earned a lot of points. */
    fun onBigShot() {}

    /** The player tapped a locked level. */
    fun onLocked() {}

    /** The next bird jumps onto the slingshot. */
    fun onHop() {}

    /**
     * Something collided with the given impulse (N*s). [first] and [second] are the
     * entities involved; null means the ground. Called often; keep it cheap.
     */
    fun onImpact(impulse: Float, first: Entity?, second: Entity?) {}

    /** A block or an enemy was destroyed (called just before its body is removed). */
    fun onDestroyed(entity: Entity) {}

    fun onWin() {}

    fun onLose() {}

    /** A "+10000" of an unused bird has reached the score; [index] is how many numbers arrived before it (0 = the first). */
    fun onBonus(index: Int) {}

    /** The result screen reveals star number [index] (0, 1 or 2). */
    fun onStar(index: Int) {}

    /** A UI button was pressed. */
    fun onClick() {}
}
