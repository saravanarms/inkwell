package com.inkwell.data.project.storage

import com.inkwell.data.project.model.CanvasMetadata
import com.inkwell.data.project.model.IndexedTileData
import com.inkwell.data.project.model.LayerMetadata
import com.inkwell.data.project.model.ProjectManifest
import com.inkwell.data.project.model.ProjectPackage
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Lightweight header for high-performance gallery listing.
 */
data class ProjectHeader(
    val projectId: String,
    val title: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val updatedAt: Long,
    val file: File,
    val thumbnailBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProjectHeader) return false
        return projectId == other.projectId && file == other.file
    }

    override fun hashCode(): Int = projectId.hashCode()
}

object InkProjectWriter {

    fun write(project: ProjectPackage, targetFile: File) {
        AtomicFileWriter.writeAtomically(targetFile) { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                zip.setLevel(6) // Balanced deflate compression

                // 1. manifest.json
                writeZipTextEntry(zip, "manifest.json", project.manifest.toJson())

                // 2. canvas.json
                writeZipTextEntry(zip, "canvas.json", project.canvas.toJson())

                // 3. thumbnail.raw (512x512 RGBA downsample)
                if (project.thumbnailRgba != null) {
                    writeZipBinaryEntry(zip, "thumbnail.raw", project.thumbnailRgba)
                }

                // 4. layers/layers.json
                val layersJson = project.layers.joinToString(prefix = "[", postfix = "]", separator = ",") {
                    it.toJson()
                }
                writeZipTextEntry(zip, "layers/layers.json", layersJson)

                // 5. Layer tiles (index.bin + tiles.bin)
                for (layer in project.layers) {
                    val tiles = project.tilesByLayer[layer.id] ?: emptyList()
                    writeLayerTiles(zip, layer.id, tiles)
                }

                // 6. palettes/swatches.json
                if (!project.palettesJson.isNullOrBlank()) {
                    writeZipTextEntry(zip, "palettes/swatches.json", project.palettesJson)
                }
            }
        }
    }

    private fun writeZipTextEntry(zip: ZipOutputStream, entryName: String, text: String) {
        zip.putNextEntry(ZipEntry(entryName))
        val bytes = text.toByteArray(Charsets.UTF_8)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeZipBinaryEntry(zip: ZipOutputStream, entryName: String, data: ByteArray) {
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(data)
        zip.closeEntry()
    }

    private fun writeLayerTiles(zip: ZipOutputStream, layerId: Int, tiles: List<IndexedTileData>) {
        val indexBos = ByteArrayOutputStream()
        val indexDos = DataOutputStream(indexBos)

        val tilesBos = ByteArrayOutputStream()
        var offset = 0

        for (tile in tiles) {
            indexDos.writeShort(tile.col)
            indexDos.writeShort(tile.row)
            indexDos.writeInt(offset)
            indexDos.writeInt(tile.pixels.size)

            tilesBos.write(tile.pixels)
            offset += tile.pixels.size
        }
        indexDos.flush()

        writeZipBinaryEntry(zip, "layers/layer_$layerId/index.bin", indexBos.toByteArray())
        writeZipBinaryEntry(zip, "layers/layer_$layerId/tiles.bin", tilesBos.toByteArray())
    }
}

object InkProjectReader {

