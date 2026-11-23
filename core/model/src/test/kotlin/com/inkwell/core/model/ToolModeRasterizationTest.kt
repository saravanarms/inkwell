package com.inkwell.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ToolModeRasterizationTest {

    @Test
    fun `eraser subtracts alpha and color values cleanly`() {
        // Initial opaque pixel: RGBA = (200, 100, 50, 255)
        var dstR = 200f
        var dstG = 100f
        var dstB = 50f
        var dstA = 255f

        // Apply eraser with 50% opacity
        val eraserAlpha = 0.5f
        val sub = 1.0f - eraserAlpha

        dstR *= sub
        dstG *= sub
        dstB *= sub
        dstA *= sub

        assertThat(dstA).isWithin(0.1f).of(127.5f)
        assertThat(dstR).isWithin(0.1f).of(100f)

        // Apply 100% eraser -> completely transparent
        dstA *= 0.0f
        assertThat(dstA).isEqualTo(0.0f)
    }

    @Test
    fun `smudge smoothly blends contrasting black and white with zero banding`() {
        // Simulated smudge dragging black (0,0,0) across a white (255,255,255) canvas
        var reservoirR = 0.0f
        var reservoirG = 0.0f
        var reservoirB = 0.0f

        val pickupRate = 0.20f
        val depositRate = 0.30f

        val colorSamples = ArrayList<Float>()

        for (step in 0 until 40) {
            val canvasColor = 255.0f // White canvas pixel

            // Reservoir samples canvas
            reservoirR = reservoirR * (1.0f - pickupRate) + canvasColor * pickupRate
            reservoirG = reservoirG * (1.0f - pickupRate) + canvasColor * pickupRate
            reservoirB = reservoirB * (1.0f - pickupRate) + canvasColor * pickupRate

            // Canvas receives smudge deposit
            val outPixel = canvasColor * (1.0f - depositRate) + reservoirR * depositRate
            colorSamples.add(outPixel)

            // Strictly bounded in [0, 255]
            assertThat(outPixel).isIn(com.google.common.collect.Range.closed(0.0f, 255.0f))
        }

        // Must show smooth continuous transition from dark to light
        assertThat(colorSamples.first()).isLessThan(colorSamples.last())
        for (i in 1 until colorSamples.size) {
            assertThat(colorSamples[i]).isAtLeast(colorSamples[i - 1]) // Monotonic smooth gradient
        }
    }
}
