package com.inkwell.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BlendModeEvaluatorTest {

    private val maxDelta = 1.0f / 255.0f // < 1/255 channel error tolerance

    @Test
    fun `multiply darkens colors accurately`() {
        val s = 0.5f
        val d = 0.5f
        val result = BlendModeEvaluator.blendChannel(s, d, BlendMode.MULTIPLY)
        assertThat(result).isWithin(maxDelta).of(0.25f)
    }

    @Test
    fun `screen lightens colors accurately`() {
        val s = 0.5f
        val d = 0.5f
        val result = BlendModeEvaluator.blendChannel(s, d, BlendMode.SCREEN)
        // 0.5 + 0.5 - 0.25 = 0.75
        assertThat(result).isWithin(maxDelta).of(0.75f)
    }

    @Test
    fun `overlay preserves darks and highlights`() {
        // Dark base
        val darkResult = BlendModeEvaluator.blendChannel(0.5f, 0.25f, BlendMode.OVERLAY)
        // 2 * 0.5 * 0.25 = 0.25
        assertThat(darkResult).isWithin(maxDelta).of(0.25f)

        // Light base
        val lightResult = BlendModeEvaluator.blendChannel(0.5f, 0.75f, BlendMode.OVERLAY)
        // 1 - 2*(1-0.5)*(1-0.75) = 1 - 2*0.5*0.25 = 0.75
        assertThat(lightResult).isWithin(maxDelta).of(0.75f)
    }

    @Test
    fun `difference of identical colors is zero`() {
        val s = 0.732f
        val d = 0.732f
        val result = BlendModeEvaluator.blendChannel(s, d, BlendMode.DIFFERENCE)
        assertThat(result).isEqualTo(0.0f)
    }

    @Test
    fun `all 12 blend equations evaluate within valid unit range`() {
        val testValues = listOf(0.0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1.0f)

        for (mode in BlendMode.entries) {
            for (s in testValues) {
                for (d in testValues) {
                    val out = BlendModeEvaluator.blendChannel(s, d, mode)
                    assertThat(out).isIn(com.google.common.collect.Range.closed(0.0f, 1.0f))
                }
            }
        }
    }

    @Test
    fun `full composite pixel incorporates layer opacity cleanly`() {
        val comp = BlendModeEvaluator.compositePixel(
            srcR = 1.0f, srcG = 0.0f, srcB = 0.0f, srcAlpha = 1.0f, // Red
            dstR = 0.0f, dstG = 0.0f, dstB = 1.0f, dstAlpha = 1.0f, // Blue
            layerOpacity = 0.5f,
            blendMode = BlendMode.NORMAL
        )

        // 50% opacity red over 100% blue -> purple (0.5 R, 0.0 G, 0.5 B, 1.0 A)
        assertThat(comp[0]).isWithin(maxDelta).of(0.5f)
        assertThat(comp[1]).isWithin(maxDelta).of(0.0f)
        assertThat(comp[2]).isWithin(maxDelta).of(0.5f)
        assertThat(comp[3]).isWithin(maxDelta).of(1.0f)
    }
}
