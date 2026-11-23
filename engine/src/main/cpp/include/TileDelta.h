#pragma once

#include <cstdint>
#include <vector>
#include <utility>
#include "TileGrid.h"

namespace inkwell {

/**
 * High-speed RLE delta compressor and decompressor for tile pixel buffers.
 */
class TileDeltaCompressor {
public:
    static void compress(const uint8_t* rawData, size_t rawSize, std::vector<uint8_t>& out);
    static bool decompress(const uint8_t* compressedData, size_t compressedSize, uint8_t* outRaw, size_t expectedRawSize);
};

struct TileDelta {
    uint64_t tileKey = 0;
    std::vector<uint8_t> preData;
    std::vector<uint8_t> postData;

    TileDelta() = default;
    TileDelta(uint64_t key, std::vector<uint8_t> pre, std::vector<uint8_t> post)
        : tileKey(key), preData(std::move(pre)), postData(std::move(post)) {}

    size_t getMemoryCost() const {
        return preData.size() + postData.size() + sizeof(TileDelta);
    }
};

struct StrokeRecord {
    int32_t layerId = 1;
    std::vector<TileDelta> deltas;

    StrokeRecord() = default;
    StrokeRecord(int32_t id) : layerId(id) {}

    size_t getMemoryCost() const {
        size_t total = sizeof(StrokeRecord);
        for (const auto& d : deltas) total += d.getMemoryCost();
        return total;
    }
};

} // namespace inkwell
