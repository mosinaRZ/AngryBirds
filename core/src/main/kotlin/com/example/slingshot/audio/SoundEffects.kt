package com.example.slingshot.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils
import com.example.slingshot.entities.BirdType
import com.example.slingshot.entities.Block
import com.example.slingshot.entities.Enemy
import com.example.slingshot.entities.Entity
import com.example.slingshot.entities.Material
import com.example.slingshot.systems.GameEvents

/**
 * Plays all sound effects from the internal assets folder: slingshot (stretch, launch, hop),
 * collisions (wood, stone, glass, enemy, ground), destruction (wood, stone, glass, enemy),
 * result (win, lose, stars) and UI clicks.
 *
 * Every file is optional: a missing or unreadable file only disables that sound and logs
 * a warning. Owned by the game and disposed once.
 *
 * A collapsing tower produces many contacts per frame, so collision and destruction
 * sounds are filtered by strength and rate-limited, and pitched randomly a little so
 * repeated hits do not sound mechanical.
 */
class SoundEffects : GameEvents {

    private val all = ArrayList<Sound>()

    /** Master switch (the sound button in the settings): while off nothing is played, including UI clicks. */
    var enabled = true
        set(value) {
            if (field == value) return
            field = value
            if (!value) stopAll()
        }

    /** Volume chosen with the sound slider (0..1); multiplies the volume of every effect, including UI clicks. */
    var volumeScale = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    private val stretchSound = load("stretch")
    private val explosionSound = load("explosion")
    private val bigShotSound = load("big_shot")
    private val launchVoice = load("bird_launch")
    private val hopSound = load("hop")
    private val hitWood = load("hit_wood")
    private val hitStone = load("hit_stone")
    private val hitGlass = load("hit_glass")
    private val hitPig = load("hit_pig")
    private val thudSound = load("thud")
    private val breakWood = load("break_wood")
    private val breakStone = load("break_stone")
    private val breakGlass = load("break_glass")
    private val pigDie = load("pig_die")
    private val winSound = load("win")
    private val loseSound = load("lose")
    private val starSound = load("star")
    private val clickSound = load("click")
    private val lockedSound = load("locked")

    // Bird voices (cartoon "voices"; replace the files with your own recordings if you like).
    private val redIdle = load("bird_red_idle")
    private val yellowIdle = load("bird_yellow_idle")
    private val blueIdle = load("bird_blue_idle")
    private val yellowDash = load("yellow_dash")
    private val blueSplit = load("blue_split")

    private var lastHitNanos = now() - HIT_MIN_INTERVAL_NANOS
    private var lastPigHitNanos = now() - PIG_HIT_MIN_INTERVAL_NANOS
    private var lastBreakNanos = now() - BREAK_MIN_INTERVAL_NANOS

    override fun onStretch() {
        stretchSound.sfx(1f)
    }

    override fun onLaunch(type: BirdType) {
        // The same bird call for every bird (the "released from the slingshot" sound).
        launchVoice.sfx(1f)
    }

    override fun onBigShot() {
        bigShotSound.sfx(1f)
    }

    override fun onAbility(type: BirdType) {
        when (type) {
            BirdType.YELLOW -> yellowDash.sfx(1f)
            BirdType.BLUE -> blueSplit.sfx(1f)
            BirdType.RED -> Unit
        }
    }

    override fun onBirdIdle(type: BirdType) {
        val voice = when (type) {
            BirdType.RED -> redIdle
            BirdType.YELLOW -> yellowIdle
            BirdType.BLUE -> blueIdle
        }
        voice.sfx(0.9f, MathUtils.random(0.97f, 1.04f))
    }

    override fun onLocked() {
        lockedSound.sfx(0.8f)
    }

    override fun onHop() {
        hopSound.sfx(0.6f, MathUtils.random(0.95f, 1.1f))
    }

    override fun onImpact(impulse: Float, first: Entity?, second: Entity?) {
        if (impulse < HIT_MIN_IMPULSE) return

        val time = now()
        val isPig = first is Enemy || second is Enemy
        if (isPig) {
            if (time - lastPigHitNanos < PIG_HIT_MIN_INTERVAL_NANOS) return
            lastPigHitNanos = time
        } else {
            if (time - lastHitNanos < HIT_MIN_INTERVAL_NANOS) return
            lastHitNanos = time
        }

        val sound = pickHitSound(first, second, isPig) ?: return
        val volume = MathUtils.clamp(impulse / HIT_FULL_VOLUME_IMPULSE, HIT_MIN_VOLUME, 1f)
        sound.sfx(volume, MathUtils.random(0.9f, 1.1f))
    }

