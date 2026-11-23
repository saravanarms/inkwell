#pragma once

#include <cstdint>
#include <vector>
#include <unordered_map>
#include <memory>
#include "LayerNode.h"
#include "SelectionMask.h"
#include "TileDelta.h"
#include "Mat3.h"

namespace inkwell {

struct SnapshotTile {
    int32_t col;
    int32_t row;
    std::vector<uint8_t> pixels;
};

class TransformSession {
public:
    TransformSession(int32_t canvasWidth, int32_t canvasHeight);
    ~TransformSession() = default;

    bool beginTransform(LayerNode* layer, const SelectionMask* mask = nullptr);
    void updateTransform(float dx, float dy, float scaleX, float scaleY, float rotationRad, float pivotX, float pivotY);
    bool commitTransform(LayerNode* layer, StrokeRecord* outRecord);
    void cancelTransform(LayerNode* layer);

    bool isActive() const { return mIsActive; }

    float getDx() const { return mDx; }
    float getDy() const { return mDy; }
    float getScaleX() const { return mScaleX; }
    float getScaleY() const { return mScaleY; }
    float getRotation() const { return mRotation; }
    float getPivotX() const { return mPivotX; }
    float getPivotY() const { return mPivotY; }

    bool getOriginalBounds(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const;

private:
    uint8_t sampleBilinear(const SnapshotTile* tile, float localX, float localY, int channel) const;
    void sampleSourceRgba(float srcX, float srcY, float& outR, float& outG, float& outB, float& outA) const;

    int32_t mCanvasWidth;
    int32_t mCanvasHeight;
    bool mIsActive = false;

    // Transform parameters
    float mDx = 0.0f;
    float mDy = 0.0f;
    float mScaleX = 1.0f;
    float mScaleY = 1.0f;
    float mRotation = 0.0f;
    float mPivotX = 0.0f;
    float mPivotY = 0.0f;

    int32_t mBoundsX0 = 0;
    int32_t mBoundsY0 = 0;
    int32_t mBoundsX1 = 0;
    int32_t mBoundsY1 = 0;

    // Snapshot of source tiles before transformation began
    std::unordered_map<uint64_t, SnapshotTile> mSourceSnapshot;
    std::unordered_map<uint64_t, std::vector<uint8_t>> mPreTransformDeltas;
};

} // namespace inkwell
