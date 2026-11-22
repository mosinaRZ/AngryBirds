package com.example.slingshot.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.math.MathUtils
import com.example.slingshot.entities.BirdType
import com.example.slingshot.entities.Material
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Loads all textures from the internal assets folder (android/assets).
 * Every file is optional: a missing or unreadable file is replaced by a
 * generated placeholder and a warning is logged, so loading never crashes.
 *
 * Textures whose size is a power of two get mip-maps (smooth, shimmer-free when a
 * sprite is drawn much smaller than the file) and block textures repeat, so a block
 * of any size is tiled instead of stretched. Other sizes simply use plain linear filtering.
 *
 * The enemy face (enemy_head.png) is cropped to a circle once at load time,
 * so replacing that single file changes the face with no code change.
 *
 * All textures are backed by managed pixmap data, so they are rebuilt
 * after an Android GL context loss like file textures are.
 */
class Assets {

    private val textures = ArrayList<Texture>()
    private val pixmaps = ArrayList<Pixmap>()

    val background: Texture = load(BACKGROUND) { PlaceholderArt.background() }
    val ground: Texture = load(GROUND) { PlaceholderArt.ground() }
    val bird: Texture = load(BIRD) { PlaceholderArt.bird() }
    val birdYellow: Texture = load(BIRD_YELLOW) { PlaceholderArt.bird() }
    val birdBlue: Texture = load(BIRD_BLUE) { PlaceholderArt.bird() }
    val enemyBody: Texture = load(ENEMY_BODY) { PlaceholderArt.enemyBody() }
    val enemyHead: Texture = loadEnemyHead()
    val enemyDamage1: Texture = load(ENEMY_DMG1) { PlaceholderArt.empty() }
    val enemyDamage2: Texture = load(ENEMY_DMG2) { PlaceholderArt.empty() }
    val slingshot: Texture = load(SLINGSHOT) { PlaceholderArt.slingshot() }
    val slingshotBack: Texture = load(SLINGSHOT_BACK) { PlaceholderArt.empty() }

    val crack1: Texture = load(CRACK_1, repeat = true) { PlaceholderArt.empty() }
    val crack2: Texture = load(CRACK_2, repeat = true) { PlaceholderArt.empty() }


    // Round icon buttons: a base (orange, blue or green) with an icon drawn on top.
    val btnOrange: Texture = load(BTN_ORANGE) { PlaceholderArt.button(0.9f, 0.55f, 0.1f) }
    val btnBlue: Texture = load(BTN_BLUE) { PlaceholderArt.button(0.2f, 0.45f, 0.85f) }
    val btnGreen: Texture = load(BTN_GREEN) { PlaceholderArt.button(0.2f, 0.65f, 0.3f) }
    val btnPill: Texture = load(BTN_PILL) { PlaceholderArt.button(0.2f, 0.65f, 0.3f) }
    val iconPlay: Texture = load(ICON_PLAY) { PlaceholderArt.empty() }
    val iconPause: Texture = load(ICON_PAUSE) { PlaceholderArt.empty() }
    val iconRestart: Texture = load(ICON_RESTART) { PlaceholderArt.empty() }
    val iconHome: Texture = load(ICON_HOME) { PlaceholderArt.empty() }
    val iconBack: Texture = load(ICON_BACK) { PlaceholderArt.empty() }
    val iconGear: Texture = load(ICON_GEAR) { PlaceholderArt.empty() }
    val iconMusic: Texture = load(ICON_MUSIC) { PlaceholderArt.empty() }
    val iconSound: Texture = load(ICON_SOUND) { PlaceholderArt.empty() }
    val iconClose: Texture = load(ICON_CLOSE) { PlaceholderArt.empty() }
    val iconSlash: Texture = load(ICON_SLASH) { PlaceholderArt.empty() }

    // Badges shown on the flight trail where a bird used its ability.
    val markDash: Texture = load(MARK_DASH) { PlaceholderArt.star(true) }
    val markSplit: Texture = load(MARK_SPLIT) { PlaceholderArt.star(false) }

    // The tower-style level list.
    val towerBg: Texture = load(TOWER_BG) { PlaceholderArt.background() }
    val towerFloors: Array<Texture> = Array(TOWER_FLOOR_COUNT) { i -> load("tower_floor_$i.png") { PlaceholderArt.panel() } }
    val doorOpen: Texture = load(DOOR_OPEN) { PlaceholderArt.button(0.15f, 0.3f, 0.7f) }
    val doorGold: Texture = load(DOOR_GOLD) { PlaceholderArt.button(0.9f, 0.7f, 0.1f) }
    val doorLocked: Texture = load(DOOR_LOCKED) { PlaceholderArt.button(0.1f, 0.15f, 0.4f) }
    val tagRed: Texture = load(TAG_RED) { PlaceholderArt.button(0.85f, 0.15f, 0.1f) }
    val tagPurple: Texture = load(TAG_PURPLE) { PlaceholderArt.button(0.5f, 0.2f, 0.7f) }

