#include "CatmullRomSpline.h"

namespace inkwell {

float CatmullRomSpline::getT(float t, const SplinePoint& p0, const SplinePoint& p1) {
    float dx = p1.x - p0.x;
    float dy = p1.y - p0.y;
    float dist = std::hypot(dx, dy);
    // Centripetal: exponent = 0.5 (square root of distance)
    return t + std::sqrt(std::max(0.0001f, dist));
}

SplinePoint CatmullRomSpline::evaluate(
    const SplinePoint& p0,
    const SplinePoint& p1,
    const SplinePoint& p2,
    const SplinePoint& p3,
    float tNormalized
) {
    float t0 = 0.0f;
    float t1 = getT(t0, p0, p1);
    float t2 = getT(t1, p1, p2);
    float t3 = getT(t2, p2, p3);

    float t = t1 + tNormalized * (t2 - t1);

    // Barry and Goldman's pyramidal formulation for centripetal splines
    auto interpolate = [](const SplinePoint& a, const SplinePoint& b, float ta, float tb, float tTarget) {
        float factor = (tTarget - ta) / (tb - ta);
        return SplinePoint{
            .x = a.x + factor * (b.x - a.x),
            .y = a.y + factor * (b.y - a.y),
            .pressure = a.pressure + factor * (b.pressure - a.pressure)
        };
    };

    SplinePoint a1 = interpolate(p0, p1, t0, t1, t);
    SplinePoint a2 = interpolate(p1, p2, t1, t2, t);
    SplinePoint a3 = interpolate(p2, p3, t2, t3, t);

    SplinePoint b1 = interpolate(a1, a2, t0, t2, t);
    SplinePoint b2 = interpolate(a2, a3, t1, t3, t);

    return interpolate(b1, b2, t1, t2, t);
}

} // namespace inkwell
