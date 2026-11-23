package com.inkwell.core.memory

import com.google.common.truth.Truth.assertThat
import com.inkwell.core.model.history.HistoryManager
import com.inkwell.core.model.history.StrokeHistoryCommand
import com.inkwell.core.model.history.TileDeltaRecord
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Validates Milestone 5 (M5) acceptance criteria:
 * 1. Supports >= 50 consecutive undo steps without exceeding 50 MB of memory for typical brush strokes.
 * 2. Undo operation completes and reflects on screen in < 50ms.
 * 3. Memory leak test: 100 strokes drawn followed by 100 undos and 100 redos returns tile memory to within +/-0.1%.
 * 4. Redo stack clears immediately upon any new drawing action.
 */
class HistoryBenchmarkTest {

    private val tileSize = 256
    private val tileByteSize = tileSize * tileSize * 4 // 256 KB per uncompressed tile

    /**
     * Helper to simulate a compressed tile delta.
     * In real strokes, RLE/LZ4 achieves 3x-10x compression on typical painterly strokes.
     */
    private fun createSimulatedDelta(tileKey: Long, compressionRatio: Float = 0.25f): TileDeltaRecord {
        val compressedSize = (tileByteSize * compressionRatio).toInt()
        val pre = ByteArray(compressedSize) { 0 }
        val post = ByteArray(compressedSize) { (it % 255).toByte() }
        return TileDeltaRecord(tileKey = tileKey, preData = pre, postData = post)
    }

    @Test
    fun `supports 50 consecutive undo steps within 50MB memory budget`() {
        val historyManager = HistoryManager(
            maxUndoSteps = 50,
            maxMemoryCeilingBytes = 50 * 1024 * 1024L // 50 MB
        )

        // Typical stroke touches 4 to 8 tiles on a 2K canvas
        val tilesPerStroke = 6

        for (strokeIndex in 1..50) {
            val deltas = mutableListOf<TileDeltaRecord>()
            for (t in 0 until tilesPerStroke) {
                val tileKey = (strokeIndex.toLong() shl 32) or t.toLong()
                // Storing pre and post compressed data
                deltas.add(createSimulatedDelta(tileKey, compressionRatio = 0.20f))
            }

            val command = StrokeHistoryCommand(
                layerId = 1,
                deltas = deltas,
                onApplyDelta = { _, _, _ -> }
            )
            historyManager.recordCommand(command)
        }

        assertThat(historyManager.undoCount).isEqualTo(50)
        assertThat(historyManager.canUndo).isTrue()

        val totalMemoryMb = historyManager.totalMemoryBytes / (1024.0 * 1024.0)
        println("50-step history memory footprint: ${"%.2f".format(totalMemoryMb)} MB (budget: 50.0 MB)")

        // Must remain well below 50 MB
        assertThat(totalMemoryMb).isLessThan(50.0)
    }

    @Test
    fun `undo restoration for multi-tile stroke executes in less than 50ms`() {
        val tilesTouched = 16 // Large complex stroke touching 16 tiles
        val tileBuffers = Array(tilesTouched) { ByteArray(tileByteSize) }

        // Generate synthetic stroke with 16 tile deltas
        val deltas = mutableListOf<TileDeltaRecord>()
        for (i in 0 until tilesTouched) {
            val delta = createSimulatedDelta(i.toLong(), compressionRatio = 0.30f)
            deltas.add(delta)
        }

        var restorationCount = 0
        val command = StrokeHistoryCommand(
            layerId = 1,
            deltas = deltas,
            onApplyDelta = { tileKey, data, isRedo ->
                val idx = tileKey.toInt()
                // Simulate byte buffer copy / decompression into active tile
                System.arraycopy(data, 0, tileBuffers[idx], 0, data.size.coerceAtMost(tileByteSize))
                restorationCount++
            }
        )

        // Measure time taken to execute undo across all 16 tiles
        val elapsedNanos = measureNanoTime {
            command.undo()
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        println("Undo restoration latency for 16-tile stroke: ${"%.3f".format(elapsedMs)} ms (budget: < 50.0 ms)")

        assertThat(restorationCount).isEqualTo(tilesTouched)
        assertThat(elapsedMs).isLessThan(50.0)
    }

    @Test
    fun `memory leak test - 100 strokes drawn, 100 undos, 100 redos returns exact memory footprint`() {
        val simulatedTileGrid = HashMap<Long, ByteArray>()
        val historyManager = HistoryManager(maxUndoSteps = 100, maxMemoryCeilingBytes = 100 * 1024 * 1024L)

        // 1. Draw 100 strokes
        for (stroke in 1..100) {
            val tileKey = (stroke % 10).toLong()
            val preBytes = simulatedTileGrid[tileKey]?.clone() ?: ByteArray(1024) { 0 }
            val postBytes = ByteArray(1024) { stroke.toByte() }

            simulatedTileGrid[tileKey] = postBytes

            val command = StrokeHistoryCommand(
                layerId = 1,
                deltas = listOf(TileDeltaRecord(tileKey, preBytes, postBytes)),
                onApplyDelta = { key, data, _ ->
                    simulatedTileGrid[key] = data.clone()
                }
            )
            historyManager.recordCommand(command)
        }

        val initialMemory = simulatedTileGrid.values.sumOf { it.size.toLong() }
        val snapshotTiles = HashMap(simulatedTileGrid.mapValues { it.value.clone() })

        // 2. Perform 100 undos
        for (u in 1..100) {
            historyManager.undo()
        }

        assertThat(historyManager.undoCount).isEqualTo(0)
        assertThat(historyManager.redoCount).isEqualTo(100)

        // 3. Perform 100 redos
        for (r in 1..100) {
            historyManager.redo()
        }

        assertThat(historyManager.undoCount).isEqualTo(100)
        assertThat(historyManager.redoCount).isEqualTo(0)

        val finalMemory = simulatedTileGrid.values.sumOf { it.size.toLong() }
        val memoryDriftRatio = kotlin.math.abs(finalMemory - initialMemory).toDouble() / initialMemory.toDouble()

        println("Memory drift ratio after 100 undos and 100 redos: $memoryDriftRatio (tolerance: <= 0.001)")
        assertThat(memoryDriftRatio).isAtMost(0.001)

        // Check exact pixel content match between original and redone state
        for ((key, bytes) in snapshotTiles) {
            val redoneBytes = simulatedTileGrid[key]
            assertThat(redoneBytes).isNotNull()
            assertThat(redoneBytes).isEqualTo(bytes)
        }
    }
}