    val dot: Texture = load(DOT) { PlaceholderArt.dot() }
    val puff: Texture = load(PUFF) { PlaceholderArt.puff() }
    val starOn: Texture = load(STAR_ON) { PlaceholderArt.star(true) }
    val starOff: Texture = load(STAR_OFF) { PlaceholderArt.star(false) }

    // Star colors by rank: bronze, silver, gold (gold is [starOn]).
    private val starBronze: Texture = load(STAR_BRONZE) { PlaceholderArt.star(true) }
    private val starSilver: Texture = load(STAR_SILVER) { PlaceholderArt.star(true) }
    private val glowBronze: Texture = load(STAR_GLOW_BRONZE) { PlaceholderArt.puff() }
    private val glowSilver: Texture = load(STAR_GLOW_SILVER) { PlaceholderArt.puff() }
    val panel: Texture = load(PANEL) { PlaceholderArt.panel() }

    /** One white pixel; tint it with the batch color to draw plain rectangles. */
    val pixel: Texture = fromPixmap(PlaceholderArt.pixel())

    private val wood: Texture = load(WOOD, repeat = true) { PlaceholderArt.block(0.65f, 0.45f, 0.20f) }
    private val stone: Texture = load(STONE, repeat = true) { PlaceholderArt.block(0.50f, 0.50f, 0.55f) }
    private val glass: Texture = load(GLASS, repeat = true) { PlaceholderArt.block(0.60f, 0.85f, 0.95f) }
    private val tnt: Texture = load(TNT) { PlaceholderArt.block(0.80f, 0.40f, 0.15f) }

    /** The wooden bench the slingshot stands on. */
    val bench: Texture = load(BENCH) { PlaceholderArt.block(0.55f, 0.35f, 0.18f) }

    /** The picture of the level's rock terrain (cliff and mound). */
    val terrain: Texture = load(TERRAIN) { PlaceholderArt.empty() }

    /** Splash / lobby artwork. */
    val splash: Texture = load(SPLASH) { PlaceholderArt.background() }

    val pigIcon: Texture = load(PIG_ICON) { PlaceholderArt.star(true) }
    val starGlow: Texture = load(STAR_GLOW) { PlaceholderArt.puff() }
    val sparkle: Texture = load(SPARKLE) { PlaceholderArt.dot() }

    /** The sprite of a bird type (all drawn with the body circle at 380/640 of the image). */
    /** The earned star at position [index]: 0 = bronze, 1 (the middle one) = gold, 2 = silver. */
    fun starOnFor(index: Int): Texture = when (index) {
        0 -> starBronze
        1 -> starOn
        else -> starSilver
    }

    /** The turning rays behind the earned star at position [index]. */
    fun starGlowFor(index: Int): Texture = when (index) {
        0 -> glowBronze
        1 -> starGlow
        else -> glowSilver
    }

    fun birdTexture(type: BirdType): Texture = when (type) {
        BirdType.RED -> bird
        BirdType.YELLOW -> birdYellow
        BirdType.BLUE -> birdBlue
    }

    fun block(material: Material): Texture = when (material) {
        Material.WOOD -> wood
        Material.STONE -> stone
        Material.GLASS -> glass
        Material.TNT -> tnt
    }

    fun dispose() {
        for (i in textures.indices) textures[i].dispose()
        for (i in pixmaps.indices) pixmaps[i].dispose()
        textures.clear()
        pixmaps.clear()
    }

    private fun load(fileName: String, repeat: Boolean = false, placeholder: () -> Pixmap): Texture {
        val file = Gdx.files.internal(fileName)
        if (file.exists()) {
            try {
                return fromPixmap(Pixmap(file), repeat)
            } catch (e: Exception) {
                warn("Could not load $fileName (${e.message}), using placeholder")
            }
        } else {
            warn("Missing $fileName, using placeholder")
        }
        return fromPixmap(placeholder(), repeat)
    }

    /**
     * The face: enemy_head.png (your own image, cropped to a circle) if present, otherwise the
     * bundled default pig face (pig_face.png), otherwise a generated smiley.
     */
    private fun loadEnemyHead(): Texture {
        val custom = Gdx.files.internal(ENEMY_HEAD)
        if (custom.exists()) {
            try {
                val source = Pixmap(custom)
                try {
                    return fromPixmap(cropToCircle(source, HEAD_SIZE))
                } finally {
                    source.dispose()
                }
            } catch (e: Exception) {
                warn("Could not load $ENEMY_HEAD (${e.message}), using the default face")
            }
        }
        return load(PIG_FACE) { PlaceholderArt.enemyHead() }
    }

