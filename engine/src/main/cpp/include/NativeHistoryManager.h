#pragma once

#include <vector>
#include "TileDelta.h"
#include "LayerManager.h"

namespace inkwell {

class NativeHistoryManager {
public:
    NativeHistoryManager(size_t maxSteps = 50, size_t maxMemoryBytes = 50 * 1024 * 1024);

    void recordStroke(StrokeRecord&& record);
    bool undo(LayerManager* layerManager);
    bool redo(LayerManager* layerManager);

    bool canUndo() const { return !mUndoStack.empty(); }
    bool canRedo() const { return !mRedoStack.empty(); }
    size_t getUndoCount() const { return mUndoStack.size(); }
    size_t getRedoCount() const { return mRedoStack.size(); }
    size_t getMemoryUsageBytes() const { return mCurrentMemoryBytes; }

    void clear();

private:
    void enforceLimits();

    size_t mMaxSteps;
    size_t mMaxMemoryBytes;
    size_t mCurrentMemoryBytes = 0;

    std::vector<StrokeRecord> mUndoStack;
    std::vector<StrokeRecord> mRedoStack;
};

} // namespace inkwell
