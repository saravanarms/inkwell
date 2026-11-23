package com.inkwell.core.model

/**
 * Immutable canvas specifications defining geometry, tiling, and DPI.
 */
data class CanvasSpec(
    val width: Int,
    val height: Int,
    val dpi: Int = 300,
    val tileSize: Int = DEFAULT_TILE_SIZE
) {
    init {
        require(width in MIN_DIMENSION..MAX_DIMENSION) {
            "Width $width out of range [$MIN_DIMENSION, $MAX_DIMENSION]"
        }
        require(height in MIN_DIMENSION..MAX_DIMENSION) {
            "Height $height out of range [$MIN_DIMENSION, $MAX_DIMENSION]"
        }
        require(tileSize in listOf(128, 256, 512)) {
            "Invalid tile size $tileSize; must be power of two (128, 256, 512)"
        }
    }

    val columns: Int = (width + tileSize - 1) / tileSize
    val rows: Int = (height + tileSize - 1) / tileSize
    val totalTilesPerLayer: Int = columns * rows

    /**
     * Estimated uncompressed memory for a fully-painted layer in bytes (RGBA8888 = 4 bytes/pixel).
     */
    val uncompressedLayerBytes: Long = totalTilesPerLayer.toLong() * tileSize * tileSize * 4L

    /**
     * Calculates the estimated safe layer count for a given dimension or this spec.
     */
    fun maxLayers(w: Int = width, h: Int = height): Int {
        val bytes = w.toLong() * h.toLong() * 4L
        if (bytes <= 0L) return 4
        val budgetBytes = 300L * 1024L * 1024L
        val calculated = (budgetBytes / bytes).toInt()
        return calculated.coerceIn(4, 64)
    }

    companion object {
        const val MIN_DIMENSION = 64
        const val MAX_DIMENSION = 4096
        const val DEFAULT_TILE_SIZE = 256

        val SQUARE_2K = CanvasSpec(2048, 2048, 300)
        val UHD_4K = CanvasSpec(3840, 2160, 300)
        val DEFAULT_PHONE = CanvasSpec(1080, 2400, 300)
        val MAXIMUM_4K = CanvasSpec(4096, 4096, 300)
        val PRINT_A4_300DPI = CanvasSpec(2480, 3508, 300)
    }
}
