#include "SelectionMask.h"
#include <cmath>
#include <algorithm>

namespace inkwell {

SelectionMask::SelectionMask(int32_t canvasWidth, int32_t canvasHeight)
    : mCanvasWidth(canvasWidth),
      mCanvasHeight(canvasHeight),
      mCols((canvasWidth + TILE_SIZE - 1) / TILE_SIZE),
      mRows((canvasHeight + TILE_SIZE - 1) / TILE_SIZE) {}

void SelectionMask::clear() {
    mTiles.clear();
    mHasSelection = false;
}

MaskTile* SelectionMask::getOrCreateTile(int32_t col, int32_t row) {
    if (col < 0 || col >= mCols || row < 0 || row >= mRows) return nullptr;
    uint64_t key = tileKey(col, row);
    auto it = mTiles.find(key);
    if (it != mTiles.end()) {
        return &it->second;
    }
    auto [insertedIt, _] = mTiles.emplace(key, MaskTile(col, row));
    return &insertedIt->second;
}

void SelectionMask::setRect(int32_t x0, int32_t y0, int32_t x1, int32_t y1) {
    clear();

    int32_t minX = std::clamp(std::min(x0, x1), 0, mCanvasWidth);
    int32_t maxX = std::clamp(std::max(x0, x1), 0, mCanvasWidth);
    int32_t minY = std::clamp(std::min(y0, y1), 0, mCanvasHeight);
    int32_t maxY = std::clamp(std::max(y0, y1), 0, mCanvasHeight);

    if (minX >= maxX || minY >= maxY) return;

    int32_t startCol = minX / TILE_SIZE;
    int32_t endCol = (maxX - 1) / TILE_SIZE;
    int32_t startRow = minY / TILE_SIZE;
    int32_t endRow = (maxY - 1) / TILE_SIZE;

    for (int32_t r = startRow; r <= endRow; ++r) {
        for (int32_t c = startCol; c <= endCol; ++c) {
            MaskTile* tile = getOrCreateTile(c, r);
            if (!tile) continue;

            int32_t tileX = c * TILE_SIZE;
            int32_t tileY = r * TILE_SIZE;

            int32_t overlapX0 = std::max(minX, tileX) - tileX;
            int32_t overlapX1 = std::min(maxX, tileX + TILE_SIZE) - tileX;
            int32_t overlapY0 = std::max(minY, tileY) - tileY;
            int32_t overlapY1 = std::min(maxY, tileY + TILE_SIZE) - tileY;

            for (int32_t y = overlapY0; y < overlapY1; ++y) {
                size_t rowOffset = static_cast<size_t>(y) * TILE_SIZE;
                for (int32_t x = overlapX0; x < overlapX1; ++x) {
                    tile->pixels[rowOffset + x] = 255;
                }
            }
        }
    }

    mHasSelection = true;
}

void SelectionMask::setLasso(const float* points, int32_t pointCount) {
    clear();
    if (!points || pointCount < 3) return;

    // 1. Calculate polygon bounding box
    float minXF = points[0], maxXF = points[0];
    float minYF = points[1], maxYF = points[1];

    for (int32_t i = 1; i < pointCount; ++i) {
        float x = points[i * 2];
        float y = points[i * 2 + 1];
        minXF = std::min(minXF, x);
        maxXF = std::max(maxXF, x);
        minYF = std::min(minYF, y);
        maxYF = std::max(maxYF, y);
    }

    int32_t minX = std::clamp(static_cast<int32_t>(std::floor(minXF)), 0, mCanvasWidth);
    int32_t maxX = std::clamp(static_cast<int32_t>(std::ceil(maxXF)), 0, mCanvasWidth);
    int32_t minY = std::clamp(static_cast<int32_t>(std::floor(minYF)), 0, mCanvasHeight);
    int32_t maxY = std::clamp(static_cast<int32_t>(std::ceil(maxYF)), 0, mCanvasHeight);

    if (minX >= maxX || minY >= maxY) return;

    // 2. Scanline rasterization (even-odd rule)
    std::vector<float> nodeX;
    nodeX.reserve(16);

    for (int32_t y = minY; y < maxY; ++y) {
        nodeX.clear();
        float curY = y + 0.5f;

        // Find intersections with all polygon edges
        int32_t j = pointCount - 1;
        for (int32_t i = 0; i < pointCount; ++i) {
            float yi = points[i * 2 + 1];
            float yj = points[j * 2 + 1];
            float xi = points[i * 2];
            float xj = points[j * 2];

            if ((yi < curY && yj >= curY) || (yj < curY && yi >= curY)) {
                float intersectX = xi + (curY - yi) / (yj - yi) * (xj - xi);
                nodeX.push_back(intersectX);
            }
            j = i;
        }

        std::sort(nodeX.begin(), nodeX.end());

        // Fill horizontal spans between intersection pairs
        for (size_t s = 0; s + 1 < nodeX.size(); s += 2) {
            int32_t spanX0 = std::clamp(static_cast<int32_t>(std::floor(nodeX[s])), 0, mCanvasWidth);
            int32_t spanX1 = std::clamp(static_cast<int32_t>(std::ceil(nodeX[s + 1])), 0, mCanvasWidth);

            int32_t row = y / TILE_SIZE;
            int32_t localY = y % TILE_SIZE;

            for (int32_t x = spanX0; x < spanX1; ++x) {
                int32_t col = x / TILE_SIZE;
                int32_t localX = x % TILE_SIZE;

                MaskTile* tile = getOrCreateTile(col, row);
                if (tile) {
                    tile->pixels[static_cast<size_t>(localY) * TILE_SIZE + localX] = 255;
                }
            }
        }
    }

    mHasSelection = true;
}

void SelectionMask::invert() {
    if (!mHasSelection) {
        // If no selection existed, inverting selects entire canvas
        setRect(0, 0, mCanvasWidth, mCanvasHeight);
        return;
    }

    // Allocate all tiles across canvas if not present
    for (int32_t r = 0; r < mRows; ++r) {
        for (int32_t c = 0; c < mCols; ++c) {
            MaskTile* tile = getOrCreateTile(c, r);
            if (tile) {
                for (size_t i = 0; i < TILE_SIZE * TILE_SIZE; ++i) {
                    tile->pixels[i] = 255 - tile->pixels[i];
                }
            }
        }
    }
}

uint8_t SelectionMask::getMaskValue(int32_t x, int32_t y) const {
    if (x < 0 || x >= mCanvasWidth || y < 0 || y >= mCanvasHeight) return 0;
    if (!mHasSelection) return 255; // When no selection is active, everything is editable

    int32_t col = x / TILE_SIZE;
    int32_t row = y / TILE_SIZE;
    uint64_t key = tileKey(col, row);

    auto it = mTiles.find(key);
    if (it != mTiles.end()) {
        int32_t localX = x % TILE_SIZE;
        int32_t localY = y % TILE_SIZE;
        return it->second.pixels[static_cast<size_t>(localY) * TILE_SIZE + localX];
    }
    return 0;
}

bool SelectionMask::getBoundingBox(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const {
    if (!mHasSelection || mTiles.empty()) return false;

    int32_t minX = mCanvasWidth, maxX = -1;
    int32_t minY = mCanvasHeight, maxY = -1;

    for (const auto& [_, tile] : mTiles) {
        int32_t tileStartX = tile.col * TILE_SIZE;
        int32_t tileStartY = tile.row * TILE_SIZE;

        for (int32_t y = 0; y < TILE_SIZE; ++y) {
            size_t rowOffset = static_cast<size_t>(y) * TILE_SIZE;
            for (int32_t x = 0; x < TILE_SIZE; ++x) {
                if (tile.pixels[rowOffset + x] > 0) {
                    int32_t gx = tileStartX + x;
                    int32_t gy = tileStartY + y;
                    minX = std::min(minX, gx);
                    maxX = std::max(maxX, gx);
                    minY = std::min(minY, gy);
                    maxY = std::max(maxY, gy);
                }
            }
        }
    }

    if (maxX < minX || maxY < minY) return false;

    outX0 = minX;
    outY0 = minY;
    outX1 = maxX + 1;
    outY1 = maxY + 1;
    return true;
}

} // namespace inkwell
