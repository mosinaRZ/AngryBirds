package com.example.slingshot.render

import com.example.slingshot.systems.GameState

/**
 * The state of the level-end sequence, shared between the screen (which runs the timeline) and the
 * renderer (which draws the celebrating birds or pigs). Reused every frame; nothing is allocated.
 *
 * The unused birds ("bonus birds") never move to a special place: each one cheers where it was waiting
 * (on the slingshot, on the bench or on the ground), see [homeX] and [homeY].
 */
class Finale {
    /** PLAYING while the game runs; WON: the unused birds cheer; LOST: the remaining pigs cheer. */
    var state = GameState.PLAYING

    /** Seconds since the attempt ended. */
    var time = 0f

    /** Index (in the level's bird list) of the first bird that was never fired, and how many such birds there are. */
    var firstBonusIndex = 0
    var bonusBirds = 0

    /** How many of the unused birds have been counted (they start their jump when counted). */
    var bonusCounted = 0

    /** How many of the counted birds have already let their "+10000" fly to the score. */
    var numbersLaunched = 0

    /** Where the [k]-th unused bird stands: the center of its body, set once when the level ends. */
    val homeX = FloatArray(MAX_BIRDS)
    val homeY = FloatArray(MAX_BIRDS)

    /** Value of [time] at which the [k]-th unused bird was counted (only valid when k < [bonusCounted]). */
    val countedAt = FloatArray(MAX_BIRDS)

    companion object {
        /** Most unused birds the sequence can show. */
        const val MAX_BIRDS = 8
    }
}
