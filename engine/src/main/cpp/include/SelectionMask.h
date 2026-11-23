#pragma once

#include <cstdint>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include "TileGrid.h"

namespace inkwell {

struct MaskTile {
    int32_t col;
    int32_t row;
    std::vector<uint8_t> pixels; // 256x256 = 65536 bytes (0..255)

    MaskTile(int32_t c, int32_t r)
        : col(c), row(r), pixels(TILE_SIZE * TILE_SIZE, 0) {}
};

class SelectionMask {
public:
    SelectionMask(int32_t canvasWidth, int32_t canvasHeight);
    ~SelectionMask() = default;

    void clear();
    bool hasSelection() const { return mHasSelection && !mTiles.empty(); }

    void setRect(int32_t x0, int32_t y0, int32_t x1, int32_t y1);
    void setLasso(const float* points, int32_t pointCount);
    void invert();

    uint8_t getMaskValue(int32_t x, int32_t y) const;
    bool getBoundingBox(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const;

    const std::unordered_map<uint64_t, MaskTile>& getTiles() const { return mTiles; }
    size_t getActiveTileCount() const { return mTiles.size(); }

private:
    uint64_t tileKey(int32_t col, int32_t row) const {
        return (static_cast<uint64_t>(col) << 32) | (static_cast<uint32_t>(row) & 0xFFFFFFFF);
    }

    MaskTile* getOrCreateTile(int32_t col, int32_t row);

    int32_t mCanvasWidth;
    int32_t mCanvasHeight;
    int32_t mCols;
    int32_t mRows;
    bool mHasSelection = false;

    std::unordered_map<uint64_t, MaskTile> mTiles;
};

} // namespace inkwell
