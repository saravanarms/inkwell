#include "LayerNode.h"
#include <algorithm>

namespace inkwell {

LayerNode::LayerNode(int32_t id, const std::string& name, int32_t cols, int32_t rows)
    : mId(id), mName(name) {
    mTileGrid = std::make_unique<TileGrid>(cols, rows);
}

void LayerNode::mergeDown(LayerNode* lowerLayer) {
    if (!lowerLayer || !lowerLayer->getTileGrid() || !mTileGrid) return;

    int32_t cols = mTileGrid->getColumns();
    int32_t rows = mTileGrid->getRows();
    float layerAlpha = mOpacity;

    for (int32_t c = 0; c < cols; ++c) {
        for (int32_t r = 0; r < rows; ++r) {
            const TileData* srcTile = mTileGrid->getTile(c, r);
            if (!srcTile) continue;

            TileData* dstTile = lowerLayer->getTileGrid()->getOrCreateTile(c, r);
            if (!dstTile) continue;

            const uint8_t* src = srcTile->pixels.data();
            uint8_t* dst = dstTile->pixels.data();

            for (size_t i = 0; i < TILE_BYTE_SIZE; i += TILE_CHANNELS) {
                float sa = (static_cast<float>(src[i + 3]) / 255.0f) * layerAlpha;
                if (sa <= 0.001f) continue;

                float da = static_cast<float>(dst[i + 3]) / 255.0f;
                float outA = sa + da * (1.0f - sa);

                if (outA > 0.0f) {
                    float invOutA = 1.0f / outA;
                    float sr = static_cast<float>(src[i]);
                    float sg = static_cast<float>(src[i + 1]);
                    float sb = static_cast<float>(src[i + 2]);

                    float dr = static_cast<float>(dst[i]);
                    float dg = static_cast<float>(dst[i + 1]);
                    float db = static_cast<float>(dst[i + 2]);

                    dst[i] = static_cast<uint8_t>(std::clamp((sr * sa + dr * da * (1.0f - sa)) * invOutA, 0.0f, 255.0f));
                    dst[i + 1] = static_cast<uint8_t>(std::clamp((sg * sa + dg * da * (1.0f - sa)) * invOutA, 0.0f, 255.0f));
                    dst[i + 2] = static_cast<uint8_t>(std::clamp((sb * sa + db * da * (1.0f - sa)) * invOutA, 0.0f, 255.0f));
                    dst[i + 3] = static_cast<uint8_t>(std::clamp(outA * 255.0f, 0.0f, 255.0f));
                }
            }
            dstTile->isDirty = true;
        }
    }

    mTileGrid->clearAll();
}

} // namespace inkwell
