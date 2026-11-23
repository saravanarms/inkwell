#include "TransformSession.h"
#include <cmath>
#include <algorithm>

namespace inkwell {

TransformSession::TransformSession(int32_t canvasWidth, int32_t canvasHeight)
    : mCanvasWidth(canvasWidth), mCanvasHeight(canvasHeight) {}

bool TransformSession::beginTransform(LayerNode* layer, const SelectionMask* mask) {
    if (!layer || !layer->getTileGrid()) return false;

    TileGrid* grid = layer->getTileGrid();
    mSourceSnapshot.clear();
    mPreTransformDeltas.clear();

    int32_t minX = mCanvasWidth, maxX = -1;
    int32_t minY = mCanvasHeight, maxY = -1;

    int32_t cols = grid->getColumns();
    int32_t rows = grid->getRows();

    // 1. Snapshot all active tiles and calculate bounds
    for (int32_t c = 0; c < cols; ++c) {
        for (int32_t r = 0; r < rows; ++r) {
            const TileData* tile = grid->getTile(c, r);
            if (!tile) continue;

            uint64_t key = (static_cast<uint64_t>(c) << 32) | static_cast<uint32_t>(r);

            SnapshotTile snap{c, r, tile->pixels};
            mSourceSnapshot.emplace(key, std::move(snap));
            mPreTransformDeltas.emplace(key, tile->pixels);

            // Bounding box of non-empty pixels
            int32_t tx = c * TILE_SIZE;
            int32_t ty = r * TILE_SIZE;
            for (int32_t y = 0; y < TILE_SIZE; ++y) {
                size_t offset = (static_cast<size_t>(y) * TILE_SIZE) * TILE_CHANNELS;
                for (int32_t x = 0; x < TILE_SIZE; ++x) {
                    if (tile->pixels[offset + x * TILE_CHANNELS + 3] > 0) {
                        minX = std::min(minX, tx + x);
                        maxX = std::max(maxX, tx + x);
                        minY = std::min(minY, ty + y);
                        maxY = std::max(maxY, ty + y);
                    }
                }
            }
        }
    }

    if (mask && mask->hasSelection()) {
        mask->getBoundingBox(mBoundsX0, mBoundsY0, mBoundsX1, mBoundsY1);
    } else if (maxX >= minX && maxY >= minY) {
        mBoundsX0 = minX;
        mBoundsY0 = minY;
        mBoundsX1 = maxX + 1;
        mBoundsY1 = maxY + 1;
    } else {
        mBoundsX0 = 0;
        mBoundsY0 = 0;
        mBoundsX1 = mCanvasWidth;
        mBoundsY1 = mCanvasHeight;
    }

    mPivotX = (mBoundsX0 + mBoundsX1) * 0.5f;
    mPivotY = (mBoundsY0 + mBoundsY1) * 0.5f;
    mDx = 0.0f;
    mDy = 0.0f;
    mScaleX = 1.0f;
    mScaleY = 1.0f;
    mRotation = 0.0f;
    mIsActive = true;

    return true;
}

void TransformSession::updateTransform(float dx, float dy, float scaleX, float scaleY, float rotationRad, float pivotX, float pivotY) {
    if (!mIsActive) return;
    mDx = dx;
    mDy = dy;
    mScaleX = (scaleX == 0.0f) ? 0.001f : scaleX;
    mScaleY = (scaleY == 0.0f) ? 0.001f : scaleY;
    mRotation = rotationRad;
    mPivotX = pivotX;
    mPivotY = pivotY;
}

void TransformSession::sampleSourceRgba(float srcX, float srcY, float& outR, float& outG, float& outB, float& outA) const {
    outR = outG = outB = outA = 0.0f;
    if (srcX < 0.0f || srcX >= mCanvasWidth - 1 || srcY < 0.0f || srcY >= mCanvasHeight - 1) return;

    int32_t x0 = static_cast<int32_t>(std::floor(srcX));
    int32_t y0 = static_cast<int32_t>(std::floor(srcY));
    int32_t x1 = x0 + 1;
    int32_t y1 = y0 + 1;

    float fx = srcX - x0;
    float fy = srcY - y0;

    auto fetchTexel = [this](int32_t px, int32_t py, float& r, float& g, float& b, float& a) {
        r = g = b = a = 0.0f;
        int32_t c = px / TILE_SIZE;
        int32_t row = py / TILE_SIZE;
        uint64_t key = (static_cast<uint64_t>(c) << 32) | static_cast<uint32_t>(row);
        auto it = mSourceSnapshot.find(key);
        if (it != mSourceSnapshot.end()) {
            int32_t lx = px % TILE_SIZE;
            int32_t ly = py % TILE_SIZE;
            size_t idx = (static_cast<size_t>(ly) * TILE_SIZE + lx) * TILE_CHANNELS;
            r = static_cast<float>(it->second.pixels[idx]);
            g = static_cast<float>(it->second.pixels[idx + 1]);
            b = static_cast<float>(it->second.pixels[idx + 2]);
            a = static_cast<float>(it->second.pixels[idx + 3]);
        }
    };

    float r00, g00, b00, a00;
    float r10, g10, b10, a10;
    float r01, g01, b01, a01;
    float r11, g11, b11, a11;

    fetchTexel(x0, y0, r00, g00, b00, a00);
    fetchTexel(x1, y0, r10, g10, b10, a10);
    fetchTexel(x0, y1, r01, g01, b01, a01);
    fetchTexel(x1, y1, r11, g11, b11, a11);

    // Bilinear interpolation
    float w00 = (1.0f - fx) * (1.0f - fy);
    float w10 = fx * (1.0f - fy);
    float w01 = (1.0f - fx) * fy;
    float w11 = fx * fy;

    outR = r00 * w00 + r10 * w10 + r01 * w01 + r11 * w11;
    outG = g00 * w00 + g10 * w10 + g01 * w01 + g11 * w11;
    outB = b00 * w00 + b10 * w10 + b01 * w01 + b11 * w11;
    outA = a00 * w00 + a10 * w10 + a01 * w01 + a11 * w11;
}

bool TransformSession::commitTransform(LayerNode* layer, StrokeRecord* outRecord) {
    if (!mIsActive || !layer || !layer->getTileGrid()) return false;
    TileGrid* grid = layer->getTileGrid();

    // 1. Clear original source tiles in active layer
    for (const auto& [_, snap] : mSourceSnapshot) {
        TileData* tile = grid->getOrCreateTile(snap.col, snap.row);
        if (tile) {
            std::fill(tile->pixels.begin(), tile->pixels.end(), 0);
            tile->isDirty = true;
        }
    }

    // 2. Compute destination bounding box from transformed corners
    float cornersX[4] = {
        static_cast<float>(mBoundsX0), static_cast<float>(mBoundsX1),
        static_cast<float>(mBoundsX1), static_cast<float>(mBoundsX0)
    };
    float cornersY[4] = {
        static_cast<float>(mBoundsY0), static_cast<float>(mBoundsY0),
        static_cast<float>(mBoundsY1), static_cast<float>(mBoundsY1)
    };

    float minDstX = 1e9f, maxDstX = -1e9f;
    float minDstY = 1e9f, maxDstY = -1e9f;

    float cosA = std::cos(mRotation);
    float sinA = std::sin(mRotation);

    for (int i = 0; i < 4; ++i) {
        float px = (cornersX[i] - mPivotX) * mScaleX;
        float py = (cornersY[i] - mPivotY) * mScaleY;
        float rx = px * cosA - py * sinA + mPivotX + mDx;
        float ry = px * sinA + py * cosA + mPivotY + mDy;

        minDstX = std::min(minDstX, rx);
        maxDstX = std::max(maxDstX, rx);
        minDstY = std::min(minDstY, ry);
        maxDstY = std::max(maxDstY, ry);
    }

    int32_t dstX0 = std::clamp(static_cast<int32_t>(std::floor(minDstX)), 0, mCanvasWidth);
    int32_t dstX1 = std::clamp(static_cast<int32_t>(std::ceil(maxDstX)), 0, mCanvasWidth);
    int32_t dstY0 = std::clamp(static_cast<int32_t>(std::floor(minDstY)), 0, mCanvasHeight);
    int32_t dstY1 = std::clamp(static_cast<int32_t>(std::ceil(maxDstY)), 0, mCanvasHeight);

    // 3. Bilinearly resample each destination pixel from source space
    float invScaleX = 1.0f / mScaleX;
    float invScaleY = 1.0f / mScaleY;
    float cosNegA = std::cos(-mRotation);
    float sinNegA = std::sin(-mRotation);

    for (int32_t dy = dstY0; dy < dstY1; ++dy) {
        int32_t row = dy / TILE_SIZE;
        int32_t localY = dy % TILE_SIZE;

        for (int32_t dx = dstX0; dx < dstX1; ++dx) {
            int32_t col = dx / TILE_SIZE;
            int32_t localX = dx % TILE_SIZE;

            // Map (dx, dy) to inverse source coordinates
            float untranslatedX = dx - (mPivotX + mDx);
            float untranslatedY = dy - (mPivotY + mDy);

            float rotX = untranslatedX * cosNegA - untranslatedY * sinNegA;
            float rotY = untranslatedX * sinNegA + untranslatedY * cosNegA;

            float srcX = rotX * invScaleX + mPivotX;
            float srcY = rotY * invScaleY + mPivotY;

            if (srcX >= mBoundsX0 && srcX < mBoundsX1 && srcY >= mBoundsY0 && srcY < mBoundsY1) {
                float r, g, b, a;
                sampleSourceRgba(srcX, srcY, r, g, b, a);

                if (a > 0.5f) {
                    TileData* tile = grid->getOrCreateTile(col, row);
                    if (tile) {
                        size_t idx = (static_cast<size_t>(localY) * TILE_SIZE + localX) * TILE_CHANNELS;
                        tile->pixels[idx] = static_cast<uint8_t>(std::clamp(r, 0.0f, 255.0f));
                        tile->pixels[idx + 1] = static_cast<uint8_t>(std::clamp(g, 0.0f, 255.0f));
                        tile->pixels[idx + 2] = static_cast<uint8_t>(std::clamp(b, 0.0f, 255.0f));
                        tile->pixels[idx + 3] = static_cast<uint8_t>(std::clamp(a, 0.0f, 255.0f));
                        tile->isDirty = true;
                    }
                }
            }
        }
    }

    // 4. Generate delta records if requested
    if (outRecord) {
        outRecord->layerId = layer->getId();
        std::vector<uint8_t> compressedPre;
        std::vector<uint8_t> compressedPost;

        // Collect all touched tile keys (source snapshot + modified destination tiles)
        std::unordered_map<uint64_t, bool> allKeys;
        for (const auto& [k, _] : mPreTransformDeltas) allKeys[k] = true;
        int32_t startCol = dstX0 / TILE_SIZE, endCol = (dstX1 > 0 ? (dstX1 - 1) / TILE_SIZE : 0);
        int32_t startRow = dstY0 / TILE_SIZE, endRow = (dstY1 > 0 ? (dstY1 - 1) / TILE_SIZE : 0);
        for (int32_t r = startRow; r <= endRow; ++r) {
            for (int32_t c = startCol; c <= endCol; ++c) {
                allKeys[(static_cast<uint64_t>(c) << 32) | static_cast<uint32_t>(r)] = true;
            }
        }

        std::vector<uint8_t> blankTile(TILE_BYTE_SIZE, 0);
        for (const auto& [key, _] : allKeys) {
            int32_t c = static_cast<int32_t>(key >> 32);
            int32_t r = static_cast<int32_t>(key & 0xFFFFFFFF);

            auto preIt = mPreTransformDeltas.find(key);
            const uint8_t* preData = (preIt != mPreTransformDeltas.end()) ? preIt->second.data() : blankTile.data();

            const TileData* postTile = grid->getTile(c, r);
            const uint8_t* postData = postTile ? postTile->pixels.data() : blankTile.data();

            if (std::memcmp(preData, postData, TILE_BYTE_SIZE) != 0) {
                TileDeltaCompressor::compress(preData, TILE_BYTE_SIZE, compressedPre);
                TileDeltaCompressor::compress(postData, TILE_BYTE_SIZE, compressedPost);
                outRecord->deltas.emplace_back(key, compressedPre, compressedPost);
            }
        }
    }

    mSourceSnapshot.clear();
    mPreTransformDeltas.clear();
    mIsActive = false;
    return true;
}

void TransformSession::cancelTransform(LayerNode* layer) {
    if (!mIsActive || !layer || !layer->getTileGrid()) return;
    TileGrid* grid = layer->getTileGrid();

    for (const auto& [key, snap] : mSourceSnapshot) {
        TileData* tile = grid->getOrCreateTile(snap.col, snap.row);
        if (tile) {
            tile->pixels = snap.pixels;
            tile->isDirty = true;
        }
    }

    mSourceSnapshot.clear();
    mPreTransformDeltas.clear();
    mIsActive = false;
}

bool TransformSession::getOriginalBounds(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const {
    if (!mIsActive) return false;
    outX0 = mBoundsX0;
    outY0 = mBoundsY0;
    outX1 = mBoundsX1;
    outY1 = mBoundsY1;
    return true;
}

} // namespace inkwell
