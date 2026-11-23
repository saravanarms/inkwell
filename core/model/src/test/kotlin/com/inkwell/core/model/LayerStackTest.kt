package com.inkwell.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LayerStackTest {

    @Test
    fun `initial stack contains single default layer`() {
        val stack = LayerStack()
        assertThat(stack.layers).hasSize(1)
        assertThat(stack.activeLayer.name).isEqualTo("Layer 1")
    }

    @Test
    fun `adding layer inserts above active layer and selects it`() {
        val stack = LayerStack().addLayer(name = "Layer 2")
        assertThat(stack.layers).hasSize(2)
        assertThat(stack.activeLayer.name).isEqualTo("Layer 2")
        assertThat(stack.activeLayerIndex).isEqualTo(1)
    }

    @Test
    fun `deleting only layer is safely prevented`() {
        val stack = LayerStack()
        val modified = stack.deleteLayer(stack.activeLayerId)
        assertThat(modified.layers).hasSize(1)
    }

    @Test
    fun `reordering shifts layers accurately`() {
        var stack = LayerStack()
        stack = stack.addLayer(name = "Layer 2")
        stack = stack.addLayer(name = "Layer 3")

        // [Layer 1, Layer 2, Layer 3] -> move Layer 3 (index 2) to bottom (index 0)
        val reordered = stack.reorder(fromIndex = 2, toIndex = 0)
        assertThat(reordered.layers[0].name).isEqualTo("Layer 3")
        assertThat(reordered.layers[1].name).isEqualTo("Layer 1")
        assertThat(reordered.layers[2].name).isEqualTo("Layer 2")
    }

    @Test
    fun `bottom layer cannot be set as clipping mask`() {
        val stack = LayerStack()
        val updated = stack.setClippingMask(stack.layers[0].id, isClippingMask = true)
        assertThat(updated.layers[0].isClippingMask).isFalse()
    }

    @Test
    fun `merge down combines upper layer into lower layer`() {
        var stack = LayerStack()
        stack = stack.addLayer(name = "Layer 2")
        assertThat(stack.layers).hasSize(2)

        val merged = stack.mergeDown(layerIndex = 1)
        assertThat(merged.layers).hasSize(1)
        assertThat(merged.layers[0].name).contains("Layer 1 + Layer 2")
    }
}
