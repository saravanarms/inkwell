#pragma once

#include <string>
#include <memory>
#include "TileGrid.h"

namespace inkwell {

class LayerNode {
public:
    LayerNode(int32_t id, const std::string& name, int32_t cols, int32_t rows);
    ~LayerNode() = default;

    int32_t getId() const { return mId; }
    const std::string& getName() const { return mName; }
    void setName(const std::string& name) { mName = name; }

    float getOpacity() const { return mOpacity; }
    void setOpacity(float opacity) { mOpacity = std::clamp(opacity, 0.0f, 1.0f); }

    bool isVisible() const { return mIsVisible; }
    void setVisible(bool visible) { mIsVisible = visible; }

    bool isAlphaLocked() const { return mIsAlphaLocked; }
    void setAlphaLocked(bool locked) { mIsAlphaLocked = locked; }

    bool isClippingMask() const { return mIsClippingMask; }
    void setClippingMask(bool mask) { mIsClippingMask = mask; }

    int32_t getBlendMode() const { return mBlendMode; }
    void setBlendMode(int32_t mode) { mBlendMode = mode; }

    TileGrid* getTileGrid() { return mTileGrid.get(); }
    const TileGrid* getTileGrid() const { return mTileGrid.get(); }

    void mergeDown(LayerNode* lowerLayer);

private:
    int32_t mId;
    std::string mName;
    float mOpacity = 1.0f;
    bool mIsVisible = true;
    bool mIsAlphaLocked = false;
    bool mIsClippingMask = false;
    int32_t mBlendMode = 0; // BlendMode::NORMAL

    std::unique_ptr<TileGrid> mTileGrid;
};

} // namespace inkwell
