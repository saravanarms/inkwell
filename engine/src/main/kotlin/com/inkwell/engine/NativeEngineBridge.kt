package com.inkwell.engine

import android.view.Surface
import com.inkwell.core.common.math.Vec2
import com.inkwell.core.model.ToolMode
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-level Kotlin bridge to the native C++ graphics, viewport, layer, stroke, and tile engine.
 */
class NativeEngineBridge(
    val width: Int,
    val height: Int,
    val tileSize: Int = 256
) : Closeable {

    private var nativeHandle: Long = 0
    private val isClosed = AtomicBoolean(false)
    private val coordBuffer = FloatArray(2)

    init {
        nativeHandle = nativeCreateEngine(width, height, tileSize)
        check(nativeHandle != 0L) { "Failed to allocate native InkwellEngine" }
    }

    fun initialize(): Boolean {
        checkNotClosed()
        return nativeInitialize(nativeHandle)
    }

    fun setSurface(surface: Surface?): Boolean {
        checkNotClosed()
        return nativeSetSurface(nativeHandle, surface)
    }

    fun surfaceChanged(width: Int, height: Int) {
        checkNotClosed()
        nativeSurfaceChanged(nativeHandle, width, height)
    }

    fun surfaceDestroyed() {
        if (!isClosed.get() && nativeHandle != 0L) {
            nativeSurfaceDestroyed(nativeHandle)
        }
    }

    // Layer System
    fun addLayer(name: String = ""): Int {
        checkNotClosed()
        return nativeAddLayer(nativeHandle, name)
    }

    fun deleteLayer(id: Int): Boolean {
        checkNotClosed()
        return nativeDeleteLayer(nativeHandle, id)
    }

    fun reorderLayer(fromIndex: Int, toIndex: Int): Boolean {
        checkNotClosed()
        return nativeReorderLayer(nativeHandle, fromIndex, toIndex)
    }

    fun setActiveLayer(id: Int) {
        checkNotClosed()
        nativeSetActiveLayer(nativeHandle, id)
    }

    fun setLayerOpacity(id: Int, opacity: Float) {
        checkNotClosed()
        nativeSetLayerOpacity(nativeHandle, id, opacity)
    }

    fun setLayerVisible(id: Int, visible: Boolean) {
        checkNotClosed()
        nativeSetLayerVisible(nativeHandle, id, visible)
    }

    fun setLayerBlendMode(id: Int, mode: Int) {
        checkNotClosed()
        nativeSetLayerBlendMode(nativeHandle, id, mode)
    }

    fun setLayerAlphaLock(id: Int, locked: Boolean) {
        checkNotClosed()
        nativeSetLayerAlphaLock(nativeHandle, id, locked)
    }

    fun setLayerClippingMask(id: Int, mask: Boolean) {
        checkNotClosed()
        nativeSetLayerClippingMask(nativeHandle, id, mask)
    }

    fun mergeDown(layerIndex: Int): Boolean {
        checkNotClosed()
        return nativeMergeDown(nativeHandle, layerIndex)
    }

    // Stroke Execution
    fun beginStroke(x: Float, y: Float, pressure: Float) {
        checkNotClosed()
        nativeBeginStroke(nativeHandle, x, y, pressure)
    }

    fun appendPoints(pointData: FloatArray, pointCount: Int) {
        checkNotClosed()
        nativeAppendPoints(nativeHandle, pointData, pointCount)
    }

    fun endStroke() {
        checkNotClosed()
        nativeEndStroke(nativeHandle)
    }

    fun setBrushParams(
        diameter: Float,
        r: Int,
        g: Int,
        b: Int,
        a: Int = 255,
        opacity: Float = 1.0f,
        spacing: Float = 0.08f
    ) {
        checkNotClosed()
        nativeSetBrushParams(nativeHandle, diameter, r, g, b, a, opacity, spacing)
    }

    fun setBrushHardness(hardness: Float) {
        checkNotClosed()
        nativeSetBrushHardness(nativeHandle, hardness)
    }

    fun setToolMode(mode: ToolMode) {
        checkNotClosed()
        nativeSetToolMode(nativeHandle, mode.id)
    }

    fun setStreamline(factor: Float) {
        checkNotClosed()
        nativeSetStreamline(nativeHandle, factor)
    }

    // Viewport Manipulation
    fun pan(dx: Float, dy: Float) {
        checkNotClosed()
        nativePan(nativeHandle, dx, dy)
    }

    fun zoom(scaleFactor: Float, focusX: Float, focusY: Float) {
        checkNotClosed()
        nativeZoom(nativeHandle, scaleFactor, focusX, focusY)
    }

    fun rotate(deltaRadians: Float, focusX: Float, focusY: Float) {
        checkNotClosed()
        nativeRotate(nativeHandle, deltaRadians, focusX, focusY)
    }

    fun resetViewport() {
        checkNotClosed()
        nativeResetViewport(nativeHandle)
    }

    fun screenToCanvas(screenX: Float, screenY: Float): Vec2? {
        checkNotClosed()
        val success = nativeScreenToCanvas(nativeHandle, screenX, screenY, coordBuffer)
        return if (success) Vec2(coordBuffer[0], coordBuffer[1]) else null
    }

    fun canvasToScreen(canvasX: Float, canvasY: Float): Vec2 {
        checkNotClosed()
        nativeCanvasToScreen(nativeHandle, canvasX, canvasY, coordBuffer)
        return Vec2(coordBuffer[0], coordBuffer[1])
    }

    fun renderFrame() {
        checkNotClosed()
        nativeRenderFrame(nativeHandle)
    }

    fun getZoom(): Float {
        checkNotClosed()
        return nativeGetZoom(nativeHandle)
    }

    fun getRotation(): Float {
        checkNotClosed()
        return nativeGetRotation(nativeHandle)
    }

    fun getActiveTileCount(): Long {
        checkNotClosed()
        return nativeGetActiveTileCount(nativeHandle)
    }

    fun getMemoryUsageBytes(): Long {
        checkNotClosed()
        return nativeGetMemoryUsageBytes(nativeHandle)
    }

    // History (Undo / Redo)
    fun undo(): Boolean {
        checkNotClosed()
        return nativeUndo(nativeHandle)
    }

    fun redo(): Boolean {
        checkNotClosed()
        return nativeRedo(nativeHandle)
    }

    fun canUndo(): Boolean {
        checkNotClosed()
        return nativeCanUndo(nativeHandle)
    }

    fun canRedo(): Boolean {
        checkNotClosed()
        return nativeCanRedo(nativeHandle)
    }

    fun getUndoCount(): Long {
        checkNotClosed()
        return nativeGetUndoCount(nativeHandle)
    }

    fun getRedoCount(): Long {
        checkNotClosed()
        return nativeGetRedoCount(nativeHandle)
    }

    fun getHistoryMemoryBytes(): Long {
        checkNotClosed()
        return nativeGetHistoryMemoryBytes(nativeHandle)
    }

    // Eyedropper Sampling
    fun samplePixel(canvasX: Float, canvasY: Float): com.inkwell.core.model.Color {
        checkNotClosed()
        val rgba = IntArray(4)
        nativeSamplePixel(nativeHandle, canvasX.toInt(), canvasY.toInt(), rgba)
        return com.inkwell.core.model.Color.fromRgbInt(rgba[0], rgba[1], rgba[2], rgba[3])
    }

    fun sampleGrid(centerX: Float, centerY: Float, gridSize: Int = 15): ByteArray {
        checkNotClosed()
        val buffer = ByteArray(gridSize * gridSize * 4)
        nativeSampleGrid(nativeHandle, centerX.toInt(), centerY.toInt(), gridSize, buffer)
        return buffer
    }

    // Selection System
    fun setSelectionRect(x0: Int, y0: Int, x1: Int, y1: Int) {
        checkNotClosed()
        nativeSetSelectionRect(nativeHandle, x0, y0, x1, y1)
    }

    fun setSelectionLasso(points: FloatArray, count: Int) {
        checkNotClosed()
        nativeSetSelectionLasso(nativeHandle, points, count)
    }

    fun clearSelection() {
        checkNotClosed()
        nativeClearSelection(nativeHandle)
    }

    fun invertSelection() {
        checkNotClosed()
        nativeInvertSelection(nativeHandle)
    }

    fun hasSelection(): Boolean {
        checkNotClosed()
        return nativeHasSelection(nativeHandle)
    }

    fun getSelectionBounds(): IntArray? {
        checkNotClosed()
        val bounds = IntArray(4)
        return if (nativeGetSelectionBounds(nativeHandle, bounds)) bounds else null
    }

    // Transform Tool
    fun beginTransform(): Boolean {
        checkNotClosed()
        return nativeBeginTransform(nativeHandle)
    }

    fun updateTransform(dx: Float, dy: Float, scaleX: Float, scaleY: Float, rotationRad: Float, pivotX: Float, pivotY: Float) {
        checkNotClosed()
        nativeUpdateTransform(nativeHandle, dx, dy, scaleX, scaleY, rotationRad, pivotX, pivotY)
    }

    fun commitTransform(): Boolean {
        checkNotClosed()
        return nativeCommitTransform(nativeHandle)
    }

    fun cancelTransform() {
        checkNotClosed()
        nativeCancelTransform(nativeHandle)
    }

    fun isTransformActive(): Boolean {
        checkNotClosed()
        return nativeIsTransformActive(nativeHandle)
    }

    fun getTransformBounds(): IntArray? {
        checkNotClosed()
        val bounds = IntArray(4)
        return if (nativeGetTransformBounds(nativeHandle, bounds)) bounds else null
    }

    // Flood Fill (ColorDrop)
    fun floodFill(seedX: Int, seedY: Int, r: Int, g: Int, b: Int, a: Int, tolerance: Float): Boolean {
        checkNotClosed()
        return nativeFloodFill(nativeHandle, seedX, seedY, r, g, b, a, tolerance)
    }

    // Full-Resolution Export Compositing
    fun compositeBand(startY: Int, rowCount: Int, transparentBg: Boolean = false): ByteArray {
        checkNotClosed()
        val buffer = ByteArray(width * rowCount * 4)
        nativeCompositeBand(nativeHandle, startY, rowCount, transparentBg, buffer)
        return buffer
    }

    fun compositeBandInto(startY: Int, rowCount: Int, transparentBg: Boolean, outBuffer: ByteArray) {
        checkNotClosed()
        nativeCompositeBand(nativeHandle, startY, rowCount, transparentBg, outBuffer)
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            if (nativeHandle != 0L) {
                nativeDestroyEngine(nativeHandle)
                nativeHandle = 0L
            }
        }
    }

    private fun checkNotClosed() {
        check(!isClosed.get()) { "NativeEngineBridge is already closed" }
    }

    // Native declarations
    private external fun nativeCreateEngine(width: Int, height: Int, tileSize: Int): Long
    private external fun nativeDestroyEngine(handle: Long)
    private external fun nativeInitialize(handle: Long): Boolean
    private external fun nativeSetSurface(handle: Long, surface: Surface?): Boolean
    private external fun nativeSurfaceChanged(handle: Long, width: Int, height: Int)
    private external fun nativeSurfaceDestroyed(handle: Long)

    // Layer JNI declarations
    private external fun nativeAddLayer(handle: Long, name: String): Int
    private external fun nativeDeleteLayer(handle: Long, id: Int): Boolean
    private external fun nativeReorderLayer(handle: Long, fromIndex: Int, toIndex: Int): Boolean
    private external fun nativeSetActiveLayer(handle: Long, id: Int)
    private external fun nativeSetLayerOpacity(handle: Long, id: Int, opacity: Float)
    private external fun nativeSetLayerVisible(handle: Long, id: Int, visible: Boolean)
    private external fun nativeSetLayerBlendMode(handle: Long, id: Int, mode: Int)
    private external fun nativeSetLayerAlphaLock(handle: Long, id: Int, locked: Boolean)
    private external fun nativeSetLayerClippingMask(handle: Long, id: Int, mask: Boolean)
    private external fun nativeMergeDown(handle: Long, layerIndex: Int): Boolean

    // Stroke declarations
    private external fun nativeBeginStroke(handle: Long, x: Float, y: Float, pressure: Float)
    private external fun nativeAppendPoints(handle: Long, pointData: FloatArray, count: Int)
    private external fun nativeEndStroke(handle: Long)
    private external fun nativeSetBrushParams(
        handle: Long,
        diameter: Float,
        r: Int,
        g: Int,
        b: Int,
        a: Int,
        opacity: Float,
        spacing: Float
    )
    private external fun nativeSetBrushHardness(handle: Long, hardness: Float)
    private external fun nativeSetToolMode(handle: Long, mode: Int)
    private external fun nativeSetStreamline(handle: Long, factor: Float)

    // Viewport declarations
    private external fun nativePan(handle: Long, dx: Float, dy: Float)
    private external fun nativeZoom(handle: Long, scaleFactor: Float, focusX: Float, focusY: Float)
    private external fun nativeRotate(handle: Long, deltaRadians: Float, focusX: Float, focusY: Float)
    private external fun nativeResetViewport(handle: Long)
    private external fun nativeScreenToCanvas(handle: Long, screenX: Float, screenY: Float, outCoords: FloatArray): Boolean
    private external fun nativeCanvasToScreen(handle: Long, canvasX: Float, canvasY: Float, outCoords: FloatArray)
    private external fun nativeRenderFrame(handle: Long)
    private external fun nativeGetZoom(handle: Long): Float
    private external fun nativeGetRotation(handle: Long): Float
    private external fun nativeGetActiveTileCount(handle: Long): Long
    private external fun nativeGetMemoryUsageBytes(handle: Long): Long

    // History declarations
    private external fun nativeUndo(handle: Long): Boolean
    private external fun nativeRedo(handle: Long): Boolean
    private external fun nativeCanUndo(handle: Long): Boolean
    private external fun nativeCanRedo(handle: Long): Boolean
    private external fun nativeGetUndoCount(handle: Long): Long
    private external fun nativeGetRedoCount(handle: Long): Long
    private external fun nativeGetHistoryMemoryBytes(handle: Long): Long

    // Eyedropper declarations
    private external fun nativeSamplePixel(handle: Long, canvasX: Int, canvasY: Int, outRgba: IntArray)
    private external fun nativeSampleGrid(handle: Long, centerX: Int, centerY: Int, gridSize: Int, outBytes: ByteArray)

    // Selection declarations
    private external fun nativeSetSelectionRect(handle: Long, x0: Int, y0: Int, x1: Int, y1: Int)
    private external fun nativeSetSelectionLasso(handle: Long, points: FloatArray, count: Int)
    private external fun nativeClearSelection(handle: Long)
    private external fun nativeInvertSelection(handle: Long)
    private external fun nativeHasSelection(handle: Long): Boolean
    private external fun nativeGetSelectionBounds(handle: Long, outBounds: IntArray): Boolean

    // Transform declarations
    private external fun nativeBeginTransform(handle: Long): Boolean
    private external fun nativeUpdateTransform(handle: Long, dx: Float, dy: Float, scaleX: Float, scaleY: Float, rotationRad: Float, pivotX: Float, pivotY: Float)
    private external fun nativeCommitTransform(handle: Long): Boolean
    private external fun nativeCancelTransform(handle: Long)
    private external fun nativeIsTransformActive(handle: Long): Boolean
    private external fun nativeGetTransformBounds(handle: Long, outBounds: IntArray): Boolean

    // Flood Fill declaration
    private external fun nativeFloodFill(handle: Long, seedX: Int, seedY: Int, r: Int, g: Int, b: Int, a: Int, tolerance: Float): Boolean

    // Export declaration
    private external fun nativeCompositeBand(handle: Long, startY: Int, rowCount: Int, transparentBg: Boolean, outBytes: ByteArray)

    companion object {
        init {
            try {
                System.loadLibrary("inkwell_engine")
            } catch (e: UnsatisfiedLinkError) {
                System.err.println("Warning: libinkwell_engine.so not loaded in JVM environment: ${e.message}")
            }
        }

        external fun nativeGetVersion(): String
    }
}
