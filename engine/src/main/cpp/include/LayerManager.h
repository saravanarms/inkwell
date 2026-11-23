#pragma once

#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include "LayerNode.h"

namespace inkwell {

class LayerManager {
public:
    LayerManager(int32_t cols, int32_t rows);

    int32_t addLayer(const std::string& name = "");
    bool deleteLayer(int32_t id);
    bool reorder(int32_t fromIndex, int32_t toIndex);
    bool mergeDown(int32_t layerIndex);

    LayerNode* getActiveLayer();
    const LayerNode* getActiveLayer() const;
    void setActiveLayer(int32_t id);

    LayerNode* getLayerById(int32_t id);
    LayerNode* getLayerByIndex(size_t index);

    size_t getLayerCount() const { return mLayers.size(); }
    const std::vector<std::unique_ptr<LayerNode>>& getLayers() const { return mLayers; }

    int32_t getActiveLayerId() const { return mActiveLayerId; }

    // Composite pixel sampling (Eyedropper)
    void sampleCompositePixel(int32_t x, int32_t y, uint8_t& outR, uint8_t& outG, uint8_t& outB, uint8_t& outA) const;
    void sampleCompositeGrid(int32_t centerX, int32_t centerY, int32_t gridSize, uint8_t* outRgbaBuffer) const;

    // Full-resolution export compositing in horizontal scanline bands
    void compositeBand(int32_t canvasWidth, int32_t startY, int32_t rowCount, bool transparentBg, uint8_t* outRgbaBuffer) const;

private:
    int32_t mCols;
    int32_t mRows;
    int32_t mNextId = 1;
    int32_t mActiveLayerId = 1;
    std::vector<std::unique_ptr<LayerNode>> mLayers;
};

} // namespace inkwell
