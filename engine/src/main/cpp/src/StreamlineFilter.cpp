#include "StreamlineFilter.h"
#include <algorithm>

namespace inkwell {

StreamlineFilter::StreamlineFilter() {
    setStreamlineFactor(0.5f);
}

void StreamlineFilter::setStreamlineFactor(float factor) {
    mStreamlineFactor = std::clamp(factor, 0.0f, 0.95f);
    // When factor = 0.0, alpha = 1.0 (pass-through). When factor = 0.95, alpha = 0.16.
    mAlpha = 1.0f - (mStreamlineFactor * 0.88f);
}

void StreamlineFilter::reset(float initialX, float initialY, float initialPressure) {
    mFilteredX = initialX;
    mFilteredY = initialY;
    mFilteredPressure = initialPressure;
    mHasHistory = true;
}

SplinePoint StreamlineFilter::filter(float rawX, float rawY, float rawPressure) {
    if (!mHasHistory) {
        reset(rawX, rawY, rawPressure);
        return SplinePoint{ .x = rawX, .y = rawY, .pressure = rawPressure };
    }

    // Low-pass Exponential Moving Average
    mFilteredX = mAlpha * rawX + (1.0f - mAlpha) * mFilteredX;
    mFilteredY = mAlpha * rawY + (1.0f - mAlpha) * mFilteredY;
    mFilteredPressure = mAlpha * rawPressure + (1.0f - mAlpha) * mFilteredPressure;

    return SplinePoint{
        .x = mFilteredX,
        .y = mFilteredY,
        .pressure = mFilteredPressure
    };
}

} // namespace inkwell
