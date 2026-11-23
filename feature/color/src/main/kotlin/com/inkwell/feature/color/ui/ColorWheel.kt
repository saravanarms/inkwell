package com.inkwell.feature.color.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.inkwell.core.model.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * High-performance interactive Color Wheel composed of an outer 360° Hue ring
 * and an inner Saturation-Value square selector.
 */
@Composable
fun ColorWheel(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val hsv = remember(color) { color.toHsv() }
    var currentHue by remember(color) { mutableStateOf(hsv[0]) }
    var currentSat by remember(color) { mutableStateOf(hsv[1]) }
    var currentVal by remember(color) { mutableStateOf(hsv[2]) }

    val ringWidthPx = 32f

    // Track which control is currently grabbed during drag: NONE, HUE_RING, SV_BOX
    var activeControl by remember { mutableStateOf(ActiveControl.NONE) }

    Box(
        modifier = modifier
            .size(240.dp)
            .aspectRatio(1f)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dist = hypot(offset.x - cx, offset.y - cy)
                        val outerR = minOf(cx, cy)
                        val innerR = outerR - ringWidthPx

                        if (dist >= innerR - 8f && dist <= outerR + 8f) {
                            // Hit Hue Ring
                            activeControl = ActiveControl.HUE_RING
                            val rad = atan2(offset.y - cy, offset.x - cx)
                            var deg = Math.toDegrees(rad.toDouble()).toFloat()
                            if (deg < 0f) deg += 360f
                            currentHue = deg
                            onColorChanged(Color.fromHsv(currentHue, currentSat, currentVal, color.alpha))
                        } else {
                            // Check Saturation/Value Box
                            val boxHalf = (innerR * 0.707f) * 0.90f
                            val left = cx - boxHalf
                            val top = cy - boxHalf
                            val right = cx + boxHalf
                            val bottom = cy + boxHalf

                            if (offset.x in (left - 8f)..(right + 8f) && offset.y in (top - 8f)..(bottom + 8f)) {
                                activeControl = ActiveControl.SV_BOX
                                val s = ((offset.x - left) / (boxHalf * 2f)).coerceIn(0f, 1f)
                                val v = (1f - (offset.y - top) / (boxHalf * 2f)).coerceIn(0f, 1f)
                                currentSat = s
                                currentVal = v
                                onColorChanged(Color.fromHsv(currentHue, currentSat, currentVal, color.alpha))
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dist = hypot(offset.x - cx, offset.y - cy)
                        val outerR = minOf(cx, cy)
                        val innerR = outerR - ringWidthPx

                        activeControl = if (dist >= innerR - 8f && dist <= outerR + 8f) {
                            ActiveControl.HUE_RING
                        } else {
                            ActiveControl.SV_BOX
                        }
                    },
                    onDragEnd = { activeControl = ActiveControl.NONE },
                    onDragCancel = { activeControl = ActiveControl.NONE },
                    onDrag = { change, _ ->
                        change.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerR = minOf(cx, cy)
                        val innerR = outerR - ringWidthPx
                        val boxHalf = (innerR * 0.707f) * 0.90f

                        when (activeControl) {
                            ActiveControl.HUE_RING -> {
                                val rad = atan2(change.position.y - cy, change.position.x - cx)
                                var deg = Math.toDegrees(rad.toDouble()).toFloat()
                                if (deg < 0f) deg += 360f
                                currentHue = deg
                                onColorChanged(Color.fromHsv(currentHue, currentSat, currentVal, color.alpha))
                            }
                            ActiveControl.SV_BOX -> {
                                val left = cx - boxHalf
                                val top = cy - boxHalf
                                val s = ((change.position.x - left) / (boxHalf * 2f)).coerceIn(0f, 1f)
                                val v = (1f - (change.position.y - top) / (boxHalf * 2f)).coerceIn(0f, 1f)
                                currentSat = s
                                currentVal = v
                                onColorChanged(Color.fromHsv(currentHue, currentSat, currentVal, color.alpha))
                            }
                            ActiveControl.NONE -> {}
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = minOf(cx, cy) - ringWidthPx / 2f
            val innerR = outerR - ringWidthPx / 2f

            // 1. Draw 360° Hue Sweep Ring
            val hueSweepColors = listOf(
                ComposeColor.Red,
                ComposeColor.Yellow,
                ComposeColor.Green,
                ComposeColor.Cyan,
                ComposeColor.Blue,
                ComposeColor.Magenta,
                ComposeColor.Red
            )
            drawCircle(
                brush = Brush.sweepGradient(hueSweepColors, center = Offset(cx, cy)),
                radius = outerR,
                center = Offset(cx, cy),
                style = Stroke(width = ringWidthPx, cap = StrokeCap.Round)
            )

            // 2. Outer Hue Thumb
            val hueAngleRad = Math.toRadians(currentHue.toDouble())
            val thumbX = cx + (outerR * cos(hueAngleRad)).toFloat()
            val thumbY = cy + (outerR * sin(hueAngleRad)).toFloat()
            drawCircle(
                color = ComposeColor.White,
                radius = ringWidthPx * 0.45f,
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = ComposeColor(Color.fromHsv(currentHue, 1f, 1f).toPackedRgba()),
                radius = ringWidthPx * 0.35f,
                center = Offset(thumbX, thumbY)
            )

            // 3. Inner Saturation / Value Box
            val boxHalf = (innerR * 0.707f) * 0.90f
            val boxLeft = cx - boxHalf
            val boxTop = cy - boxHalf
            val boxSize = boxHalf * 2f

            // Base horizontal gradient: White to Pure Hue
            val pureHueColor = ComposeColor(Color.fromHsv(currentHue, 1f, 1f).toPackedRgba())
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(ComposeColor.White, pureHueColor),
                    startX = boxLeft,
                    endX = boxLeft + boxSize
                ),
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxSize, boxSize)
            )

            // Overlay vertical gradient: Transparent to Black
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(ComposeColor.Transparent, ComposeColor.Black),
                    startY = boxTop,
                    endY = boxTop + boxSize
                ),
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxSize, boxSize)
            )

            // Box outline
            drawRect(
                color = ComposeColor(0x33FFFFFF),
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxSize, boxSize),
                style = Stroke(width = 1.5f)
            )

            // 4. Inner SV Thumb
            val svThumbX = boxLeft + currentSat * boxSize
            val svThumbY = boxTop + (1f - currentVal) * boxSize
            drawCircle(
                color = ComposeColor.White,
                radius = 8f,
                center = Offset(svThumbX, svThumbY),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = ComposeColor(color.toPackedRgba()),
                radius = 5.5f,
                center = Offset(svThumbX, svThumbY)
            )
        }
    }
}

private enum class ActiveControl {
    NONE, HUE_RING, SV_BOX
}
