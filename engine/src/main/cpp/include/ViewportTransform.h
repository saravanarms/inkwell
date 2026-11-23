#pragma once

#include <cstdint>
#include "Mat3.h"

namespace inkwell {

constexpr float MIN_ZOOM = 0.10f;
constexpr float MAX_ZOOM = 32.0f;
constexpr float ROTATION_SNAP_THRESHOLD_RAD = 3.0f * (3.14159265358979323846f / 180.0f); // 3.0 degrees in radians

class ViewportTransform {
public:
    ViewportTransform(int32_t canvasWidth, int32_t canvasHeight);

    void setViewportSize(int32_t viewportWidth, int32_t viewportHeight);
    void resetToCenter();

    void pan(float dx, float dy);
    void zoom(float scaleFactor, float focusX, float focusY);
    void rotate(float deltaRadians, float focusX, float focusY);

    bool screenToCanvas(float screenX, float screenY, float& outCanvasX, float& outCanvasY) const;
    void canvasToScreen(float canvasX, float canvasY, float& outScreenX, float& outScreenY) const;

    float getZoom() const { return mZoom; }
    float getRotation() const { return mRotation; }
    float getTranslationX() const { return mTranslationX; }
    float getTranslationY() const { return mTranslationY; }

    int32_t getCanvasWidth() const { return mCanvasWidth; }
    int32_t getCanvasHeight() const { return mCanvasHeight; }
    int32_t getViewportWidth() const { return mViewportWidth; }
    int32_t getViewportHeight() const { return mViewportHeight; }

    const Mat3& getCanvasToScreenMatrix() const { return mCanvasToScreen; }
    const Mat3& getScreenToCanvasMatrix() const { return mScreenToCanvas; }

    /**
     * Computes the OpenGL ES projection matrix mapping canvas coordinates to NDC [-1, 1].
     */
    void getGLProjectionMatrix(std::array<float, 16>& outGlMatrix) const;

private:
    void updateMatrices();
    float applyRotationSnap(float rawRadians);

    int32_t mCanvasWidth;
    int32_t mCanvasHeight;
    int32_t mViewportWidth = 1080;
    int32_t mViewportHeight = 1920;

    float mTranslationX = 0.0f;
    float mTranslationY = 0.0f;
    float mZoom = 1.0f;
    float mRotation = 0.0f; // in radians

    Mat3 mCanvasToScreen;
    Mat3 mScreenToCanvas;
};

} // namespace inkwell
