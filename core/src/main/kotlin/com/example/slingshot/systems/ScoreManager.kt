package com.example.slingshot.systems

import com.example.slingshot.entities.Block
import com.example.slingshot.entities.Enemy
import com.example.slingshot.entities.Entity
import com.example.slingshot.entities.Material

/**
 * Keeps the score: points for destroyed enemies and blocks, bonus for unused birds.
 * Also converts a score into the 0..3 star rating.
 */
class ScoreManager {

    var score = 0
        private set

    /** Adds the points for a destroyed [entity] and returns them. */
    fun onDestroyed(entity: Entity): Int {
        val points = pointsFor(entity)
        score += points
        return points
    }

    /** Adds the bonus for one bird that was not needed (the level-end screen counts the birds one by one). */
    fun addBirdBonus() {
        score += BIRD_BONUS
    }

    companion object {
        private const val ENEMY_POINTS = 5000
        private const val WOOD_POINTS = 500
        private const val STONE_POINTS = 800
        private const val GLASS_POINTS = 300
        private const val TNT_POINTS = 500
        /** Points for each bird that was never fired when the level is won. */
        const val BIRD_BONUS = 10000

        /**
         * Star thresholds for level 1 (five enemies = 25000). Clearing the level always means at
         * least [STAR_ONE], so one star = "level cleared". Two and three stars reward saving
         * birds: each unused bird is worth [BIRD_BONUS] (about one spare bird for two stars, two
         * spare birds for three).
         */
        const val STAR_ONE = 25000
        const val STAR_TWO = 35000
        const val STAR_THREE = 48000

        /** Score at which the progress bar in the HUD is full. */
        const val BAR_MAX = 52000

        fun pointsFor(entity: Entity): Int = when (entity) {
            is Enemy -> ENEMY_POINTS
            is Block -> when (entity.material) {
                Material.WOOD -> WOOD_POINTS
                Material.STONE -> STONE_POINTS
                Material.GLASS -> GLASS_POINTS
                Material.TNT -> TNT_POINTS
            }
            else -> 0
        }

        /** 0 stars when the level was not cleared, otherwise 1 to 3 depending on [score]. */
        fun starsFor(score: Int, cleared: Boolean): Int = when {
            !cleared -> 0
            score >= STAR_THREE -> 3
            score >= STAR_TWO -> 2
            else -> 1
        }
    }
}
