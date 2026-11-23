package com.inkwell.data.repository

import com.inkwell.core.model.CanvasSpec
import com.inkwell.data.project.model.CanvasMetadata
import com.inkwell.data.project.model.LayerMetadata
import com.inkwell.data.project.model.ProjectManifest
import com.inkwell.data.project.model.ProjectPackage
import com.inkwell.data.project.storage.InkProjectReader
import com.inkwell.data.project.storage.InkProjectWriter
import com.inkwell.data.project.storage.ProjectHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Manages project storage, asynchronous I/O, file duplication, and project deletion.
 */
class ProjectRepository(
    private val projectsDir: File
) {
    init {
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
    }

    suspend fun listProjects(): List<ProjectHeader> = withContext(Dispatchers.IO) {
        val files = projectsDir.listFiles { _, name -> name.endsWith(".ink", ignoreCase = true) }
            ?: return@withContext emptyList()

        files.mapNotNull { file ->
            try {
                InkProjectReader.readHeader(file)
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun loadProject(file: File): ProjectPackage = withContext(Dispatchers.IO) {
        InkProjectReader.read(file)
    }

    suspend fun saveProject(project: ProjectPackage, targetFile: File? = null): File = withContext(Dispatchers.IO) {
        val destFile = targetFile ?: File(projectsDir, "${project.manifest.projectId}.ink")
        InkProjectWriter.write(project, destFile)
        destFile
    }

    suspend fun deleteProject(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete() else false
    }

    suspend fun duplicateProject(file: File): File = withContext(Dispatchers.IO) {
        val original = InkProjectReader.read(file)
        val newId = UUID.randomUUID().toString()
        val duplicatedManifest = original.manifest.copy(
            projectId = newId,
            title = "${original.manifest.title} Copy",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val duplicatedPackage = original.copy(manifest = duplicatedManifest)
        val newFile = File(projectsDir, "$newId.ink")
        InkProjectWriter.write(duplicatedPackage, newFile)
        newFile
    }

    fun createNewProject(title: String = "Untitled Artwork", spec: CanvasSpec = CanvasSpec.SQUARE_2K): ProjectPackage {
        val manifest = ProjectManifest(
            projectId = UUID.randomUUID().toString(),
            title = title
        )
        val canvas = CanvasMetadata(
            width = spec.width,
            height = spec.height,
            dpi = spec.dpi,
            tileSize = spec.tileSize
        )
        val baseLayer = LayerMetadata(
            id = 1,
            name = "Layer 1",
            opacity = 1.0f,
            blendMode = 0,
            visible = true,
            tileCount = 0
        )
        return ProjectPackage(
            manifest = manifest,
            canvas = canvas,
            layers = listOf(baseLayer),
            tilesByLayer = emptyMap()
        )
    }
}
