package com.example.slingshot.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.Viewport
import com.example.slingshot.assets.Assets
import com.example.slingshot.entities.Bird
import com.example.slingshot.entities.BirdState
import com.example.slingshot.entities.BirdType
import com.example.slingshot.entities.Block
import com.example.slingshot.entities.Enemy
import com.example.slingshot.entities.Material
import com.example.slingshot.level.Level
import com.example.slingshot.level.Levels
import com.example.slingshot.systems.GameState
import com.example.slingshot.systems.SlingshotConfig
import com.example.slingshot.world.Units
import kotlin.math.exp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Draws a background image so it covers the whole visible area (like CSS "cover"), bottom aligned.
 * [zoom] above 1 draws it larger (used for a gentle parallax while the camera zooms in).
 */
object Backdrop {
    fun draw(
        batch: SpriteBatch, texture: Texture, centerX: Float, viewWidth: Float, viewHeight: Float, zoom: Float = 1f
    ) {
        val scale = max(viewWidth / texture.width, viewHeight / texture.height) * zoom
        val w = texture.width * scale
        val h = texture.height * scale
        batch.draw(texture, centerX - w / 2f, 0f, w, h)
    }
}

/**
 * Draws the world: background, ground, blocks, enemies, slingshot, birds, the aiming dots,
 * the dotted trail of the last shot, the elastic band and the visual effects.
 * The batch, shape renderer and [assets] are owned by the game, so they are not disposed here.
 *
 * Everything is sized in meters and drawn at the interpolated positions of the entities
 * ([Entity.renderX] and friends), so motion is smooth at any refresh rate.
 *
 * Block textures are tiled along the length of the block (never stretched), a border is drawn
 * around each block, and damage shows as cracks. Damaged enemies get bruises and bandages.
 */
