package com.example.slingshot.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/**
 * Player settings saved on the device: music on/off, sound effects on/off and the volume (0..1) of each.
 * The on/off switch mutes completely and keeps the volume; the volume is what the slider sets.
 * The game applies them to the audio classes ([com.example.slingshot.AngryBirds.setMusicOn],
 * [com.example.slingshot.AngryBirds.setSoundOn], [com.example.slingshot.AngryBirds.setMusicVolume] and
 * [com.example.slingshot.AngryBirds.setSoundVolume]); this class only remembers them.
 */
class Settings {

    private val prefs: Preferences = Gdx.app.getPreferences(PREFS_NAME)

    var musicOn: Boolean = prefs.getBoolean(KEY_MUSIC, true)
        set(value) {
            field = value
            prefs.putBoolean(KEY_MUSIC, value)
            prefs.flush()
        }

    var soundOn: Boolean = prefs.getBoolean(KEY_SOUND, true)
        set(value) {
            field = value
            prefs.putBoolean(KEY_SOUND, value)
            prefs.flush()
        }

    /** Music volume, 0 (silent) .. 1 (full). */
    var musicVolume: Float = prefs.getFloat(KEY_MUSIC_VOLUME, DEFAULT_VOLUME).coerceIn(0f, 1f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            prefs.putFloat(KEY_MUSIC_VOLUME, field)
            prefs.flush()
        }

    /** Sound effects volume, 0 (silent) .. 1 (full). */
    var soundVolume: Float = prefs.getFloat(KEY_SOUND_VOLUME, DEFAULT_VOLUME).coerceIn(0f, 1f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            prefs.putFloat(KEY_SOUND_VOLUME, field)
            prefs.flush()
        }

    companion object {
        /** The volume of a fresh install, and the one restored when a switch is turned on while its volume is 0. */
        const val DEFAULT_VOLUME = 1f
        const val RESTORE_VOLUME = 0.5f

        private const val PREFS_NAME = "slingshot_settings"
        private const val KEY_MUSIC = "music_on"
        private const val KEY_SOUND = "sound_on"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_SOUND_VOLUME = "sound_volume"
    }
}
