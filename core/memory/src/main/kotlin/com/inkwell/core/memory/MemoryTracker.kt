package com.inkwell.core.memory

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Listener invoked when memory stress requires cache or undo eviction.
 */
fun interface MemoryTrimListener {
    fun onTrimMemory(level: MemoryStressLevel)
}

/**
 * Process-wide memory tracker monitoring tile allocations, undo journals,
 * and memory leaks across project lifecycle transitions.
 */
class MemoryTracker(
    val budgetManager: MemoryBudgetManager
) {
    private val liveTileBytes = AtomicLong(0L)
    private val liveHistoryBytes = AtomicLong(0L)
    private val auxiliaryBytes = AtomicLong(0L)

    private val trimListeners = CopyOnWriteArrayList<MemoryTrimListener>()

    // Project lifecycle tracking for zero-leak verification
    private var baselineBytes: Long = 0L
    private var openProjectsCount = 0

    fun addTrimListener(listener: MemoryTrimListener) {
        trimListeners.add(listener)
    }

    fun removeTrimListener(listener: MemoryTrimListener) {
        trimListeners.remove(listener)
    }

    fun recordTileAllocation(bytes: Long) {
        liveTileBytes.addAndGet(bytes)
        checkMemoryPressure()
    }

    fun recordTileDeallocation(bytes: Long) {
        liveTileBytes.addAndGet(-bytes)
    }

    fun recordHistoryAllocation(bytes: Long) {
        liveHistoryBytes.addAndGet(bytes)
        checkMemoryPressure()
    }

    fun recordHistoryDeallocation(bytes: Long) {
        liveHistoryBytes.addAndGet(-bytes)
    }

    fun recordAuxiliaryAllocation(bytes: Long) {
        auxiliaryBytes.addAndGet(bytes)
        checkMemoryPressure()
    }

    fun recordAuxiliaryDeallocation(bytes: Long) {
        auxiliaryBytes.addAndGet(-bytes)
    }

    fun getTotalAllocatedBytes(): Long {
        return liveTileBytes.get() + liveHistoryBytes.get() + auxiliaryBytes.get()
    }

    fun getLiveTileBytes(): Long = liveTileBytes.get()
    fun getLiveHistoryBytes(): Long = liveHistoryBytes.get()
    fun getAuxiliaryBytes(): Long = auxiliaryBytes.get()

    fun getCurrentStressLevel(): MemoryStressLevel {
        return budgetManager.evaluateStressLevel(getTotalAllocatedBytes())
    }

    fun isWithinSafetyCeiling(): Boolean {
        return budgetManager.isWithinSafetyCeiling(getTotalAllocatedBytes())
    }

    /**
     * Called when opening a project to track session memory.
     */
    fun onProjectOpen() {
        if (openProjectsCount == 0) {
            baselineBytes = getTotalAllocatedBytes()
        }
        openProjectsCount++
    }

    /**
     * Called when closing a project. Resets active tiles and returns whether
     * memory leaked compared to pre-open baseline.
     */
    fun onProjectClose(): Boolean {
        openProjectsCount = (openProjectsCount - 1).coerceAtLeast(0)
        if (openProjectsCount == 0) {
            val current = getTotalAllocatedBytes()
            val diff = kotlin.math.abs(current - baselineBytes)
            // Memory is leak-free if within 1KB alignment margin
            return diff <= 1024L
        }
        return true
    }

    /**
     * Forces an immediate trim pass across all registered listeners.
     */
    fun performTrim(level: MemoryStressLevel) {
        for (listener in trimListeners) {
            listener.onTrimMemory(level)
        }
    }

    private fun checkMemoryPressure() {
        val stress = getCurrentStressLevel()
        if (stress != MemoryStressLevel.NORMAL) {
            performTrim(stress)
        }
    }

    fun reset() {
        liveTileBytes.set(0L)
        liveHistoryBytes.set(0L)
        auxiliaryBytes.set(0L)
        baselineBytes = 0L
        openProjectsCount = 0
    }
}
