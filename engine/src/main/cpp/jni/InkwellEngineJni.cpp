#include <jni.h>
#include <string>
#include <android/native_window_jni.h>
#include "InkwellEngine.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeCreateEngine(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint canvasWidth,
    jint canvasHeight,
    jint tileSize
) {
    auto* engine = new inkwell::InkwellEngine(canvasWidth, canvasHeight, tileSize);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeDestroyEngine(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        delete engine;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeInitialize(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->initialize() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetSurface(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jobject surface
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        if (surface != nullptr) {
            ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
            return engine->setSurface(window) ? JNI_TRUE : JNI_FALSE;
        } else {
            engine->surfaceDestroyed();
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSurfaceChanged(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint width,
    jint height
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->surfaceChanged(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSurfaceDestroyed(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->surfaceDestroyed();
    }
}

// Layer Operations
JNIEXPORT jint JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeAddLayer(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jstring name
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        std::string layerName = "";
        if (name != nullptr) {
            const char* utf = env->GetStringUTFChars(name, nullptr);
            layerName = utf;
            env->ReleaseStringUTFChars(name, utf);
        }
        return engine->addLayer(layerName);
    }
    return -1;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeDeleteLayer(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint id
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->deleteLayer(id) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeReorderLayer(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint fromIndex,
    jint toIndex
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->reorderLayer(fromIndex, toIndex) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetActiveLayer(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint id
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setActiveLayer(id);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetLayerOpacity(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint id,
    jfloat opacity
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setLayerOpacity(id, opacity);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetLayerVisible(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint id,
    jboolean visible
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setLayerVisible(id, visible == JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetLayerBlendMode(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint id,
    jint mode
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setLayerBlendMode(id, mode);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetLayerAlphaLock(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint id,
    jboolean locked
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setLayerAlphaLock(id, locked == JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetLayerClippingMask(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint id,
    jboolean mask
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setLayerClippingMask(id, mask == JNI_TRUE);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeMergeDown(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint layerIndex
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->mergeDown(layerIndex) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

// Stroke Pipeline
JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeBeginStroke(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat x,
    jfloat y,
    jfloat pressure
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->beginStroke(x, y, pressure);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeAppendPoints(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jfloatArray pointData,
    jint count
) {
    if (engineHandle != 0 && pointData != nullptr && count > 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        jfloat* buf = env->GetFloatArrayElements(pointData, nullptr);
        if (buf != nullptr) {
            engine->appendPoints(buf, count);
            env->ReleaseFloatArrayElements(pointData, buf, JNI_ABORT);
        }
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeEndStroke(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->endStroke();
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetBrushParams(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat diameter,
    jint r,
    jint g,
    jint b,
    jint a,
    jfloat opacity,
    jfloat spacing
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setBrushSize(diameter);
        engine->setBrushColor(
            static_cast<uint8_t>(r & 0xFF),
            static_cast<uint8_t>(g & 0xFF),
            static_cast<uint8_t>(b & 0xFF),
            static_cast<uint8_t>(a & 0xFF)
        );
        engine->setBrushOpacity(opacity);
        engine->setBrushSpacing(spacing);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetBrushHardness(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat hardness
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setBrushHardness(hardness);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetToolMode(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint mode
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setToolMode(static_cast<inkwell::ToolMode>(mode));
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetStreamline(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat factor
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setStreamline(factor);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativePan(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat dx,
    jfloat dy
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->pan(dx, dy);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeZoom(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat scaleFactor,
    jfloat focusX,
    jfloat focusY
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->zoom(scaleFactor, focusX, focusY);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeRotate(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat deltaRadians,
    jfloat focusX,
    jfloat focusY
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->rotate(deltaRadians, focusX, focusY);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeResetViewport(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->resetViewport();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeScreenToCanvas(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat screenX,
    jfloat screenY,
    jfloatArray outCoords
) {
    if (engineHandle != 0 && outCoords != nullptr) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        float canvasX = 0.0f, canvasY = 0.0f;
        if (engine->screenToCanvas(screenX, screenY, canvasX, canvasY)) {
            jfloat buf[2] = { canvasX, canvasY };
            env->SetFloatArrayRegion(outCoords, 0, 2, buf);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeCanvasToScreen(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat canvasX,
    jfloat canvasY,
    jfloatArray outCoords
) {
    if (engineHandle != 0 && outCoords != nullptr) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        float screenX = 0.0f, screenY = 0.0f;
        engine->canvasToScreen(canvasX, canvasY, screenX, screenY);
        jfloat buf[2] = { screenX, screenY };
        env->SetFloatArrayRegion(outCoords, 0, 2, buf);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeRenderFrame(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->renderFrame();
    }
}

JNIEXPORT jfloat JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetZoom(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->getZoom();
    }
    return 1.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetRotation(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->getRotation();
    }
    return 0.0f;
}

JNIEXPORT jstring JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetVersion(
    JNIEnv* env,
    jobject /* thiz */
) {
    const char* version = inkwell::InkwellEngine::getVersion();
    return env->NewStringUTF(version);
}

JNIEXPORT jlong JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetActiveTileCount(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        if (engine->getTileGrid()) {
            return static_cast<jlong>(engine->getTileGrid()->getActiveTileCount());
        }
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetMemoryUsageBytes(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        if (engine->getTileGrid()) {
            return static_cast<jlong>(engine->getTileGrid()->getMemoryUsageBytes());
        }
    }
    return 0;
}

// History JNI implementations
JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeUndo(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->undo() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeRedo(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->redo() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeCanUndo(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->canUndo() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeCanRedo(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->canRedo() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetUndoCount(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return static_cast<jlong>(engine->getUndoCount());
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetRedoCount(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return static_cast<jlong>(engine->getRedoCount());
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetHistoryMemoryBytes(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return static_cast<jlong>(engine->getHistoryMemoryUsage());
    }
    return 0;
}

// Eyedropper sampling JNI
JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSamplePixel(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jint canvasX,
    jint canvasY,
    jintArray outRgbaArray
) {
    if (engineHandle != 0 && outRgbaArray != nullptr) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        uint8_t r = 0, g = 0, b = 0, a = 0;
        engine->sampleCompositePixel(canvasX, canvasY, r, g, b, a);
        jint rgba[4] = { r, g, b, a };
        env->SetIntArrayRegion(outRgbaArray, 0, 4, rgba);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSampleGrid(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jint centerX,
    jint centerY,
    jint gridSize,
    jbyteArray outBytes
) {
    if (engineHandle != 0 && outBytes != nullptr && gridSize > 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        size_t totalBytes = static_cast<size_t>(gridSize) * gridSize * 4;
        std::vector<uint8_t> buffer(totalBytes);
        engine->sampleCompositeGrid(centerX, centerY, gridSize, buffer.data());
        env->SetByteArrayRegion(outBytes, 0, static_cast<jsize>(totalBytes), reinterpret_cast<const jbyte*>(buffer.data()));
    }
}

// Selection JNI methods
JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetSelectionRect(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint x0, jint y0, jint x1, jint y1
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->setSelectionRect(x0, y0, x1, y1);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeSetSelectionLasso(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jfloatArray pointsArray,
    jint count
) {
    if (engineHandle != 0 && pointsArray != nullptr && count >= 3) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        jfloat* pts = env->GetFloatArrayElements(pointsArray, nullptr);
        engine->setSelectionLasso(pts, count);
        env->ReleaseFloatArrayElements(pointsArray, pts, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeClearSelection(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->clearSelection();
    }
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeInvertSelection(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->invertSelection();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeHasSelection(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->hasSelection() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetSelectionBounds(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jintArray outBounds
) {
    if (engineHandle != 0 && outBounds != nullptr) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        int32_t x0, y0, x1, y1;
        if (engine->getSelectionBounds(x0, y0, x1, y1)) {
            jint b[4] = { x0, y0, x1, y1 };
            env->SetIntArrayRegion(outBounds, 0, 4, b);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

// Transform JNI methods
JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeBeginTransform(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->beginTransform() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeUpdateTransform(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jfloat dx, jfloat dy,
    jfloat scaleX, jfloat scaleY,
    jfloat rotationRad,
    jfloat pivotX, jfloat pivotY
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->updateTransform(dx, dy, scaleX, scaleY, rotationRad, pivotX, pivotY);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeCommitTransform(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->commitTransform() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeCancelTransform(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        engine->cancelTransform();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeIsTransformActive(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->isTransformActive() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeGetTransformBounds(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jintArray outBounds
) {
    if (engineHandle != 0 && outBounds != nullptr) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        int32_t x0, y0, x1, y1;
        if (engine->getTransformBounds(x0, y0, x1, y1)) {
            jint b[4] = { x0, y0, x1, y1 };
            env->SetIntArrayRegion(outBounds, 0, 4, b);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

// Flood Fill JNI method
JNIEXPORT jboolean JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeFloodFill(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong engineHandle,
    jint seedX, jint seedY,
    jint r, jint g, jint b, jint a,
    jfloat tolerance
) {
    if (engineHandle != 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        return engine->floodFill(
            seedX, seedY,
            static_cast<uint8_t>(r),
            static_cast<uint8_t>(g),
            static_cast<uint8_t>(b),
            static_cast<uint8_t>(a),
            tolerance
        ) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

// Full-Resolution Export JNI
JNIEXPORT void JNICALL
Java_com_inkwell_engine_NativeEngineBridge_nativeCompositeBand(
    JNIEnv* env,
    jobject /* thiz */,
    jlong engineHandle,
    jint startY,
    jint rowCount,
    jboolean transparentBg,
    jbyteArray outBytes
) {
    if (engineHandle != 0 && outBytes != nullptr && rowCount > 0) {
        auto* engine = reinterpret_cast<inkwell::InkwellEngine*>(engineHandle);
        jbyte* buffer = env->GetByteArrayElements(outBytes, nullptr);
        engine->compositeBand(startY, rowCount, transparentBg == JNI_TRUE, reinterpret_cast<uint8_t*>(buffer));
        env->ReleaseByteArrayElements(outBytes, buffer, 0);
    }
}

} // extern "C"
