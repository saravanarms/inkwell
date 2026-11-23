package com.inkwell.core.model.palette

import com.inkwell.core.model.Color
import java.util.UUID

/**
 * Represents a curated palette of colors with an identifier and name.
 */
data class Palette(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val swatches: List<Color>,
    val isDefault: Boolean = false
) {
    /**
     * Serializes this palette to a clean JSON string representation.
     */
    fun toJson(): String {
        val swatchesJson = swatches.joinToString(separator = ",") { color ->
            "\"${color.toHex(includeAlpha = true)}\""
        }
        val escapedName = name.replace("\"", "\\\"")
        return """{"id":"$id","name":"$escapedName","isDefault":$isDefault,"swatches":[$swatchesJson]}"""
    }

    companion object {
        val ESSENTIALS = Palette(
            id = "palette_essentials",
            name = "Studio Essentials",
            isDefault = true,
            swatches = listOf(
                Color.fromRgbInt(20, 20, 22),       // Jet Black
                Color.fromRgbInt(245, 245, 250),    // Pure White
                Color.fromRgbInt(120, 125, 135),    // Medium Neutral Gray
                Color.fromRgbInt(220, 45, 55),      // Crimson Red
                Color.fromRgbInt(245, 130, 32),     // Cadmium Orange
                Color.fromRgbInt(250, 210, 40),     // Primary Yellow
                Color.fromRgbInt(46, 175, 80),      // Emerald Green
                Color.fromRgbInt(33, 150, 243),     // Cobalt Blue
                Color.fromRgbInt(125, 60, 215),     // Royal Purple
                Color.fromRgbInt(240, 180, 160),    // Warm Flesh Tint
                Color.fromRgbInt(140, 80, 50),      // Burnt Sienna
                Color.fromRgbInt(70, 40, 25)        // Espresso Brown
            )
        )

        val PASTEL = Palette(
            id = "palette_pastel",
            name = "Pastel Dreams",
            swatches = listOf(
                Color.fromRgbInt(255, 204, 213),    // Powder Pink
                Color.fromRgbInt(255, 230, 204),    // Apricot Creme
                Color.fromRgbInt(255, 250, 204),    // Pale Lemon
                Color.fromRgbInt(214, 255, 214),    // Mint Leaf
                Color.fromRgbInt(204, 240, 255),    // Cloud Blue
                Color.fromRgbInt(224, 214, 255),    // Soft Wisteria
                Color.fromRgbInt(242, 218, 255),    // Lilac Petal
                Color.fromRgbInt(220, 235, 225)     // Celadon
            )
        )

        val EARTH_TONES = Palette(
            id = "palette_earth",
            name = "Earth & Foliage",
            swatches = listOf(
                Color.fromRgbInt(45, 75, 40),       // Deep Pine
                Color.fromRgbInt(90, 125, 65),      // Olive Grove
                Color.fromRgbInt(160, 175, 90),     // Lichen
                Color.fromRgbInt(210, 180, 110),    // Raw Ochre
                Color.fromRgbInt(185, 120, 65),     // Terracotta
                Color.fromRgbInt(135, 75, 45),      // Burnt Umber
                Color.fromRgbInt(80, 50, 35),       // Dark Earth
                Color.fromRgbInt(195, 185, 160)     // River Pebble
            )
        )

        val NEON = Palette(
            id = "palette_neon",
            name = "Cyberpunk Neon",
            swatches = listOf(
                Color.fromRgbInt(255, 0, 128),      // Laser Magenta
                Color.fromRgbInt(0, 255, 240),      // Electric Cyan
                Color.fromRgbInt(180, 255, 0),      // Acid Lime
                Color.fromRgbInt(255, 230, 0),      // Warning Yellow
                Color.fromRgbInt(255, 90, 0),       // Hyper Orange
                Color.fromRgbInt(140, 0, 255),      // Ultra Violet
                Color.fromRgbInt(0, 255, 130),      // Matrix Green
                Color.fromRgbInt(15, 15, 25)        // Void Black
            )
        )

        val DEFAULT_PRESETS = listOf(ESSENTIALS, PASTEL, EARTH_TONES, NEON)

        /**
         * Parses a JSON string back into a Palette object.
         */
        fun fromJson(json: String): Palette {
            val idMatch = """"id"\s*:\s*"([^"]+)"""".toRegex().find(json)
            val nameMatch = """"name"\s*:\s*"([^"]+)"""".toRegex().find(json)
            val isDefaultMatch = """"isDefault"\s*:\s*(true|false)""".toRegex().find(json)

            val id = idMatch?.groupValues?.get(1) ?: UUID.randomUUID().toString()
            val name = nameMatch?.groupValues?.get(1)?.replace("\\\"", "\"") ?: "Untitled"
            val isDefault = isDefaultMatch?.groupValues?.get(1)?.toBoolean() ?: false

            val swatches = mutableListOf<Color>()
            val swatchesSection = json.substringAfter("\"swatches\":", "").substringBefore("]")
            val hexMatches = """"#([0-9a-fA-F]{6,8})"""".toRegex().findAll(swatchesSection)
            for (m in hexMatches) {
                swatches.add(Color.fromHex(m.groupValues[1]))
            }

            return Palette(id = id, name = name, swatches = swatches, isDefault = isDefault)
        }
    }
}
