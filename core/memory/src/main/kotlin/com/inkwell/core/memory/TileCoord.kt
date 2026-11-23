package com.inkwell.core.memory

import com.inkwell.core.common.math.Vec2
import kotlin.math.max
import kotlin.math.min

/**
 * Coordinate identifier for a single tile in the canvas grid.
 */
data class TileCoord(val col: Int, val row: Int) {

    /**
     * Packs col and row into a single 64-bit Long key for zero-allocation hash map keys.
     */
    fun toKey(): Long = pack(col, row)

    companion object {
        fun pack(col: Int, row: Int): Long {
            return (col.toLong() shl 32) or (row.toLong() and 0xFFFFFFFFL)
        }

        fun unpack(key: Long): TileCoord {
            val col = (key shr 32).toInt()
            val row = (key and 0xFFFFFFFFL).toInt()
            return TileCoord(col, row)
        }

        /**
         * Computes the bounding tile coordinates intersected by a circular dab stamp.
         */
        fun getIntersectingTiles(
            center: Vec2,
            radius: Float,
            tileSize: Int,
            maxCols: Int,
            maxRows: Int
        ): List<TileCoord> {
            val minX = center.x - radius
            val maxX = center.x + radius
            val minY = center.y - radius
            val maxY = center.y + radius

            val startCol = max(0, (minX / tileSize).toInt())
            val endCol = min(maxCols - 1, (maxX / tileSize).toInt())
            val startRow = max(0, (minY / tileSize).toInt())
            val endRow = min(maxRows - 1, (maxY / tileSize).toInt())

            if (startCol > endCol || startRow > endRow) return emptyList()

            val tiles = ArrayList<TileCoord>((endCol - startCol + 1) * (endRow - startRow + 1))
            for (c in startCol..endCol) {
                for (r in startRow..endRow) {
                    tiles.add(TileCoord(c, r))
                }
            }
            return tiles
        }
    }
}
