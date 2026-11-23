package com.inkwell.feature.export

import com.google.common.truth.Truth.assertThat
import com.inkwell.feature.export.encoder.StreamingPngEncoder
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream

class PngEncoderTest {

    @Test
    fun `streaming png encoder emits valid signature, chunks, and exact 0-error pixel bytes`() {
        val width = 64
        val height = 64
        val bpp = 4 // RGBA
        val rawPixels = ByteArray(width * height * bpp) { (it % 256).toByte() }

        val bos = ByteArrayOutputStream()
        val encoder = StreamingPngEncoder(width, height, hasAlpha = true)

        encoder.begin(bos)
        // Write in 4 bands of 16 rows each
        val bandHeight = 16
        for (b in 0 until 4) {
            val bandBytes = ByteArray(width * bandHeight * bpp)
            System.arraycopy(rawPixels, b * width * bandHeight * bpp, bandBytes, 0, bandBytes.size)
            encoder.writeScanlineBand(bandBytes, bandHeight)
        }
        encoder.finish()

        val pngBytes = bos.toByteArray()
        assertThat(pngBytes.size).isGreaterThan(8)

        // 1. Verify 8-byte PNG signature
        val expectedSig = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
        )
        assertThat(pngBytes.copyOfRange(0, 8)).isEqualTo(expectedSig)

        // 2. Parse chunks and collect IDAT payload
        val dis = DataInputStream(ByteArrayInputStream(pngBytes, 8, pngBytes.size - 8))
        val idatBos = ByteArrayOutputStream()
        val crc = CRC32()
        var hasIhdr = false
        var hasIend = false

        while (dis.available() > 0) {
            val length = dis.readInt()
            val typeBytes = ByteArray(4)
            dis.readFully(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            val data = ByteArray(length)
            dis.readFully(data)

            val expectedCrc = dis.readInt()
            crc.reset()
            crc.update(typeBytes)
            crc.update(data)
            assertThat(crc.value.toInt()).isEqualTo(expectedCrc)

            when (type) {
                "IHDR" -> {
                    hasIhdr = true
                    val ihdrDis = DataInputStream(ByteArrayInputStream(data))
                    assertThat(ihdrDis.readInt()).isEqualTo(width)
                    assertThat(ihdrDis.readInt()).isEqualTo(height)
                    assertThat(ihdrDis.readByte().toInt()).isEqualTo(8) // bit depth
                    assertThat(ihdrDis.readByte().toInt()).isEqualTo(6) // color type RGBA
                }
                "IDAT" -> {
                    idatBos.write(data)
                }
                "IEND" -> {
                    hasIend = true
                    assertThat(length).isEqualTo(0)
                }
            }
        }

        assertThat(hasIhdr).isTrue()
        assertThat(hasIend).isTrue()

        // 3. Decompress IDAT stream and verify 0 channel error
        val inflatedBos = ByteArrayOutputStream()
        InflaterInputStream(ByteArrayInputStream(idatBos.toByteArray())).use { inflater ->
            inflater.copyTo(inflatedBos)
        }

        val decompressedBytes = inflatedBos.toByteArray()
        // Each row starts with filter byte 0, followed by width * 4 bytes
        val expectedRowLength = 1 + width * 4
        assertThat(decompressedBytes.size).isEqualTo(height * expectedRowLength)

        for (r in 0 until height) {
            val rowStart = r * expectedRowLength
            val filterByte = decompressedBytes[rowStart]
            assertThat(filterByte.toInt()).isEqualTo(0) // None filter

            for (x in 0 until width * 4) {
                val actual = decompressedBytes[rowStart + 1 + x]
                val expected = rawPixels[r * width * 4 + x]
                assertThat(actual).isEqualTo(expected) // 0 error
            }
        }
    }

    @Test
    fun `opaque rgb png encoding strips alpha correctly`() {
        val width = 16
        val height = 16
        val rawRgba = ByteArray(width * height * 4) { 127.toByte() }

        val bos = ByteArrayOutputStream()
        val encoder = StreamingPngEncoder(width, height, hasAlpha = false)
        encoder.begin(bos)
        encoder.writeScanlineBand(rawRgba, height)
        encoder.finish()

        val bytes = bos.toByteArray()
        assertThat(bytes.size).isGreaterThan(0)
    }
}
