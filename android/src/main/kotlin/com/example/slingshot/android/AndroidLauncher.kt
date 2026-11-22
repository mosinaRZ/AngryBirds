package com.example.slingshot.android

import android.os.Build
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.example.slingshot.AngryBirds

/**
 * Android entry point: landscape, true full screen (status and navigation bars hidden,
 * content drawn under a camera cut-out), screen kept on while playing.
 */
class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The game never runs in split screen, a floating window or picture-in-picture: the manifest forbids
        // resizing, and onMultiWindowModeChanged closes the game if the system forces it anyway.
        useWholeScreen()
        val configuration = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useWakelock = true
            useAccelerometer = false
            useCompass = false
            // 2x multisampling makes the edges of rotated sprites and lines smooth.
            numSamples = 2
        }
        initialize(AngryBirds(), configuration)
    }

    /** If the system forces the game into multi-window mode anyway (developer options), it closes instead. */
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        if (isInMultiWindowMode) finish()
    }

    /** Lets the game draw behind the notch / punch-hole and the system bars. */
    private fun useWholeScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attributes = window.attributes
            attributes.layoutInDisplayCutoutMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            window.attributes = attributes
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
    }
}
