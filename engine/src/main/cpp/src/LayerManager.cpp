#include "LayerManager.h"

namespace inkwell {

LayerManager::LayerManager(int32_t cols, int32_t rows)
    : mCols(cols), mRows(rows) {
    addLayer("Layer 1");
}

int32_t LayerManager::addLayer(const std::string& name) {
    int32_t id = mNextId++;
    std::string layerName = name.empty() ? ("Layer " + std::to_string(id)) : name;

    auto layer = std::make_unique<LayerNode>(id, layerName, mCols, mRows);
    mLayers.push_back(std::move(layer));
    mActiveLayerId = id;
    return id;
}

bool LayerManager::deleteLayer(int32_t id) {
    if (mLayers.size() <= 1) return false;

    for (auto it = mLayers.begin(); it != mLayers.end(); ++it) {
        if ((*it)->getId() == id) {
            mLayers.erase(it);
            if (mActiveLayerId == id) {
                mActiveLayerId = mLayers.back()->getId();
            }
            return true;
        }
    }
    return false;
}

bool LayerManager::reorder(int32_t fromIndex, int32_t toIndex) {
    if (fromIndex == toIndex) return true;
    if (fromIndex < 0 || fromIndex >= static_cast<int32_t>(mLayers.size()) ||
        toIndex < 0 || toIndex >= static_cast<int32_t>(mLayers.size())) {
        return false;
    }

    auto layer = std::move(mLayers[fromIndex]);
    mLayers.erase(mLayers.begin() + fromIndex);
    mLayers.insert(mLayers.begin() + toIndex, std::move(layer));
    return true;
}

bool LayerManager::mergeDown(int32_t layerIndex) {
    if (layerIndex <= 0 || layerIndex >= static_cast<int32_t>(mLayers.size())) {
        return false;
    }

    LayerNode* upper = mLayers[layerIndex].get();
    LayerNode* lower = mLayers[layerIndex - 1].get();

    upper->mergeDown(lower);
    int32_t lowerId = lower->getId();

    mLayers.erase(mLayers.begin() + layerIndex);
    mActiveLayerId = lowerId;
    return true;
}

LayerNode* LayerManager::getActiveLayer() {
    return getLayerById(mActiveLayerId);
}

const LayerNode* LayerManager::getActiveLayer() const {
    for (const auto& layer : mLayers) {
        if (layer->getId() == mActiveLayerId) {
            return layer.get();
        }
    }
    return mLayers.empty() ? nullptr : mLayers.front().get();
}

void LayerManager::setActiveLayer(int32_t id) {
    for (const auto& layer : mLayers) {
        if (layer->getId() == id) {
            mActiveLayerId = id;
            return;
        }
    }
}

LayerNode* LayerManager::getLayerById(int32_t id) {
    for (auto& layer : mLayers) {
        if (layer->getId() == id) {
            return layer.get();
        }
    }
    return mLayers.empty() ? nullptr : mLayers.front().get();
}

LayerNode* LayerManager::getLayerByIndex(size_t index) {
    if (index < mLayers.size()) {
        return mLayers[index].get();
    }
    return nullptr;
}

void LayerManager::sampleCompositePixel(int32_t x, int32_t y, uint8_t& outR, uint8_t& outG, uint8_t& outB, uint8_t& outA) const {
    if (x < 0 || y < 0) {
        outR = outG = outB = outA = 0;
        return;
    }
    int32_t col = x / TILE_SIZE;
    int32_t row = y / TILE_SIZE;
    if (col >= mCols || row >= mRows) {
        outR = outG = outB = outA = 0;
        return;
    }
    int32_t localX = x % TILE_SIZE;
    int32_t localY = y % TILE_SIZE;
    size_t pixelOffset = (static_cast<size_t>(localY) * TILE_SIZE + localX) * TILE_CHANNELS;

    float accR = 0.0f, accG = 0.0f, accB = 0.0f, accA = 0.0f;

    for (const auto& layer : mLayers) {
        if (!layer->isVisible()) continue;
        const TileData* tile = layer->getTileGrid()->getTile(col, row);
        if (!tile) continue;

        const uint8_t* p = &tile->pixels[pixelOffset];
        float sa = (static_cast<float>(p[3]) / 255.0f) * layer->getOpacity();
        if (sa <= 0.001f) continue;

        float da = accA;
        float outAlpha = sa + da * (1.0f - sa);
        if (outAlpha > 0.0f) {
            float invOutAlpha = 1.0f / outAlpha;
            float sr = static_cast<float>(p[0]);
            float sg = static_cast<float>(p[1]);
            float sb = static_cast<float>(p[2]);

            accR = (sr * sa + accR * da * (1.0f - sa)) * invOutAlpha;
            accG = (sg * sa + accG * da * (1.0f - sa)) * invOutAlpha;
            accB = (sb * sa + accB * da * (1.0f - sa)) * invOutAlpha;
            accA = outAlpha;
        }
    }

    outR = static_cast<uint8_t>(std::clamp(accR, 0.0f, 255.0f));
    outG = static_cast<uint8_t>(std::clamp(accG, 0.0f, 255.0f));
    outB = static_cast<uint8_t>(std::clamp(accB, 0.0f, 255.0f));
    outA = static_cast<uint8_t>(std::clamp(accA * 255.0f, 0.0f, 255.0f));
}

void LayerManager::sampleCompositeGrid(int32_t centerX, int32_t centerY, int32_t gridSize, uint8_t* outRgbaBuffer) const {
    if (!outRgbaBuffer || gridSize <= 0) return;
    int32_t half = gridSize / 2;
    int32_t outIdx = 0;

    for (int32_t dy = -half; dy <= half; ++dy) {
        for (int32_t dx = -half; dx <= half; ++dx) {
            uint8_t r = 0, g = 0, b = 0, a = 0;
            sampleCompositePixel(centerX + dx, centerY + dy, r, g, b, a);
            outRgbaBuffer[outIdx++] = r;
            outRgbaBuffer[outIdx++] = g;
            outRgbaBuffer[outIdx++] = b;
            outRgbaBuffer[outIdx++] = a;
        }
    }
}

void LayerManager::compositeBand(int32_t canvasWidth, int32_t startY, int32_t rowCount, bool transparentBg, uint8_t* outRgbaBuffer) const {
    if (!outRgbaBuffer || canvasWidth <= 0 || rowCount <= 0) return;

    size_t totalBytes = static_cast<size_t>(canvasWidth) * rowCount * TILE_CHANNELS;
    if (transparentBg) {
        std::memset(outRgbaBuffer, 0, totalBytes);
    } else {
        for (size_t i = 0; i < totalBytes; i += TILE_CHANNELS) {
            outRgbaBuffer[i] = 255;
            outRgbaBuffer[i + 1] = 255;
            outRgbaBuffer[i + 2] = 255;
            outRgbaBuffer[i + 3] = 255;
        }
    }

    int32_t startRow = startY / TILE_SIZE;
    int32_t endRow = (startY + rowCount - 1) / TILE_SIZE;

    for (const auto& layer : mLayers) {
        if (!layer->isVisible()) continue;
        float layerAlpha = layer->getOpacity();
        if (layerAlpha <= 0.001f) continue;

        const TileGrid* grid = layer->getTileGrid();
        if (!grid) continue;

        for (int32_t tr = startRow; tr <= endRow; ++tr) {
            if (tr >= mRows) break;

            int32_t tileYStart = tr * TILE_SIZE;
            int32_t yOverlap0 = std::max(startY, tileYStart);
            int32_t yOverlap1 = std::min(startY + rowCount, tileYStart + TILE_SIZE);

            for (int32_t tc = 0; tc < mCols; ++tc) {
                const TileData* tile = grid->getTile(tc, tr);
                if (!tile) continue;

                int32_t tileXStart = tc * TILE_SIZE;
                int32_t xOverlap0 = std::max(0, tileXStart);
                int32_t xOverlap1 = std::min(canvasWidth, tileXStart + TILE_SIZE);

                for (int32_t y = yOverlap0; y < yOverlap1; ++y) {
                    int32_t localY = y - tileYStart;
                    size_t tileRowOffset = (static_cast<size_t>(localY) * TILE_SIZE) * TILE_CHANNELS;
                    size_t outRowOffset = (static_cast<size_t>(y - startY) * canvasWidth) * TILE_CHANNELS;

                    for (int32_t x = xOverlap0; x < xOverlap1; ++x) {
                        int32_t localX = x - tileXStart;
                        size_t tileIdx = tileRowOffset + localX * TILE_CHANNELS;
                        size_t outIdx = outRowOffset + x * TILE_CHANNELS;

                        const uint8_t* src = &tile->pixels[tileIdx];
                        uint8_t* dst = &outRgbaBuffer[outIdx];

                        float sa = (static_cast<float>(src[3]) / 255.0f) * layerAlpha;
                        if (sa <= 0.001f) continue;

                        float da = static_cast<float>(dst[3]) / 255.0f;
                        float outAlpha = sa + da * (1.0f - sa);

                        if (outAlpha > 0.0f) {
                            float invOutAlpha = 1.0f / outAlpha;
                            float sr = static_cast<float>(src[0]);
                            float sg = static_cast<float>(src[1]);
                            float sb = static_cast<float>(src[2]);

                            float dr = static_cast<float>(dst[0]);
                            float dg = static_cast<float>(dst[1]);
                            float db = static_cast<float>(dst[2]);

                            dst[0] = static_cast<uint8_t>(std::clamp((sr * sa + dr * da * (1.0f - sa)) * invOutAlpha, 0.0f, 255.0f));
                            dst[1] = static_cast<uint8_t>(std::clamp((sg * sa + dg * da * (1.0f - sa)) * invOutAlpha, 0.0f, 255.0f));
                            dst[2] = static_cast<uint8_t>(std::clamp((sb * sa + db * da * (1.0f - sa)) * invOutAlpha, 0.0f, 255.0f));
                            dst[3] = static_cast<uint8_t>(std::clamp(outAlpha * 255.0f, 0.0f, 255.0f));
                        }
                    }
                }
            }
        }
    }
}

} // namespace inkwell
