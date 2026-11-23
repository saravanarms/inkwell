package com.inkwell.feature.canvas.view

/**
 * Controller protecting mobile hardware from thermal throttling and battery drain.
 * Adjusts target GPU presentation frame rate gracefully from 120Hz to 60Hz without
 * dropping stylus input polling (which remains at 240Hz).
 */
class ThermalThrottleController(
    val supports120Hz: Boolean = true
) {
    /**
     * Thermal status mirror matching Android PowerManager.THERMAL_STATUS_* constants.
     */
    enum class ThermalState(val level: Int, val isThrottled: Boolean) {
        NONE(0, false),
        LIGHT(1, false),
        MODERATE(2, true),
        SEVERE(3, true),
        CRITICAL(4, true),
        EMERGENCY(5, true),
        SHUTDOWN(6, true);

        companion object {
            fun fromInt(value: Int): ThermalState = entries.firstOrNull { it.level == value } ?: NONE
        }
    }

    var currentThermalState: ThermalState = ThermalState.NONE
        private set

    /**
     * Target GPU frame rate (fps).
     */
    val targetFps: Int
        get() = when {
            currentThermalState.isThrottled -> 60
            supports120Hz -> 120
            else -> 60
        }

    /**
     * Target frame interval budget in milliseconds (8.33ms for 120Hz, 16.66ms for 60Hz).
     */
    val targetFrameIntervalMs: Float
        get() = 1000f / targetFps

    /**
     * Minimum millisecond threshold between consecutive render passes.
     */
    val minRenderIntervalNanos: Long
        get() = (1_000_000_000L / targetFps)

    private var lastRenderTimeNanos: Long = 0L

    /**
     * Updates thermal status when notified by PowerManager.OnThermalStatusChangedListener.
     */
    fun onThermalStatusChanged(statusCode: Int) {
        currentThermalState = ThermalState.fromInt(statusCode)
    }

    /**
     * Evaluates if sufficient time has elapsed to present the next frame,
     * maintaining smooth pacing without GPU buffer congestion.
     */
    fun shouldRenderFrame(nowNanos: Long): Boolean {
        val elapsed = nowNanos - lastRenderTimeNanos
        if (elapsed >= minRenderIntervalNanos) {
            lastRenderTimeNanos = nowNanos
            return true
        }
        return false
    }

    fun reset() {
        currentThermalState = ThermalState.NONE
        lastRenderTimeNanos = 0L
    }
}
