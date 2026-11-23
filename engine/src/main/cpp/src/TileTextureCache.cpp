#include "TileTextureCache.h"

namespace inkwell {

TileTextureCache::TileTextureCache() = default;

TileTextureCache::~TileTextureCache() {
    destroy();
}

void TileTextureCache::updateAndRenderLayer(
    const ViewportTransform& viewport,
    TileGrid* grid,
    float layerOpacity,
    int32_t blendMode,
    GLuint program,
    GLint matrixLoc,
    GLint tileOriginLoc,
    GLint opacityLoc,
    GLint blendModeLoc
) {
    if (!grid || layerOpacity <= 0.001f) return;

    int32_t cols = grid->getColumns();
    int32_t rows = grid->getRows();

    // 1. Synchronize GL textures for dirty tiles
    for (int32_t c = 0; c < cols; ++c) {
        for (int32_t r = 0; r < rows; ++r) {
            const TileData* tile = grid->getTile(c, r);
            if (!tile) continue;

            uint64_t key = TileGrid::packKey(c, r);
            auto it = mTextures.find(key);

            if (it == mTextures.end()) {
                GLuint tex = 0;
                glGenTextures(1, &tex);
                glBindTexture(GL_TEXTURE_2D, tex);

                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, TILE_SIZE, TILE_SIZE, 0,
                            GL_RGBA, GL_UNSIGNED_BYTE, tile->pixels.data());

                mTextures[key] = CachedTexture{
                    .textureId = tex,
                    .col = c,
                    .row = r
                };
            } else if (tile->isDirty) {
                glBindTexture(GL_TEXTURE_2D, it->second.textureId);
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, TILE_SIZE, TILE_SIZE,
                               GL_RGBA, GL_UNSIGNED_BYTE, tile->pixels.data());
            }
        }
    }
    grid->resetDirtyFlags();

    // 2. Render layer tiles
    std::array<float, 16> projMatrix;
    viewport.getGLProjectionMatrix(projMatrix);
    glUniformMatrix4fv(matrixLoc, 1, GL_FALSE, projMatrix.data());
    glUniform1f(opacityLoc, layerOpacity);
    glUniform1i(blendModeLoc, blendMode);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glActiveTexture(GL_TEXTURE0);

    for (int32_t c = 0; c < cols; ++c) {
        for (int32_t r = 0; r < rows; ++r) {
            if (!grid->hasTile(c, r)) continue;

            uint64_t key = TileGrid::packKey(c, r);
            auto it = mTextures.find(key);
            if (it != mTextures.end()) {
                glBindTexture(GL_TEXTURE_2D, it->second.textureId);
                glUniform2f(tileOriginLoc,
                            static_cast<float>(c * TILE_SIZE),
                            static_cast<float>(r * TILE_SIZE));
                glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            }
        }
    }

    glBindTexture(GL_TEXTURE_2D, 0);
    glDisable(GL_BLEND);
}

void TileTextureCache::destroy() {
    for (auto& pair : mTextures) {
        if (pair.second.textureId != 0) {
            glDeleteTextures(1, &pair.second.textureId);
        }
    }
    mTextures.clear();
}

} // namespace inkwell
