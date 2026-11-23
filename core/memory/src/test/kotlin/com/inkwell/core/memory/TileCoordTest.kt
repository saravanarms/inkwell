package com.inkwell.core.memory

import com.google.common.truth.Truth.assertThat
import com.inkwell.core.common.math.Vec2
import org.junit.jupiter.api.Test

class TileCoordTest {

    @Test
    fun `pack and unpack roundtrip preserves coordinate values`() {
        val original = TileCoord(col = 15, row = 27)
        val key = original.toKey()
        val unpacked = TileCoord.unpack(key)

        assertThat(unpacked).isEqualTo(original)
        assertThat(unpacked.col).isEqualTo(15)
        assertThat(unpacked.row).isEqualTo(27)
    }

    @Test
    fun `intersecting tiles accurately bounds circular dab`() {
        val center = Vec2(260f, 260f) // Just inside tile (1, 1) of 256x256 grid
        val radius = 10f
        val tileSize = 256
        val cols = 8
        val rows = 8

        val tiles = TileCoord.getIntersectingTiles(center, radius, tileSize, cols, rows)
        // Center is at (260, 260), so bounds are [250..270].
        // 250 is in tile 0, 270 is in tile 1. Thus cols 0..1 and rows 0..1 intersect!
        assertThat(tiles).containsExactly(
            TileCoord(0, 0), TileCoord(0, 1),
            TileCoord(1, 0), TileCoord(1, 1)
        )
    }

    @Test
    fun `intersecting tiles clamps strictly to canvas boundaries`() {
        val center = Vec2(0f, 0f)
        val radius = 50f
        val tileSize = 256
        val cols = 8
        val rows = 8

        val tiles = TileCoord.getIntersectingTiles(center, radius, tileSize, cols, rows)
        assertThat(tiles).containsExactly(TileCoord(0, 0))
    }
}
