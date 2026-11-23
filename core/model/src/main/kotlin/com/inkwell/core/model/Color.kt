package com.inkwell.core.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Normalized RGBA color model with bidirectional HSV and Hexadecimal conversions.
 */
data class Color(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float = 1.0f
) {
    init {
        require(red in 0f..1f && green in 0f..1f && blue in 0f..1f && alpha in 0f..1f) {
            "Color channels must be normalized in [0.0, 1.0]. Provided: R=$red, G=$green, B=$blue, A=$alpha"
        }
    }

    val redInt: Int get() = (red * 255f).roundToInt().coerceIn(0, 255)
    val greenInt: Int get() = (green * 255f).roundToInt().coerceIn(0, 255)
    val blueInt: Int get() = (blue * 255f).roundToInt().coerceIn(0, 255)
    val alphaInt: Int get() = (alpha * 255f).roundToInt().coerceIn(0, 255)

    /**
     * Converts RGBA to HSV components:
     * - Hue: [0.0, 360.0)
     * - Saturation: [0.0, 1.0]
     * - Value (Brightness): [0.0, 1.0]
     */
    fun toHsv(): FloatArray {
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val delta = max - min

        var h = 0f
        if (delta > 0f) {
            h = when (max) {
                red -> 60f * (((green - blue) / delta) % 6f)
                green -> 60f * (((blue - red) / delta) + 2f)
                else -> 60f * (((red - green) / delta) + 4f)
            }
            if (h < 0f) h += 360f
        }

        val s = if (max == 0f) 0f else delta / max
        val v = max

        return floatArrayOf(h, s, v)
    }

    fun toHex(includeAlpha: Boolean = false): String {
        return if (includeAlpha) {
            String.format("#%02X%02X%02X%02X", alphaInt, redInt, greenInt, blueInt)
        } else {
            String.format("#%02X%02X%02X", redInt, greenInt, blueInt)
        }
    }

    fun toPackedRgba(): Int {
        return (redInt shl 24) or (greenInt shl 16) or (blueInt shl 8) or alphaInt
    }

    companion object {
        val BLACK = Color(0f, 0f, 0f, 1f)
        val WHITE = Color(1f, 1f, 1f, 1f)
        val RED = Color(1f, 0f, 0f, 1f)
        val GREEN = Color(0f, 1f, 0f, 1f)
        val BLUE = Color(0f, 0f, 1f, 1f)
        val TRANSPARENT = Color(0f, 0f, 0f, 0f)

        fun fromRgbInt(r: Int, g: Int, b: Int, a: Int = 255): Color {
            return Color(
                red = r.coerceIn(0, 255) / 255f,
                green = g.coerceIn(0, 255) / 255f,
                blue = b.coerceIn(0, 255) / 255f,
                alpha = a.coerceIn(0, 255) / 255f
            )
        }

        fun fromHsv(hue: Float, saturation: Float, value: Float, alpha: Float = 1.0f): Color {
            val h = (hue % 360f + 360f) % 360f
            val s = saturation.coerceIn(0f, 1f)
            val v = value.coerceIn(0f, 1f)

            val c = v * s
            val x = c * (1f - abs((h / 60f) % 2f - 1f))
            val m = v - c

            val (rPrime, gPrime, bPrime) = when ((h / 60f).toInt()) {
                0 -> Triple(c, x, 0f)
                1 -> Triple(x, c, 0f)
                2 -> Triple(0f, c, x)
                3 -> Triple(0f, x, c)
                4 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }

            return Color(rPrime + m, gPrime + m, bPrime + m, alpha.coerceIn(0f, 1f))
        }

        fun fromHex(hexString: String): Color {
            val cleanHex = hexString.removePrefix("#").trim()
            return when (cleanHex.length) {
                6 -> {
                    val r = cleanHex.substring(0, 2).toInt(16)
                    val g = cleanHex.substring(2, 4).toInt(16)
                    val b = cleanHex.substring(4, 6).toInt(16)
                    fromRgbInt(r, g, b, 255)
                }
                8 -> {
                    val a = cleanHex.substring(0, 2).toInt(16)
                    val r = cleanHex.substring(2, 4).toInt(16)
                    val g = cleanHex.substring(4, 6).toInt(16)
                    val b = cleanHex.substring(6, 8).toInt(16)
                    fromRgbInt(r, g, b, a)
                }
                else -> throw IllegalArgumentException("Invalid hex color format: $hexString")
            }
        }
    }
}
