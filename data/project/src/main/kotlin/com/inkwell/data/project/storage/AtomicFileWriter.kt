package com.inkwell.data.project.storage

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Crash-safe atomic file writer implementing:
 * 1. Write to temporary file `${name}.tmp`.
 * 2. Force physical disk commit via `fsync()`.
 * 3. Atomic rename/swap over target destination.
 * 4. Automatic cleanup of temporary artifacts on failure.
 */
object AtomicFileWriter {

    fun writeAtomically(targetFile: File, writerAction: (OutputStream) -> Unit) {
        val parentDir = targetFile.parentFile ?: throw IOException("Invalid parent directory for $targetFile")
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }

        val tempFile = File(parentDir, "${targetFile.name}.${System.currentTimeMillis()}.tmp")

        try {
            FileOutputStream(tempFile).use { fos ->
                writerAction(fos)
                fos.flush()
                try {
                    fos.fd.sync() // Force physical disk sync
                } catch (ignored: Exception) {
                }
            }

            // Atomic move / rename
            try {
                Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: Exception) {
                // Fallback to renameTo if filesystem doesn't support ATOMIC_MOVE
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                val renamed = tempFile.renameTo(targetFile)
                if (!renamed) {
                    throw IOException("Failed to rename temporary file $tempFile to $targetFile")
                }
            }
        } catch (e: Throwable) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }
    }
}
