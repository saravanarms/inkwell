package com.inkwell.feature.canvas.gesture

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ViewportGestureDetectorTest {

    private class TestGestureListener : ViewportGestureListener {
        var panDx = 0f
        var panDy = 0f
        var zoomFactor = 0f
        var rotateRad = 0f
        var twoFingerTapCount = 0

        override fun onPan(dx: Float, dy: Float) {
            panDx += dx
            panDy += dy
        }

        override fun onZoom(scaleFactor: Float, focusX: Float, focusY: Float) {
            zoomFactor = scaleFactor
        }

        override fun onRotate(deltaRadians: Float, focusX: Float, focusY: Float) {
            rotateRad = deltaRadians
        }

        override fun onTwoFingerTap() {
            twoFingerTapCount++
        }
    }

    @Test
    fun `gesture listener receives pan and zoom events accurately`() {
        val listener = TestGestureListener()

        listener.onPan(25f, -15f)
        assertThat(listener.panDx).isEqualTo(25f)
        assertThat(listener.panDy).isEqualTo(-15f)

        listener.onZoom(1.25f, 500f, 500f)
        assertThat(listener.zoomFactor).isEqualTo(1.25f)

        listener.onTwoFingerTap()
        assertThat(listener.twoFingerTapCount).isEqualTo(1)
    }
}
