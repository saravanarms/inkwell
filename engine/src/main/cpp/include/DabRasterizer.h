#pragma once

#include <cstdint>
#include <vector>
#include <unordered_map>
#include "Dab.h"
#include "TileGrid.h"
#include "CatmullRomSpline.h"
#include "StreamlineFilter.h"
#include "TileDelta.h"

namespace inkwell {

class DabRasterizer {
public:
    DabRasterizer();

    void setBrushSize(float diameterPx);
    void setBrushSpacing(float spacingFraction);
    void setBrushColor(uint8_t r, uint8_t g, uint8_t b, uint8_t a);
    void setBrushOpacity(float opacity);
    void setBrushHardness(float hardness);
    void setToolMode(ToolMode mode);
    void setStreamline(float factor);

    void beginStroke(float x, float y, float pressure, TileGrid* grid);
    void appendPoints(const float* pointData, int pointCount, TileGrid* grid);
    StrokeRecord endStroke(int32_t activeLayerId, TileGrid* grid);

    void stampDab(const Dab& dab, TileGrid* grid);

    float getBrushSize() const { return mBrushDiameter; }
    float getBrushSpacing() const { return mSpacingFraction; }
    ToolMode getToolMode() const { return mToolMode; }

private:
    void processSegment(const SplinePoint& p0, const SplinePoint& p1,
                        const SplinePoint& p2, const SplinePoint& p3,
                        TileGrid* grid);

    void sampleCanvasColor(float x, float y, float radius, TileGrid* grid,
                           float& outR, float& outG, float& outB, float& outA);

    float mBrushDiameter = 30.0f;
    float mSpacingFraction = 0.08f;
    float mBrushOpacity = 1.0f;
    float mBrushHardness = 0.8f;
    ToolMode mToolMode = ToolMode::PAINT;

    uint8_t mColorR = 30;
    uint8_t mColorG = 30;
    uint8_t mColorB = 30;
    uint8_t mColorA = 255;

    // Smudge pigment reservoir
    float mReservoirR = 0.0f;
    float mReservoirG = 0.0f;
    float mReservoirB = 0.0f;
    float mReservoirA = 0.0f;
    bool mReservoirPrimed = false;

    bool mIsStroking = false;
    float mLastX = 0.0f;
    float mLastY = 0.0f;
    float mLastRadius = 15.0f;
    float mDistanceAccumulator = 0.0f;

    StreamlineFilter mStreamline;
    std::vector<SplinePoint> mSplineBuffer;

    // Tile-diff delta tracking for undo/redo
    std::unordered_map<uint64_t, std::vector<uint8_t>> mPreStrokeTiles;
};

} // namespace inkwell
