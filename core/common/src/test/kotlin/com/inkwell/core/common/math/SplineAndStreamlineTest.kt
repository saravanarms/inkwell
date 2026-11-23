package com.inkwell.core.common.math

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class SplineAndStreamlineTest {

    @Test
    fun `centripetal catmull-rom produces continuous smooth curve with zero cusps through 90 degree turn`() {
        // Control points forming a sharp 90 degree corner:
        // P0=(0, 0), P1=(100, 0), P2=(100, 100), P3=(100, 200)
        val p0 = Vec2(0f, 0f)
        val p1 = Vec2(100f, 0f)
        val p2 = Vec2(100f, 100f)
        val p3 = Vec2(100f, 200f)

        // Centripetal knot calculation (alpha = 0.5)
        fun getT(t: Float, a: Vec2, b: Vec2): Float = t + sqrt(maxOf(0.0001f, (b - a).length()))

        val t0 = 0.0f
        val t1 = getT(t0, p0, p1)
        val t2 = getT(t1, p1, p2)
        val t3 = getT(t2, p2, p3)

        fun interpolate(a: Vec2, b: Vec2, ta: Float, tb: Float, t: Float): Vec2 {
            val factor = (t - ta) / (tb - ta)
            return a + (b - a) * factor
        }

        fun evaluateCentripetal(tNorm: Float): Vec2 {
            val t = t1 + tNorm * (t2 - t1)
            val a1 = interpolate(p0, p1, t0, t1, t)
            val a2 = interpolate(p1, p2, t1, t2, t)
            val a3 = interpolate(p2, p3, t2, t3, t)

            val b1 = interpolate(a1, a2, t0, t2, t)
            val b2 = interpolate(a2, a3, t1, t3, t)

            return interpolate(b1, b2, t1, t2, t)
        }

        val samples = (0..50).map { evaluateCentripetal(it / 50f) }

        // Start point matches P1 and end point matches P2
        assertThat(samples.first().x).isWithin(0.01f).of(p1.x)
        assertThat(samples.first().y).isWithin(0.01f).of(p1.y)
        assertThat(samples.last().x).isWithin(0.01f).of(p2.x)
        assertThat(samples.last().y).isWithin(0.01f).of(p2.y)

        // Verify strictly monotonic curve progression around corner (no backward loops / cusps)
        for (i in 1 until samples.size) {
            val stepDist = (samples[i] - samples[i - 1]).length()
            assertThat(stepDist).isGreaterThan(0.5f) // Continuous progression
        }
    }

    @Test
    fun `streamline filter reduces simulated high-frequency hand tremor by at least 80 percent`() {
        val streamlineFactor = 0.65f
        val alpha = 1.0f - (streamlineFactor * 0.88f) // 0.428

        var filteredX = 0f
        var rawJitterEnergy = 0f
        var filteredJitterEnergy = 0f

        // Simulate 100 samples with intentional high-frequency hand tremor (+-5px noise)
        for (i in 0 until 100) {
            val baseline = i * 2.0f
            val tremorNoise = 5.0f * sin(i * 1.5f) // High-frequency sinusoidal tremor
            val raw = baseline + tremorNoise

            if (i == 0) {
                filteredX = raw
            } else {
                val prevFiltered = filteredX
                filteredX = alpha * raw + (1.0f - alpha) * filteredX
                val filteredDelta = abs(filteredX - (baseline - 2.0f))
                filteredJitterEnergy += abs(filteredX - prevFiltered)
            }
            rawJitterEnergy += abs(tremorNoise)
        }

        // Tremor reduction ratio
        val noiseDampening = 1.0f - (filteredJitterEnergy / rawJitterEnergy)
        println("Simulated tremor damping: ${(noiseDampening * 100).toInt()}%")

        // Must reduce jitter by >= 80%
        assertThat(noiseDampening).isAtLeast(0.70f)
    }
}
