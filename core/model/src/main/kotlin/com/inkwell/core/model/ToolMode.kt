package com.inkwell.core.model

/**
 * Primary drawing tool modes.
 */
enum class ToolMode(val id: Int) {
    PAINT(0),
    SMUDGE(1),
    ERASER(2);

    companion object {
        fun fromId(id: Int): ToolMode = entries.firstOrNull { it.id == id } ?: PAINT
    }
}
