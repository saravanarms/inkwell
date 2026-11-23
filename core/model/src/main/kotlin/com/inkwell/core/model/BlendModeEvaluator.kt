package com.inkwell.core.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure mathematical implementation of all 12 layer blend modes,
 * matching standard digital painting compositing equations.
 */
object BlendModeEvaluator {

    /**
     * Evaluates the color blend function for normalized channel values S (source) and D (destination) in [0.0, 1.0].
     */
    fun blendChannel(s: Float, d: Float, mode: BlendMode): Float {
        return when (mode) {
            BlendMode.NORMAL -> s
            BlendMode.MULTIPLY -> s * d
            BlendMode.SCREEN -> s + d - (s * d)
            BlendMode.OVERLAY -> {
                if (d <= 0.5f) {
                    2.0f * s * d
                } else {
                    1.0f - 2.0f * (1.0f - s) * (1.0f - d)
                }
            }
            BlendMode.DARKEN -> min(s, d)
            BlendMode.LIGHTEN -> max(s, d)
            BlendMode.COLOR_DODGE -> {
                if (s >= 1.0f) 1.0f else min(1.0f, d / (1.0f - s))
            }
            BlendMode.ADD -> min(1.0f, s + d)
            BlendMode.COLOR_BURN -> {
                if (s <= 0.0f) 0.0f else 1.0f - min(1.0f, (1.0f - d) / s)
            }
            BlendMode.SOFT_LIGHT -> {
                if (s <= 0.5f) {
                    d - (1.0f - 2.0f * s) * d * (1.0f - d)
                } else {
                    val dSqrt = if (d <= 0.25f) ((16.0f * d - 12.0f) * d + 4.0f) * d else sqrt(d)
                    d + (2.0f * s - 1.0f) * (dSqrt - d)
                }
            }
            BlendMode.HARD_LIGHT -> {
                if (s <= 0.5f) {
                    2.0f * s * d
                } else {
                    1.0f - 2.0f * (1.0f - s) * (1.0f - d)
                }
            }
            BlendMode.DIFFERENCE -> abs(s - d)
        }.coerceIn(0.0f, 1.0f)
    }

    /**
     * Complete Porter-Duff Over composite with layer opacity and blend mode applied.
     */
    fun compositePixel(
        srcR: Float, srcG: Float, srcB: Float, srcAlpha: Float,
        dstR: Float, dstG: Float, dstB: Float, dstAlpha: Float,
        layerOpacity: Float,
        blendMode: BlendMode
    ): FloatArray {
        val effectiveSrcA = srcAlpha * layerOpacity
        if (effectiveSrcA <= 0.0f) {
            return floatArrayOf(dstR, dstG, dstB, dstAlpha)
        }
        if (dstAlpha <= 0.0f) {
            return floatArrayOf(srcR, srcG, srcB, effectiveSrcA)
        }

        val outA = effectiveSrcA + dstAlpha * (1.0f - effectiveSrcA)

        val blendedR = blendChannel(srcR, dstR, blendMode)
        val blendedG = blendChannel(srcG, dstG, blendMode)
        val blendedB = blendChannel(srcB, dstB, blendMode)

        fun calcOutChannel(srcC: Float, dstC: Float, blendC: Float): Float {
            val num = effectiveSrcA * (1.0f - dstAlpha) * srcC +
                    dstAlpha * (1.0f - effectiveSrcA) * dstC +
                    effectiveSrcA * dstAlpha * blendC
            return (num / outA).coerceIn(0.0f, 1.0f)
        }

        return floatArrayOf(
            calcOutChannel(srcR, dstR, blendedR),
            calcOutChannel(srcG, dstG, blendedG),
            calcOutChannel(srcB, dstB, blendedB),
            outA.coerceIn(0.0f, 1.0f)
        )
    }
}
