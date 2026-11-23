package com.inkwell.feature.canvas.input

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StylusInputProcessorTest {

    private class TestStrokeListener : StrokeInputListener {
        var strokeBegun = false
        var strokeEnded = false
        var updateCallCount = 0
        var totalPointsReceived = 0

        override fun onStrokeBegin(canvasX: Float, canvasY: Float, pressure: Float, tilt: Float) {
            strokeBegun = true
        }

        override fun onStrokeUpdate(pointData: FloatArray, pointCount: Int) {
            updateCallCount++
            totalPointsReceived += pointCount
        }

        override fun onStrokeEnd() {
            strokeEnded = true
        }
    }

    @Test
    fun `stroke listener receives lifecycle callbacks`() {
        val listener = TestStrokeListener()
        val processor = StylusInputProcessor(listener)

        listener.onStrokeBegin(100f, 100f, 0.5f, 0.0f)
        assertThat(listener.strokeBegun).isTrue()

        val dummyData = floatArrayOf(
            100f, 100f, 0.5f, 0f, 0f,
            110f, 110f, 0.6f, 0f, 0f
        )
        listener.onStrokeUpdate(dummyData, 2)
        assertThat(listener.totalPointsReceived).isEqualTo(2)

        listener.onStrokeEnd()
        assertThat(listener.strokeEnded).isTrue()
    }
}
