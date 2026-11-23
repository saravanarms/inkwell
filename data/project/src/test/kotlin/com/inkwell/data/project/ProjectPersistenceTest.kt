package com.inkwell.data.project

import com.google.common.truth.Truth.assertThat
import com.inkwell.data.project.model.CanvasMetadata
import com.inkwell.data.project.model.IndexedTileData
import com.inkwell.data.project.model.LayerMetadata
import com.inkwell.data.project.model.ProjectManifest
import com.inkwell.data.project.model.ProjectPackage
import com.inkwell.data.project.storage.AtomicFileWriter
import com.inkwell.data.project.storage.InkProjectReader
import com.inkwell.data.project.storage.InkProjectWriter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class ProjectPersistenceTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `save and load project package preserves exact layer data and tile bytes`() {
        val targetFile = File(tempDir, "test_artwork.ink")

        val manifest = ProjectManifest(
            title = "Sunset Overdrive",
            appVersion = "1.0.0"
        )
        val canvas = CanvasMetadata(
            width = 2048,
            height = 2048,
            dpi = 300,
            tileSize = 256
        )

        val layer1 = LayerMetadata(id = 1, name = "Background", opacity = 1.0f, tileCount = 1)
        val layer2 = LayerMetadata(id = 2, name = "Lineart", opacity = 0.85f, tileCount = 1)

        val tileBytes1 = ByteArray(256 * 256 * 4) { (it % 100).toByte() }
        val tileBytes2 = ByteArray(256 * 256 * 4) { ((it * 3) % 255).toByte() }

        val tilesByLayer = mapOf(
            1 to listOf(IndexedTileData(0, 0, tileBytes1)),
            2 to listOf(IndexedTileData(1, 1, tileBytes2))
        )

        val originalPackage = ProjectPackage(
            manifest = manifest,
            canvas = canvas,
            layers = listOf(layer1, layer2),
            tilesByLayer = tilesByLayer,
            thumbnailRgba = ByteArray(1024) { 42 },
            palettesJson = """{"active":"test"}"""
        )

        // 1. Write package
        InkProjectWriter.write(originalPackage, targetFile)
        assertThat(targetFile.exists()).isTrue()
        assertThat(targetFile.length()).isGreaterThan(0L)

        // 2. Read package back
        val loaded = InkProjectReader.read(targetFile)

        assertThat(loaded.manifest.title).isEqualTo("Sunset Overdrive")
        assertThat(loaded.canvas.width).isEqualTo(2048)
        assertThat(loaded.canvas.height).isEqualTo(2048)
        assertThat(loaded.layers).hasSize(2)
        assertThat(loaded.layers[0].name).isEqualTo("Background")
        assertThat(loaded.layers[1].opacity).isWithin(0.001f).of(0.85f)

        // 3. Tile byte verification
        val loadedTiles1 = loaded.tilesByLayer[1]
        assertThat(loadedTiles1).isNotNull()
        assertThat(loadedTiles1).hasSize(1)
        assertThat(loadedTiles1!![0].col).isEqualTo(0)
        assertThat(loadedTiles1[0].row).isEqualTo(0)
        assertThat(loadedTiles1[0].pixels).isEqualTo(tileBytes1)

        val loadedTiles2 = loaded.tilesByLayer[2]
        assertThat(loadedTiles2).isNotNull()
        assertThat(loadedTiles2).hasSize(1)
        assertThat(loadedTiles2!![0].col).isEqualTo(1)
        assertThat(loadedTiles2[0].row).isEqualTo(1)
        assertThat(loadedTiles2[0].pixels).isEqualTo(tileBytes2)
    }

    @Test
    fun `zero data loss - atomic file writer preserves original file undamaged on mid-write crash`() {
        val targetFile = File(tempDir, "important_art.ink")

        // 1. Write an initial valid file
        val initialPackage = ProjectPackage(
            manifest = ProjectManifest(title = "Initial State"),
            canvas = CanvasMetadata(2048, 2048),
            layers = listOf(LayerMetadata(id = 1, name = "Base")),
            tilesByLayer = emptyMap()
        )
        InkProjectWriter.write(initialPackage, targetFile)
        val originalBytes = targetFile.readBytes()
        assertThat(originalBytes).isNotEmpty()

        // 2. Simulate a crash / failure during atomic write
        var exceptionThrown = false
        try {
            AtomicFileWriter.writeAtomically(targetFile) { outputStream ->
                // Write partial corrupted data and throw unexpected failure
                outputStream.write("PARTIAL CORRUPTED STREAM".toByteArray())
                throw IOException("Simulated process kill / out of disk space mid-write")
            }
        } catch (e: IOException) {
            exceptionThrown = true
        }

        assertThat(exceptionThrown).isTrue()

        // 3. Assert original file is 100% undamaged and loadable
        assertThat(targetFile.exists()).isTrue()
        assertThat(targetFile.readBytes()).isEqualTo(originalBytes)

        val loaded = InkProjectReader.read(targetFile)
        assertThat(loaded.manifest.title).isEqualTo("Initial State")

        // 4. Verify no lingering .tmp files
        val tmpFiles = tempDir.listFiles { _, name -> name.endsWith(".tmp") } ?: emptyArray()
        assertThat(tmpFiles).isEmpty()
    }
}
