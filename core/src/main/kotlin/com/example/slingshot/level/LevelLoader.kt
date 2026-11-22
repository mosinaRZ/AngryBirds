package com.example.slingshot.level

import com.example.slingshot.entities.BirdType
import com.example.slingshot.entities.Block
import com.example.slingshot.entities.Enemy
import com.example.slingshot.world.BodyFactory

/** The live entities of a loaded level. Destroyed entities are removed by the damage system. */
class Level(
    val blocks: MutableList<Block>,
    val enemies: MutableList<Enemy>,
    /** The birds of this attempt, in firing order. */
    val birds: List<BirdType>
)

/** Turns a [LevelData] into physics bodies and entities. Not to be called while the world steps. */
class LevelLoader(private val bodies: BodyFactory) {

    fun load(data: LevelData): Level {
        for (polygon in data.terrain) bodies.createStaticPolygon(polygon)

        val blocks = ArrayList<Block>(data.blocks.size)
        for (spec in data.blocks) {
            blocks.add(
                Block.create(
                    bodies = bodies,
                    material = spec.material,
                    x = spec.x,
                    y = spec.bottomY + spec.height / 2f,
                    width = spec.width,
                    height = spec.height
                )
            )
        }

        val enemies = ArrayList<Enemy>(data.enemies.size)
        for (spec in data.enemies) {
            enemies.add(
                Enemy.create(
                    bodies = bodies,
                    x = spec.x,
                    y = spec.bottomY + spec.radius,
                    radius = spec.radius
                )
            )
        }
        return Level(blocks, enemies, data.birds)
    }
}
