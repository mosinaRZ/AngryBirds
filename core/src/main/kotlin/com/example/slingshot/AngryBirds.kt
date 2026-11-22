package com.example.slingshot

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.physics.box2d.Box2D
import com.example.slingshot.assets.Assets
import com.example.slingshot.audio.MusicPlayer
import com.example.slingshot.audio.SoundEffects
import com.example.slingshot.screens.SplashScreen
import com.example.slingshot.systems.Progress
import com.example.slingshot.systems.Settings

/**
 * Entry point of the game. Owns resources shared between screens
 * (batch, shape renderer, font, assets, sounds, saved progress) and disposes them once.
 */
class AngryBirds : Game() {

    lateinit var batch: SpriteBatch
        private set
    lateinit var shapes: ShapeRenderer
        private set
    lateinit var font: BitmapFont
        private set
    lateinit var assets: Assets
        private set
    lateinit var sounds: SoundEffects
        private set
    lateinit var progress: Progress
        private set
    lateinit var music: MusicPlayer
        private set
    lateinit var settings: Settings
        private set

    /**
     * The font scale that gives the normal UI text size (1.0 = the 64 px glyphs of font.fnt).
     * Code that temporarily changes `font.data` scale must restore it with [resetFontScale].
     */
    var uiFontScale = DEFAULT_FONT_UI_SCALE
        private set

    override fun create() {
        // Loads the Box2D native library. Must run before any World is created.
        Box2D.init()

        batch = SpriteBatch()
        assets = Assets()
        sounds = SoundEffects()
        music = MusicPlayer()
        shapes = ShapeRenderer()
        progress = Progress()
        settings = Settings()
        music.enabled = settings.musicOn
        sounds.enabled = settings.soundOn
        music.volumeScale = settings.musicVolume
        sounds.volumeScale = settings.soundVolume
        font = loadFont()
        setScreen(SplashScreen(this))
    }

    /**
     * Switches the music on or off (a complete mute that keeps the volume) and remembers the choice.
     * Turning it on while the volume is 0 brings the volume back to [Settings.RESTORE_VOLUME].
     */
    fun setMusicOn(on: Boolean) {
        if (on && settings.musicVolume <= 0f) setMusicVolume(Settings.RESTORE_VOLUME)
        settings.musicOn = on
        music.enabled = on
    }

    /**
     * Switches all sound effects (including button clicks) on or off (a complete mute that keeps the volume) and
     * remembers the choice. Turning it on while the volume is 0 brings the volume back to [Settings.RESTORE_VOLUME].
     */
    fun setSoundOn(on: Boolean) {
        if (on && settings.soundVolume <= 0f) setSoundVolume(Settings.RESTORE_VOLUME)
        settings.soundOn = on
        sounds.enabled = on
    }

    /** Sets the music volume (0..1) and remembers it. Moving the slider while the music is muted unmutes it. */
    fun setMusicVolume(volume: Float) {
        settings.musicVolume = volume
        music.volumeScale = settings.musicVolume
        if (settings.musicVolume > 0f && !settings.musicOn) setMusicOn(true)
    }

    /** Sets the sound effects volume (0..1) and remembers it. Moving the slider while the sound is muted unmutes it. */
    fun setSoundVolume(volume: Float) {
        settings.soundVolume = volume
        sounds.volumeScale = settings.soundVolume
        if (settings.soundVolume > 0f && !settings.soundOn) setSoundOn(true)
    }

    /** Puts the shared font back to its normal size and white color. */
    fun resetFontScale() {
        font.data.setScale(uiFontScale)
        font.setColor(1f, 1f, 1f, 1f)
    }

    override fun render() {
        music.update(Gdx.graphics.deltaTime)
        super.render()
    }

    override fun pause() {
        super.pause()
        if (::sounds.isInitialized) sounds.stopAll()
        if (::music.isInitialized) music.pauseAll()
    }

    override fun resume() {
        super.resume()
        if (::music.isInitialized) music.resumeAll()
    }

    override fun dispose() {
        super.dispose()
        music.dispose()
        sounds.dispose()
        assets.dispose()
        batch.dispose()
        shapes.dispose()
        font.dispose()
    }

    /** Loads the outlined game font (font.fnt); falls back to libGDX's built-in font when it is missing. */
    private fun loadFont(): BitmapFont {
        val file = Gdx.files.internal(FONT_FILE)
        if (file.exists()) {
            try {
                val loaded = BitmapFont(file)
                loaded.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                uiFontScale = CUSTOM_FONT_UI_SCALE
                loaded.data.setScale(uiFontScale)
                return loaded
            } catch (e: Exception) {
                Gdx.app.log(TAG, "WARNING: Could not load $FONT_FILE (${e.message}), using the default font")
            }
        } else {
            Gdx.app.log(TAG, "WARNING: Missing $FONT_FILE, using the default font")
        }
        uiFontScale = DEFAULT_FONT_UI_SCALE
        return BitmapFont().apply {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            data.setScale(uiFontScale)
        }
    }

    private companion object {
        const val TAG = "AngryBirds"
        const val FONT_FILE = "font.fnt"

        /** The built-in font has 15 px glyphs, font.fnt has 64 px glyphs. */
        const val DEFAULT_FONT_UI_SCALE = 2.5f
        const val CUSTOM_FONT_UI_SCALE = 0.6f
    }
}
