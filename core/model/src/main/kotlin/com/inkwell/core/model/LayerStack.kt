package com.inkwell.core.model

/**
 * Manages the ordered stack of layers and layer state mutations.
 * Index 0 is the bottom-most layer, and index (size - 1) is the top-most layer.
 */
data class LayerStack(
    val layers: List<Layer> = listOf(Layer(name = "Layer 1")),
    val activeLayerId: String = layers.first().id
) {
    init {
        require(layers.isNotEmpty()) { "Layer stack must contain at least one layer" }
        require(layers.any { it.id == activeLayerId }) { "Active layer ID $activeLayerId must exist in layer stack" }
    }

    val activeLayer: Layer get() = layers.first { it.id == activeLayerId }
    val activeLayerIndex: Int get() = layers.indexOfFirst { it.id == activeLayerId }

    fun addLayer(
        name: String = "Layer ${layers.size + 1}",
        blendMode: BlendMode = BlendMode.NORMAL,
        insertAboveActive: Boolean = true
    ): LayerStack {
        val newLayer = Layer(name = name, blendMode = blendMode)
        val mutable = layers.toMutableList()
        val insertIndex = if (insertAboveActive) activeLayerIndex + 1 else mutable.size
        mutable.add(insertIndex.coerceIn(0, mutable.size), newLayer)
        return copy(layers = mutable, activeLayerId = newLayer.id)
    }

    fun deleteLayer(id: String): LayerStack {
        if (layers.size <= 1) return this // Cannot delete only remaining layer

        val mutable = layers.toMutableList()
        val removeIndex = mutable.indexOfFirst { it.id == id }
        if (removeIndex == -1) return this

        mutable.removeAt(removeIndex)
        val newActiveId = if (activeLayerId == id) {
            val fallbackIndex = removeIndex.coerceAtMost(mutable.size - 1)
            mutable[fallbackIndex].id
        } else {
            activeLayerId
        }

        return copy(layers = mutable, activeLayerId = newActiveId)
    }

    fun selectLayer(id: String): LayerStack {
        if (layers.none { it.id == id }) return this
        return copy(activeLayerId = id)
    }

    fun reorder(fromIndex: Int, toIndex: Int): LayerStack {
        if (fromIndex == toIndex) return this
        if (fromIndex !in layers.indices || toIndex !in layers.indices) return this

        val mutable = layers.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return copy(layers = mutable)
    }

    fun updateLayer(id: String, transform: (Layer) -> Layer): LayerStack {
        val mutable = layers.map { if (it.id == id) transform(it) else it }
        return copy(layers = mutable)
    }

    fun setOpacity(id: String, opacity: Float): LayerStack {
        return updateLayer(id) { it.copy(opacity = opacity.coerceIn(0.0f, 1.0f)) }
    }

    fun setVisibility(id: String, isVisible: Boolean): LayerStack {
        return updateLayer(id) { it.copy(isVisible = isVisible) }
    }

    fun setBlendMode(id: String, blendMode: BlendMode): LayerStack {
        return updateLayer(id) { it.copy(blendMode = blendMode) }
    }

    fun setAlphaLock(id: String, locked: Boolean): LayerStack {
        return updateLayer(id) { it.copy(isAlphaLocked = locked) }
    }

    fun setClippingMask(id: String, isClippingMask: Boolean): LayerStack {
        val idx = layers.indexOfFirst { it.id == id }
        if (idx <= 0 && isClippingMask) return this // Bottom layer cannot be a clipping mask
        return updateLayer(id) { it.copy(isClippingMask = isClippingMask) }
    }

    fun mergeDown(layerIndex: Int): LayerStack {
        if (layerIndex <= 0 || layerIndex !in layers.indices) return this

        val mutable = layers.toMutableList()
        val upperLayer = mutable.removeAt(layerIndex)
        val lowerLayer = mutable[layerIndex - 1]

        // Merge upper into lower
        val mergedLayer = lowerLayer.copy(name = "${lowerLayer.name} + ${upperLayer.name}")
        mutable[layerIndex - 1] = mergedLayer

        return copy(layers = mutable, activeLayerId = mergedLayer.id)
    }
}
