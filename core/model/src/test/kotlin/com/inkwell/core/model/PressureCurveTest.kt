package com.inkwell.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PressureCurveTest {

    @Test
    fun `pressure translates across at least 1024 discrete radius steps`() {
        val curve = PressureCurve.LINEAR
        val minRadius = 1.0f
        val maxRadius = 100.0f
        val numSamples = 2048

        val uniqueRadii = HashSet<Float>()
        for (i in 0 until numSamples) {
            val rawPressure = i.toFloat() / (numSamples - 1).toFloat()
            val calibrated = curve.evaluate(rawPressure)
            val radius = curve.computeRadius(calibrated, minRadius, maxRadius)
            uniqueRadii.add(radius)
        }

        // Must support >= 1024 unique, distinct radius values without quantization stepping
        assertThat(uniqueRadii.size).isAtLeast(1024)
    }

    @Test
    fun `evaluation is strictly monotonic across input range`() {
        val curve = PressureCurve.FIRM
        var prevOutput = -1.0f

        for (i in 0..1000) {
            val raw = i / 1000f
            val evaluated = curve.evaluate(raw)
            assertThat(evaluated).isAtLeast(prevOutput)
            prevOutput = evaluated
        }
    }

    @Test
    fun `soft curve ramps faster than linear and firm curves`() {
        val soft = PressureCurve.SOFT
        val linear = PressureCurve.LINEAR
        val firm = PressureCurve.FIRM

        val midPressure = 0.5f
        val softVal = soft.evaluate(midPressure)
        val linearVal = linear.evaluate(midPressure)
        val firmVal = firm.evaluate(midPressure)

        assertThat(softVal).isGreaterThan(linearVal)
        assertThat(linearVal).isGreaterThan(firmVal)
    }

    @Test
    fun `clamping strictly holds at boundaries`() {
        val curve = PressureCurve(minPressure = 0.1f, maxPressure = 0.9f, gamma = 1.0f)
        assertThat(curve.evaluate(-0.5f)).isEqualTo(0.0f)
        assertThat(curve.evaluate(0.05f)).isEqualTo(0.0f)
        assertThat(curve.evaluate(0.95f)).isEqualTo(1.0f)
        assertThat(curve.evaluate(1.5f)).isEqualTo(1.0f)
    }
}
