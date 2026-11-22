package com.example.slingshot.level

import com.example.slingshot.entities.BirdType
import com.example.slingshot.entities.Enemy
import com.example.slingshot.entities.Material

/**
 * A block described by its horizontal center [x] and its bottom edge [bottomY].
 * Using the bottom edge makes stacking easy to write and read. Units are meters.
 */
class BlockSpec(
    val material: Material,
    val x: Float,
    val bottomY: Float,
    val width: Float,
    val height: Float
)

/** An enemy described by its horizontal center [x] and the lowest point of its circle. */
class EnemySpec(
    val x: Float,
    val bottomY: Float,
    val radius: Float = Enemy.DEFAULT_RADIUS
)

/** Pure data description of a level. [birds] is the order in which the birds are fired. */
class LevelData(
    val blocks: List<BlockSpec>,
    val enemies: List<EnemySpec>,
    val birds: List<BirdType>,
    /** Static rock pieces: each is a convex polygon given as x0, y0, x1, y1, ... in meters. */
    val terrain: List<FloatArray> = emptyList()
)
