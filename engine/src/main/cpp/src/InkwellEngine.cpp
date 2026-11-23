#include "InkwellEngine.h"
#include <android/log.h>

#define LOG_TAG "InkwellEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace inkwell {

constexpr const char* ENGINE_VERSION = "1.0.0-alpha";

InkwellEngine::InkwellEngine(int32_t canvasWidth, int32_t canvasHeight, int32_t tileSize)
    : mCanvasWidth(canvasWidth),
      mCanvasHeight(canvasHeight),
      mTileSize(tileSize),
      mInitialized(false),
      mLayerManager((canvasWidth + tileSize - 1) / tileSize, (canvasHeight + tileSize - 1) / tileSize),
      mViewport(canvasWidth, canvasHeight),
      mSelectionMask(canvasWidth, canvasHeight),
      mTransformSession(canvasWidth, canvasHeight) {
    mRenderer = std::make_unique<GLES3Renderer>();
    LOGI("InkwellEngine constructed: %dx%d (tile size %d)", canvasWidth, canvasHeight, tileSize);
}

InkwellEngine::~InkwellEngine() {
    destroy();
}

bool InkwellEngine::initialize() {
    if (mInitialized) return true;
    mInitialized = true;
    LOGI("InkwellEngine successfully initialized");
    return true;
}

void InkwellEngine::destroy() {
    if (mInitialized) {
        surfaceDestroyed();
        mInitialized = false;
        LOGI("InkwellEngine destroyed");
    }
}

bool InkwellEngine::setSurface(ANativeWindow* window) {
    if (!mRenderer) {
        mRenderer = std::make_unique<GLES3Renderer>();
    }
    return mRenderer->initialize(window);
}

void InkwellEngine::surfaceChanged(int32_t width, int32_t height) {
    mViewport.setViewportSize(width, height);
    if (mRenderer) {
        mRenderer->resize(width, height);
    }
}

void InkwellEngine::surfaceDestroyed() {
    if (mRenderer) {
        mRenderer->destroy();
    }
}

int32_t InkwellEngine::addLayer(const std::string& name) {
    return mLayerManager.addLayer(name);
}

bool InkwellEngine::deleteLayer(int32_t id) {
    return mLayerManager.deleteLayer(id);
}

bool InkwellEngine::reorderLayer(int32_t fromIndex, int32_t toIndex) {
    return mLayerManager.reorder(fromIndex, toIndex);
}

void InkwellEngine::setActiveLayer(int32_t id) {
    mLayerManager.setActiveLayer(id);
}

void InkwellEngine::setLayerOpacity(int32_t id, float opacity) {
    LayerNode* layer = mLayerManager.getLayerById(id);
    if (layer) layer->setOpacity(opacity);
}

void InkwellEngine::setLayerVisible(int32_t id, bool visible) {
    LayerNode* layer = mLayerManager.getLayerById(id);
    if (layer) layer->setVisible(visible);
}

void InkwellEngine::setLayerBlendMode(int32_t id, int32_t mode) {
    LayerNode* layer = mLayerManager.getLayerById(id);
    if (layer) layer->setBlendMode(mode);
}

void InkwellEngine::setLayerAlphaLock(int32_t id, bool locked) {
    LayerNode* layer = mLayerManager.getLayerById(id);
    if (layer) layer->setAlphaLocked(locked);
}

void InkwellEngine::setLayerClippingMask(int32_t id, bool mask) {
    LayerNode* layer = mLayerManager.getLayerById(id);
    if (layer) layer->setClippingMask(mask);
}

bool InkwellEngine::mergeDown(int32_t layerIndex) {
    return mLayerManager.mergeDown(layerIndex);
}

void InkwellEngine::beginStroke(float x, float y, float pressure) {
    LayerNode* active = mLayerManager.getActiveLayer();
    if (active) {
        mRasterizer.beginStroke(x, y, pressure, active->getTileGrid());
    }
}

