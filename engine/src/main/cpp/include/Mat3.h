#pragma once

#include <cmath>
#include <array>

namespace inkwell {

/**
 * 3x3 Affine Transform Matrix in C++20 for high-performance viewport calculations.
 * Row-major representation:
 * [ m00, m01, m02 ]
 * [ m10, m11, m12 ]
 * [   0,   0,   1 ]
 */
struct Mat3 {
    float m00 = 1.0f;
    float m01 = 0.0f;
    float m02 = 0.0f;

    float m10 = 0.0f;
    float m11 = 1.0f;
    float m12 = 0.0f;

    Mat3() = default;

    Mat3(float a00, float a01, float a02,
         float a10, float a11, float a12)
        : m00(a00), m01(a01), m02(a02),
          m10(a10), m11(a11), m12(a12) {}

    Mat3 operator*(const Mat3& o) const {
        return Mat3(
            m00 * o.m00 + m01 * o.m10,
            m00 * o.m01 + m01 * o.m11,
            m00 * o.m02 + m01 * o.m12 + m02,

            m10 * o.m00 + m11 * o.m10,
            m10 * o.m01 + m11 * o.m11,
            m10 * o.m02 + m11 * o.m12 + m12
        );
    }

    void mapPoint(float inX, float inY, float& outX, float& outY) const {
        outX = m00 * inX + m01 * inY + m02;
        outY = m10 * inX + m11 * inY + m12;
    }

    float determinant() const {
        return m00 * m11 - m01 * m10;
    }

    bool invert(Mat3& out) const {
        float det = determinant();
        if (std::abs(det) < 1e-7f) {
            return false;
        }
        float invDet = 1.0f / det;

        out.m00 = m11 * invDet;
        out.m01 = -m01 * invDet;
        out.m02 = (m01 * m12 - m11 * m02) * invDet;

        out.m10 = -m10 * invDet;
        out.m11 = m00 * invDet;
        out.m12 = (m10 * m02 - m00 * m12) * invDet;

        return true;
    }

    /**
     * Converts to 4x4 column-major float array for OpenGL ES uniform upload.
     */
    void toGLMatrix4x4(std::array<float, 16>& glMat) const {
        glMat = {
            m00,  m10,  0.0f, 0.0f,
            m01,  m11,  0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            m02,  m12,  0.0f, 1.0f
        };
    }

    static Mat3 identity() {
        return Mat3();
    }

    static Mat3 translation(float tx, float ty) {
        return Mat3(1.0f, 0.0f, tx, 0.0f, 1.0f, ty);
    }

    static Mat3 scale(float sx, float sy) {
        return Mat3(sx, 0.0f, 0.0f, 0.0f, sy, 0.0f);
    }

    static Mat3 rotation(float radians) {
        float c = std::cos(radians);
        float s = std::sin(radians);
        return Mat3(c, -s, 0.0f, s, c, 0.0f);
    }
};

} // namespace inkwell
