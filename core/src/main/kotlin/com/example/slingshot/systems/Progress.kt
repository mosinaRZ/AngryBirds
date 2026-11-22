package com.example.slingshot.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/** Best score and best star rating, saved on the device. */
class Progress {

    private val prefs: Preferences = Gdx.app.getPreferences(PREFS_NAME)

    var bestScore: Int = prefs.getInteger(KEY_SCORE, 0)
        private set

    var bestStars: Int = prefs.getInteger(KEY_STARS, 0)
        private set

    /** Stores a finished attempt. Returns true when [score] beats the previous best. */
    fun record(score: Int, stars: Int): Boolean {
        val improved = score > bestScore
        if (improved) bestScore = score
        if (stars > bestStars) bestStars = stars
        prefs.putInteger(KEY_SCORE, bestScore)
        prefs.putInteger(KEY_STARS, bestStars)
        prefs.flush()
        return improved
    }

    private companion object {
        const val PREFS_NAME = "slingshot_progress"
        const val KEY_SCORE = "best_score"
        const val KEY_STARS = "best_stars"
    }
}
