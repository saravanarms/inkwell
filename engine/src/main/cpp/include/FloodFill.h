#pragma once

#include <cstdint>
#include <vector>
#include "TileGrid.h"
#include "SelectionMask.h"
#include "TileDelta.h"

namespace inkwell {

class FloodFill {
public:
    static bool execute(
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
        const SelectionMask* mask = nullptr,
        StrokeRecord* outRecord = nullptr
    );

private:
    struct Span {
        int32_t xLeft;
        int32_t xRight;
        int32_t y;
        int32_t dy; // +1 or -1
    };
};

} // namespace inkwell
