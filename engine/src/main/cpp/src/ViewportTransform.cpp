#include "ViewportTransform.h"
#include <algorithm>
#include <cmath>

namespace inkwell {

constexpr float TWO_PI = 2.0f * 3.14159265358979323846f;

ViewportTransform::ViewportTransform(int32_t canvasWidth, int32_t canvasHeight)
    : mCanvasWidth(canvasWidth), mCanvasHeight(canvasHeight) {
    updateMatrices();
}

void ViewportTransform::setViewportSize(int32_t viewportWidth, int32_t viewportHeight) {
    mViewportWidth = viewportWidth;
    mViewportHeight = viewportHeight;
    resetToCenter();
}

void ViewportTransform::resetToCenter() {
    if (mViewportWidth <= 0 || mViewportHeight <= 0 || mCanvasWidth <= 0 || mCanvasHeight <= 0) {
        return;
    }

    // Fit canvas nicely in viewport with 10% padding
    float scaleX = (static_cast<float>(mViewportWidth) * 0.90f) / static_cast<float>(mCanvasWidth);
    float scaleY = (static_cast<float>(mViewportHeight) * 0.90f) / static_cast<float>(mCanvasHeight);
    mZoom = std::clamp(std::min(scaleX, scaleY), MIN_ZOOM, MAX_ZOOM);

    mRotation = 0.0f;
    mTranslationX = (static_cast<float>(mViewportWidth) - (static_cast<float>(mCanvasWidth) * mZoom)) * 0.5f;
    mTranslationY = (static_cast<float>(mViewportHeight) - (static_cast<float>(mCanvasHeight) * mZoom)) * 0.5f;

    updateMatrices();
}

void ViewportTransform::pan(float dx, float dy) {
    mTranslationX += dx;
    mTranslationY += dy;
    updateMatrices();
}

void ViewportTransform::zoom(float scaleFactor, float focusX, float focusY) {
    if (scaleFactor <= 0.0f) return;

    // 1. Identify canvas coordinate under focus point before zooming
    float canvasFocusX = 0.0f;
    float canvasFocusY = 0.0f;
    if (!screenToCanvas(focusX, focusY, canvasFocusX, canvasFocusY)) {
        return;
    }

    // 2. Clamp zoom scale factor
    float newZoom = std::clamp(mZoom * scaleFactor, MIN_ZOOM, MAX_ZOOM);
    if (std::abs(newZoom - mZoom) < 1e-6f) return;
    mZoom = newZoom;

    // 3. Recompute translation so canvasFocus stays at focusX, focusY
    updateMatrices();
    float newScreenFocusX = 0.0f;
    float newScreenFocusY = 0.0f;
    canvasToScreen(canvasFocusX, canvasFocusY, newScreenFocusX, newScreenFocusY);

    mTranslationX += (focusX - newScreenFocusX);
    mTranslationY += (focusY - newScreenFocusY);
    updateMatrices();
}

void ViewportTransform::rotate(float deltaRadians, float focusX, float focusY) {
    // 1. Identify canvas coordinate under focus point
    float canvasFocusX = 0.0f;
    float canvasFocusY = 0.0f;
    if (!screenToCanvas(focusX, focusY, canvasFocusX, canvasFocusY)) {
        return;
    }

    // 2. Accumulate rotation and apply snap
    float rawRotation = mRotation + deltaRadians;
    while (rawRotation > 3.14159265358979323846f) rawRotation -= TWO_PI;
    while (rawRotation < -3.14159265358979323846f) rawRotation += TWO_PI;
    mRotation = applyRotationSnap(rawRotation);

    // 3. Adjust translation to preserve focus point position
    updateMatrices();
    float newScreenFocusX = 0.0f;
    float newScreenFocusY = 0.0f;
    canvasToScreen(canvasFocusX, canvasFocusY, newScreenFocusX, newScreenFocusY);

    mTranslationX += (focusX - newScreenFocusX);
    mTranslationY += (focusY - newScreenFocusY);
    updateMatrices();
}

float ViewportTransform::applyRotationSnap(float rawRadians) {
    // Snap to 0.0 (horizontal) within threshold
    if (std::abs(rawRadians) <= ROTATION_SNAP_THRESHOLD_RAD) {
        return 0.0f;
    }
    return rawRadians;
}

void ViewportTransform::updateMatrices() {
    // CanvasToScreen = Translate(tx, ty) * Translate(canvasPivot) * Rotate(rot) * Scale(zoom, zoom) * Translate(-canvasPivot)
    float canvasCenterX = static_cast<float>(mCanvasWidth) * 0.5f;
    float canvasCenterY = static_cast<float>(mCanvasHeight) * 0.5f;

    Mat3 toCenter = Mat3::translation(-canvasCenterX, -canvasCenterY);
    Mat3 scale = Mat3::scale(mZoom, mZoom);
    Mat3 rot = Mat3::rotation(mRotation);
    Mat3 fromCenter = Mat3::translation(canvasCenterX, canvasCenterY);
    Mat3 trans = Mat3::translation(mTranslationX, mTranslationY);

    mCanvasToScreen = trans * fromCenter * rot * scale * toCenter;
    mCanvasToScreen.invert(mScreenToCanvas);
}

bool ViewportTransform::screenToCanvas(float screenX, float screenY, float& outCanvasX, float& outCanvasY) const {
    mScreenToCanvas.mapPoint(screenX, screenY, outCanvasX, outCanvasY);
    return true;
}

void ViewportTransform::canvasToScreen(float canvasX, float canvasY, float& outScreenX, float& outScreenY) const {
    mCanvasToScreen.mapPoint(canvasX, canvasY, outScreenX, outScreenY);
}

void ViewportTransform::getGLProjectionMatrix(std::array<float, 16>& outGlMatrix) const {
    // Maps screen pixel coordinates [0..viewportWidth, 0..viewportHeight] to NDC [-1, 1]
    // Screen (0,0) is top-left, OpenGL (-1, 1) is top-left
    float w = static_cast<float>(mViewportWidth);
    float h = static_cast<float>(mViewportHeight);
    if (w <= 0.0f) w = 1.0f;
    if (h <= 0.0f) h = 1.0f;

    // Screen to NDC:
    // ndcX = (2.0f * screenX / w) - 1.0f;
    // ndcY = 1.0f - (2.0f * screenY / h);
    Mat3 screenToNdc(
        2.0f / w,  0.0f,     -1.0f,
        0.0f,     -2.0f / h,  1.0f
    );

    Mat3 canvasToNdc = screenToNdc * mCanvasToScreen;
    canvasToNdc.toGLMatrix4x4(outGlMatrix);
}

} // namespace inkwell
