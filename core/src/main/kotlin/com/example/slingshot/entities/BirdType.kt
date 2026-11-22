package com.example.slingshot.entities

/**
 * The kinds of birds.
 *
 * - [RED]: plain bird, no ability.
 * - [YELLOW]: tap while flying to dash forward at high speed.
 * - [BLUE]: tap while flying to split into three birds. It is smaller and lighter than the others.
 *
 * [radius] is the radius of the physics circle in meters.
 */
enum class BirdType(val radius: Float) {
    RED(0.25f),
    YELLOW(0.25f),
    BLUE(0.2f)
}
