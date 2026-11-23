#include "TileGrid.h"

namespace inkwell {

TileGrid::TileGrid(int32_t cols, int32_t rows)
    : mColumns(cols), mRows(rows) {}

TileData* TileGrid::getOrCreateTile(int32_t col, int32_t row) {
    if (col < 0 || col >= mColumns || row < 0 || row >= mRows) {
        return nullptr;
    }

    uint64_t key = packKey(col, row);
    auto it = mTiles.find(key);
    if (it != mTiles.end()) {
        return it->second.get();
    }

    auto newTile = std::make_unique<TileData>(col, row);
    TileData* ptr = newTile.get();
    mTiles[key] = std::move(newTile);
    return ptr;
}

const TileData* TileGrid::getTile(int32_t col, int32_t row) const {
    if (col < 0 || col >= mColumns || row < 0 || row >= mRows) {
        return nullptr;
    }

    uint64_t key = packKey(col, row);
    auto it = mTiles.find(key);
    if (it != mTiles.end()) {
        return it->second.get();
    }
    return nullptr;
}

bool TileGrid::hasTile(int32_t col, int32_t row) const {
    uint64_t key = packKey(col, row);
    return mTiles.find(key) != mTiles.end();
}

size_t TileGrid::getActiveTileCount() const {
    return mTiles.size();
}

size_t TileGrid::getMemoryUsageBytes() const {
    return mTiles.size() * TILE_BYTE_SIZE;
}

void TileGrid::clearAll() {
    mTiles.clear();
}

void TileGrid::resetDirtyFlags() {
    for (auto& pair : mTiles) {
        pair.second->isDirty = false;
    }
}

} // namespace inkwell