    fun read(sourceFile: File): ProjectPackage {
        if (!sourceFile.exists()) throw IOException("Project file does not exist: $sourceFile")

        var manifest: ProjectManifest? = null
        var canvas: CanvasMetadata? = null
        val layers = mutableListOf<LayerMetadata>()
        var thumbnail: ByteArray? = null
        var palettesJson: String? = null

        val layerIndexMap = mutableMapOf<Int, ByteArray>()
        val layerTilesMap = mutableMapOf<Int, ByteArray>()

        ZipInputStream(BufferedInputStream(FileInputStream(sourceFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == "manifest.json" -> {
                        manifest = ProjectManifest.fromJson(zis.reader().readText())
                    }
                    name == "canvas.json" -> {
                        canvas = CanvasMetadata.fromJson(zis.reader().readText())
                    }
                    name == "thumbnail.raw" -> {
                        thumbnail = zis.readBytes()
                    }
                    name == "layers/layers.json" -> {
                        val text = zis.reader().readText()
                        val itemRegex = """\{"id"[^}]+\}""".toRegex()
                        for (m in itemRegex.findAll(text)) {
                            layers.add(LayerMetadata.fromJson(m.value))
                        }
                    }
                    name.startsWith("layers/layer_") && name.endsWith("/index.bin") -> {
                        val layerId = name.substringAfter("layers/layer_").substringBefore("/").toIntOrNull() ?: 1
                        layerIndexMap[layerId] = zis.readBytes()
                    }
                    name.startsWith("layers/layer_") && name.endsWith("/tiles.bin") -> {
                        val layerId = name.substringAfter("layers/layer_").substringBefore("/").toIntOrNull() ?: 1
                        layerTilesMap[layerId] = zis.readBytes()
                    }
                    name == "palettes/swatches.json" -> {
                        palettesJson = zis.reader().readText()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val checkedManifest = manifest ?: throw IOException("Corrupted .ink container: missing manifest.json")
        val checkedCanvas = canvas ?: throw IOException("Corrupted .ink container: missing canvas.json")

        // Reconstruct tiles for each layer
        val tilesByLayer = mutableMapOf<Int, List<IndexedTileData>>()
        for (layer in layers) {
            val indexBytes = layerIndexMap[layer.id]
            val tilesBytes = layerTilesMap[layer.id]

            if (indexBytes != null && tilesBytes != null) {
                val tileList = mutableListOf<IndexedTileData>()
                val dis = DataInputStream(indexBytes.inputStream())

                val entryCount = indexBytes.size / 12 // 2 shorts (4 bytes) + 2 ints (8 bytes) = 12 bytes
                for (i in 0 until entryCount) {
                    val col = dis.readShort().toInt()
                    val row = dis.readShort().toInt()
                    val offset = dis.readInt()
                    val size = dis.readInt()

                    val tilePixels = ByteArray(size)
                    System.arraycopy(tilesBytes, offset, tilePixels, 0, size)
                    tileList.add(IndexedTileData(col, row, tilePixels))
                }
                tilesByLayer[layer.id] = tileList
            }
        }

        return ProjectPackage(
            manifest = checkedManifest,
            canvas = checkedCanvas,
            layers = layers,
            tilesByLayer = tilesByLayer,
            thumbnailRgba = thumbnail,
            palettesJson = palettesJson
        )
    }

    /**
     * Fast reader for gallery previews without loading full layer tile payloads.
     */
    fun readHeader(file: File): ProjectHeader {
        var manifest: ProjectManifest? = null
        var canvas: CanvasMetadata? = null
        var thumbnail: ByteArray? = null

        ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                when (name) {
                    "manifest.json" -> {
                        manifest = ProjectManifest.fromJson(zis.reader().readText())
                    }
                    "canvas.json" -> {
                        canvas = CanvasMetadata.fromJson(zis.reader().readText())
                    }
                    "thumbnail.raw" -> {
                        thumbnail = zis.readBytes()
                    }
                }
                zis.closeEntry()
                if (manifest != null && canvas != null && thumbnail != null) {
                    break // Stop early once headers and thumbnail are collected
                }
                entry = zis.nextEntry
            }
        }

        val m = manifest ?: ProjectManifest(title = file.nameWithoutExtension)
        val c = canvas ?: CanvasMetadata(width = 2048, height = 2048)

        return ProjectHeader(
            projectId = m.projectId,
            title = m.title,
            width = c.width,
            height = c.height,
            fileSizeBytes = file.length(),
            updatedAt = m.updatedAt,
            file = file,
            thumbnailBytes = thumbnail
        )
    }
}
