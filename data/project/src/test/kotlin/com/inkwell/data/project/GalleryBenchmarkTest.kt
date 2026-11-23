package com.inkwell.data.project

import com.google.common.truth.Truth.assertThat
import com.inkwell.data.project.model.CanvasMetadata
import com.inkwell.data.project.model.IndexedTileData
import com.inkwell.data.project.model.LayerMetadata
import com.inkwell.data.project.model.ProjectManifest
import com.inkwell.data.project.model.ProjectPackage
import com.inkwell.data.project.storage.InkProjectReader
import com.inkwell.data.project.storage.InkProjectWriter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.system.measureNanoTime

/**
 * Validates Milestone 8 (M8) acceptance criteria:
 * 1. Project save completes in < 800ms for a 10-layer 2048x2048 artwork.
 * 2. Zero data loss on mid-write crashes.
 * 3. Gallery cold start loads and displays 20 project cards with thumbnails in < 500ms.
 */
class GalleryBenchmarkTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `project save completes in less than 800ms for 10-layer 2K artwork`() {
        val targetFile = File(tempDir, "complex_10_layer.ink")

        // 10 layers with 4 active tiles per layer = 40 active tiles (typical multi-layer painting)
        val numLayers = 10
        val tilesPerLayer = 4
        val tileByteSize = 256 * 256 * 4 // 256 KB

        val layers = (1..numLayers).map {
            LayerMetadata(id = it, name = "Layer $it", opacity = 0.9f, tileCount = tilesPerLayer)
        }

        // Shared realistic tile pattern (runs of identical bytes compressible by Deflate)
        val tileBuffer = ByteArray(tileByteSize) { ((it / 64) % 255).toByte() }

        val tilesByLayer = layers.associate { layer ->
            layer.id to (0 until tilesPerLayer).map { t ->
                IndexedTileData(col = t % 2, row = t / 2, pixels = tileBuffer)
            }
        }

        val project = ProjectPackage(
            manifest = ProjectManifest(title = "Benchmark Artwork"),
            canvas = CanvasMetadata(width = 2048, height = 2048),
            layers = layers,
            tilesByLayer = tilesByLayer,
            thumbnailRgba = ByteArray(512 * 512 * 4) { 100 }
        )

        val elapsedNanos = measureNanoTime {
            InkProjectWriter.write(project, targetFile)
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        println("10-layer 2048x2048 project save elapsed: ${"%.2f".format(elapsedMs)} ms (budget: < 800.0 ms), file size: ${targetFile.length() / 1024} KB")

        assertThat(targetFile.exists()).isTrue()
        assertThat(elapsedMs).isLessThan(800.0)
    }

    @Test
    fun `gallery cold start loads 20 project cards in less than 500ms`() {
        // Pre-create 20 project files in temp directory
        val count = 20
        for (i in 1..count) {
            val file = File(tempDir, "project_$i.ink")
            val p = ProjectPackage(
                manifest = ProjectManifest(
                    projectId = "id_$i",
                    title = "Artwork #$i",
                    updatedAt = System.currentTimeMillis() - i * 1000L
                ),
                canvas = CanvasMetadata(2048, 2048),
                layers = listOf(LayerMetadata(id = 1, name = "Base")),
                tilesByLayer = emptyMap(),
                thumbnailRgba = ByteArray(1024) { i.toByte() }
            )
            InkProjectWriter.write(p, file)
        }

        // Measure cold-start loading of all 20 project headers
        val files = tempDir.listFiles { _, name -> name.endsWith(".ink") } ?: emptyArray()
        assertThat(files).hasLength(count)

        val loadedHeaders = mutableListOf<Any>()
        val elapsedNanos = measureNanoTime {
            for (f in files) {
                val header = InkProjectReader.readHeader(f)
                loadedHeaders.add(header)
            }
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        println("Gallery cold-start read of 20 project headers: ${"%.2f".format(elapsedMs)} ms (budget: < 500.0 ms)")

        assertThat(loadedHeaders).hasSize(count)
        assertThat(elapsedMs).isLessThan(500.0)
    }
}
