#pragma once

#include <GLES3/gl3.h>
#include <unordered_map>
#include <cstdint>
#include "TileGrid.h"
#include "ViewportTransform.h"

namespace inkwell {

class TileTextureCache {
public:
    TileTextureCache();
    ~TileTextureCache();

    void updateAndRenderLayer(
        const ViewportTransform& viewport,
        TileGrid* grid,
        float layerOpacity,
        int32_t blendMode,
        GLuint program,
        GLint matrixLoc,
        GLint tileOriginLoc,
        GLint opacityLoc,
        GLint blendModeLoc
    );

    void destroy();

private:
    struct CachedTexture {
        GLuint textureId = 0;
        int32_t col = 0;
        int32_t row = 0;
    };

    std::unordered_map<uint64_t, CachedTexture> mTextures;
};

} // namespace inkwell
