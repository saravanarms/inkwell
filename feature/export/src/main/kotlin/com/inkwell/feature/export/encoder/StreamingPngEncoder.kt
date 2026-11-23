package com.inkwell.feature.export.encoder

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/**
 * High-performance streaming PNG encoder compliant with ISO/IEC 15948:2004.
 * Streams scanline bands directly into a DeflaterOutputStream, eliminating
 * the need to allocate a monolithic 64MB contiguous bitmap for 4096x4096 canvases.
 */
class StreamingPngEncoder(
    val width: Int,
    val height: Int,
    val hasAlpha: Boolean = true
) {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
        0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
    )

    private val crc = CRC32()
    private var deflater: Deflater? = null
    private var deflaterStream: DeflaterOutputStream? = null
    private var underlyingStream: OutputStream? = null
    private var idatBuffer: ByteArrayOutputStream? = null
    private var rowsWritten = 0

    fun begin(outputStream: OutputStream) {
        underlyingStream = outputStream
        rowsWritten = 0

        // 1. Write PNG Signature
        outputStream.write(pngSignature)

        // 2. Write IHDR Chunk
        val ihdrBos = ByteArrayOutputStream(13)
        val ihdrDos = DataOutputStream(ihdrBos)
        ihdrDos.writeInt(width)
        ihdrDos.writeInt(height)
        ihdrDos.writeByte(8) // 8 bits per channel
        ihdrDos.writeByte(if (hasAlpha) 6 else 2) // 6 = RGBA, 2 = RGB
        ihdrDos.writeByte(0) // Deflate compression
        ihdrDos.writeByte(0) // Filter method 0
        ihdrDos.writeByte(0) // Interlace method 0 (no interlace)
        ihdrDos.flush()

        writeChunk(outputStream, "IHDR", ihdrBos.toByteArray())

        // 3. Setup streaming IDAT Deflater
        deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        idatBuffer = ByteArrayOutputStream(65536)
        deflaterStream = DeflaterOutputStream(idatBuffer, deflater!!, true)
    }

    /**
     * Streams a band of scanlines into the Deflater.
     * @param rawRgbaBytes RGBA pixel buffer of size `width * rowCount * 4`.
     * @param rowCount Number of rows in this band.
     */
    fun writeScanlineBand(rawRgbaBytes: ByteArray, rowCount: Int) {
        val stream = deflaterStream ?: throw IllegalStateException("Encoder not initialized; call begin() first")
        val bpp = if (hasAlpha) 4 else 3
        val scanlineBytes = width * bpp
        val rowBuffer = ByteArray(scanlineBytes + 1)
        rowBuffer[0] = 0 // Filter type: None

        for (r in 0 until rowCount) {
            val srcRowOffset = r * width * 4
            if (hasAlpha) {
                System.arraycopy(rawRgbaBytes, srcRowOffset, rowBuffer, 1, scanlineBytes)
            } else {
                // Strip alpha channel for 24-bit RGB
                var dstIdx = 1
                for (x in 0 until width) {
                    val sIdx = srcRowOffset + x * 4
                    rowBuffer[dstIdx] = rawRgbaBytes[sIdx]         // R
                    rowBuffer[dstIdx + 1] = rawRgbaBytes[sIdx + 1] // G
                    rowBuffer[dstIdx + 2] = rawRgbaBytes[sIdx + 2] // B
                    dstIdx += 3
                }
            }
            stream.write(rowBuffer, 0, scanlineBytes + 1)
            rowsWritten++

            // Flush IDAT chunk if buffer accumulates > 32KB
            if (idatBuffer!!.size() >= 32768) {
                flushIdatChunk()
            }
        }
    }

    fun finish() {
        val stream = deflaterStream ?: return
        val out = underlyingStream ?: return

        stream.finish()
        stream.flush()

        // Flush remaining IDAT data
        if (idatBuffer != null && idatBuffer!!.size() > 0) {
            flushIdatChunk()
        }

        deflater?.end()

        // 4. Write IEND Chunk
        writeChunk(out, "IEND", ByteArray(0))
        out.flush()

        underlyingStream = null
        deflaterStream = null
        idatBuffer = null
        deflater = null
    }

    private fun flushIdatChunk() {
        val buffer = idatBuffer ?: return
        val out = underlyingStream ?: return
        val bytes = buffer.toByteArray()
        if (bytes.isNotEmpty()) {
            writeChunk(out, "IDAT", bytes)
            buffer.reset()
        }
    }

    private fun writeChunk(out: OutputStream, type: String, data: ByteArray) {
        val dos = DataOutputStream(out)
        dos.writeInt(data.size)

        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        crc.reset()
        crc.update(typeBytes)
        crc.update(data)

        dos.write(typeBytes)
        dos.write(data)
        dos.writeInt(crc.value.toInt())
        dos.flush()
    }
}
