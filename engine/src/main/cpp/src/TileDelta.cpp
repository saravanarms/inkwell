#include "TileDelta.h"
#include <cstring>
#include <algorithm>

namespace inkwell {

void TileDeltaCompressor::compress(const uint8_t* rawData, size_t rawSize, std::vector<uint8_t>& out) {
    out.clear();
    if (!rawData || rawSize == 0) return;

    out.reserve(rawSize / 4); // Pre-reserve estimated compressed size

    size_t i = 0;
    while (i < rawSize) {
        // Check for run of identical bytes
        uint8_t currentVal = rawData[i];
        size_t runLen = 1;
        while (i + runLen < rawSize && rawData[i + runLen] == currentVal && runLen < 127) {
            runLen++;
        }

        if (runLen >= 3) {
            // Encode run: marker bit set (0x80 | runLen), followed by value
            out.push_back(static_cast<uint8_t>(0x80 | runLen));
            out.push_back(currentVal);
            i += runLen;
        } else {
            // Encode literal run
            size_t litStart = i;
            size_t litLen = 0;
            while (i < rawSize && litLen < 127) {
                // Check if a run of 3+ identical bytes begins
                if (i + 2 < rawSize && rawData[i] == rawData[i + 1] && rawData[i] == rawData[i + 2]) {
                    break;
                }
                litLen++;
                i++;
            }
            if (litLen > 0) {
                // Encode literal count (0x00 to 0x7F)
                out.push_back(static_cast<uint8_t>(litLen));
                out.insert(out.end(), rawData + litStart, rawData + litStart + litLen);
            }
        }
    }
}

bool TileDeltaCompressor::decompress(
    const uint8_t* compressedData,
    size_t compressedSize,
    uint8_t* outRaw,
    size_t expectedRawSize
) {
    if (!compressedData || compressedSize == 0 || !outRaw) return false;

    size_t inIdx = 0;
    size_t outIdx = 0;

    while (inIdx < compressedSize && outIdx < expectedRawSize) {
        uint8_t header = compressedData[inIdx++];

        if (header & 0x80) {
            // Run of identical bytes
            size_t runLen = header & 0x7F;
            if (inIdx >= compressedSize) return false;
            uint8_t val = compressedData[inIdx++];

            size_t toWrite = std::min(runLen, expectedRawSize - outIdx);
            std::memset(outRaw + outIdx, val, toWrite);
            outIdx += toWrite;
        } else {
            // Literal sequence
            size_t litLen = header;
            size_t toWrite = std::min(litLen, expectedRawSize - outIdx);
            if (inIdx + toWrite > compressedSize) return false;

            std::memcpy(outRaw + outIdx, compressedData + inIdx, toWrite);
            inIdx += litLen;
            outIdx += toWrite;
        }
    }

    return outIdx == expectedRawSize;
}

} // namespace inkwell
