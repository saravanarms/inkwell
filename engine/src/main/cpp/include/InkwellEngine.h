#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <android/native_window.h>
#include "LayerManager.h"
#include "ViewportTransform.h"
#include "DabRasterizer.h"
#include "GLES3Renderer.h"
#include "NativeHistoryManager.h"
#include "SelectionMask.h"
#include "TransformSession.h"
#include "FloodFill.h"

namespace inkwell {

class InkwellEngine {
public:
    InkwellEngine(int32_t canvasWidth, int32_t canvasHeight, int32_t tileSize = TILE_SIZE);
    ~InkwellEngine();

    bool initialize();
    void destroy();

    // Surface & Lifecycle
    bool setSurface(ANativeWindow* window);
    void surfaceChanged(int32_t width, int32_t height);
    void surfaceDestroyed();

    // Layer System
    int32_t addLayer(const std::string& name = "");
    bool deleteLayer(int32_t id);
    bool reorderLayer(int32_t fromIndex, int32_t toIndex);
    void setActiveLayer(int32_t id);
    void setLayerOpacity(int32_t id, float opacity);
    void setLayerVisible(int32_t id, bool visible);
    void setLayerBlendMode(int32_t id, int32_t mode);
    void setLayerAlphaLock(int32_t id, bool locked);
    void setLayerClippingMask(int32_t id, bool mask);
    bool mergeDown(int32_t layerIndex);

    // Stroke & Brush Pipeline
    void beginStroke(float x, float y, float pressure);
    void appendPoints(const float* pointData, int pointCount);
    void endStroke();

    void setBrushSize(float diameterPx);
    void setBrushColor(uint8_t r, uint8_t g, uint8_t b, uint8_t a);
    void setBrushOpacity(float opacity);
    void setBrushHardness(float hardness);
    void setBrushSpacing(float spacingFraction);
    void setToolMode(ToolMode mode);
    void setStreamline(float factor);

    // Viewport Interactions
    void pan(float dx, float dy);
    void zoom(float scaleFactor, float focusX, float focusY);
    void rotate(float deltaRadians, float focusX, float focusY);
    void resetViewport();

    // Coordinate Mapping
    bool screenToCanvas(float screenX, float screenY, float& outCanvasX, float& outCanvasY) const;
    void canvasToScreen(float canvasX, float canvasY, float& outScreenX, float& outScreenY) const;

    // Rendering
    void renderFrame();

    // History (Undo / Redo)
    bool undo();
    bool redo();
    bool canUndo() const;
    bool canRedo() const;
    size_t getUndoCount() const;
    size_t getRedoCount() const;
    size_t getHistoryMemoryUsage() const;

    // Eyedropper & Sampling
    void sampleCompositePixel(int32_t canvasX, int32_t canvasY, uint8_t& outR, uint8_t& outG, uint8_t& outB, uint8_t& outA) const;
    void sampleCompositeGrid(int32_t centerX, int32_t centerY, int32_t gridSize, uint8_t* outRgbaBuffer) const;

    // Full-Resolution Export Compositing
    void compositeBand(int32_t startY, int32_t rowCount, bool transparentBg, uint8_t* outRgbaBuffer) const;

    // Selection System
    void setSelectionRect(int32_t x0, int32_t y0, int32_t x1, int32_t y1);
    void setSelectionLasso(const float* points, int32_t pointCount);
    void clearSelection();
    void invertSelection();
    bool hasSelection() const;
    bool getSelectionBounds(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const;

    // Transform Tool
    bool beginTransform();
    void updateTransform(float dx, float dy, float scaleX, float scaleY, float rotationRad, float pivotX, float pivotY);
    bool commitTransform();
    void cancelTransform();
    bool isTransformActive() const;
    bool getTransformBounds(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const;

    // Flood Fill (ColorDrop)
    bool floodFill(int32_t seedX, int32_t seedY, uint8_t r, uint8_t g, uint8_t b, uint8_t a, float tolerance);

    // Diagnostics & State
    int32_t getWidth() const { return mCanvasWidth; }
    int32_t getHeight() const { return mCanvasHeight; }
    int32_t getTileSize() const { return mTileSize; }
    float getZoom() const { return mViewport.getZoom(); }
    float getRotation() const { return mViewport.getRotation(); }
    float getBrushSize() const { return mRasterizer.getBrushSize(); }
    ToolMode getToolMode() const { return mRasterizer.getToolMode(); }

    TileGrid* getTileGrid();
    const TileGrid* getTileGrid() const;
    LayerManager& getLayerManager() { return mLayerManager; }
    const LayerManager& getLayerManager() const { return mLayerManager; }
    ViewportTransform& getViewport() { return mViewport; }
    const ViewportTransform& getViewport() const { return mViewport; }

    static const char* getVersion();

private:
    int32_t mCanvasWidth;
    int32_t mCanvasHeight;
    int32_t mTileSize;
    bool mInitialized;

    LayerManager mLayerManager;
    ViewportTransform mViewport;
    DabRasterizer mRasterizer;
    NativeHistoryManager mHistoryManager;
    SelectionMask mSelectionMask;
    TransformSession mTransformSession;
    std::unique_ptr<GLES3Renderer> mRenderer;
};

} // namespace inkwell
