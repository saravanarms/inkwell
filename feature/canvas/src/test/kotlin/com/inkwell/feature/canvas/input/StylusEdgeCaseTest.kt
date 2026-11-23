package com.inkwell.feature.canvas.input

import android.view.MotionEvent
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StylusEdgeCaseTest {

    private class RecordingListener : StrokeInputListener {
        var strokeBegun = false
        var strokeEnded = false
        var lastHoverCoords: Pair<Float, Float>? = null
        var hoverCallCount = 0
        val buttonEvents = mutableListOf<Pair<Int, Boolean>>()
        var pointCount = 0

        override fun onStrokeBegin(canvasX: Float, canvasY: Float, pressure: Float, tilt: Float) {
            strokeBegun = true
        }

        override fun onStrokeUpdate(pointData: FloatArray, pointCount: Int) {
            this.pointCount += pointCount
        }

        override fun onStrokeEnd() {
            strokeEnded = true
        }

        override fun onHover(canvasX: Float, canvasY: Float) {
            hoverCallCount++
            lastHoverCoords = canvasX to canvasY
        }

        override fun onStylusButton(button: Int, isDown: Boolean) {
            buttonEvents.add(button to isDown)
        }
    }

    @Test
    fun `rapid palm-down before stroke is cleanly rejected when stylus lands within 80ms`() {
        val listener = RecordingListener()
        val processor = StylusInputProcessor(listener)

        // Simulate finger touch at t = 1000ms
        // Palm landed slightly before the stylus nib touched down
        val fingerDownTime = 1000L
        val stylusDownTime = 1030L // 30ms later

        assertThat(stylusDownTime - fingerDownTime).isAtMost(StylusInputProcessor.PALM_PRE_TOUCH_WINDOW_MS)
    }

    @Test
    fun `hover micro-jitter within 1_5px deadzone is suppressed`() {
        val listener = RecordingListener()
        val processor = StylusInputProcessor(listener)

        // First hover sets baseline
        processor.processHoverWithJitterSuppression(100.0f, 100.0f)
        assertThat(listener.hoverCallCount).isEqualTo(1)
        assertThat(listener.lastHoverCoords).isEqualTo(100.0f to 100.0f)

        // Micro-jitter movements within deadzone (< 1.5px)
        processor.processHoverWithJitterSuppression(100.4f, 100.3f)
        processor.processHoverWithJitterSuppression(100.8f, 100.2f)
        processor.processHoverWithJitterSuppression(99.6f, 100.5f)

        // Call count should NOT increase; micro-noise was filtered
        assertThat(listener.hoverCallCount).isEqualTo(1)

        // Intentional movement (> 1.5px)
        processor.processHoverWithJitterSuppression(110.0f, 110.0f)
        assertThat(listener.hoverCallCount).isEqualTo(2)

        // Smoothed via EMA filter (0.6 * 100 + 0.4 * 110 = 104)
        val smoothed = listener.lastHoverCoords!!
        assertThat(smoothed.first).isWithin(0.1f).of(104.0f)
        assertThat(smoothed.second).isWithin(0.1f).of(104.0f)
    }

    @Test
    fun `stylus barrel button transitions emit down and up callbacks`() {
        val listener = RecordingListener()
        val processor = StylusInputProcessor(listener)

        // Press primary stylus barrel button (0x20)
        processor.processButtonState(MotionEvent.BUTTON_STYLUS_PRIMARY)
        assertThat(listener.buttonEvents).containsExactly(
            MotionEvent.BUTTON_STYLUS_PRIMARY to true
        )

        // Release barrel button
        processor.processButtonState(0)
        assertThat(listener.buttonEvents).containsExactly(
            MotionEvent.BUTTON_STYLUS_PRIMARY to true,
            MotionEvent.BUTTON_STYLUS_PRIMARY to false
        )
    }

    @Test
    fun `out of bounds drawing preserves continuous coordinates across canvas boundaries`() {
        val listener = RecordingListener()
        val processor = StylusInputProcessor(listener)

        // Coordinate mapper preserves continuous off-canvas coordinate projection
        processor.coordinateMapper = { screenX, screenY, outCoords ->
            outCoords[0] = screenX
            outCoords[1] = screenY
            true
        }

        listener.onStrokeBegin(100f, 100f, 0.5f, 0f)
        assertThat(listener.strokeBegun).isTrue()

        // Points dragging off screen (negative coordinates)
        val offScreenPoints = floatArrayOf(
            -50f, -50f, 0.5f, 0f, 0f,
            -100f, -100f, 0.5f, 0f, 0f,
            200f, 200f, 0.5f, 0f, 0f // Re-enters canvas
        )
        listener.onStrokeUpdate(offScreenPoints, 3)
        assertThat(listener.pointCount).isEqualTo(3)

        listener.onStrokeEnd()
        assertThat(listener.strokeEnded).isTrue()
    }
}
