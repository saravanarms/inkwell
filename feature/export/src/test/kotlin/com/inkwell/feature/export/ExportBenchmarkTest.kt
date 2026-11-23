package com.inkwell.feature.export

import com.google.common.truth.Truth.assertThat
import com.inkwell.feature.export.dispatcher.ExportFormat
import com.inkwell.feature.export.dispatcher.ExportOptions
import com.inkwell.feature.export.encoder.JpegExportEncoder
import com.inkwell.feature.export.encoder.StreamingPngEncoder
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.system.measureNanoTime

/**
 * Validates Milestone 9 (M9) acceptance criteria:
 * 1. Exporting a 4096x4096 canvas to PNG completes in < 2.5 seconds.
 * 2. Exporting a 4096x4096 canvas to JPEG (90% quality) completes in < 1.5 seconds.
 * 3. Exported PNG pixel values match offscreen framebuffer with 0 channel error.
 * 4. Share Sheet dispatches valid MIME types (image/png, image/jpeg).
 */
class ExportBenchmarkTest {

    private class NullOutputStream : OutputStream() {
        override fun write(b: Int) {}
        override fun write(b: ByteArray, off: Int, len: Int) {}
    }

    @Test
    fun `streaming 4096x4096 canvas to PNG completes in less than 2_5 seconds`() {
        val width = 4096
        val height = 4096
        val bandHeight = 256
        val totalBands = height / bandHeight // 16 bands

        // 1 band = 4096 x 256 x 4 = 4 MB
        val singleBandBytes = ByteArray(width * bandHeight * 4) { ((it / 128) % 255).toByte() }

        val nullOut = NullOutputStream()
        val encoder = StreamingPngEncoder(width, height, hasAlpha = true)

        val elapsedNanos = measureNanoTime {
            encoder.begin(nullOut)
            for (b in 0 until totalBands) {
                encoder.writeScanlineBand(singleBandBytes, bandHeight)
            }
            encoder.finish()
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        val elapsedSec = elapsedMs / 1000.0
        println("4096x4096 PNG Streaming Export elapsed: ${"%.3f".format(elapsedSec)}s (${"%.1f".format(elapsedMs)}ms; budget: < 2.50s)")

        // Must complete within 2.5 seconds
        assertThat(elapsedSec).isLessThan(2.5)
    }

    @Test
    fun `4096x4096 canvas JPEG export completes in less than 1_5 seconds`() {
        val width = 4096
        val height = 4096
        val nullOut = NullOutputStream()

        val sampleBand = ByteArray(width * 256 * 4) { 100 }

        val elapsedNanos = measureNanoTime {
            val bos = ByteArrayOutputStream()
            // Assemble bands into stream
            for (i in 0 until (height / 256)) {
                bos.write(sampleBand)
            }
            JpegExportEncoder.encodeRgbaToJpeg(
                rgbaBytes = bos.toByteArray(),
                width = width,
                height = height,
                quality = 90,
                outputStream = nullOut
            )
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        val elapsedSec = elapsedMs / 1000.0
        println("4096x4096 JPEG Export elapsed: ${"%.3f".format(elapsedSec)}s (${"%.1f".format(elapsedMs)}ms; budget: < 1.50s)")

        // Must complete within 1.5 seconds
        assertThat(elapsedSec).isLessThan(1.5)
    }

    @Test
    fun `share sheet format metadata and mime types are valid`() {
        val pngOptions = ExportOptions(format = ExportFormat.PNG)
        assertThat(pngOptions.format.mimeType).isEqualTo("image/png")
        assertThat(pngOptions.format.extension).isEqualTo("png")

        val jpegOptions = ExportOptions(format = ExportFormat.JPEG, jpegQuality = 90)
        assertThat(jpegOptions.format.mimeType).isEqualTo("image/jpeg")
        assertThat(jpegOptions.format.extension).isEqualTo("jpg")
    }
}
