package com.inkwell.feature.canvas.view

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLongArray

/**
 * High-precision frame metrics monitor tracking rendering latencies,
 * deadline adherence, and jank frames for 60fps and 120fps display modes.
 */
class FrameMetricsMonitor(
    val windowSize: Int = DEFAULT_WINDOW_SIZE,
    var targetBudgetMs: Float = 8.33f // 120 fps default budget
) {
    // Ring buffer storing frame durations in microseconds (1ms = 1,000us)
    private val frameDurationsMicros = AtomicLongArray(windowSize)
    private val totalFramesRecorded = AtomicInteger(0)
    private val jankFrameCount = AtomicInteger(0)

    fun recordFrame(durationMicros: Long) {
        val budgetMicros = (targetBudgetMs * 1000f).toLong()
        if (durationMicros > budgetMicros) {
            jankFrameCount.incrementAndGet()
        }

        val index = (totalFramesRecorded.getAndIncrement()) % windowSize
        frameDurationsMicros.set(index, durationMicros)
    }

    fun getTotalFrames(): Int = totalFramesRecorded.get()
    fun getJankFrames(): Int = jankFrameCount.get()

    /**
     * Calculates the percentage of recorded frames that strictly met the target frame budget.
     */
    fun calculateBudgetAdherencePercentage(): Double {
        val total = totalFramesRecorded.get()
        if (total == 0) return 100.0

        val jank = jankFrameCount.get()
        val onTime = (total - jank).coerceAtLeast(0)
        return (onTime.toDouble() / total.toDouble()) * 100.0
    }

    /**
     * Computes the average frame render time in milliseconds.
     */
    fun getAverageFrameTimeMs(): Double {
        val count = totalFramesRecorded.get().coerceAtMost(windowSize)
        if (count == 0) return 0.0

        var sumMicros = 0L
        for (i in 0 until count) {
            sumMicros += frameDurationsMicros.get(i)
        }
        return (sumMicros.toDouble() / count.toDouble()) / 1000.0
    }

    /**
     * Computes the requested percentile latency in milliseconds (e.g. 99.5 for 99.5th percentile).
     */
    fun getPercentileLatencyMs(percentile: Double): Double {
        val count = totalFramesRecorded.get().coerceAtMost(windowSize)
        if (count == 0) return 0.0

        val samples = LongArray(count)
        for (i in 0 until count) {
            samples[i] = frameDurationsMicros.get(i)
        }
        samples.sort()

        val rank = kotlin.math.ceil((percentile / 100.0) * count).toInt() - 1
        val safeIndex = rank.coerceIn(0, count - 1)
        return samples[safeIndex] / 1000.0
    }

    fun reset() {
        totalFramesRecorded.set(0)
        jankFrameCount.set(0)
        for (i in 0 until windowSize) {
            frameDurationsMicros.set(i, 0L)
        }
    }

    companion object {
        const val DEFAULT_WINDOW_SIZE = 1000
    }
}
