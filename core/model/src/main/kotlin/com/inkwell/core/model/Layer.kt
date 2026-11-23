package com.inkwell.core.model

import java.util.UUID

/**
 * Domain entity representing a canvas layer.
 */
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val opacity: Float = 1.0f,
    val isVisible: Boolean = true,
    val isAlphaLocked: Boolean = false,
    val isClippingMask: Boolean = false,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val zIndex: Int = 0
) {
    init {
        require(opacity in 0.0f..1.0f) {
            "Opacity $opacity must be in range [0.0, 1.0]"
        }
    }
}
