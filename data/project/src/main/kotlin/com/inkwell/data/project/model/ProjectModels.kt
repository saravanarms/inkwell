package com.inkwell.data.project.model

import java.util.UUID

/**
 * Manifest metadata defining project identity and versioning.
 */
data class ProjectManifest(
    val version: String = "1.0",
    val appVersion: String = "1.0.0-alpha",
    val projectId: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Artwork",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val safeTitle = title.replace("\"", "\\\"")
        return """{"version":"$version","appVersion":"$appVersion","projectId":"$projectId","title":"$safeTitle","createdAt":$createdAt,"updatedAt":$updatedAt}"""
    }

    companion object {
        fun fromJson(json: String): ProjectManifest {
            fun extractString(key: String): String =
                """"$key"\s*:\s*"([^"]*)"""".toRegex().find(json)?.groupValues?.get(1) ?: ""
            fun extractLong(key: String): Long =
                """"$key"\s*:\s*([0-9]+)""".toRegex().find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

            return ProjectManifest(
                version = extractString("version").ifEmpty { "1.0" },
                appVersion = extractString("appVersion").ifEmpty { "1.0.0-alpha" },
                projectId = extractString("projectId").ifEmpty { UUID.randomUUID().toString() },
                title = extractString("title").replace("\\\"", "\"").ifEmpty { "Untitled" },
                createdAt = extractLong("createdAt"),
                updatedAt = extractLong("updatedAt")
            )
        }
    }
}

/**
 * Canvas dimensions, color profile, and physical specifications.
 */
data class CanvasMetadata(
    val width: Int,
    val height: Int,
    val dpi: Int = 300,
    val tileSize: Int = 256,
    val backgroundColor: String = "#FFFFFFFF"
) {
    fun toJson(): String {
        return """{"width":$width,"height":$height,"dpi":$dpi,"tileSize":$tileSize,"backgroundColor":"$backgroundColor"}"""
    }

    companion object {
        fun fromJson(json: String): CanvasMetadata {
            fun extractInt(key: String, default: Int): Int =
                """"$key"\s*:\s*([0-9]+)""".toRegex().find(json)?.groupValues?.get(1)?.toIntOrNull() ?: default
            fun extractString(key: String, default: String): String =
                """"$key"\s*:\s*"([^"]*)"""".toRegex().find(json)?.groupValues?.get(1) ?: default

            return CanvasMetadata(
                width = extractInt("width", 2048),
                height = extractInt("height", 2048),
                dpi = extractInt("dpi", 300),
                tileSize = extractInt("tileSize", 256),
                backgroundColor = extractString("backgroundColor", "#FFFFFFFF")
            )
        }
    }
}

/**
 * Individual layer descriptor in the project container.
 */
data class LayerMetadata(
    val id: Int,
    val name: String,
    val opacity: Float = 1.0f,
    val blendMode: Int = 0,
    val visible: Boolean = true,
    val alphaLocked: Boolean = false,
    val clippingMask: Boolean = false,
    val tileCount: Int = 0
) {
    fun toJson(): String {
        val safeName = name.replace("\"", "\\\"")
        return """{"id":$id,"name":"$safeName","opacity":$opacity,"blendMode":$blendMode,"visible":$visible,"alphaLocked":$alphaLocked,"clippingMask":$clippingMask,"tileCount":$tileCount}"""
    }

    companion object {
        fun fromJson(json: String): LayerMetadata {
            fun extractInt(key: String, default: Int): Int =
                """"$key"\s*:\s*([0-9]+)""".toRegex().find(json)?.groupValues?.get(1)?.toIntOrNull() ?: default
            fun extractFloat(key: String, default: Float): Float =
                """"$key"\s*:\s*([0-9.]+)""".toRegex().find(json)?.groupValues?.get(1)?.toFloatOrNull() ?: default
            fun extractBool(key: String, default: Boolean): Boolean =
                """"$key"\s*:\s*(true|false)""".toRegex().find(json)?.groupValues?.get(1)?.toBoolean() ?: default
            fun extractString(key: String, default: String): String =
                """"$key"\s*:\s*"([^"]*)"""".toRegex().find(json)?.groupValues?.get(1) ?: default

            return LayerMetadata(
                id = extractInt("id", 1),
                name = extractString("name", "Layer").replace("\\\"", "\""),
                opacity = extractFloat("opacity", 1.0f),
                blendMode = extractInt("blendMode", 0),
                visible = extractBool("visible", true),
                alphaLocked = extractBool("alphaLocked", false),
                clippingMask = extractBool("clippingMask", false),
                tileCount = extractInt("tileCount", 0)
            )
        }
    }
}

/**
 * Single 256x256 RGBA tile payload in a layer.
 */
data class IndexedTileData(
    val col: Int,
    val row: Int,
    val pixels: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IndexedTileData) return false
        return col == other.col && row == other.row && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = col
        result = 31 * result + row
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}

/**
 * Complete in-memory representation of an Inkwell `.ink` project package.
 */
data class ProjectPackage(
    val manifest: ProjectManifest,
    val canvas: CanvasMetadata,
    val layers: List<LayerMetadata>,
    val tilesByLayer: Map<Int, List<IndexedTileData>>,
    val thumbnailRgba: ByteArray? = null,
    val palettesJson: String? = null
)
