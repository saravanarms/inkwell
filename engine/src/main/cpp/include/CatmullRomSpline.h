#pragma once

#include <cmath>

namespace inkwell {

struct SplinePoint {
    float x = 0.0f;
    float y = 0.0f;
    float pressure = 0.5f;
};

/**
 * Centripetal Catmull-Rom spline evaluator (alpha = 0.5)
 * eliminating cusps and self-intersecting loops through sharp curves.
 */
class CatmullRomSpline {
public:
    static SplinePoint evaluate(
        const SplinePoint& p0,
        const SplinePoint& p1,
        const SplinePoint& p2,
        const SplinePoint& p3,
        float t // normalized [0.0, 1.0] between p1 and p2
    );

private:
    static float getT(float t, const SplinePoint& p0, const SplinePoint& p1);
};

} // namespace inkwell
