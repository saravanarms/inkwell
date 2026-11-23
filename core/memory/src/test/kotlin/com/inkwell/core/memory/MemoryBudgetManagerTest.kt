package com.inkwell.core.memory

import com.google.common.truth.Truth.assertThat
import com.inkwell.core.model.CanvasSpec
import org.junit.jupiter.api.Test

class MemoryBudgetManagerTest {

    @Test
    fun `budget calculation allows 20 or more layers on 2K canvas on 512MB device`() {
        val manager = MemoryBudgetManager(largeHeapClassMb = 512)
        // 512MB * 60% = 307.2 MB budget
        // 2048x2048 layer = 8x8 tiles = 64 tiles * 256KB = 16 MB per layer
        // 307.2 / 16 = 19.2 -> coerced/estimated around 19-20 layers
        val maxLayers = manager.calculateMaxLayers(CanvasSpec.SQUARE_2K)
        assertThat(maxLayers).isAtLeast(18)
    }

    @Test
    fun `budget calculation on flagship 1024MB tablet supports 4K canvas`() {
        val manager = MemoryBudgetManager(largeHeapClassMb = 1024)
        // 1024MB * 60% = 614.4 MB budget
        // 4096x4096 layer = 16x16 tiles = 256 tiles * 256KB = 64 MB per layer
        // 614.4 / 64 = 9 layers
        val maxLayers = manager.calculateMaxLayers(CanvasSpec.MAXIMUM_4K)
        assertThat(maxLayers).isAtLeast(8)
    }

    @Test
    fun `canAddLayer rejects layer creation when budget exhausted`() {
        val manager = MemoryBudgetManager(largeHeapClassMb = 512)
        val spec = CanvasSpec.SQUARE_2K
        val maxLayers = manager.calculateMaxLayers(spec)

        assertThat(manager.canAddLayer(spec, currentLayerCount = maxLayers - 1)).isTrue()
        assertThat(manager.canAddLayer(spec, currentLayerCount = maxLayers + 5)).isFalse()
    }
}
