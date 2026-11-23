package com.inkwell.core.common.math

import kotlin.math.cos
import kotlin.math.sin

/**
 * 3x3 Affine transformation matrix for 2D viewport pan, zoom, and 360-degree rotation.
 * Row-major representation:
 * [ m00, m01, m02 ]
 * [ m10, m11, m12 ]
 * [   0,   0,   1 ]
 */
data class Matrix3(
    val m00: Float = 1f, val m01: Float = 0f, val m02: Float = 0f,
    val m10: Float = 0f, val m11: Float = 1f, val m12: Float = 0f
) {

    operator fun times(other: Matrix3): Matrix3 {
        return Matrix3(
            m00 = m00 * other.m00 + m01 * other.m10,
            m01 = m00 * other.m01 + m01 * other.m11,
            m02 = m00 * other.m02 + m01 * other.m12 + m02,

            m10 = m10 * other.m00 + m11 * other.m10,
            m11 = m10 * other.m01 + m11 * other.m11,
            m12 = m10 * other.m02 + m11 * other.m12 + m12
        )
    }

    fun mapPoint(point: Vec2): Vec2 {
        val px = m00 * point.x + m01 * point.y + m02
        val py = m10 * point.x + m11 * point.y + m12
        return Vec2(px, py)
    }

    fun determinant(): Float = m00 * m11 - m01 * m10

    fun inverted(): Matrix3? {
        val det = determinant()
        if (det == 0f) return null
        val invDet = 1f / det

        val inv00 = m11 * invDet
        val inv01 = -m01 * invDet
        val inv02 = (m01 * m12 - m11 * m02) * invDet

        val inv10 = -m10 * invDet
        val inv11 = m00 * invDet
        val inv12 = (m10 * m02 - m00 * m12) * invDet

        return Matrix3(inv00, inv01, inv02, inv10, inv11, inv12)
    }

    companion object {
        val IDENTITY = Matrix3()

        fun translation(tx: Float, ty: Float): Matrix3 = Matrix3(
            m00 = 1f, m01 = 0f, m02 = tx,
            m10 = 0f, m11 = 1f, m12 = ty
        )

        fun scale(sx: Float, sy: Float): Matrix3 = Matrix3(
            m00 = sx, m01 = 0f, m02 = 0f,
            m10 = 0f, m11 = sy, m12 = 0f
        )

        fun scaleAround(sx: Float, sy: Float, px: Float, py: Float): Matrix3 {
            return translation(px, py) * scale(sx, sy) * translation(-px, -py)
        }

        fun rotation(radians: Float): Matrix3 {
            val c = cos(radians)
            val s = sin(radians)
            return Matrix3(
                m00 = c, m01 = -s, m02 = 0f,
                m10 = s, m11 = c, m12 = 0f
            )
        }

        fun rotationAround(radians: Float, px: Float, py: Float): Matrix3 {
            return translation(px, py) * rotation(radians) * translation(-px, -py)
        }
    }
}
