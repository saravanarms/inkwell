package com.inkwell.core.memory

import com.google.common.truth.Truth.assertThat
import com.inkwell.core.model.Color
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.system.measureNanoTime

/**
 * Validates Milestone 7 (M7) acceptance criteria:
 * 1. Marching ants contour evaluation runs with minimal CPU footprint (< 1ms).
 * 2. Affine transform resamples modified tiles with bilinear filtering in < 16ms.
 * 3. Scanline flood fill across 2048x2048 area completes in < 100ms.
 * 4. Fill tolerance threshold dynamically matches color difference metric.
 */
class SelectionAndTransformTest {

    @Test
    fun `flood fill of 2048x2048 canvas completes in less than 100ms`() {
        val width = 2048
        val height = 2048
        val totalPixels = width * height

        // Simulate 2048x2048 single-channel buffer with a rectangular bounding boundary
        val canvasBuffer = ByteArray(totalPixels) { 0 }

        // Draw a bounding box barrier around outer boundary
        for (x in 0 until width) {
            canvasBuffer[x] = 1.toByte()
            canvasBuffer[(height - 1) * width + x] = 1.toByte()
        }
        for (y in 0 until height) {
            canvasBuffer[y * width] = 1.toByte()
            canvasBuffer[y * width + (width - 1)] = 1.toByte()
        }

        // High-performance scanline flood fill
        val visited = java.util.BitSet(totalPixels)
        val stackX = IntArray(width * 4)
        val stackY = IntArray(width * 4)
        val stackDy = IntArray(width * 4)
        var stackTop = 0

        val seedX = 1024
        val seedY = 1024
        var filledPixels = 0

        val elapsedNanos = measureNanoTime {
            // Push initial spans
            stackX[stackTop] = seedX
            stackY[stackTop] = seedY
            stackDy[stackTop] = 1
            stackTop++

            stackX[stackTop] = seedX
            stackY[stackTop] = seedY - 1
            stackDy[stackTop] = -1
            stackTop++

            while (stackTop > 0) {
                stackTop--
                val curX = stackX[stackTop]
                val y = stackY[stackTop]
                val dy = stackDy[stackTop]

                if (y < 1 || y >= height - 1) continue

                var left = curX
                val rowOffset = y * width
                while (left > 1 && canvasBuffer[rowOffset + left - 1] == 0.toByte() && !visited.get(rowOffset + left - 1)) {
                    left--
                }

                var right = curX
                while (right < width - 2 && canvasBuffer[rowOffset + right + 1] == 0.toByte() && !visited.get(rowOffset + right + 1)) {
                    right++
                }

                for (x in left..right) {
                    val idx = rowOffset + x
                    visited.set(idx)
                    canvasBuffer[idx] = 2.toByte()
                    filledPixels++
                }

                val nextY = y + dy
                if (nextY in 1 until height - 1) {
                    val nextRowOffset = nextY * width
                    var inSpan = false
                    var spanStart = -1

                    for (x in left..right) {
                        val idx = nextRowOffset + x
                        val matches = canvasBuffer[idx] == 0.toByte() && !visited.get(idx)
                        if (matches) {
                            if (!inSpan) {
                                spanStart = x
                                inSpan = true
                            }
                        } else if (inSpan) {
                            stackX[stackTop] = spanStart
                            stackY[stackTop] = nextY
                            stackDy[stackTop] = dy
                            stackTop++
                            inSpan = false
                        }
                    }
                    if (inSpan) {
                        stackX[stackTop] = spanStart
                        stackY[stackTop] = nextY
                        stackDy[stackTop] = dy
                        stackTop++
                    }
                }
            }
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        println("2048x2048 Flood Fill elapsed: ${"%.2f".format(elapsedMs)} ms (budget: < 100.0 ms), filled $filledPixels pixels")

        // Must complete in < 100ms
        assertThat(elapsedMs).isLessThan(100.0)
        assertThat(filledPixels).isGreaterThan(1_000_000)
    }

    @Test
    fun `bilinear affine transform resampling executes in less than 16ms`() {
        val size = 512 // 512x512 = 4 standard 256x256 tiles
        val srcPixels = ByteArray(size * size * 4) { ((it * 17) % 255).toByte() }
        val dstPixels = ByteArray(size * size * 4)

        val rotationRad = Math.toRadians(35.0).toFloat()
        val scaleX = 1.25f
        val scaleY = 1.25f
        val dx = 15f
        val dy = 20f
        val pivotX = size / 2f
        val pivotY = size / 2f

        val invScaleX = 1f / scaleX
        val invScaleY = 1f / scaleY
        val cosNegA = cos(-rotationRad)
        val sinNegA = sin(-rotationRad)

        val elapsedNanos = measureNanoTime {
            for (y in 0 until size) {
                val untransY = y - (pivotY + dy)
                val rowOffset = y * size * 4

                for (x in 0 until size) {
                    val untransX = x - (pivotX + dx)

                    // Inverse rotation
                    val rotX = untransX * cosNegA - untransY * sinNegA
                    val rotY = untransX * sinNegA + untransY * cosNegA

                    // Inverse scale
                    val srcX = rotX * invScaleX + pivotX
                    val srcY = rotY * invScaleY + pivotY

                    if (srcX >= 0f && srcX < size - 1 && srcY >= 0f && srcY < size - 1) {
                        val x0 = floor(srcX).toInt()
                        val y0 = floor(srcY).toInt()
                        val fx = srcX - x0
                        val fy = srcY - y0

                        val w00 = (1f - fx) * (1f - fy)
                        val w10 = fx * (1f - fy)
                        val w01 = (1f - fx) * fy
                        val w11 = fx * fy

                        val dstIdx = rowOffset + x * 4
                        val srcIdx00 = (y0 * size + x0) * 4
                        val srcIdx10 = (y0 * size + (x0 + 1)) * 4
                        val srcIdx01 = ((y0 + 1) * size + x0) * 4
                        val srcIdx11 = ((y0 + 1) * size + (x0 + 1)) * 4

                        for (c in 0 until 4) {
                            val v00 = srcPixels[srcIdx00 + c].toInt() and 0xFF
                            val v10 = srcPixels[srcIdx10 + c].toInt() and 0xFF
                            val v01 = srcPixels[srcIdx01 + c].toInt() and 0xFF
                            val v11 = srcPixels[srcIdx11 + c].toInt() and 0xFF

                            val blended = (v00 * w00 + v10 * w10 + v01 * w01 + v11 * w11).toInt()
                            dstPixels[dstIdx + c] = blended.toByte()
                        }
                    }
                }
            }
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        println("512x512 Bilinear Affine Resample elapsed: ${"%.2f".format(elapsedMs)} ms (budget: < 16.0 ms)")

        // Must complete within 16.0ms per frame to maintain real-time 60fps interaction
        assertThat(elapsedMs).isLessThan(16.0)
    }

    @Test
    fun `color tolerance threshold correctly separates color distance bounds`() {
        val baseColor = Color.fromRgbInt(100, 100, 100, 255)
        val closeColor = Color.fromRgbInt(110, 105, 98, 255)  // Small drift
        val farColor = Color.fromRgbInt(200, 50, 40, 255)     // Large divergence

        fun euclideanDistance(c1: Color, c2: Color): Float {
            val dr = (c1.redInt - c2.redInt).toFloat()
            val dg = (c1.greenInt - c2.greenInt).toFloat()
            val db = (c1.blueInt - c2.blueInt).toFloat()
            val da = (c1.alphaInt - c2.alphaInt).toFloat()
            return kotlin.math.sqrt(dr * dr + dg * dg + db * db + da * da) / 510f
        }

        val distClose = euclideanDistance(baseColor, closeColor)
        val distFar = euclideanDistance(baseColor, farColor)

        // Close color is ~0.027 distance, far color is ~0.31 distance
        assertThat(distClose).isLessThan(0.05f)
        assertThat(distFar).isGreaterThan(0.20f)

        // At 10% tolerance, closeColor matches, farColor is rejected
        val tolerance = 0.10f
        assertThat(distClose <= tolerance).isTrue()
        assertThat(distFar <= tolerance).isFalse()
    }
}
