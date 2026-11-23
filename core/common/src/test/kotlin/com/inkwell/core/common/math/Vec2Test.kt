package com.inkwell.core.common.math

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Vec2Test {

    @Test
    fun `vector arithmetic operations compute correctly`() {
        val a = Vec2(3f, 4f)
        val b = Vec2(1f, 2f)

        assertThat(a + b).isEqualTo(Vec2(4f, 6f))
        assertThat(a - b).isEqualTo(Vec2(2f, 2f))
        assertThat(a * 2f).isEqualTo(Vec2(6f, 8f))
        assertThat(a / 2f).isEqualTo(Vec2(1.5f, 2f))
        assertThat(-a).isEqualTo(Vec2(-3f, -4f))
    }

    @Test
    fun `division by zero throws IllegalArgumentException`() {
        val a = Vec2(5f, 5f)
        assertThrows<IllegalArgumentException> {
            a / 0f
        }
    }

    @Test
    fun `magnitude and normalization compute correctly`() {
        val v = Vec2(3f, 4f)
        assertThat(v.lengthSquared()).isEqualTo(25f)
        assertThat(v.length()).isEqualTo(5f)

        val normalized = v.normalized()
        assertThat(normalized.x).isWithin(0.0001f).of(0.6f)
        assertThat(normalized.y).isWithin(0.0001f).of(0.8f)
        assertThat(normalized.length()).isWithin(0.0001f).of(1.0f)
    }

    @Test
    fun `lerp smoothly interpolates between points`() {
        val start = Vec2(0f, 0f)
        val end = Vec2(100f, 200f)

        assertThat(start.lerp(end, 0f)).isEqualTo(start)
        assertThat(start.lerp(end, 1f)).isEqualTo(end)
        assertThat(start.lerp(end, 0.5f)).isEqualTo(Vec2(50f, 100f))
    }
}