void InkwellEngine::appendPoints(const float* pointData, int pointCount) {
    LayerNode* active = mLayerManager.getActiveLayer();
    if (active) {
        mRasterizer.appendPoints(pointData, pointCount, active->getTileGrid());
    }
}

void InkwellEngine::endStroke() {
    LayerNode* active = mLayerManager.getActiveLayer();
    if (active) {
        StrokeRecord record = mRasterizer.endStroke(active->getId(), active->getTileGrid());
        if (!record.deltas.empty()) {
            mHistoryManager.recordStroke(std::move(record));
        }
    } else {
        mRasterizer.endStroke(0, nullptr);
    }
}

void InkwellEngine::setBrushSize(float diameterPx) {
    mRasterizer.setBrushSize(diameterPx);
}

void InkwellEngine::setBrushColor(uint8_t r, uint8_t g, uint8_t b, uint8_t a) {
    mRasterizer.setBrushColor(r, g, b, a);
}

void InkwellEngine::setBrushOpacity(float opacity) {
    mRasterizer.setBrushOpacity(opacity);
}

void InkwellEngine::setBrushHardness(float hardness) {
    mRasterizer.setBrushHardness(hardness);
}

void InkwellEngine::setBrushSpacing(float spacingFraction) {
    mRasterizer.setBrushSpacing(spacingFraction);
}

void InkwellEngine::setToolMode(ToolMode mode) {
    mRasterizer.setToolMode(mode);
}

void InkwellEngine::setStreamline(float factor) {
    mRasterizer.setStreamline(factor);
}

void InkwellEngine::pan(float dx, float dy) {
    mViewport.pan(dx, dy);
}

void InkwellEngine::zoom(float scaleFactor, float focusX, float focusY) {
    mViewport.zoom(scaleFactor, focusX, focusY);
}

void InkwellEngine::rotate(float deltaRadians, float focusX, float focusY) {
    mViewport.rotate(deltaRadians, focusX, focusY);
}

void InkwellEngine::resetViewport() {
    mViewport.resetToCenter();
}

bool InkwellEngine::screenToCanvas(float screenX, float screenY, float& outCanvasX, float& outCanvasY) const {
    return mViewport.screenToCanvas(screenX, screenY, outCanvasX, outCanvasY);
}

void InkwellEngine::canvasToScreen(float canvasX, float canvasY, float& outScreenX, float& outScreenY) const {
    mViewport.canvasToScreen(canvasX, canvasY, outScreenX, outScreenY);
}

void InkwellEngine::renderFrame() {
    if (mRenderer && mRenderer->isInitialized()) {
        mRenderer->renderFrame(mViewport, &mLayerManager);
    }
}

bool InkwellEngine::undo() {
    bool ok = mHistoryManager.undo(&mLayerManager);
    if (ok) {
        renderFrame();
    }
    return ok;
}

bool InkwellEngine::redo() {
    bool ok = mHistoryManager.redo(&mLayerManager);
    if (ok) {
        renderFrame();
    }
    return ok;
}

bool InkwellEngine::canUndo() const {
    return mHistoryManager.canUndo();
}

bool InkwellEngine::canRedo() const {
    return mHistoryManager.canRedo();
}

size_t InkwellEngine::getUndoCount() const {
    return mHistoryManager.getUndoCount();
}

size_t InkwellEngine::getRedoCount() const {
    return mHistoryManager.getRedoCount();
}

size_t InkwellEngine::getHistoryMemoryUsage() const {
    return mHistoryManager.getMemoryUsageBytes();
}

void InkwellEngine::sampleCompositePixel(int32_t canvasX, int32_t canvasY, uint8_t& outR, uint8_t& outG, uint8_t& outB, uint8_t& outA) const {
    mLayerManager.sampleCompositePixel(canvasX, canvasY, outR, outG, outB, outA);
}

void InkwellEngine::sampleCompositeGrid(int32_t centerX, int32_t centerY, int32_t gridSize, uint8_t* outRgbaBuffer) const {
    mLayerManager.sampleCompositeGrid(centerX, centerY, gridSize, outRgbaBuffer);
}

