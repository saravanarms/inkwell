package com.inkwell.core.model

import kotlin.math.pow

/**
 * Calibrated pressure curve mapping raw digitizer hardware pressure [0.0, 1.0]
 * to calibrated dynamic brush dynamics across >= 1,024 discrete steps.
 */
data class PressureCurve(
    val minPressure: Float = 0.0f,
    val maxPressure: Float = 1.0f,
    val gamma: Float = 1.0f // 1.0 = linear, < 1.0 = soft (faster ramp), > 1.0 = firm (needs more force)
) {
    init {
        require(minPressure in 0.0f..0.9f) { "minPressure must be in [0.0, 0.9]" }
        require(maxPressure in (minPressure + 0.1f)..1.0f) { "maxPressure must be in (minPressure, 1.0]" }
        require(gamma in 0.2f..5.0f) { "gamma must be in [0.2, 5.0]" }
    }

    /**
     * Maps raw hardware pressure [0.0, 1.0] to calibrated response [0.0, 1.0].
     */
    fun evaluate(rawPressure: Float): Float {
        if (rawPressure <= minPressure) return 0.0f
        if (rawPressure >= maxPressure) return 1.0f

        val normalized = (rawPressure - minPressure) / (maxPressure - minPressure)
        return normalized.pow(gamma).coerceIn(0.0f, 1.0f)
    }

    /**
     * Computes the dynamic brush radius given a calibrated pressure value.
     */
    fun computeRadius(calibratedPressure: Float, minRadius: Float, maxRadius: Float): Float {
        return minRadius + calibratedPressure * (maxRadius - minRadius)
    }

    companion object {
        val LINEAR = PressureCurve(0.0f, 1.0f, 1.0f)
        val SOFT = PressureCurve(0.0f, 1.0f, 0.7f)
        val FIRM = PressureCurve(0.05f, 1.0f, 1.5f)
    }
}
