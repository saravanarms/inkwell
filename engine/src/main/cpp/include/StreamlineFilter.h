#pragma once

#include "CatmullRomSpline.h"

namespace inkwell {

class StreamlineFilter {
public:
    StreamlineFilter();

    void setStreamlineFactor(float factor);
    float getStreamlineFactor() const { return mStreamlineFactor; }

    void reset(float initialX, float initialY, float initialPressure);
    SplinePoint filter(float rawX, float rawY, float rawPressure);

private:
    float mStreamlineFactor = 0.5f;
    float mAlpha = 0.56f;

    bool mHasHistory = false;
    float mFilteredX = 0.0f;
    float mFilteredY = 0.0f;
    float mFilteredPressure = 0.5f;
};

} // namespace inkwell
