#pragma once

#include <cstdint>
#include <vector>
#include <unordered_map>
#include <memory>

namespace inkwell {

constexpr int TILE_SIZE = 256;
constexpr int TILE_CHANNELS = 4; // RGBA8888
constexpr size_t TILE_BYTE_SIZE = TILE_SIZE * TILE_SIZE * TILE_CHANNELS; // 262,144 bytes

struct TileData {
    int32_t col = 0;
    int32_t row = 0;
    bool isDirty = false;
    std::vector<uint8_t> pixels;

    TileData(int32_t c, int32_t r)
        : col(c), row(r), isDirty(false), pixels(TILE_BYTE_SIZE, 0) {}
};

class TileGrid {
public:
    TileGrid(int32_t cols, int32_t rows);
    ~TileGrid() = default;

    TileData* getOrCreateTile(int32_t col, int32_t row);
    const TileData* getTile(int32_t col, int32_t row) const;
    bool hasTile(int32_t col, int32_t row) const;

    size_t getActiveTileCount() const;
    size_t getMemoryUsageBytes() const;

    void clearAll();
    void resetDirtyFlags();

    int32_t getColumns() const { return mColumns; }
    int32_t getRows() const { return mRows; }

    static uint64_t packKey(int32_t col, int32_t row) {
        return (static_cast<uint64_t>(col) << 32) | (static_cast<uint64_t>(row) & 0xFFFFFFFFULL);
    }

private:
    int32_t mColumns;
    int32_t mRows;
    std::unordered_map<uint64_t, std::unique_ptr<TileData>> mTiles;
};

} // namespace inkwell