    override fun onDestroyed(entity: Entity) {
        val time = now()
        val sound = when (entity) {
            is Enemy -> pigDie
            is Block -> {
                // Several blocks can break in the same frame; one sound is enough.
                if (entity.material != Material.TNT && time - lastBreakNanos < BREAK_MIN_INTERVAL_NANOS) return
                when (entity.material) {
                    Material.WOOD -> breakWood
                    Material.STONE -> breakStone
                    Material.GLASS -> breakGlass
                    Material.TNT -> explosionSound
                }
            }
            else -> null
        } ?: return
        if (entity !is Enemy) lastBreakNanos = time
        sound.sfx(1f, MathUtils.random(0.92f, 1.08f))
    }

    override fun onWin() {
        winSound.sfx(1f)
    }

    override fun onLose() {
        loseSound.sfx(1f)
    }

    /** Each counted bird rings a little higher than the previous one. */
    override fun onBonus(index: Int) {
        starSound.sfx(0.9f, 0.9f + 0.1f * index)
    }

    /** Each star is a little higher than the previous one. */
    override fun onStar(index: Int) {
        starSound.sfx(1f, 1f + 0.15f * index)
    }

    override fun onClick() {
        clickSound.sfx(0.8f)
    }

    /** Stops everything that is playing, for example when the app goes to the background. */
    fun stopAll() {
        for (i in all.indices) all[i].stop()
    }

    fun dispose() {
        for (i in all.indices) all[i].dispose()
        all.clear()
    }

    /** The most brittle material involved decides the sound: glass, then stone, then wood. */
    private fun pickHitSound(first: Entity?, second: Entity?, isPig: Boolean): Sound? {
        if (isPig) return hitPig
        val a = (first as? Block)?.material
        val b = (second as? Block)?.material
        return when {
            a == Material.GLASS || b == Material.GLASS -> hitGlass
            a == Material.STONE || b == Material.STONE -> hitStone
            a == Material.WOOD || b == Material.WOOD || a == Material.TNT || b == Material.TNT -> hitWood
            else -> thudSound // the bird on the ground
        }
    }

    /** Loads "[name].ogg" (or "[name].wav"); returns null when neither exists or it cannot be decoded. */
    private fun load(name: String): Sound? {
        for (extension in EXTENSIONS) {
            val file = Gdx.files.internal("$name.$extension")
            if (!file.exists()) continue
            return try {
                Gdx.audio.newSound(file).also { all.add(it) }
            } catch (e: Exception) {
                warn("Could not load $name.$extension (${e.message}), this sound is disabled")
                null
            }
        }
        warn("Missing $name.ogg, this sound is disabled")
        return null
    }

    /** Plays this sound unless the sound effects are switched off or at volume 0 (or the file is missing). */
    private fun Sound?.sfx(volume: Float, pitch: Float = 1f) {
        if (enabled && volumeScale > 0f) this?.play(volume * volumeScale, pitch, 0f)
    }

    private fun now(): Long = TimeUtils.nanoTime()

    private fun warn(message: String) {
        Gdx.app.log(TAG, "WARNING: $message")
    }

    private companion object {
        const val TAG = "SoundEffects"

        val EXTENSIONS = arrayOf("ogg", "wav")

        /** Impacts weaker than this are silent (resting contacts, small bumps). */
        const val HIT_MIN_IMPULSE = 0.6f

        /** An impact of this strength plays the hit sound at full volume. */
        const val HIT_FULL_VOLUME_IMPULSE = 3f
        const val HIT_MIN_VOLUME = 0.3f

        /** At most one hit sound per this many nanoseconds (70 ms). */
        const val HIT_MIN_INTERVAL_NANOS = 70_000_000L
        const val PIG_HIT_MIN_INTERVAL_NANOS = 250_000_000L
        const val BREAK_MIN_INTERVAL_NANOS = 60_000_000L
    }
}
