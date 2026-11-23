package com.inkwell.core.common.math

import kotlin.math.sqrt

/**
 * Immutable 2D floating-point vector for sub-pixel canvas coordinates and velocity vectors.
 */
data class Vec2(val x: Float, val y: Float) {

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)

    operator fun div(scalar: Float): Vec2 {
        require(scalar != 0f) { "Division by zero" }
        return Vec2(x / scalar, y / scalar)
    }

    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    fun dot(other: Vec2): Float = x * other.x + y * other.y

    fun lengthSquared(): Float = x * x + y * y

    fun length(): Float = sqrt(lengthSquared())

    fun distanceTo(other: Vec2): Float = (this - other).length()

    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0f) this / len else ZERO
    }

    fun lerp(other: Vec2, t: Float): Vec2 {
        val clampedT = t.coerceIn(0f, 1f)
        return Vec2(
            x + (other.x - x) * clampedT,
            y + (other.y - y) * clampedT
        )
    }

    companion object {
        val ZERO = Vec2(0f, 0f)
        val ONE = Vec2(1f, 1f)
    }
}
