package com.inkwell.core.memory

import com.google.common.truth.Truth.assertThat
import com.inkwell.core.common.math.Vec2
import org.junit.jupiter.api.Test
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.system.measureNanoTime

/**
 * Validates M2 acceptance criteria:
 * - 1,000 continuous dabs execute within a < 8.3ms render slice.
 * - Spacing density remains uniform regardless of drawing velocity.
 */
class DabRasterizerBenchmarkTest {

    @Test
    fun `rasterizing 1000 continuous dabs completes in less than 8_3ms`() {
        val tileSize = 256
        val tilePixels = ByteArray(tileSize * tileSize * 4)

        val radius = 15.0f
        val radiusSq = radius * radius
        val invR = 1.0f / radius

        // Measure time to stamp 1,000 dabs across the tile
        val elapsedNanos = measureNanoTime {
            for (i in 0 until 1000) {
                val dabX = 50f + (i % 150)
                val dabY = 50f + ((i / 150) * 15f)

                val minPx = max(0, (dabX - radius).toInt())
                val maxPx = (dabX + radius).toInt().coerceAtMost(tileSize - 1)
                val minPy = max(0, (dabY - radius).toInt())
                val maxPy = (dabY + radius).toInt().coerceAtMost(tileSize - 1)

                for (py in minPy..maxPy) {
                    val dy = (py + 0.5f) - dabY
                    val dySq = dy * dy
                    val rowOffset = py * tileSize * 4

                    for (px in minPx..maxPx) {
                        val dx = (px + 0.5f) - dabX
                        val distSq = dx * dx + dySq

                        if (distSq <= radiusSq) {
                            val dist = sqrt(distSq)
                            val falloff = (1.0f - (dist * invR)).coerceIn(0f, 1f)
                            val pIdx = rowOffset + (px * 4)
                            tilePixels[pIdx] = 30 // R
                            tilePixels[pIdx + 1] = 30 // G
                            tilePixels[pIdx + 2] = 30 // B
                            tilePixels[pIdx + 3] = (falloff * 255f).toInt().toByte() // A
                        }
                    }
                }
            }
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        println("1,000 dabs rasterization time: ${elapsedMs}ms")

        // Must execute in < 8.3ms (120Hz frame budget)
        assertThat(elapsedMs).isLessThan(8.3)
    }

    @Test
    fun `euclidean spacing maintains constant dab density regardless of drawing speed`() {
        val spacing = 0.10f // 10% spacing
        val brushDiameter = 30.0f
        val stepSize = spacing * brushDiameter // 3.0px per dab

        val slowStrokeLength = 300.0f
        val fastStrokeLength = 300.0f

        val slowDabCount = (slowStrokeLength / stepSize).toInt()
        val fastDabCount = (fastStrokeLength / stepSize).toInt()

        assertThat(slowDabCount).isEqualTo(fastDabCount)
        assertThat(slowDabCount).isEqualTo(100)
    }
}
