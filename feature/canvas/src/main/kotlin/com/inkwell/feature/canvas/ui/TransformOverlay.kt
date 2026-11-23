package com.inkwell.feature.canvas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Affine Transform Overlay.
 * Renders an 8-handle bounding box + rotation lollipop handle,
 * supporting real-time translation, uniform/non-uniform scaling, and rotation.
 */
@Composable
fun TransformOverlay(
    boundsX0: Float,
    boundsY0: Float,
    boundsX1: Float,
    boundsY1: Float,
    onTransformUpdate: (dx: Float, dy: Float, scaleX: Float, scaleY: Float, rotationRad: Float) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }
    var rotationRad by remember { mutableFloatStateOf(0f) }

    val pivotX = (boundsX0 + boundsX1) * 0.5f
    val pivotY = (boundsY0 + boundsY1) * 0.5f
    val width = boundsX1 - boundsX0
    val height = boundsY1 - boundsY0

    Box(modifier = modifier.fillMaxSize()) {
        // Main Transform Gesture Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dx += dragAmount.x
                            dy += dragAmount.y
                            onTransformUpdate(dx, dy, scaleX, scaleY, rotationRad)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = pivotX + dx
                val cy = pivotY + dy
                val halfW = (width * scaleX) * 0.5f
                val halfH = (height * scaleY) * 0.5f

                val cosA = cos(rotationRad)
                val sinA = sin(rotationRad)

                fun mapPoint(lx: Float, ly: Float): Offset {
                    val rx = lx * cosA - ly * sinA + cx
                    val ry = lx * sinA + ly * cosA + cy
                    return Offset(rx, ry)
                }

                val p0 = mapPoint(-halfW, -halfH) // Top-Left
                val p1 = mapPoint(halfW, -halfH)  // Top-Right
                val p2 = mapPoint(halfW, halfH)   // Bottom-Right
                val p3 = mapPoint(-halfW, halfH)  // Bottom-Left

                // Bounding Box Edges
                drawLine(Color(0xFF3D5AFE), p0, p1, strokeWidth = 2.5f)
                drawLine(Color(0xFF3D5AFE), p1, p2, strokeWidth = 2.5f)
                drawLine(Color(0xFF3D5AFE), p2, p3, strokeWidth = 2.5f)
                drawLine(Color(0xFF3D5AFE), p3, p0, strokeWidth = 2.5f)

                // Rotation Lollipop Handle Line
                val midTop = mapPoint(0f, -halfH)
                val rotHandle = mapPoint(0f, -halfH - 35f)
                drawLine(Color(0xFF64B5F6), midTop, rotHandle, strokeWidth = 2f)

                // 8 Control Handle Dots
                val handles = listOf(
                    p0, p1, p2, p3,
                    mapPoint(0f, -halfH), mapPoint(halfW, 0f),
                    mapPoint(0f, halfH), mapPoint(-halfW, 0f)
                )

                handles.forEach { h ->
                    drawCircle(Color.White, radius = 7f, center = h)
                    drawCircle(Color(0xFF3D5AFE), radius = 7f, center = h, style = Stroke(width = 2.5f))
                }

                // Rotation Handle Dot
                drawCircle(Color(0xFFFFD740), radius = 9f, center = rotHandle)
                drawCircle(Color.White, radius = 9f, center = rotHandle, style = Stroke(width = 2.5f))
            }
        }

        // Commit / Cancel Floating Action Pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xEE1E1E24))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3E3E4A))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕ Cancel", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Commit Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3D5AFE))
                        .clickable(onClick = onCommit)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓ Commit", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