class GameRenderer(
    private val batch: SpriteBatch,
    private val shapes: ShapeRenderer,
    private val assets: Assets,
    private val world: World,
    val effects: Effects
) {

    private val debugRenderer = Box2DDebugRenderer()

    var debugEnabled = false

    private val aim = AimPreview()
    private val trail = FlightTrail()
    private val pose = CheerPose()
    private var trackedBird: Bird? = null
    private var cloudAdded = false

    /** Seconds since the screen was created; drives idle animations. */
    private var time = 0f

    // Elastic band state.
    private var wasHolding = false
    private val lastHeld = Vector2()
    private var snapTime = SNAP_DURATION
    private var bandAttached = false
    private var tipX = 0f
    private var tipY = 0f

    /**
     * Draws one frame.
     *
     * @param extraBirds the extra birds of a blue bird's split (drawn like the main bird)
     * @param spawnedBirds how many birds have already been put on the slingshot; the others wait
     * @param finale the level-end sequence: WON = the unused birds cheer where they stand (each one jumps when it is
     *               counted), LOST = the remaining pigs celebrate
     */
    fun render(
        viewport: Viewport,
        delta: Float,
        bird: Bird?,
        extraBirds: List<Bird>,
        level: Level,
        spawnedBirds: Int,
        finale: Finale
    ) {
        val endState = finale.state
        val endTime = finale.time
        time += delta
        effects.update(delta)

        val camera = viewport.camera
        val projection = camera.combined
        val viewWidth = viewport.worldWidth
        val viewHeight = viewport.worldHeight
        val zoom = (camera as? OrthographicCamera)?.zoom ?: 1f
        val left = camera.position.x - viewWidth * zoom / 2f
        val right = camera.position.x + viewWidth * zoom / 2f
        val birdsWon = endState == GameState.WON
        val pigsWon = endState == GameState.LOST

        // Once the first unused bird has been counted it jumps off the slingshot, so the band goes slack.
        val released = birdsWon && finale.bonusCounted > 0
        val holding = bird != null && bird.state != BirdState.FLYING && !bird.isHopping && !released
        updateBand(bird, holding, delta)
        trackFlight(bird, extraBirds)
        if (bird != null && bird.state == BirdState.DRAGGING) {
            aim.update(bird.renderX, bird.renderY, time)
        } else {
            aim.clear()
        }

        // 1) scenery and everything behind the band
        batch.projectionMatrix = projection
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        // The background moves and zooms less than the foreground (a little parallax).
        val worldCenter = Units.WORLD_WIDTH / 2f
        Backdrop.draw(
            batch, assets.background,
            worldCenter + (camera.position.x - worldCenter) * BG_PAN_SHARE,
            viewWidth, viewHeight,
            1f + (1f / zoom - 1f) * BG_ZOOM_SHARE
        )
        drawGround(left, right)
        batch.draw(assets.terrain, Levels.TERRAIN_X, Levels.TERRAIN_Y, Levels.TERRAIN_WIDTH, Levels.TERRAIN_HEIGHT)
        batch.draw(
            assets.bench,
            SlingshotConfig.BENCH_LEFT, Units.GROUND_TOP,
            SlingshotConfig.BENCH_RIGHT - SlingshotConfig.BENCH_LEFT, SlingshotConfig.TABLE_TOP - Units.GROUND_TOP
        )
        for (index in spawnedBirds until level.birds.size) {
            if (!birdsWon) {
                drawWaitingBird(index, level.birds[index])
            } else {
                val k = index - finale.firstBonusIndex
                if (k in 0 until finale.bonusBirds) drawBonusBird(level, finale, k, bird, spawnedBirds)
            }
        }
        drawSlingshotLayer(assets.slingshotBack)
        batch.end()

        // 2) back half of the band and the leather pouch
        shapes.projectionMatrix = projection
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (bandAttached) {
            drawBand(SlingshotConfig.BACK_BAND_X, SlingshotConfig.BACK_BAND_Y)
            if (holding && bird != null) drawPouch(bird)
        }
        shapes.end()

        // 3) trail, structure, effects, aiming dots, bird and the front of the slingshot
        batch.begin()
        trail.draw(batch, assets, time)
        val blocks = level.blocks
        for (i in blocks.indices) drawBlock(blocks[i])
        val enemies = level.enemies
        for (i in enemies.indices) drawEnemy(enemies[i], i, pigsWon, endTime)
        batch.setColor(1f, 1f, 1f, 1f)
        effects.drawParticles(batch)
        aim.draw(batch, assets.dot)
        for (i in extraBirds.indices) drawBird(extraBirds[i])
        if (birdsWon) {
            // A bird that is still flying when the level ends keeps flying; the unused bird that waits on the
            // slingshot cheers there (the other unused birds were drawn with the waiting birds).
            if (bird != null && bird.state == BirdState.FLYING) drawBird(bird)
            if (finale.bonusBirds > 0 && finale.firstBonusIndex < spawnedBirds) {
                drawBonusBird(level, finale, 0, bird, spawnedBirds)
            }
        } else if (bird != null) {
            drawBird(bird)
        }
        drawSlingshotLayer(assets.slingshot)
        batch.end()

        // 4) front half of the band (or the slack band when no bird is held)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (bandAttached) {
            drawBand(SlingshotConfig.FRONT_BAND_X, SlingshotConfig.FRONT_BAND_Y)
        } else {
            drawRestingBand()
        }
        shapes.end()

        // 5) floating score numbers on top of everything
        batch.begin()
        effects.drawPopups(batch)
        batch.end()

        if (debugEnabled) debugRenderer.render(world, projection)
    }

    // ------------------------------------------------------------------ band

    /** Tracks where the band's tip is: on the held bird, or springing back after a launch. */
    private fun updateBand(bird: Bird?, holding: Boolean, delta: Float) {
        if (holding && bird != null) {
            wasHolding = true
            lastHeld.set(bird.renderX, bird.renderY)
            tipX = bird.renderX
            tipY = bird.renderY
            bandAttached = true
            return
        }
        if (wasHolding && bird != null && bird.state == BirdState.FLYING) snapTime = 0f
        wasHolding = false

        if (snapTime < SNAP_DURATION) {
            snapTime += delta
            // Damped oscillation: the band overshoots the anchor and settles.
            val f = exp(-SNAP_DAMPING * snapTime) * MathUtils.cos(SNAP_FREQUENCY * snapTime)
            tipX = SlingshotConfig.ANCHOR_X + (lastHeld.x - SlingshotConfig.ANCHOR_X) * f
            tipY = SlingshotConfig.ANCHOR_Y + (lastHeld.y - SlingshotConfig.ANCHOR_Y) * f
            bandAttached = true
        } else {
            bandAttached = false
        }
    }

    private fun drawBand(fromX: Float, fromY: Float) {
        val dx = tipX - SlingshotConfig.ANCHOR_X
        val dy = tipY - SlingshotConfig.ANCHOR_Y
        val stretch = (sqrt(dx * dx + dy * dy) / SlingshotConfig.MAX_PULL).coerceIn(0f, 1f)
        // The band gets thinner as it stretches.
        val width = BAND_WIDTH * (1.2f - 0.55f * stretch)
        shapes.setColor(BAND_R, BAND_G, BAND_B, 1f)
        shapes.rectLine(fromX, fromY, tipX, tipY, width)
        shapes.setColor(BAND_LIGHT_R, BAND_LIGHT_G, BAND_LIGHT_B, 1f)
        shapes.rectLine(fromX, fromY, tipX, tipY, width * 0.4f)
    }

    /** Slack band between the two fork tips when no bird is held. */
    private fun drawRestingBand() {
        shapes.setColor(BAND_R, BAND_G, BAND_B, 1f)
        shapes.rectLine(
            SlingshotConfig.BACK_BAND_X, SlingshotConfig.BACK_BAND_Y,
            SlingshotConfig.FRONT_BAND_X, SlingshotConfig.FRONT_BAND_Y,
            BAND_WIDTH * 1.2f
        )
    }

    /** The leather pouch that cradles the bird, perpendicular to the pull direction. */
    private fun drawPouch(bird: Bird) {
        var dx = bird.renderX - SlingshotConfig.ANCHOR_X
        var dy = bird.renderY - SlingshotConfig.ANCHOR_Y
        val length = sqrt(dx * dx + dy * dy)
        if (length < 0.001f) {
            dx = -1f
            dy = 0f
        } else {
            dx /= length
            dy /= length
        }
        val nx = -dy
        val ny = dx
        shapes.setColor(POUCH_R, POUCH_G, POUCH_B, 1f)
        shapes.rectLine(
            bird.renderX - nx * POUCH_HALF - dx * 0.04f, bird.renderY - ny * POUCH_HALF - dy * 0.04f,
            bird.renderX + nx * POUCH_HALF - dx * 0.04f, bird.renderY + ny * POUCH_HALF - dy * 0.04f,
            POUCH_THICKNESS
        )
    }

    // ------------------------------------------------------------------ scenery

    /** Tiles the ground texture over the visible width; tiling by hand works for any texture size. */
    private fun drawGround(left: Float, right: Float) {
        var x = MathUtils.floor(left / GROUND_TILE_WIDTH) * GROUND_TILE_WIDTH
        while (x < right) {
            // A hair of overlap hides seams between tiles.
            batch.draw(assets.ground, x, 0f, GROUND_TILE_WIDTH + 0.01f, Units.GROUND_TOP)
            x += GROUND_TILE_WIDTH
        }
    }

    /** Draws one layer of the slingshot; its height follows the texture's aspect ratio. */
    private fun drawSlingshotLayer(texture: Texture) {
        val width = SlingshotConfig.SLINGSHOT_WIDTH
        val height = width * texture.height / texture.width
        batch.draw(
            texture,
            SlingshotConfig.ANCHOR_X - width / 2f, SlingshotConfig.TABLE_TOP,
            width, height
        )
    }

    // ------------------------------------------------------------------ birds

    private fun drawBird(bird: Bird) {
        val size = spriteSize(bird.type)
        drawRotated(assets.birdTexture(bird.type), bird.renderX, bird.renderY, size, size, bird.renderAngle)
    }

    /** Sprite size in meters: the body circle of the sprite must match the physics circle of the type. */
    private fun spriteSize(type: BirdType): Float = type.radius * 2f * BIRD_SPRITE_RATIO

    /** How high the waiting bird with the given index is lifted by its idle hop right now (it hops now and then). */
    private fun waitingLift(index: Int): Float {
        val wave = max(0f, MathUtils.sin(time * 2.2f + index * 1.9f))
        return wave.pow(8) * 0.14f
    }

    /** A bird waiting on the bench or the ground for its turn; it hops now and then. */
    private fun drawWaitingBird(index: Int, type: BirdType) {
        val size = spriteSize(type)
        drawRotated(
            assets.birdTexture(type),
            SlingshotConfig.waitingX(index), SlingshotConfig.waitingBaseY(index) + type.radius + waitingLift(index),
            size, size, 0f
        )
    }

    /**
     * The [k]-th unused bird of a cleared level. It stays exactly where it waited (slingshot, bench or ground) and
     * keeps its idle hops until it is counted; then it crouches, jumps once, lands and goes on with small happy
     * hops. The whole motion is a continuous function of the time since it was counted ([CheerPose]), so the bird
     * never teleports or snaps into its jump.
     */
    private fun drawBonusBird(level: Level, finale: Finale, k: Int, bird: Bird?, spawnedBirds: Int) {
        val index = finale.firstBonusIndex + k
        var x = finale.homeX[k]
        var y = finale.homeY[k]
        var angle = 0f
        if (index < spawnedBirds && bird != null) {
            // The bird on the slingshot may still be hopping onto it when the level ends.
            x = bird.renderX
            y = bird.renderY
            angle = bird.renderAngle
        }
        val age = if (k < finale.bonusCounted) finale.time - finale.countedAt[k] else -1f
        pose.update(age)
        // The idle hop fades out while the bird crouches, so the jump starts from the ground without a snap.
        val idle = if (age <= 0f) 1f else 1f - Ease.smooth(age / CheerPose.TAKE_OFF)
        drawCheering(level.birds[index], x, y, angle, pose.lift + waitingLift(index) * idle)
    }

    /**
     * Draws a bird standing at ([x], [y]) (the center of its body), lifted by [lift] and squashed and tilted by
     * [pose]. The squash pivots on the point where the bird touches the ground, so it never sinks into it.
     */
    private fun drawCheering(type: BirdType, x: Float, y: Float, angle: Float, lift: Float) {
        val texture = assets.birdTexture(type)
        val size = spriteSize(type)
        val half = size / 2f
        batch.draw(
            texture,
            x - half, y + lift - half,
            half, half - type.radius,
            size, size,
            pose.scaleX, pose.scaleY,
            (angle + pose.tilt) * MathUtils.radiansToDegrees,
            0, 0, texture.width, texture.height,
            false, false
        )
    }

    /**
     * Remembers where flying birds have been, to leave the dotted trail behind them: the main bird, and after a
     * blue bird's split also its two copies. An ability leaves a small cloud on the path.
     * Only the last shot keeps its trail: the old one stays while the player aims, and is wiped the moment the
     * next bird leaves the slingshot.
     */
    private fun trackFlight(bird: Bird?, extraBirds: List<Bird>) {
        if (bird != null && bird.state == BirdState.FLYING && bird !== trackedBird) {
            trackedBird = bird
            cloudAdded = false
            trail.clear()
        }
        trail.beginFrame()
        if (bird != null && bird.state == BirdState.FLYING) {
            if (bird.abilityUsed && !cloudAdded) {
                cloudAdded = true
                trail.addCloud(bird.renderX, bird.renderY, time)
            }
            if (isFlyingFast(bird)) trail.record(0, bird.renderX, bird.renderY, time)
            for (i in extraBirds.indices) {
                val extra = extraBirds[i]
                if (i < 2 && isFlyingFast(extra)) trail.record(i + 1, extra.renderX, extra.renderY, time)
            }
        }
        trail.endFrame()
    }

    private fun isFlyingFast(bird: Bird): Boolean =
        bird.state == BirdState.FLYING && bird.body.linearVelocity.len2() > TRAIL_MIN_SPEED * TRAIL_MIN_SPEED

    // ------------------------------------------------------------------ enemies

    /** [celebrate]: the pigs won, so they hop and wobble with joy ([endTime] is the animation clock). */
    private fun drawEnemy(enemy: Enemy, index: Int, celebrate: Boolean, endTime: Float) {
        val health = enemy.healthFraction
        val tint = ENEMY_TINT_MIN + (1f - ENEMY_TINT_MIN) * health
        batch.setColor(1f, tint, tint, 1f)
        var x = enemy.renderX
        var y = enemy.renderY
        var angle = enemy.renderAngle
        if (celebrate) {
            val ramp = min(1f, endTime / 0.6f)
            val phase = endTime * 5.5f + index * 1.3f
            y += abs(MathUtils.sin(phase)) * 0.32f * ramp
            angle += MathUtils.sin(phase) * 0.16f * ramp
        }
        val size = enemy.radius * 2f
        val bodySize = size * PIG_BODY_SCALE
        drawRotated(assets.enemyBody, x, y, bodySize, bodySize, angle)
        val headSize = size * HEAD_SCALE
        drawRotated(assets.enemyHead, x, y, headSize, headSize, angle)
        if (health < HURT_BADLY_BELOW) {
            drawRotated(assets.enemyDamage2, x, y, headSize, headSize, angle)
        } else if (health < HURT_BELOW) {
            drawRotated(assets.enemyDamage1, x, y, headSize, headSize, angle)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    // ------------------------------------------------------------------ blocks

    private fun drawBlock(block: Block) {
        val x = block.renderX
        val y = block.renderY
        val angle = block.renderAngle
        val w = block.width
        val h = block.height
        val health = block.healthFraction

        val shade = MIN_SHADE + (1f - MIN_SHADE) * health
        batch.setColor(shade, shade, shade, 1f)
        drawMaterial(assets.block(block.material), x, y, w, h, angle)

        batch.setColor(1f, 1f, 1f, 1f)
        if (health < CRACKS_HEAVY_BELOW) {
            drawMaterial(assets.crack2, x, y, w, h, angle)
        } else if (health < CRACKS_LIGHT_BELOW) {
            drawMaterial(assets.crack1, x, y, w, h, angle)
        }
        drawBorder(block.material, x, y, w, h, angle)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /**
     * Fills the block with [texture]. Repeating textures are tiled at a fixed size per meter with
     * their grain running along the long side of the block; other textures are stretched.
     */
    private fun drawMaterial(texture: Texture, cx: Float, cy: Float, w: Float, h: Float, angle: Float) {
        val degrees = angle * MathUtils.radiansToDegrees
        if (texture.uWrap != Texture.TextureWrap.Repeat) {
            batch.draw(
                texture, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, degrees,
                0, 0, texture.width, texture.height, false, false
            )
        } else if (h > w) {
            drawTiled(texture, cx, cy, h, w, degrees + 90f)
        } else {
            drawTiled(texture, cx, cy, w, h, degrees)
        }
    }

    /** Draws a [length] x [thickness] rectangle centered at ([cx], [cy]), repeating [texture] along it. */
    private fun drawTiled(texture: Texture, cx: Float, cy: Float, length: Float, thickness: Float, degrees: Float) {
        val srcWidth = max(1, (length / TILE_METERS * texture.width).toInt())
        val srcHeight = max(1, (thickness / TILE_METERS * texture.height).toInt())
        batch.draw(
            texture,
            cx - length / 2f, cy - thickness / 2f, length / 2f, thickness / 2f, length, thickness,
            1f, 1f, degrees,
            0, 0, srcWidth, srcHeight, false, false
        )
    }

    /** A dark outline with a light line along the top edge, drawn as four thin rotated rectangles. */
    private fun drawBorder(material: Material, cx: Float, cy: Float, w: Float, h: Float, angle: Float) {
        val t = (min(w, h) * 0.16f).coerceIn(0.016f, 0.035f)
        val cos = MathUtils.cos(angle)
        val sin = MathUtils.sin(angle)
        val degrees = angle * MathUtils.radiansToDegrees

        when (material) {
            Material.WOOD -> batch.setColor(0.3f, 0.15f, 0.04f, 1f)
            Material.STONE -> batch.setColor(0.2f, 0.2f, 0.24f, 1f)
            Material.GLASS -> batch.setColor(0.12f, 0.42f, 0.6f, 1f)
            Material.TNT -> batch.setColor(0.32f, 0.16f, 0.05f, 1f)
        }
        strip(cx, cy, 0f, h / 2f - t / 2f, w, t, cos, sin, degrees)
        strip(cx, cy, 0f, -h / 2f + t / 2f, w, t, cos, sin, degrees)
        strip(cx, cy, -w / 2f + t / 2f, 0f, t, h - 2f * t, cos, sin, degrees)
        strip(cx, cy, w / 2f - t / 2f, 0f, t, h - 2f * t, cos, sin, degrees)

        batch.setColor(1f, 1f, 1f, 0.32f)
        strip(cx, cy, 0f, h / 2f - t * 1.9f, w - 2f * t, t * 0.8f, cos, sin, degrees)
    }

    /** One rectangle of the border: [ox], [oy] is its center relative to the block center, in the block's rotated frame. */
    private fun strip(
        cx: Float, cy: Float, ox: Float, oy: Float, w: Float, h: Float,
        cos: Float, sin: Float, degrees: Float
    ) {
        val x = cx + ox * cos - oy * sin
        val y = cy + ox * sin + oy * cos
        batch.draw(
            assets.pixel,
            x - w / 2f, y - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, degrees,
            0, 0, 1, 1, false, false
        )
    }

    private fun drawRotated(
        texture: Texture,
        centerX: Float, centerY: Float,
        width: Float, height: Float,
        angleRadians: Float
    ) {
        batch.draw(
            texture,
            centerX - width / 2f, centerY - height / 2f,
            width / 2f, height / 2f,
            width, height,
            1f, 1f,
            angleRadians * MathUtils.radiansToDegrees,
            0, 0, texture.width, texture.height,
            false, false
        )
    }

    fun dispose() {
        debugRenderer.dispose()
    }

    private companion object {
        const val GROUND_TILE_WIDTH = 4f

        /** How much of the camera pan and zoom the background follows (the foreground follows all of it). */
        const val BG_PAN_SHARE = 0.5f
        const val BG_ZOOM_SHARE = 0.4f

        /** Bird textures show the body circle at 380 of 640 px; sprite size = physics diameter times this. */
        const val BIRD_SPRITE_RATIO = 640f / 380f

        /** The pig body texture shows the body circle at 380 of 512 px. */
        const val PIG_BODY_SCALE = 512f / 380f

        /** The face is drawn slightly smaller than the body so the body shows around it. */
        const val HEAD_SCALE = 0.9f

        /** One block texture repetition covers this many meters. */
        const val TILE_METERS = 0.5f

        const val MIN_SHADE = 0.72f
        const val CRACKS_LIGHT_BELOW = 0.67f
        const val CRACKS_HEAVY_BELOW = 0.34f

        const val ENEMY_TINT_MIN = 0.7f
        const val HURT_BELOW = 0.7f
        const val HURT_BADLY_BELOW = 0.35f

        const val TRAIL_MIN_SPEED = 1f

        /** Band spring-back after a launch. */
        const val SNAP_DURATION = 0.45f
        const val SNAP_DAMPING = 9f
        const val SNAP_FREQUENCY = 32f

        const val BAND_WIDTH = 0.085f
        const val BAND_R = 0.24f
        const val BAND_G = 0.12f
        const val BAND_B = 0.05f
        const val BAND_LIGHT_R = 0.46f
        const val BAND_LIGHT_G = 0.27f
        const val BAND_LIGHT_B = 0.12f

        const val POUCH_R = 0.36f
        const val POUCH_G = 0.2f
        const val POUCH_B = 0.09f
        const val POUCH_HALF = 0.17f
        const val POUCH_THICKNESS = 0.11f
    }
}
