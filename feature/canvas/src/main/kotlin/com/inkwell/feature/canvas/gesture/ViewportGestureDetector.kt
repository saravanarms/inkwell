package com.inkwell.feature.canvas.gesture

import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.hypot

interface ViewportGestureListener {
    fun onPan(dx: Float, dy: Float)
    fun onZoom(scaleFactor: Float, focusX: Float, focusY: Float)
    fun onRotate(deltaRadians: Float, focusX: Float, focusY: Float)
    fun onTwoFingerTap() {}
    fun onThreeFingerTap() {}
    fun onFourFingerTap() {}
}

/**
 * High-performance multi-touch gesture detector specialized for fluid viewport pan, zoom, rotate,
 * and multi-finger tap shortcuts (2-finger undo, 3-finger redo, 4-finger immersive mode toggle).
 */
class ViewportGestureDetector(
    private val listener: ViewportGestureListener
) {
    private var isGestureActive = false
    private var prevFocusX = 0f
    private var prevFocusY = 0f
    private var prevSpan = 0f
    private var prevAngle = 0f

    private var downTime = 0L
    private var touchSlopMoved = false
    private var maxPointersInGesture = 1

    fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        // Quick multi-finger tap detection (undo, redo, immersive HUD toggle)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTime = event.eventTime
                touchSlopMoved = false
                maxPointersInGesture = 1
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                maxPointersInGesture = maxOf(maxPointersInGesture, pointerCount)
                if (pointerCount == 2) {
                    initTwoFingerGesture(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerCount >= 2) {
                    handleMultiTouchMove(event)
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val elapsed = event.eventTime - downTime
                if (!touchSlopMoved && elapsed < TAP_TIMEOUT_MS) {
                    when (maxPointersInGesture) {
                        2 -> listener.onTwoFingerTap()
                        3 -> listener.onThreeFingerTap()
                        4 -> listener.onFourFingerTap()
                    }
                }
                isGestureActive = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isGestureActive = false
                maxPointersInGesture = 1
            }
        }

        return isGestureActive
    }

    private fun initTwoFingerGesture(event: MotionEvent) {
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)

        prevFocusX = (x0 + x1) * 0.5f
        prevFocusY = (y0 + y1) * 0.5f
        prevSpan = hypot(x1 - x0, y1 - y0)
        prevAngle = atan2(y1 - y0, x1 - x0)
        isGestureActive = true
    }

    private fun handleMultiTouchMove(event: MotionEvent) {
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)

        val focusX = (x0 + x1) * 0.5f
        val focusY = (y0 + y1) * 0.5f
        val span = hypot(x1 - x0, y1 - y0)
        val angle = atan2(y1 - y0, x1 - x0)

        if (!isGestureActive) {
            prevFocusX = focusX
            prevFocusY = focusY
            prevSpan = span
            prevAngle = angle
            isGestureActive = true
            return
        }

        // 1. Pan
        val dx = focusX - prevFocusX
        val dy = focusY - prevFocusY
        if (hypot(dx, dy) > 2f) {
            touchSlopMoved = true
            listener.onPan(dx, dy)
        }

        // 2. Zoom
        if (prevSpan > 10f && span > 10f) {
            val scaleFactor = span / prevSpan
            if (kotlin.math.abs(scaleFactor - 1.0f) > 0.005f) {
                touchSlopMoved = true
                listener.onZoom(scaleFactor, focusX, focusY)
            }
        }

        // 3. Rotate
        var deltaAngle = angle - prevAngle
        while (deltaAngle > Math.PI) deltaAngle -= (2.0 * Math.PI).toFloat()
        while (deltaAngle < -Math.PI) deltaAngle += (2.0 * Math.PI).toFloat()
        if (kotlin.math.abs(deltaAngle) > 0.008f) { // ~0.5 degree threshold
            touchSlopMoved = true
            listener.onRotate(deltaAngle, focusX, focusY)
        }

        prevFocusX = focusX
        prevFocusY = focusY
        prevSpan = span
        prevAngle = angle
    }

    companion object {
        private const val TAP_TIMEOUT_MS = 300L
    }
}
