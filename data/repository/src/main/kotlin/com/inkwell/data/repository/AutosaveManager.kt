package com.inkwell.data.repository

import com.inkwell.data.project.model.ProjectPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background autosave coordinator managing:
 * - 60-second idle timer autosave.
 * - Instant autosave on app backgrounding / lifecycle pause.
 * - Non-blocking execution on Dispatchers.IO.
 */
class AutosaveManager(
    private val repository: ProjectRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val autosaveIntervalMs: Long = 60_000L
) {
    private val isDirty = AtomicBoolean(false)
    private var autosaveJob: Job? = null
    private var currentProjectSupplier: (() -> ProjectPackage?)? = null
    private var targetFileSupplier: (() -> File?)? = null

    var onAutosaveComplete: ((File) -> Unit)? = null
    var onAutosaveError: ((Throwable) -> Unit)? = null

    fun start(
        projectSupplier: () -> ProjectPackage?,
        targetFile: () -> File?
    ) {
        currentProjectSupplier = projectSupplier
        targetFileSupplier = targetFile
        stop()

        autosaveJob = scope.launch {
            while (isActive) {
                delay(autosaveIntervalMs)
                if (isDirty.get()) {
                    executeAutosave()
                }
            }
        }
    }

    fun markDirty() {
        isDirty.set(true)
    }

    fun triggerAutosaveNow() {
        if (isDirty.get()) {
            scope.launch {
                executeAutosave()
            }
        }
    }

    private suspend fun executeAutosave() {
        val project = currentProjectSupplier?.invoke() ?: return
        val file = targetFileSupplier?.invoke()
        try {
            val savedFile = repository.saveProject(project, file)
            isDirty.set(false)
            onAutosaveComplete?.invoke(savedFile)
        } catch (e: Throwable) {
            onAutosaveError?.invoke(e)
        }
    }

    fun stop() {
        autosaveJob?.cancel()
        autosaveJob = null
    }
}