    /**
     * Takes ownership of [pixmap]; it is kept for GL context restore and disposed in [dispose].
     * Power-of-two images get mip-maps; with [repeat] they also tile.
     */
    private fun fromPixmap(pixmap: Pixmap, repeat: Boolean = false): Texture {
        pixmaps.add(pixmap)
        val pot = MathUtils.isPowerOfTwo(pixmap.width) && MathUtils.isPowerOfTwo(pixmap.height)
        val texture = Texture(PixmapTextureData(pixmap, null, pot, false, true))
        texture.setFilter(
            if (pot) Texture.TextureFilter.MipMapLinearLinear else Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        )
        if (repeat && pot) texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        textures.add(texture)
        return texture
    }

    private fun warn(message: String) {
        Gdx.app.log(TAG, "WARNING: $message")
    }

    private companion object {
        const val TAG = "Assets"

        const val BACKGROUND = "bg.png"
        const val GROUND = "ground.png"
        const val BIRD = "bird.png"
        const val BIRD_YELLOW = "bird_yellow.png"
        const val BIRD_BLUE = "bird_blue.png"
        const val ENEMY_BODY = "enemy_body.png"
        const val ENEMY_HEAD = "enemy_head.png"
        const val PIG_FACE = "pig_face.png"
        const val ENEMY_DMG1 = "enemy_dmg1.png"
        const val ENEMY_DMG2 = "enemy_dmg2.png"
        const val SLINGSHOT = "slingshot.png"
        const val SLINGSHOT_BACK = "slingshot_back.png"
        const val WOOD = "wood.png"
        const val STONE = "stone.png"
        const val GLASS = "glass.png"
        const val TNT = "tnt.png"
        const val BENCH = "bench.png"
        const val TERRAIN = "terrain_l1.png"
        const val SPLASH = "sp_bg.png"
        const val PIG_ICON = "pig_icon.png"
        const val STAR_GLOW = "star_glow.png"
        const val SPARKLE = "sparkle.png"
        const val CRACK_1 = "crack_1.png"
        const val CRACK_2 = "crack_2.png"
        const val BTN_ORANGE = "btn_orange.png"
        const val BTN_BLUE = "btn_blue.png"
        const val BTN_GREEN = "btn_green.png"
        const val BTN_PILL = "btn_pill.png"
        const val ICON_PLAY = "icon_play.png"
        const val ICON_PAUSE = "icon_pause.png"
        const val ICON_RESTART = "icon_restart.png"
        const val ICON_HOME = "icon_home.png"
        const val ICON_BACK = "icon_back.png"
        const val ICON_GEAR = "icon_gear.png"
        const val ICON_MUSIC = "icon_music.png"
        const val ICON_SOUND = "icon_sound.png"
        const val ICON_CLOSE = "icon_close.png"
        const val ICON_SLASH = "icon_slash.png"
        const val MARK_DASH = "mark_dash.png"
        const val MARK_SPLIT = "mark_split.png"
        const val TOWER_BG = "tower_bg.png"
        const val TOWER_FLOOR_COUNT = 4
        const val DOOR_OPEN = "door_open.png"
        const val DOOR_GOLD = "door_gold.png"
        const val DOOR_LOCKED = "door_locked.png"
        const val TAG_RED = "tag_red.png"
        const val TAG_PURPLE = "tag_purple.png"
        const val DOT = "dot.png"
        const val PUFF = "puff.png"
        const val STAR_ON = "star_on.png"
        const val STAR_OFF = "star_off.png"
        const val STAR_BRONZE = "star_bronze.png"
        const val STAR_SILVER = "star_silver.png"
        const val STAR_GLOW_BRONZE = "star_glow_bronze.png"
        const val STAR_GLOW_SILVER = "star_glow_silver.png"
        const val PANEL = "panel.png"

        /** Resolution of the cropped face texture. */
        const val HEAD_SIZE = 256

        /** Width of the soft edge of the circular crop, in pixels. */
        const val EDGE_WIDTH = 1.5f

        private val RGB_MASK = 0xFFFFFF00.toInt()

        /** Scales the centered square of [source] to [size] and makes everything outside the inscribed circle transparent. */
        private fun cropToCircle(source: Pixmap, size: Int): Pixmap {
            val side = min(source.width, source.height)
            val startX = (source.width - side) / 2
            val startY = (source.height - side) / 2

            val result = blankPixmap(size, size)
            result.drawPixmap(source, startX, startY, side, side, 0, 0, size, size)
            result.setBlending(Pixmap.Blending.None)

            val radius = size / 2f
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val dx = x + 0.5f - radius
                    val dy = y + 0.5f - radius
                    val distance = sqrt(dx * dx + dy * dy)
                    val pixel = result.getPixel(x, y)
                    if (distance >= radius) {
                        // Fully transparent, but keep the color so mip-maps do not get dark fringes.
                        result.drawPixel(x, y, pixel and RGB_MASK)
                    } else if (distance > radius - EDGE_WIDTH) {
                        val alpha = (pixel and 0xFF) * ((radius - distance) / EDGE_WIDTH)
                        result.drawPixel(x, y, (pixel and RGB_MASK) or alpha.toInt())
                    }
                }
            }
            result.setBlending(Pixmap.Blending.SourceOver)
            return result
        }
    }
}
