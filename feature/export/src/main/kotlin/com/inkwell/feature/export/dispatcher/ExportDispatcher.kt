package com.inkwell.feature.export.dispatcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.inkwell.engine.NativeEngineBridge
import com.inkwell.feature.export.encoder.JpegExportEncoder
import com.inkwell.feature.export.encoder.StreamingPngEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    PNG("png", "image/png", "PNG Image"),
    JPEG("jpg", "image/jpeg", "JPEG Image")
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.PNG,
    val transparentBackground: Boolean = false,
    val jpegQuality: Int = 90
)

object ExportDispatcher {

    /**
     * Executes the full-resolution export pipeline streaming directly from native engine tiles.
     */
    suspend fun exportCanvas(
        engineBridge: NativeEngineBridge,
        options: ExportOptions,
        outputStream: OutputStream,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val width = engineBridge.width
        val height = engineBridge.height
        val bandHeight = 256
        val totalBands = (height + bandHeight - 1) / bandHeight

        when (options.format) {
            ExportFormat.PNG -> {
                val encoder = StreamingPngEncoder(
                    width = width,
                    height = height,
                    hasAlpha = options.transparentBackground
                )
                encoder.begin(outputStream)

                for (band in 0 until totalBands) {
                    val startY = band * bandHeight
                    val rows = (height - startY).coerceAtMost(bandHeight)
                    val rawPixels = engineBridge.compositeBand(startY, rows, options.transparentBackground)

                    encoder.writeScanlineBand(rawPixels, rows)
                    onProgress?.invoke((band + 1).toFloat() / totalBands.toFloat())
                }

                encoder.finish()
            }

            ExportFormat.JPEG -> {
                // Collect bands and stream into JPEG encoder
                val fullBuffer = ByteArrayOutputStream(width * height * 4)
                for (band in 0 until totalBands) {
                    val startY = band * bandHeight
                    val rows = (height - startY).coerceAtMost(bandHeight)
                    val rawPixels = engineBridge.compositeBand(startY, rows, false)
                    fullBuffer.write(rawPixels)
                    onProgress?.invoke((band + 1).toFloat() / totalBands.toFloat() * 0.7f)
                }

                JpegExportEncoder.encodeRgbaToJpeg(
                    rgbaBytes = fullBuffer.toByteArray(),
                    width = width,
                    height = height,
                    quality = options.jpegQuality,
                    outputStream = outputStream
                )
                onProgress?.invoke(1.0f)
            }
        }
    }

    /**
     * Dispatches an exported file to the Android System Share Sheet.
     */
    fun dispatchShareSheet(
        context: Context,
        exportedFile: File,
        mimeType: String,
        title: String = "Share Artwork"
    ): Intent {
        val contentUri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportedFile)
        } catch (e: Exception) {
            Uri.fromFile(exportedFile)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, exportedFile.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
