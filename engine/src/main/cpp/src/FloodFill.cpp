#include "FloodFill.h"
#include <cmath>
#include <algorithm>
#include <unordered_map>
#include <cstring>

namespace inkwell {

bool FloodFill::execute(
    TileGrid* grid,
    int32_t canvasWidth,
    int32_t canvasHeight,
    int32_t seedX,
    int32_t seedY,
    uint8_t fillR,
    uint8_t fillG,
    uint8_t fillB,
    uint8_t fillA,
    float tolerance,
    const SelectionMask* mask,
    StrokeRecord* outRecord
) {
    if (!grid || seedX < 0 || seedX >= canvasWidth || seedY < 0 || seedY >= canvasHeight) {
        return false;
    }

    if (mask && mask->hasSelection() && mask->getMaskValue(seedX, seedY) == 0) {
        return false;
    }

    auto fetchPixel = [grid](int32_t x, int32_t y, uint8_t& r, uint8_t& g, uint8_t& b, uint8_t& a) {
        int32_t col = x / TILE_SIZE;
        int32_t row = y / TILE_SIZE;
        const TileData* tile = grid->getTile(col, row);
        if (tile) {
            int32_t lx = x % TILE_SIZE;
            int32_t ly = y % TILE_SIZE;
            size_t idx = (static_cast<size_t>(ly) * TILE_SIZE + lx) * TILE_CHANNELS;
            r = tile->pixels[idx];
            g = tile->pixels[idx + 1];
            b = tile->pixels[idx + 2];
            a = tile->pixels[idx + 3];
        } else {
            r = g = b = a = 0;
        }
    };

    uint8_t targetR, targetG, targetB, targetA;
    fetchPixel(seedX, seedY, targetR, targetG, targetB, targetA);

    // If target and fill color are already identical, nothing to do
    if (targetR == fillR && targetG == fillG && targetB == fillB && targetA == fillA && tolerance == 0.0f) {
        return false;
    }

    // Color distance metric (Euclidean in RGBA)
    float maxDeltaSq = (tolerance * 510.0f) * (tolerance * 510.0f);
    auto matchesTarget = [&](int32_t x, int32_t y) {
        if (mask && mask->hasSelection() && mask->getMaskValue(x, y) == 0) {
            return false;
        }
        uint8_t r, g, b, a;
        fetchPixel(x, y, r, g, b, a);
        float dr = static_cast<float>(r) - static_cast<float>(targetR);
        float dg = static_cast<float>(g) - static_cast<float>(targetG);
        float db = static_cast<float>(b) - static_cast<float>(targetB);
        float da = static_cast<float>(a) - static_cast<float>(targetA);
        return (dr * dr + dg * dg + db * db + da * da) <= maxDeltaSq;
    };

    // Allocate 1-bit visited bitmap
    size_t totalPixels = static_cast<size_t>(canvasWidth) * canvasHeight;
    std::vector<uint8_t> visited((totalPixels + 7) / 8, 0);

    auto isVisited = [&](int32_t x, int32_t y) -> bool {
        size_t idx = static_cast<size_t>(y) * canvasWidth + x;
        return (visited[idx >> 3] & (1 << (idx & 7))) != 0;
    };

    auto setVisited = [&](int32_t x, int32_t y) {
        size_t idx = static_cast<size_t>(y) * canvasWidth + x;
        visited[idx >> 3] |= (1 << (idx & 7));
    };

    // Pre-capture tile state for undo
    std::unordered_map<uint64_t, std::vector<uint8_t>> preTiles;
    auto capturePreTile = [&](int32_t col, int32_t row) {
        uint64_t key = (static_cast<uint64_t>(col) << 32) | static_cast<uint32_t>(row);
        if (preTiles.find(key) == preTiles.end()) {
            const TileData* tile = grid->getTile(col, row);
            if (tile) {
                preTiles.emplace(key, tile->pixels);
            } else {
                preTiles.emplace(key, std::vector<uint8_t>(TILE_BYTE_SIZE, 0));
            }
        }
    };

    std::vector<Span> stack;
    stack.reserve(1024);

    setVisited(seedX, seedY);
    stack.push_back({seedX, seedX, seedY, 1});
    stack.push_back({seedX, seedX, seedY - 1, -1});

    while (!stack.empty()) {
        Span span = stack.back();
        stack.pop_back();

        int32_t y = span.y;
        if (y < 0 || y >= canvasHeight) continue;

        // Expand left
        int32_t l = span.xLeft;
        while (l > 0 && !isVisited(l - 1, y) && matchesTarget(l - 1, y)) {
            --l;
            setVisited(l, y);
        }

        // Expand right
        int32_t r = span.xRight;
        while (r + 1 < canvasWidth && !isVisited(r + 1, y) && matchesTarget(r + 1, y)) {
            ++r;
            setVisited(r, y);
        }

        // Fill span [l, r] on scanline y
        for (int32_t x = l; x <= r; ++x) {
            int32_t col = x / TILE_SIZE;
            int32_t row = y / TILE_SIZE;
            capturePreTile(col, row);

            TileData* tile = grid->getOrCreateTile(col, row);
            if (tile) {
                int32_t lx = x % TILE_SIZE;
                int32_t ly = y % TILE_SIZE;
                size_t idx = (static_cast<size_t>(ly) * TILE_SIZE + lx) * TILE_CHANNELS;
                tile->pixels[idx] = fillR;
                tile->pixels[idx + 1] = fillG;
                tile->pixels[idx + 2] = fillB;
                tile->pixels[idx + 3] = fillA;
                tile->isDirty = true;
            }
        }

        // Check adjacent scanlines in span direction
        auto scanLine = [&](int32_t checkY, int32_t dy) {
            if (checkY < 0 || checkY >= canvasHeight) return;
            int32_t spanStart = -1;

            for (int32_t x = l; x <= r; ++x) {
                bool match = !isVisited(x, checkY) && matchesTarget(x, checkY);
                if (match) {
                    setVisited(x, checkY);
                    if (spanStart == -1) {
                        spanStart = x;
                    }
                } else if (spanStart != -1) {
                    stack.push_back({spanStart, x - 1, checkY, dy});
                    spanStart = -1;
                }
            }
            if (spanStart != -1) {
                stack.push_back({spanStart, r, checkY, dy});
            }
        };

        scanLine(y + span.dy, span.dy);
        if (span.dy != 0) {
            scanLine(y - span.dy, -span.dy);
        }
    }

    // Build undo delta record
    if (outRecord) {
        std::vector<uint8_t> compressedPre;
        std::vector<uint8_t> compressedPost;
        std::vector<uint8_t> blankTile(TILE_BYTE_SIZE, 0);

        for (const auto& [key, preData] : preTiles) {
            int32_t col = static_cast<int32_t>(key >> 32);
            int32_t row = static_cast<int32_t>(key & 0xFFFFFFFF);

            const TileData* postTile = grid->getTile(col, row);
            const uint8_t* postData = postTile ? postTile->pixels.data() : blankTile.data();

            if (std::memcmp(preData.data(), postData, TILE_BYTE_SIZE) != 0) {
                TileDeltaCompressor::compress(preData.data(), TILE_BYTE_SIZE, compressedPre);
                TileDeltaCompressor::compress(postData, TILE_BYTE_SIZE, compressedPost);
                outRecord->deltas.emplace_back(key, compressedPre, compressedPost);
            }
        }
    }

    return true;
}

} // namespace inkwell
