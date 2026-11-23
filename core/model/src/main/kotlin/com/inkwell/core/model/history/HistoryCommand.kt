package com.inkwell.core.model.history

import com.inkwell.core.model.BlendMode
import com.inkwell.core.model.Layer

/**
 * Base interface for reversible commands in the Inkwell undo/redo history system.
 */
interface HistoryCommand {
    val description: String
    val memoryCostBytes: Long

    fun undo()
    fun redo()
}

/**
 * Tile-diff delta record for a single modified canvas tile.
 */
data class TileDeltaRecord(
    val tileKey: Long,
    val preStrokeCompressed: ByteArray,
    val postStrokeCompressed: ByteArray
) {
    val memoryCost: Long = (preStrokeCompressed.size + postStrokeCompressed.size + 16L)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TileDeltaRecord
        return tileKey == other.tileKey &&
                preStrokeCompressed.contentEquals(other.preStrokeCompressed) &&
                postStrokeCompressed.contentEquals(other.postStrokeCompressed)
    }

    override fun hashCode(): Int {
        var result = tileKey.hashCode()
        result = 31 * result + preStrokeCompressed.contentHashCode()
        result = 31 * result + postStrokeCompressed.contentHashCode()
        return result
    }
}

/**
 * History command recording brush stroke tile deltas on a specific layer.
 */
class StrokeHistoryCommand(
    val layerId: Int,
    val deltas: List<TileDeltaRecord>,
    override val description: String = "Brush Stroke",
    private val onApplyDeltas: (layerId: Int, isUndo: Boolean, deltas: List<TileDeltaRecord>) -> Unit
) : HistoryCommand {

    override val memoryCostBytes: Long = deltas.sumOf { it.memoryCost } + 64L

    override fun undo() {
        onApplyDeltas(layerId, true, deltas)
    }

    override fun redo() {
        onApplyDeltas(layerId, false, deltas)
    }
}

/**
 * History command for structural layer additions.
 */
class LayerAddHistoryCommand(
    val layer: Layer,
    val index: Int,
    override val description: String = "Add Layer",
    private val onUndo: (layerId: String) -> Unit,
    private val onRedo: (layer: Layer, index: Int) -> Unit
) : HistoryCommand {

    override val memoryCostBytes: Long = 256L

    override fun undo() {
        onUndo(layer.id)
    }

    override fun redo() {
        onRedo(layer, index)
    }
}

/**
 * History command for layer deletions.
 */
class LayerDeleteHistoryCommand(
    val layer: Layer,
    val index: Int,
    val preservedTiles: List<TileDeltaRecord> = emptyList(),
    override val description: String = "Delete Layer",
    private val onUndo: (layer: Layer, index: Int, tiles: List<TileDeltaRecord>) -> Unit,
    private val onRedo: (layerId: String) -> Unit
) : HistoryCommand {

    override val memoryCostBytes: Long = preservedTiles.sumOf { it.memoryCost } + 512L

    override fun undo() {
        onUndo(layer, index, preservedTiles)
    }

    override fun redo() {
        onRedo(layer.id)
    }
}

/**
 * History command for layer property changes (opacity, visibility, blend mode, alpha lock, clipping mask).
 */
class LayerPropertyHistoryCommand(
    val layerId: String,
    val oldOpacity: Float,
    val newOpacity: Float,
    val oldVisible: Boolean,
    val newVisible: Boolean,
    val oldBlendMode: BlendMode,
    val newBlendMode: BlendMode,
    val oldAlphaLock: Boolean,
    val newAlphaLock: Boolean,
    val oldClippingMask: Boolean,
    val newClippingMask: Boolean,
    override val description: String = "Layer Property Change",
    private val onApply: (
        layerId: String,
        opacity: Float,
        visible: Boolean,
        blendMode: BlendMode,
        alphaLock: Boolean,
        clippingMask: Boolean
    ) -> Unit
) : HistoryCommand {

    override val memoryCostBytes: Long = 128L

    override fun undo() {
        onApply(layerId, oldOpacity, oldVisible, oldBlendMode, oldAlphaLock, oldClippingMask)
    }

    override fun redo() {
        onApply(layerId, newOpacity, newVisible, newBlendMode, newAlphaLock, newClippingMask)
    }
}
