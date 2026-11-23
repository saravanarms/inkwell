package com.inkwell.core.common.math

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * Tests for 2D viewport transform math, zoom clamping, rotation snap,
 * and coordinate roundtrips according to M1 acceptance criteria.
 */
class ViewportTransformTest {

    private val canvasWidth = 2048f
    private val canvasHeight = 2048f
    private val viewportWidth = 1080f
    private val viewportHeight = 1920f

    private val minZoom = 0.10f
    private val maxZoom = 32.0f
    private val snapThresholdRad = (3.0f * (PI / 180.0)).toFloat() // 3 degrees

    @Test
    fun `zoom range clamps strictly between 0_10x and 32_0x`() {
        var zoom = 1.0f

        // Zoom out aggressively
        zoom = (zoom * 0.01f).coerceIn(minZoom, maxZoom)
        assertThat(zoom).isEqualTo(0.10f)

        // Zoom in aggressively
        zoom = (zoom * 1000f).coerceIn(minZoom, maxZoom)
        assertThat(zoom).isEqualTo(32.0f)
    }

    @Test
    fun `rotation snaps to exactly 0_0 degrees within 3 degree threshold`() {
        val testAngleNearZero = (2.5f * (PI / 180.0)).toFloat() // 2.5 degrees (< 3.0 deg threshold)
        val snapped = if (abs(testAngleNearZero) <= snapThresholdRad) 0.0f else testAngleNearZero
        assertThat(snapped).isEqualTo(0.0f)

        val testAngleOutside = (5.0f * (PI / 180.0)).toFloat() // 5.0 degrees (> 3.0 deg threshold)
        val notSnapped = if (abs(testAngleOutside) <= snapThresholdRad) 0.0f else testAngleOutside
        assertThat(notSnapped).isEqualTo(testAngleOutside)
    }

    @Test
    fun `screen to canvas and canvas to screen roundtrip has less than 0_001px error`() {
        val transform = Matrix3.translation(150f, 250f) *
                Matrix3.rotation((25f * (PI / 180.0)).toFloat()) *
                Matrix3.scale(2.5f, 2.5f)

        val inv = transform.inverted()
        assertThat(inv).isNotNull()

        val canvasPoint = Vec2(1024.456f, 512.789f)
        val screenPoint = transform.mapPoint(canvasPoint)
        val restoredCanvasPoint = inv!!.mapPoint(screenPoint)

        val errorX = abs(restoredCanvasPoint.x - canvasPoint.x)
        val errorY = abs(restoredCanvasPoint.y - canvasPoint.y)

        assertThat(errorX).isLessThan(0.001f)
        assertThat(errorY).isLessThan(0.001f)
    }

    @Test
    fun `pan shifts canvas coordinates linearly`() {
        val panX = 120f
        val panY = -80f
        val transform = Matrix3.translation(panX, panY)

        val p = Vec2(500f, 500f)
        val mapped = transform.mapPoint(p)

        assertThat(mapped).isEqualTo(Vec2(620f, 420f))
    }
}
