package com.inkwell.feature.canvas.input

import android.view.MotionEvent
import com.inkwell.core.model.PressureCurve
import kotlin.math.hypot

interface StrokeInputListener {
    fun onStrokeBegin(canvasX: Float, canvasY: Float, pressure: Float, tilt: Float)
    fun onStrokeUpdate(pointData: FloatArray, pointCount: Int)
    fun onStrokeEnd()
    fun onHover(canvasX: Float, canvasY: Float) {}
    fun onStylusButton(button: Int, isDown: Boolean) {}
}

/**
 * High-speed input processor isolating stylus input from finger touch,
 * executing hardware palm rejection, hover jitter suppression,
 * extreme tilt clamping, and extracting 240Hz historical batches.
 */
class StylusInputProcessor(
    private val listener: StrokeInputListener,
    var pressureCurve: PressureCurve = PressureCurve.LINEAR
) {
    var isStylusActive = false
        private set
    var isHovering = false
        private set

    private var lastStylusEventTime = 0L
    private var lastFingerDownTime = 0L
    private var isFingerStrokeActive = false

    // Hover smoothing state (deadzone + exponential moving average)
    private var lastHoverX = Float.NaN
    private var lastHoverY = Float.NaN

    // Stylus barrel button tracking
    private var lastButtonState = 0

    // Reusable point buffer: 5 floats per sample: [x, y, calibratedPressure, tilt, orientation]
    private var pointBuffer = FloatArray(MAX_BATCH_SAMPLES * FLOATS_PER_SAMPLE)

    /**
     * Coordinate converter mapping screen touch coordinates to canvas pixel space.
     */
    var coordinateMapper: ((screenX: Float, screenY: Float, outCoords: FloatArray) -> Boolean)? = null
    private val tempCoord = FloatArray(2)

    fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)
        val currentTime = event.eventTime

        // 1. Process Stylus Barrel Button Transitions
        processButtonState(event.buttonState)

        // 2. Tool Arbitration & Palm Pre-Contact Rejection
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            isStylusActive = true
            lastStylusEventTime = currentTime

            // Rapid palm-down edge case: if a finger touch began within the pre-touch window,
            // immediately abort that finger touch because the palm landed just before the stylus.
            if (isFingerStrokeActive && (currentTime - lastFingerDownTime) <= PALM_PRE_TOUCH_WINDOW_MS) {
                listener.onStrokeEnd()
                isFingerStrokeActive = false
            }
        } else if (toolType == MotionEvent.TOOL_TYPE_FINGER) {
            // Reject finger touch if:
            // a) Stylus is currently in hover range or drawing
            // b) Stylus was active within cooldown window
            // c) Contact area exceeds palm size threshold
            val touchMajor = try { event.touchMajor } catch (_: Throwable) { 0f }
            if (isHovering || isStylusActive ||
                (currentTime - lastStylusEventTime) < STYLUS_COOLDOWN_MS ||
                touchMajor > PALM_SIZE_THRESHOLD_PX
            ) {
                return false // Drop touch: palm rejection asserted
            }
        }

        // 3. Dispatch Stroke Events
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (toolType == MotionEvent.TOOL_TYPE_FINGER) {
                    lastFingerDownTime = currentTime
                    isFingerStrokeActive = true
                }

                if (mapToCanvas(event.x, event.y)) {
                    val rawP = event.pressure
                    val calibratedP = pressureCurve.evaluate(rawP)
                    val tilt = clampTilt(event.getAxisValue(MotionEvent.AXIS_TILT))
                    listener.onStrokeBegin(tempCoord[0], tempCoord[1], calibratedP, tilt)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val historySize = event.historySize
                val totalPoints = historySize + 1
                ensureBufferCapacity(totalPoints)

                var offset = 0
                // Process historical batch samples (up to 240Hz digitizer points)
                for (h in 0 until historySize) {
                    val hx = event.getHistoricalX(h)
                    val hy = event.getHistoricalY(h)
                    if (mapToCanvas(hx, hy)) {
                        val hp = pressureCurve.evaluate(event.getHistoricalPressure(h))
                        val htilt = clampTilt(event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, h))
                        val horient = normalizeOrientation(event.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, h))

                        pointBuffer[offset++] = tempCoord[0]
                        pointBuffer[offset++] = tempCoord[1]
                        pointBuffer[offset++] = hp
                        pointBuffer[offset++] = htilt
                        pointBuffer[offset++] = horient
                    }
                }

                // Append current point
                if (mapToCanvas(event.x, event.y)) {
                    val cp = pressureCurve.evaluate(event.pressure)
                    val ctilt = clampTilt(event.getAxisValue(MotionEvent.AXIS_TILT))
                    val corient = normalizeOrientation(event.getAxisValue(MotionEvent.AXIS_ORIENTATION))

                    pointBuffer[offset++] = tempCoord[0]
                    pointBuffer[offset++] = tempCoord[1]
                    pointBuffer[offset++] = cp
                    pointBuffer[offset++] = ctilt
                    pointBuffer[offset++] = corient
                }

                val pointsEmitted = offset / FLOATS_PER_SAMPLE
                if (pointsEmitted > 0) {
                    listener.onStrokeUpdate(pointBuffer, pointsEmitted)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                listener.onStrokeEnd()
                isStylusActive = false
                isFingerStrokeActive = false
                lastStylusEventTime = currentTime
                return true
            }

            MotionEvent.ACTION_HOVER_ENTER -> {
                isHovering = true
                lastHoverX = Float.NaN
                lastHoverY = Float.NaN
                return true
            }

            MotionEvent.ACTION_HOVER_MOVE -> {
                isHovering = true
                if (mapToCanvas(event.x, event.y)) {
                    processHoverWithJitterSuppression(tempCoord[0], tempCoord[1])
                }
                return true
            }

            MotionEvent.ACTION_HOVER_EXIT -> {
                isHovering = false
                lastHoverX = Float.NaN
                lastHoverY = Float.NaN
                return true
            }
        }

        return false
    }

    /**
     * Filters hover sensor micro-noise using deadzone gating and exponential smoothing.
     */
    fun processHoverWithJitterSuppression(x: Float, y: Float) {
        if (lastHoverX.isNaN() || lastHoverY.isNaN()) {
            lastHoverX = x
            lastHoverY = y
            listener.onHover(x, y)
            return
        }

        val delta = hypot(x - lastHoverX, y - lastHoverY)
        if (delta >= HOVER_DEADZONE_PX) {
            // Apply exponential moving average filter (alpha = 0.40)
            val smoothedX = lastHoverX * (1f - HOVER_EMA_ALPHA) + x * HOVER_EMA_ALPHA
            val smoothedY = lastHoverY * (1f - HOVER_EMA_ALPHA) + y * HOVER_EMA_ALPHA
            lastHoverX = smoothedX
            lastHoverY = smoothedY
            listener.onHover(smoothedX, smoothedY)
        }
    }

    /**
     * Checks button state transitions for S-Pen / Wacom barrel buttons.
     */
    fun processButtonState(newButtonState: Int) {
        if (newButtonState == lastButtonState) return

        val changedBits = newButtonState xor lastButtonState

        // Primary stylus barrel button (0x20)
        if ((changedBits and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0) {
            val isDown = (newButtonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
            listener.onStylusButton(MotionEvent.BUTTON_STYLUS_PRIMARY, isDown)
        }

        // Secondary stylus barrel button (0x40)
        if ((changedBits and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0) {
            val isDown = (newButtonState and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0
            listener.onStylusButton(MotionEvent.BUTTON_STYLUS_SECONDARY, isDown)
        }

        lastButtonState = newButtonState
    }

    private fun mapToCanvas(screenX: Float, screenY: Float): Boolean {
        val mapper = coordinateMapper
        if (mapper != null) {
            val success = mapper(screenX, screenY, tempCoord)
            if (success) return true
        }
        // Fallback: un-mapped coordinate or continuous off-bounds drawing
        tempCoord[0] = screenX
        tempCoord[1] = screenY
        return true
    }

    private fun clampTilt(tilt: Float): Float {
        // Safe clamp to [0.0, PI / 2] radians to eliminate extreme angle discontinuities
        return tilt.coerceIn(0.0f, (Math.PI / 2.0).toFloat())
    }

    private fun normalizeOrientation(orient: Float): Float {
        // Normalize orientation angle to [-PI, PI]
        val twoPi = (2.0 * Math.PI).toFloat()
        val pi = Math.PI.toFloat()
        var angle = orient % twoPi
        if (angle > pi) angle -= twoPi
        if (angle < -pi) angle += twoPi
        return angle
    }

    private fun ensureBufferCapacity(sampleCount: Int) {
        val requiredFloats = sampleCount * FLOATS_PER_SAMPLE
        if (pointBuffer.size < requiredFloats) {
            pointBuffer = FloatArray(requiredFloats * 2)
        }
    }

    companion object {
        const val FLOATS_PER_SAMPLE = 5
        private const val MAX_BATCH_SAMPLES = 64
        private const val STYLUS_COOLDOWN_MS = 300L
        const val PALM_PRE_TOUCH_WINDOW_MS = 80L
        const val PALM_SIZE_THRESHOLD_PX = 45.0f
        const val HOVER_DEADZONE_PX = 1.5f
        private const val HOVER_EMA_ALPHA = 0.40f
    }
}
