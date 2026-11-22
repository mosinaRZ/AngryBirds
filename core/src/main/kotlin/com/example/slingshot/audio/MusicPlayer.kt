package com.example.slingshot.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.math.MathUtils

/**
 * Plays the looping background music with smooth cross-fades.
 *
 * Two tracks exist: [Track.MENU] (menu and level select) and [Track.GAME] (the ambience during play).
 * Each track is read from `music_menu.ogg` / `music_game.ogg` in the assets folder (".mp3" and ".wav"
 * also work). A missing file simply means silence for that track; a warning is logged.
 * The bundled files are original compositions: replace them with your own files of the same name.
 *
 * [update] must be called every frame (the game does it). The music can be "ducked" (made quieter,
 * for example while the game is paused).
 */
class MusicPlayer {

    enum class Track(val fileName: String, val volume: Float) {
        MENU("music_menu", 0.6f),
        GAME("music_game", 0.5f)
    }

    private class Channel(val track: Track) {
        var music: Music? = null
        var triedToLoad = false
        var level = 0f
        var target = 0f
    }

    private val tracks = Track.values()
    private val channels = Array(tracks.size) { Channel(tracks[it]) }
    private var requested: Track? = null
    private var ducked = false

    /**
     * Master switch (the music button in the settings). While off, the current track fades out and pauses;
     * switching it on again fades the requested track back in.
     */
    var enabled = true
        set(value) {
            if (field == value) return
            field = value
            if (value) requested?.let { play(it) }
        }

    /** Volume chosen with the music slider (0..1); multiplies the volume of every track. */
    var volumeScale = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    /** Fades in [track] (and fades every other track out). Does nothing special if it already plays. */
    fun play(track: Track) {
        requested = track
        for (i in channels.indices) {
            val channel = channels[i]
            if (channel.track == track) {
                channel.target = track.volume
                val music = load(channel)
                if (enabled && music != null && !music.isPlaying) music.play()
            } else {
                channel.target = 0f
            }
        }
    }

    fun stop() {
        requested = null
        for (i in channels.indices) channels[i].target = 0f
    }

    /** Makes the music quieter (true) or normal (false). */
    fun setDucked(value: Boolean) {
        ducked = value
    }

    fun update(delta: Float) {
        val duck = if (ducked) DUCK_FACTOR else 1f
        for (i in channels.indices) {
            val channel = channels[i]
            val music = channel.music ?: continue
            val goal = if (enabled) channel.target * duck else 0f
            val step = FADE_SPEED * delta
            channel.level += MathUtils.clamp(goal - channel.level, -step, step)
            music.volume = channel.level * volumeScale
            if (goal <= 0f && channel.level <= 0.001f && music.isPlaying) music.pause()
        }
    }

    /** Pauses everything (the app went to the background). */
    fun pauseAll() {
        for (i in channels.indices) channels[i].music?.pause()
    }

    /** Continues the requested track after [pauseAll]. */
    fun resumeAll() {
        val track = requested ?: return
        if (!enabled) return
        for (i in channels.indices) {
            val channel = channels[i]
            if (channel.track == track) channel.music?.play()
        }
    }

    fun dispose() {
        for (i in channels.indices) {
            channels[i].music?.dispose()
            channels[i].music = null
        }
    }

    private fun load(channel: Channel): Music? {
        if (channel.triedToLoad) return channel.music
        channel.triedToLoad = true
        for (extension in EXTENSIONS) {
            val file = Gdx.files.internal("${channel.track.fileName}.$extension")
            if (!file.exists()) continue
            try {
                val music = Gdx.audio.newMusic(file)
                music.isLooping = true
                music.volume = 0f
                channel.music = music
                return music
            } catch (e: Exception) {
                Gdx.app.log(TAG, "WARNING: Could not load ${file.name()} (${e.message}), no music for this track")
                return null
            }
        }
        Gdx.app.log(TAG, "WARNING: Missing ${channel.track.fileName}.ogg, no music for this track")
        return null
    }

    private companion object {
        const val TAG = "MusicPlayer"
        val EXTENSIONS = arrayOf("ogg", "mp3", "wav")

        /** Volume change per second while fading. */
        const val FADE_SPEED = 1.2f
        const val DUCK_FACTOR = 0.3f
    }
}
