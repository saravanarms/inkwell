#include "DabRasterizer.h"
#include <cmath>
#include <algorithm>

namespace inkwell {

DabRasterizer::DabRasterizer() = default;

void DabRasterizer::setBrushSize(float diameterPx) {
    mBrushDiameter = std::clamp(diameterPx, 1.0f, 500.0f);
}

void DabRasterizer::setBrushSpacing(float spacingFraction) {
    mSpacingFraction = std::clamp(spacingFraction, 0.01f, 1.0f);
}

void DabRasterizer::setBrushColor(uint8_t r, uint8_t g, uint8_t b, uint8_t a) {
    mColorR = r;
    mColorG = g;
    mColorB = b;
    mColorA = a;
}

void DabRasterizer::setBrushOpacity(float opacity) {
    mBrushOpacity = std::clamp(opacity, 0.0f, 1.0f);
}

void DabRasterizer::setBrushHardness(float hardness) {
    mBrushHardness = std::clamp(hardness, 0.0f, 1.0f);
}

void DabRasterizer::setToolMode(ToolMode mode) {
    mToolMode = mode;
}

void DabRasterizer::setStreamline(float factor) {
    mStreamline.setStreamlineFactor(factor);
}

void DabRasterizer::beginStroke(float x, float y, float pressure, TileGrid* grid) {
    mIsStroking = true;
    mStreamline.reset(x, y, pressure);
    mPreStrokeTiles.clear();

    mLastX = x;
    mLastY = y;
    float maxR = mBrushDiameter * 0.5f;
    mLastRadius = std::max(0.5f, maxR * (0.05f + 0.95f * pressure));
    mDistanceAccumulator = 0.0f;

    mSplineBuffer.clear();
    SplinePoint pt{ .x = x, .y = y, .pressure = pressure };
    mSplineBuffer.push_back(pt);
    mSplineBuffer.push_back(pt);

    mReservoirPrimed = false;
    if (mToolMode == ToolMode::SMUDGE && grid) {
        sampleCanvasColor(x, y, mLastRadius, grid,
                          mReservoirR, mReservoirG, mReservoirB, mReservoirA);
        mReservoirPrimed = true;
    }

    Dab initialDab{
        .x = x,
        .y = y,
        .radius = mLastRadius,
        .opacity = mBrushOpacity,
        .hardness = mBrushHardness,
        .r = mColorR,
        .g = mColorG,
        .b = mColorB,
        .a = mColorA,
        .mode = mToolMode
    };
    stampDab(initialDab, grid);
}

void DabRasterizer::appendPoints(const float* pointData, int pointCount, TileGrid* grid) {
    if (!mIsStroking || !pointData || pointCount <= 0 || !grid) return;

    for (int i = 0; i < pointCount; ++i) {
        int idx = i * 5;
        float rawX = pointData[idx];
        float rawY = pointData[idx + 1];
        float rawP = pointData[idx + 2];

        SplinePoint smoothed = mStreamline.filter(rawX, rawY, rawP);
        mSplineBuffer.push_back(smoothed);

        while (mSplineBuffer.size() >= 4) {
            processSegment(mSplineBuffer[0], mSplineBuffer[1],
                           mSplineBuffer[2], mSplineBuffer[3], grid);
            mSplineBuffer.erase(mSplineBuffer.begin());
        }
    }
}

void DabRasterizer::processSegment(
    const SplinePoint& p0,
    const SplinePoint& p1,
    const SplinePoint& p2,
    const SplinePoint& p3,
    TileGrid* grid
) {
    float segDist = std::hypot(p2.x - p1.x, p2.y - p1.y);
    if (segDist < 0.001f) return;

    float maxR = mBrushDiameter * 0.5f;
    float startR = std::max(0.5f, maxR * (0.05f + 0.95f * p1.pressure));
    float endR = std::max(0.5f, maxR * (0.05f + 0.95f * p2.pressure));

    float avgRadius = (startR + endR) * 0.5f;
    float targetSpacing = std::max(1.0f, mSpacingFraction * avgRadius * 2.0f);

    while (mDistanceAccumulator + targetSpacing <= segDist) {
        mDistanceAccumulator += targetSpacing;
        float t = mDistanceAccumulator / segDist;

        SplinePoint curvePt = CatmullRomSpline::evaluate(p0, p1, p2, p3, t);
        float dabRadius = std::max(0.5f, maxR * (0.05f + 0.95f * curvePt.pressure));

        Dab dab{
            .x = curvePt.x,
            .y = curvePt.y,
            .radius = dabRadius,
            .opacity = mBrushOpacity,
            .hardness = mBrushHardness,
            .r = mColorR,
            .g = mColorG,
            .b = mColorB,
            .a = mColorA,
            .mode = mToolMode
        };
        stampDab(dab, grid);
    }

    mDistanceAccumulator -= segDist;
    mLastX = p2.x;
    mLastY = p2.y;
    mLastRadius = endR;
}

StrokeRecord DabRasterizer::endStroke(int32_t activeLayerId, TileGrid* grid) {
    mIsStroking = false;
    mDistanceAccumulator = 0.0f;
    mSplineBuffer.clear();
    mReservoirPrimed = false;

    StrokeRecord record;
    record.layerId = activeLayerId;

    if (!grid) {
        mPreStrokeTiles.clear();
        return record;
    }

    // Compress pre- and post-stroke tile pixels for undo/redo
    for (const auto& pair : mPreStrokeTiles) {
        uint64_t key = pair.first;
        int32_t c = static_cast<int32_t>(key >> 32);
        int32_t r = static_cast<int32_t>(key & 0xFFFFFFFF);

        const TileData* tile = grid->getTile(c, r);
        if (tile) {
            TileDelta delta;
            delta.tileKey = key;
            TileDeltaCompressor::compress(pair.second.data(), pair.second.size(), delta.preData);
            TileDeltaCompressor::compress(tile->pixels.data(), tile->pixels.size(), delta.postData);
            record.deltas.push_back(std::move(delta));
        }
    }

    mPreStrokeTiles.clear();
    return record;
}

void DabRasterizer::sampleCanvasColor(
    float x, float y, float radius, TileGrid* grid,
    float& outR, float& outG, float& outB, float& outA
) {
    int32_t col = static_cast<int32_t>(x / TILE_SIZE);
    int32_t row = static_cast<int32_t>(y / TILE_SIZE);

    const TileData* tile = grid->getTile(col, row);
    if (!tile) {
        outR = 255.0f; outG = 255.0f; outB = 255.0f; outA = 0.0f;
        return;
    }

    int32_t px = std::clamp(static_cast<int32_t>(x) - (col * TILE_SIZE), 0, TILE_SIZE - 1);
    int32_t py = std::clamp(static_cast<int32_t>(y) - (row * TILE_SIZE), 0, TILE_SIZE - 1);
    int32_t pIdx = (py * TILE_SIZE + px) * TILE_CHANNELS;

    outR = static_cast<float>(tile->pixels[pIdx]);
    outG = static_cast<float>(tile->pixels[pIdx + 1]);
    outB = static_cast<float>(tile->pixels[pIdx + 2]);
    outA = static_cast<float>(tile->pixels[pIdx + 3]);
}

void DabRasterizer::stampDab(const Dab& dab, TileGrid* grid) {
    if (!grid) return;

    float r = dab.radius;
    float rSq = r * r;
    if (r <= 0.0f) return;

    if (dab.mode == ToolMode::SMUDGE) {
        float sampleR = 0.0f, sampleG = 0.0f, sampleB = 0.0f, sampleA = 0.0f;
        sampleCanvasColor(dab.x, dab.y, r, grid, sampleR, sampleG, sampleB, sampleA);

        float pickup = 0.25f;
        mReservoirR = mReservoirR * (1.0f - pickup) + sampleR * pickup;
        mReservoirG = mReservoirG * (1.0f - pickup) + sampleG * pickup;
        mReservoirB = mReservoirB * (1.0f - pickup) + sampleB * pickup;
        mReservoirA = mReservoirA * (1.0f - pickup) + sampleA * pickup;
    }

    int32_t startCol = std::max(0, static_cast<int32_t>((dab.x - r) / TILE_SIZE));
    int32_t endCol = std::min(grid->getColumns() - 1, static_cast<int32_t>((dab.x + r) / TILE_SIZE));
    int32_t startRow = std::max(0, static_cast<int32_t>((dab.y - r) / TILE_SIZE));
    int32_t endRow = std::min(grid->getRows() - 1, static_cast<int32_t>((dab.y + r) / TILE_SIZE));

    if (startCol > endCol || startRow > endRow) return;

    float invR = 1.0f / r;
    float hardnessExp = 1.0f + (1.0f - dab.hardness) * 3.0f;
    float dabAlphaNorm = (static_cast<float>(dab.a) / 255.0f) * dab.opacity;

    for (int32_t c = startCol; c <= endCol; ++c) {
        for (int32_t row = startRow; row <= endRow; ++row) {
            uint64_t tileKey = TileGrid::packKey(c, row);

            // Capture pre-stroke state of tile before first modification
            if (mPreStrokeTiles.find(tileKey) == mPreStrokeTiles.end()) {
                const TileData* existingTile = grid->getTile(c, row);
                if (existingTile) {
                    mPreStrokeTiles[tileKey] = existingTile->pixels;
                } else {
                    mPreStrokeTiles[tileKey] = std::vector<uint8_t>(TILE_BYTE_SIZE, 0);
                }
            }

            TileData* tile = grid->getOrCreateTile(c, row);
            if (!tile) continue;

            int32_t tileOriginX = c * TILE_SIZE;
            int32_t tileOriginY = row * TILE_SIZE;

            int32_t minPx = std::max(0, static_cast<int32_t>(dab.x - r) - tileOriginX);
            int32_t maxPx = std::min(TILE_SIZE - 1, static_cast<int32_t>(dab.x + r) - tileOriginX);
            int32_t minPy = std::max(0, static_cast<int32_t>(dab.y - r) - tileOriginY);
            int32_t maxPy = std::min(TILE_SIZE - 1, static_cast<int32_t>(dab.y + r) - tileOriginY);

            uint8_t* rawPixels = tile->pixels.data();
            bool modified = false;

            for (int32_t py = minPy; py <= maxPy; ++py) {
                float pixelCenterY = static_cast<float>(tileOriginY + py) + 0.5f;
                float diffY = pixelCenterY - dab.y;
                float diffYSq = diffY * diffY;
                int32_t rowOffset = py * TILE_SIZE * TILE_CHANNELS;

                for (int32_t px = minPx; px <= maxPx; ++px) {
                    float pixelCenterX = static_cast<float>(tileOriginX + px) + 0.5f;
                    float diffX = pixelCenterX - dab.x;
                    float distSq = diffX * diffX + diffYSq;

                    if (distSq <= rSq) {
                        float dist = std::sqrt(distSq);
                        float edgeFalloff = std::clamp(1.0f - (dist * invR), 0.0f, 1.0f);
                        float alphaCurve = std::pow(edgeFalloff, hardnessExp);
                        float stampAlpha = alphaCurve * dabAlphaNorm;

                        if (stampAlpha > 0.001f) {
                            int32_t pIdx = rowOffset + (px * TILE_CHANNELS);

                            if (dab.mode == ToolMode::ERASER) {
                                float sub = 1.0f - stampAlpha;
                                rawPixels[pIdx] = static_cast<uint8_t>(rawPixels[pIdx] * sub);
                                rawPixels[pIdx + 1] = static_cast<uint8_t>(rawPixels[pIdx + 1] * sub);
                                rawPixels[pIdx + 2] = static_cast<uint8_t>(rawPixels[pIdx + 2] * sub);
                                rawPixels[pIdx + 3] = static_cast<uint8_t>(rawPixels[pIdx + 3] * sub);
                                modified = true;
                            } else if (dab.mode == ToolMode::SMUDGE) {
                                float dstR = static_cast<float>(rawPixels[pIdx]);
                                float dstG = static_cast<float>(rawPixels[pIdx + 1]);
                                float dstB = static_cast<float>(rawPixels[pIdx + 2]);
                                float dstA = static_cast<float>(rawPixels[pIdx + 3]);

                                float blendRate = stampAlpha * 0.50f;
                                rawPixels[pIdx] = static_cast<uint8_t>(std::clamp(dstR * (1.0f - blendRate) + mReservoirR * blendRate, 0.0f, 255.0f));
                                rawPixels[pIdx + 1] = static_cast<uint8_t>(std::clamp(dstG * (1.0f - blendRate) + mReservoirG * blendRate, 0.0f, 255.0f));
                                rawPixels[pIdx + 2] = static_cast<uint8_t>(std::clamp(dstB * (1.0f - blendRate) + mReservoirB * blendRate, 0.0f, 255.0f));
                                rawPixels[pIdx + 3] = static_cast<uint8_t>(std::clamp(dstA * (1.0f - blendRate) + mReservoirA * blendRate, 0.0f, 255.0f));
                                modified = true;
                            } else {
                                float dstR = static_cast<float>(rawPixels[pIdx]);
                                float dstG = static_cast<float>(rawPixels[pIdx + 1]);
                                float dstB = static_cast<float>(rawPixels[pIdx + 2]);
                                float dstAlpha = static_cast<float>(rawPixels[pIdx + 3]) / 255.0f;

                                float outAlpha = stampAlpha + dstAlpha * (1.0f - stampAlpha);
                                if (outAlpha > 0.0f) {
                                    float invOutAlpha = 1.0f / outAlpha;
                                    float outR = (static_cast<float>(dab.r) * stampAlpha + dstR * dstAlpha * (1.0f - stampAlpha)) * invOutAlpha;
                                    float outG = (static_cast<float>(dab.g) * stampAlpha + dstG * dstAlpha * (1.0f - stampAlpha)) * invOutAlpha;
                                    float outB = (static_cast<float>(dab.b) * stampAlpha + dstB * dstAlpha * (1.0f - stampAlpha)) * invOutAlpha;

                                    rawPixels[pIdx] = static_cast<uint8_t>(std::clamp(outR, 0.0f, 255.0f));
                                    rawPixels[pIdx + 1] = static_cast<uint8_t>(std::clamp(outG, 0.0f, 255.0f));
                                    rawPixels[pIdx + 2] = static_cast<uint8_t>(std::clamp(outB, 0.0f, 255.0f));
                                    rawPixels[pIdx + 3] = static_cast<uint8_t>(std::clamp(outAlpha * 255.0f, 0.0f, 255.0f));
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }

            if (modified) {
                tile->isDirty = true;
            }
        }
    }
}

} // namespace inkwell
