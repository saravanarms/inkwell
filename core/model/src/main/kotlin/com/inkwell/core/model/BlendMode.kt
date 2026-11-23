package com.inkwell.core.model

/**
 * 12 standard layer blend modes supported by the Inkwell composite shader pipeline.
 */
enum class BlendMode(val id: Int, val displayName: String) {
    NORMAL(0, "Normal"),
    MULTIPLY(1, "Multiply"),
    SCREEN(2, "Screen"),
    OVERLAY(3, "Overlay"),
    DARKEN(4, "Darken"),
    LIGHTEN(5, "Lighten"),
    COLOR_DODGE(6, "Color Dodge"),
    ADD(7, "Add (Linear Dodge)"),
    COLOR_BURN(8, "Color Burn"),
    SOFT_LIGHT(9, "Soft Light"),
    HARD_LIGHT(10, "Hard Light"),
    DIFFERENCE(11, "Difference");

    companion object {
        fun fromId(id: Int): BlendMode = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
