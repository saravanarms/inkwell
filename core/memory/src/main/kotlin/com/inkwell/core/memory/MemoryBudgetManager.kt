package com.inkwell.core.memory

import com.inkwell.core.model.CanvasSpec

/**
 * RAM tiers for dynamic hardware adaptation.
 */
enum class DeviceRamTier(val displayName: String, val maxDimension: Int, val maxLayers4K: Int) {
    LOW_RAM_4GB("Budget / 4GB RAM", 2048, 8),
    STANDARD_6GB_TO_8GB("Standard / 6GB-8GB RAM", 4096, 20),
    FLAGSHIP_12GB_PLUS("Flagship / 12GB+ RAM", 4096, 64);

    companion object {
        fun fromHeapClass(largeHeapMb: Int): DeviceRamTier = when {
            largeHeapMb <= 256 -> LOW_RAM_4GB
            largeHeapMb <= 512 -> STANDARD_6GB_TO_8GB
            else -> FLAGSHIP_12GB_PLUS
        }
    }
}

/**
 * Memory stress levels for dynamic cache trimming and thermal safety.
 */
enum class MemoryStressLevel {
    NORMAL,
    MODERATE,
    CRITICAL
}

/**
 * Calculates and enforces canvas memory budgets, layer limits,
 * and memory ceiling safety based on device heap profiles.
 */
class MemoryBudgetManager(
    val largeHeapClassMb: Int
) {
    init {
        require(largeHeapClassMb >= MIN_HEAP_MB) {
            "Large heap class $largeHeapClassMb MB is below minimum required $MIN_HEAP_MB MB"
        }
    }

    /**
     * RAM tier resolved from heap profile.
     */
    val ramTier: DeviceRamTier = DeviceRamTier.fromHeapClass(largeHeapClassMb)

    /**
     * Maximum canvas dimension allowed for this hardware tier.
     */
    val maxAllowedDimension: Int = ramTier.maxDimension

    /**
     * 60% of large heap class reserved for uncompressed in-memory canvas tiles.
     */
    val maxCanvasBudgetBytes: Long = (largeHeapClassMb.toLong() * 1024L * 1024L * 60L) / 100L

    /**
     * Strict 75% memory ceiling requirement for v1.0 acceptance criteria.
     */
    val maxProcessSafetyCeilingBytes: Long = (largeHeapClassMb.toLong() * 1024L * 1024L * 75L) / 100L

    /**
     * Calculates the maximum safe layer count for a given canvas specification.
     * Enforces a minimum of 4 layers and a maximum based on hardware tier.
     */
    fun calculateMaxLayers(spec: CanvasSpec): Int {
        val bytesPerLayer = spec.uncompressedLayerBytes
        if (bytesPerLayer <= 0L) return 4

        val calculated = (maxCanvasBudgetBytes / bytesPerLayer).toInt()
        val tierCap = when {
            spec.width >= 4096 || spec.height >= 4096 -> ramTier.maxLayers4K
            else -> 64
        }
        return calculated.coerceIn(4, tierCap)
    }

    /**
     * Verifies if adding another layer exceeds the safe memory budget.
     */
    fun canAddLayer(spec: CanvasSpec, currentLayerCount: Int): Boolean {
        if (currentLayerCount >= calculateMaxLayers(spec)) return false
        val potentialTotalBytes = (currentLayerCount + 1) * spec.uncompressedLayerBytes
        return potentialTotalBytes <= maxCanvasBudgetBytes
    }

    /**
     * Evaluates the current memory stress level based on active usage.
     */
    fun evaluateStressLevel(activeBytes: Long): MemoryStressLevel {
        val ratio = activeBytes.toDouble() / maxProcessSafetyCeilingBytes.toDouble()
        return when {
            ratio >= 0.90 -> MemoryStressLevel.CRITICAL
            ratio >= 0.70 -> MemoryStressLevel.MODERATE
            else -> MemoryStressLevel.NORMAL
        }
    }

    /**
     * Verifies if total footprint stays strictly within the 75% large heap ceiling.
     */
    fun isWithinSafetyCeiling(totalAllocatedBytes: Long): Boolean {
        return totalAllocatedBytes <= maxProcessSafetyCeilingBytes
    }

    companion object {
        const val MIN_HEAP_MB = 128
        const val BUDGET_PHONE_HEAP_MB = 256
        const val STANDARD_TABLET_HEAP_MB = 512
        const val FLAGSHIP_TABLET_HEAP_MB = 1024
    }
}
