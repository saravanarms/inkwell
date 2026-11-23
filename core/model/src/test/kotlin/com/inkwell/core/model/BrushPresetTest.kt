package com.inkwell.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BrushPresetTest {

    @Test
    fun `default presets catalog contains exactly 12 production brushes`() {
        assertThat(BrushPreset.DEFAULT_PRESETS).hasSize(12)
    }

    @Test
    fun `all presets have unique identifiers`() {
        val ids = BrushPreset.DEFAULT_PRESETS.map { it.id }
        assertThat(ids.toSet()).hasSize(12)
    }

    @Test
    fun `catalog includes all three tool modes`() {
        val modes = BrushPreset.DEFAULT_PRESETS.map { it.toolMode }.toSet()
        assertThat(modes).containsExactly(
            ToolMode.PAINT,
            ToolMode.SMUDGE,
            ToolMode.ERASER
        )
    }

    @Test
    fun `preset sizes and spacing parameters conform to strict invariants`() {
        for (preset in BrushPreset.DEFAULT_PRESETS) {
            assertThat(preset.minSize).isGreaterThan(0f)
            assertThat(preset.maxSize).isAtLeast(preset.minSize)
            assertThat(preset.spacing).isIn(com.google.common.collect.Range.closed(0.01f, 1.0f))
            assertThat(preset.hardness).isIn(com.google.common.collect.Range.closed(0.0f, 1.0f))
            assertThat(preset.streamline).isIn(com.google.common.collect.Range.closed(0.0f, 0.95f))
        }
    }
}
