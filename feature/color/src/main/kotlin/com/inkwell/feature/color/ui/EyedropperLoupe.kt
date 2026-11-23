package com.inkwell.feature.color.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.inkwell.core.model.Color
import kotlin.math.roundToInt

/**
 * Interactive Eyedropper Magnifying Loupe.
 * Displays a 15x15 magnified pixel grid, a central sampling crosshair reticle,
 * and an outer ring comparing the active sampled color against the previous brush color.
 */
@Composable
fun EyedropperLoupe(
    screenX: Float,
    screenY: Float,
    sampledColor: Color,
    previousColor: Color,
    gridPixels: ByteArray?,
    gridSize: Int = 15,
    modifier: Modifier = Modifier
) {
    // Offset loupe ~80dp above finger to avoid touch occlusion
    val loupeSizeDp = 120.dp
    val offsetYDp = (-85).dp

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (screenX - 60 * density).roundToInt(),
                    y = (screenY - 145 * density).roundToInt()
                )
            }
            .size(loupeSizeDp)
    ) {
        Canvas(modifier = Modifier.size(loupeSizeDp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerRadius = minOf(cx, cy)
            val ringWidth = 14f
            val innerRadius = outerRadius - ringWidth

            // 1. Draw Inner 15x15 Magnified Pixel Grid
            if (gridPixels != null && gridPixels.size >= gridSize * gridSize * 4) {
                val cellSize = (innerRadius * 2f) / gridSize
                val startX = cx - innerRadius
                val startY = cy - innerRadius

                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        val pxX = startX + col * cellSize
                        val pxY = startY + row * cellSize

                        // Clip to circle visually
                        val distFromCenter = kotlin.math.hypot(pxX + cellSize / 2f - cx, pxY + cellSize / 2f - cy)
                        if (distFromCenter <= innerRadius) {
                            val byteIdx = (row * gridSize + col) * 4
                            val r = gridPixels[byteIdx].toInt() and 0xFF
                            val g = gridPixels[byteIdx + 1].toInt() and 0xFF
                            val b = gridPixels[byteIdx + 2].toInt() and 0xFF
                            val a = gridPixels[byteIdx + 3].toInt() and 0xFF

                            drawRect(
                                color = ComposeColor(r, g, b, a),
                                topLeft = Offset(pxX, pxY),
                                size = Size(cellSize, cellSize)
                            )

                            // Subtle grid lines
                            drawRect(
                                color = ComposeColor(0x22000000),
                                topLeft = Offset(pxX, pxY),
                                size = Size(cellSize, cellSize),
                                style = Stroke(width = 0.5f)
                            )
                        }
                    }
                }
            } else {
                // Fallback: solid sampled color
                drawCircle(
                    color = ComposeColor(sampledColor.toPackedRgba()),
                    radius = innerRadius,
                    center = Offset(cx, cy)
                )
            }

            // 2. Central Reticle / Crosshair Highlighting Exact Target Pixel
            val centerCellSize = (innerRadius * 2f) / gridSize
            drawRect(
                color = ComposeColor.Black,
                topLeft = Offset(cx - centerCellSize / 2f - 1f, cy - centerCellSize / 2f - 1f),
                size = Size(centerCellSize + 2f, centerCellSize + 2f),
                style = Stroke(width = 2f)
            )
            drawRect(
                color = ComposeColor.White,
                topLeft = Offset(cx - centerCellSize / 2f, cy - centerCellSize / 2f),
                size = Size(centerCellSize, centerCellSize),
                style = Stroke(width = 1.5f)
            )

            // Crosshair tick marks
            drawLine(
                color = ComposeColor.White,
                start = Offset(cx - centerCellSize, cy),
                end = Offset(cx - centerCellSize / 2f - 2f, cy),
                strokeWidth = 2f
            )
            drawLine(
                color = ComposeColor.White,
                start = Offset(cx + centerCellSize / 2f + 2f, cy),
                end = Offset(cx + centerCellSize, cy),
                strokeWidth = 2f
            )
            drawLine(
                color = ComposeColor.White,
                start = Offset(cx, cy - centerCellSize),
                end = Offset(cx, cy - centerCellSize / 2f - 2f),
                strokeWidth = 2f
            )
            drawLine(
                color = ComposeColor.White,
                start = Offset(cx, cy + centerCellSize / 2f + 2f),
                end = Offset(cx, cy + centerCellSize),
                strokeWidth = 2f
            )

            // 3. Outer Comparison Ring:
            // Top/Right Arc = Currently Sampled Color
            drawArc(
                color = ComposeColor(sampledColor.toPackedRgba()),
                startAngle = -45f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - outerRadius + ringWidth / 2f, cy - outerRadius + ringWidth / 2f),
                size = Size((outerRadius - ringWidth / 2f) * 2f, (outerRadius - ringWidth / 2f) * 2f),
                style = Stroke(width = ringWidth)
            )

            // Bottom/Left Arc = Previous Color
            drawArc(
                color = ComposeColor(previousColor.toPackedRgba()),
                startAngle = 135f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - outerRadius + ringWidth / 2f, cy - outerRadius + ringWidth / 2f),
                size = Size((outerRadius - ringWidth / 2f) * 2f, (outerRadius - ringWidth / 2f) * 2f),
                style = Stroke(width = ringWidth)
            )

            // Outer white boundary ring
            drawCircle(
                color = ComposeColor.White,
                radius = outerRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = ComposeColor(0x66000000),
                radius = innerRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }
    }
}
