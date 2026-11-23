package com.inkwell.core.common.math

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.PI

class Matrix3Test {

    @Test
    fun `identity matrix maps point unchanged`() {
        val p = Vec2(42f, 99f)
        val transformed = Matrix3.IDENTITY.mapPoint(p)
        assertThat(transformed).isEqualTo(p)
    }

    @Test
    fun `translation shifts point correctly`() {
        val t = Matrix3.translation(100f, -50f)
        val p = Vec2(10f, 20f)
        assertThat(t.mapPoint(p)).isEqualTo(Vec2(110f, -30f))
    }

    @Test
    fun `scale multiplies coordinates correctly`() {
        val s = Matrix3.scale(2f, 3f)
        val p = Vec2(15f, 20f)
        assertThat(s.mapPoint(p)).isEqualTo(Vec2(30f, 60f))
    }

    @Test
    fun `scale around pivot preserves pivot point`() {
        val pivot = Vec2(50f, 50f)
        val s = Matrix3.scaleAround(2f, 2f, pivot.x, pivot.y)
        val mappedPivot = s.mapPoint(pivot)
        assertThat(mappedPivot.x).isWithin(0.0001f).of(pivot.x)
        assertThat(mappedPivot.y).isWithin(0.0001f).of(pivot.y)
    }

    @Test
    fun `rotation by 90 degrees rotates vector correctly`() {
        val r = Matrix3.rotation((PI / 2.0).toFloat())
        val p = Vec2(1f, 0f)
        val rotated = r.mapPoint(p)
        assertThat(rotated.x).isWithin(0.0001f).of(0f)
        assertThat(rotated.y).isWithin(0.0001f).of(1f)
    }

    @Test
    fun `matrix inverse undoes transformation with high precision`() {
        val transform = Matrix3.translation(200f, 150f) *
                Matrix3.rotation((PI / 4.0).toFloat()) *
                Matrix3.scale(1.5f, 1.5f)

        val inv = transform.inverted()
        assertThat(inv).isNotNull()

        val original = Vec2(350.25f, 720.5f)
        val mapped = transform.mapPoint(original)
        val restored = inv!!.mapPoint(mapped)

        assertThat(restored.x).isWithin(0.001f).of(original.x)
        assertThat(restored.y).isWithin(0.001f).of(original.y)
    }

    @Test
    fun `degenerate matrix returns null on inversion`() {
        val singular = Matrix3(
            m00 = 0f, m01 = 0f, m02 = 0f,
            m10 = 0f, m11 = 0f, m12 = 0f
        )
        assertThat(singular.inverted()).isNull()
    }
}
