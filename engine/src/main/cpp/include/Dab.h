#pragma once

#include <cstdint>

namespace inkwell {

enum class ToolMode : int32_t {
    PAINT = 0,
    SMUDGE = 1,
    ERASER = 2
};

/**
 * Immutable dab stamp descriptor.
 */
struct Dab {
    float x = 0.0f;
    float y = 0.0f;
    float radius = 10.0f;
    float opacity = 1.0f;
    float hardness = 0.8f;
    uint8_t r = 20;
    uint8_t g = 20;
    uint8_t b = 20;
    uint8_t a = 255;
    ToolMode mode = ToolMode::PAINT;
};

} // namespace inkwell