void InkwellEngine::compositeBand(int32_t startY, int32_t rowCount, bool transparentBg, uint8_t* outRgbaBuffer) const {
    mLayerManager.compositeBand(mCanvasWidth, startY, rowCount, transparentBg, outRgbaBuffer);
}

// Selection System
void InkwellEngine::setSelectionRect(int32_t x0, int32_t y0, int32_t x1, int32_t y1) {
    mSelectionMask.setRect(x0, y0, x1, y1);
    renderFrame();
}

void InkwellEngine::setSelectionLasso(const float* points, int32_t pointCount) {
    mSelectionMask.setLasso(points, pointCount);
    renderFrame();
}

void InkwellEngine::clearSelection() {
    mSelectionMask.clear();
    renderFrame();
}

void InkwellEngine::invertSelection() {
    mSelectionMask.invert();
    renderFrame();
}

bool InkwellEngine::hasSelection() const {
    return mSelectionMask.hasSelection();
}

bool InkwellEngine::getSelectionBounds(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const {
    return mSelectionMask.getBoundingBox(outX0, outY0, outX1, outY1);
}

// Transform Tool
bool InkwellEngine::beginTransform() {
    LayerNode* active = mLayerManager.getActiveLayer();
    if (!active) return false;
    return mTransformSession.beginTransform(active, mSelectionMask.hasSelection() ? &mSelectionMask : nullptr);
}

void InkwellEngine::updateTransform(float dx, float dy, float scaleX, float scaleY, float rotationRad, float pivotX, float pivotY) {
    mTransformSession.updateTransform(dx, dy, scaleX, scaleY, rotationRad, pivotX, pivotY);
    renderFrame();
}

bool InkwellEngine::commitTransform() {
    LayerNode* active = mLayerManager.getActiveLayer();
    if (!active) return false;

    StrokeRecord record;
    bool success = mTransformSession.commitTransform(active, &record);
    if (success) {
        if (!record.deltas.empty()) {
            mHistoryManager.recordStroke(std::move(record));
        }
        renderFrame();
    }
    return success;
}

void InkwellEngine::cancelTransform() {
    LayerNode* active = mLayerManager.getActiveLayer();
    if (active) {
        mTransformSession.cancelTransform(active);
        renderFrame();
    }
}

bool InkwellEngine::isTransformActive() const {
    return mTransformSession.isActive();
}

bool InkwellEngine::getTransformBounds(int32_t& outX0, int32_t& outY0, int32_t& outX1, int32_t& outY1) const {
    return mTransformSession.getOriginalBounds(outX0, outY0, outX1, outY1);
}

// Flood Fill (ColorDrop)
bool InkwellEngine::floodFill(int32_t seedX, int32_t seedY, uint8_t r, uint8_t g, uint8_t b, uint8_t a, float tolerance) {
    LayerNode* active = mLayerManager.getActiveLayer();
    if (!active || !active->getTileGrid()) return false;

    StrokeRecord record;
    record.layerId = active->getId();

    bool success = FloodFill::execute(
        active->getTileGrid(),
        mCanvasWidth,
        mCanvasHeight,
        seedX,
        seedY,
        r,
        g,
        b,
        a,
        tolerance,
        mSelectionMask.hasSelection() ? &mSelectionMask : nullptr,
        &record
    );

    if (success) {
        if (!record.deltas.empty()) {
            mHistoryManager.recordStroke(std::move(record));
        }
        renderFrame();
    }
    return success;
}

TileGrid* InkwellEngine::getTileGrid() {
    LayerNode* active = mLayerManager.getActiveLayer();
    return active ? active->getTileGrid() : nullptr;
}

const TileGrid* InkwellEngine::getTileGrid() const {
    const LayerNode* active = mLayerManager.getActiveLayer();
    return active ? active->getTileGrid() : nullptr;
}

const char* InkwellEngine::getVersion() {
    return ENGINE_VERSION;
}

} // namespace inkwell
