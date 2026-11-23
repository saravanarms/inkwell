#include "NativeHistoryManager.h"

namespace inkwell {

NativeHistoryManager::NativeHistoryManager(size_t maxSteps, size_t maxMemoryBytes)
    : mMaxSteps(maxSteps), mMaxMemoryBytes(maxMemoryBytes) {}

void NativeHistoryManager::recordStroke(StrokeRecord&& record) {
    // 1. Clear redo stack on new action
    for (const auto& item : mRedoStack) {
        mCurrentMemoryBytes -= item.getMemoryCost();
    }
    mRedoStack.clear();

    // 2. Push to undo stack
    mCurrentMemoryBytes += record.getMemoryCost();
    mUndoStack.push_back(std::move(record));

    // 3. Enforce limits
    enforceLimits();
}

bool NativeHistoryManager::undo(LayerManager* layerManager) {
    if (mUndoStack.empty() || !layerManager) return false;

    StrokeRecord stroke = std::move(mUndoStack.back());
    mUndoStack.pop_back();

    LayerNode* layer = layerManager->getLayerById(stroke.layerId);
    if (layer && layer->getTileGrid()) {
        TileGrid* grid = layer->getTileGrid();

        for (const auto& delta : stroke.deltas) {
            int32_t col = static_cast<int32_t>(delta.tileKey >> 32);
            int32_t row = static_cast<int32_t>(delta.tileKey & 0xFFFFFFFF);

            TileData* tile = grid->getOrCreateTile(col, row);
            if (tile) {
                TileDeltaCompressor::decompress(
                    delta.preData.data(),
                    delta.preData.size(),
                    tile->pixels.data(),
                    TILE_BYTE_SIZE
                );
                tile->isDirty = true;
            }
        }
    }

    mRedoStack.push_back(std::move(stroke));
    return true;
}

bool NativeHistoryManager::redo(LayerManager* layerManager) {
    if (mRedoStack.empty() || !layerManager) return false;

    StrokeRecord stroke = std::move(mRedoStack.back());
    mRedoStack.pop_back();

    LayerNode* layer = layerManager->getLayerById(stroke.layerId);
    if (layer && layer->getTileGrid()) {
        TileGrid* grid = layer->getTileGrid();

        for (const auto& delta : stroke.deltas) {
            int32_t col = static_cast<int32_t>(delta.tileKey >> 32);
            int32_t row = static_cast<int32_t>(delta.tileKey & 0xFFFFFFFF);

            TileData* tile = grid->getOrCreateTile(col, row);
            if (tile) {
                TileDeltaCompressor::decompress(
                    delta.postData.data(),
                    delta.postData.size(),
                    tile->pixels.data(),
                    TILE_BYTE_SIZE
                );
                tile->isDirty = true;
            }
        }
    }

    mUndoStack.push_back(std::move(stroke));
    return true;
}

void NativeHistoryManager::enforceLimits() {
    // Step limit
    while (mUndoStack.size() > mMaxSteps) {
        mCurrentMemoryBytes -= mUndoStack.front().getMemoryCost();
        mUndoStack.erase(mUndoStack.begin());
    }

    // Memory ceiling
    while (mCurrentMemoryBytes > mMaxMemoryBytes && mUndoStack.size() > 1) {
        mCurrentMemoryBytes -= mUndoStack.front().getMemoryCost();
        mUndoStack.erase(mUndoStack.begin());
    }
}

void NativeHistoryManager::clear() {
    mUndoStack.clear();
    mRedoStack.clear();
    mCurrentMemoryBytes = 0;
}

} // namespace inkwell
