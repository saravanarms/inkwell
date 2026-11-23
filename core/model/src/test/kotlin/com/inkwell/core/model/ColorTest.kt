package com.inkwell.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ColorTest {

    @Test
    fun `primary colors convert between RGB and HSV accurately`() {
        val redHsv = Color.RED.toHsv()
        assertThat(redHsv[0]).isWithin(0.01f).of(0f)
        assertThat(redHsv[1]).isWithin(0.01f).of(1f)
        assertThat(redHsv[2]).isWithin(0.01f).of(1f)

        val greenHsv = Color.GREEN.toHsv()
        assertThat(greenHsv[0]).isWithin(0.01f).of(120f)
        assertThat(greenHsv[1]).isWithin(0.01f).of(1f)
        assertThat(greenHsv[2]).isWithin(0.01f).of(1f)

        val blueHsv = Color.BLUE.toHsv()
        assertThat(blueHsv[0]).isWithin(0.01f).of(240f)
        assertThat(blueHsv[1]).isWithin(0.01f).of(1f)
        assertThat(blueHsv[2]).isWithin(0.01f).of(1f)
    }

    @Test
    fun `hsv reconstruction matches original color`() {
        val original = Color(0.2f, 0.6f, 0.8f, 1.0f)
        val hsv = original.toHsv()
        val reconstructed = Color.fromHsv(hsv[0], hsv[1], hsv[2], original.alpha)

        assertThat(reconstructed.red).isWithin(0.01f).of(original.red)
        assertThat(reconstructed.green).isWithin(0.01f).of(original.green)
        assertThat(reconstructed.blue).isWithin(0.01f).of(original.blue)
    }

    @Test
    fun `hex formatting and parsing roundtrips precisely`() {
        val hex = "#3A86FF"
        val color = Color.fromHex(hex)
        assertThat(color.toHex(includeAlpha = false)).isEqualTo(hex)

        val hexAlpha = "#80FF007F"
        val colorWithAlpha = Color.fromHex(hexAlpha)
        assertThat(colorWithAlpha.toHex(includeAlpha = true)).isEqualTo(hexAlpha)
    }

    @Test
    fun `monochrome colors have zero saturation`() {
        val blackHsv = Color.BLACK.toHsv()
        assertThat(blackHsv[1]).isEqualTo(0f)
        assertThat(blackHsv[2]).isEqualTo(0f)

        val whiteHsv = Color.WHITE.toHsv()
        assertThat(whiteHsv[1]).isEqualTo(0f)
        assertThat(whiteHsv[2]).isEqualTo(1f)
    }
}
