package com.inkwell.feature.canvas.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.inkwell.engine.NativeEngineBridge
import com.inkwell.feature.canvas.gesture.ViewportGestureDetector
import com.inkwell.feature.canvas.gesture.ViewportGestureListener
import com.inkwell.feature.canvas.input.StrokeInputListener
import com.inkwell.feature.canvas.input.StylusInputProcessor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dedicated hardware SurfaceView running an independent background render loop,
 * routing low-latency stylus stroke batches and multi-touch viewport gestures.
 */
class CanvasSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr),
    SurfaceHolder.Callback2,
    ViewportGestureListener,
    StrokeInputListener {

    var engineBridge: NativeEngineBridge? = null
        set(value) {
            field = value
            setupCoordinateMapper()
            requestRender()
        }

    private val gestureDetector = ViewportGestureDetector(this)
    private val stylusProcessor = StylusInputProcessor(this)

    val thermalController = ThermalThrottleController()
    val frameMetricsMonitor = FrameMetricsMonitor()

    var onStylusButtonAction: ((button: Int, isDown: Boolean) -> Unit)? = null
    var onFourFingerTapAction: (() -> Unit)? = null

    private var renderThread: RenderThread? = null
    private val isSurfaceReady = AtomicBoolean(false)
    private val frameRequested = AtomicBoolean(false)

    // Eyedropper state & callbacks
    var onEyedropperUpdate: ((screenX: Float, screenY: Float, color: com.inkwell.core.model.Color, grid: ByteArray) -> Unit)? = null
    var onEyedropperEnd: ((com.inkwell.core.model.Color) -> Unit)? = null

    private var isEyedropperActive = false
    private var downScreenX = 0f
    private var downScreenY = 0f
    private var lastSampledColor: com.inkwell.core.model.Color = com.inkwell.core.model.Color.BLACK
    private val eyedropperHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val eyedropperRunnable = Runnable {
        if (!isEyedropperActive) {
            isEyedropperActive = true
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            engineBridge?.undo()
            requestRender()
            sampleEyedropper(downScreenX, downScreenY)
        }
    }

    private fun sampleEyedropper(screenX: Float, screenY: Float) {
        val bridge = engineBridge ?: return
        val canvasCoords = bridge.screenToCanvas(screenX, screenY) ?: return
        val color = bridge.samplePixel(canvasCoords.x, canvasCoords.y)
        lastSampledColor = color
        val grid = bridge.sampleGrid(canvasCoords.x, canvasCoords.y, 15)
        onEyedropperUpdate?.invoke(screenX, screenY, color, grid)
    }

    init {
        holder.addCallback(this)
        setupCoordinateMapper()
    }

    private fun setupCoordinateMapper() {
        stylusProcessor.coordinateMapper = { screenX, screenY, outCoords ->
            val bridge = engineBridge
            if (bridge != null) {
                val pt = bridge.screenToCanvas(screenX, screenY)
                if (pt != null) {
                    outCoords[0] = pt.x
                    outCoords[1] = pt.y
                    true
                } else {
                    false
                }
            } else {
                outCoords[0] = screenX
                outCoords[1] = screenY
                true
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val bridge = engineBridge ?: return
        bridge.setSurface(holder.surface)
        isSurfaceReady.set(true)

        renderThread = RenderThread().apply {
            start()
        }
        requestRender()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        engineBridge?.surfaceChanged(width, height)
        requestRender()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady.set(false)
        renderThread?.stopRendering()
        renderThread = null
        engineBridge?.surfaceDestroyed()
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        requestRender()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Multi-touch gestures cancel pending eyedropper hold
        if (event.pointerCount > 1) {
            eyedropperHandler.removeCallbacks(eyedropperRunnable)
            if (isEyedropperActive) {
                isEyedropperActive = false
                onEyedropperEnd?.invoke(lastSampledColor)
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downScreenX = event.x
                downScreenY = event.y
                eyedropperHandler.removeCallbacks(eyedropperRunnable)
                eyedropperHandler.postDelayed(eyedropperRunnable, 250L)
            }
            MotionEvent.ACTION_MOVE -> {
                if (kotlin.math.hypot(event.x - downScreenX, event.y - downScreenY) > 20f) {
                    eyedropperHandler.removeCallbacks(eyedropperRunnable)
                }
                if (isEyedropperActive) {
                    sampleEyedropper(event.x, event.y)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                eyedropperHandler.removeCallbacks(eyedropperRunnable)
                if (isEyedropperActive) {
                    isEyedropperActive = false
                    onEyedropperEnd?.invoke(lastSampledColor)
                    return true
                }
            }
        }

        val toolType = event.getToolType(0)

        // 1. Stylus always takes priority
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            val handled = stylusProcessor.onTouchEvent(event)
            if (handled) return true
        }

        // 2. Multi-touch (2+ fingers) reserved for Viewport Pan/Zoom/Rotate
        if (event.pointerCount >= 2) {
            val handled = gestureDetector.onTouchEvent(event)
            if (handled) return true
        }

        // 3. Fallback single-finger touch (supports finger drawing or triggers palm rejection)
        if (event.pointerCount == 1 && toolType == MotionEvent.TOOL_TYPE_FINGER) {
            val handled = stylusProcessor.onTouchEvent(event)
            if (handled) return true
        }

        return super.onTouchEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
            stylusProcessor.onTouchEvent(event)
            return true
        }
        return super.onHoverEvent(event)
    }

    // StrokeInputListener callbacks
    override fun onStrokeBegin(canvasX: Float, canvasY: Float, pressure: Float, tilt: Float) {
        engineBridge?.beginStroke(canvasX, canvasY, pressure)
        requestRender()
    }

    override fun onStrokeUpdate(pointData: FloatArray, pointCount: Int) {
        engineBridge?.appendPoints(pointData, pointCount)
        requestRender()
    }

    override fun onStrokeEnd() {
        engineBridge?.endStroke()
        requestRender()
    }

    override fun onHover(canvasX: Float, canvasY: Float) {
        // Cursor hover position updated
        requestRender()
    }

    override fun onStylusButton(button: Int, isDown: Boolean) {
        onStylusButtonAction?.invoke(button, isDown)
    }

    // ViewportGestureListener callbacks
    override fun onPan(dx: Float, dy: Float) {
        engineBridge?.pan(dx, dy)
        requestRender()
    }

    override fun onZoom(scaleFactor: Float, focusX: Float, focusY: Float) {
        engineBridge?.zoom(scaleFactor, focusX, focusY)
        requestRender()
    }

    override fun onRotate(deltaRadians: Float, focusX: Float, focusY: Float) {
        engineBridge?.rotate(deltaRadians, focusX, focusY)
        requestRender()
    }

    override fun onTwoFingerTap() {
        engineBridge?.undo()
        requestRender()
    }

    override fun onThreeFingerTap() {
        engineBridge?.redo()
        requestRender()
    }

    override fun onFourFingerTap() {
        onFourFingerTapAction?.invoke()
    }

    fun requestRender() {
        frameRequested.set(true)
    }

    private inner class RenderThread : Thread("InkwellRenderThread") {
        @Volatile
        private var isRunning = true

        override fun run() {
            while (isRunning) {
                if (frameRequested.compareAndSet(true, false)) {
                    if (isSurfaceReady.get()) {
                        val startNanos = System.nanoTime()
                        if (thermalController.shouldRenderFrame(startNanos)) {
                            engineBridge?.renderFrame()
                            val durationMicros = (System.nanoTime() - startNanos) / 1000L
                            frameMetricsMonitor.recordFrame(durationMicros)
                        } else {
                            // Frame was paced for thermal throttling, mark for next tick
                            frameRequested.set(true)
                        }
                    }
                }
                try {
                    sleep(2) // High-frequency ~500Hz loop check
                } catch (ignored: InterruptedException) {
                    break
                }
            }
        }

        fun stopRendering() {
            isRunning = false
            interrupt()
            try {
                join(500)
            } catch (ignored: InterruptedException) {
            }
        }
    }
}
