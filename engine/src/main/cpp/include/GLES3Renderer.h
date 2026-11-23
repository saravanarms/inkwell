#pragma once

#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <array>
#include "ViewportTransform.h"
#include "LayerManager.h"
#include "TileTextureCache.h"

namespace inkwell {

class GLES3Renderer {
public:
    GLES3Renderer();
    ~GLES3Renderer();

    bool initialize(ANativeWindow* window);
    void destroy();

    void resize(int32_t width, int32_t height);
    void renderFrame(const ViewportTransform& viewport, const LayerManager* layerManager);

    bool isInitialized() const { return mInitialized; }

private:
    bool initEGL(ANativeWindow* window);
    void destroyEGL();
    bool initShaders();
    void destroyShaders();

    GLuint compileShader(GLenum type, const char* source);
    GLuint createProgram(const char* vertexSource, const char* fragmentSource);

    ANativeWindow* mWindow = nullptr;
    EGLDisplay mDisplay = EGL_NO_DISPLAY;
    EGLContext mContext = EGL_NO_CONTEXT;
    EGLSurface mSurface = EGL_NO_SURFACE;
    EGLConfig mConfig = nullptr;

    int32_t mSurfaceWidth = 0;
    int32_t mSurfaceHeight = 0;
    bool mInitialized = false;

    // Canvas background shader
    GLuint mCanvasProgram = 0;
    GLint mCanvasMatrixLoc = -1;
    GLint mCanvasDimLoc = -1;

    // Tile layer shader
    GLuint mTileProgram = 0;
    GLint mTileMatrixLoc = -1;
    GLint mTileOriginLoc = -1;
    GLint mTileOpacityLoc = -1;
    GLint mTileBlendModeLoc = -1;
    GLint mTileSamplerLoc = -1;

    GLuint mQuadVao = 0;
    GLuint mQuadVbo = 0;

    TileTextureCache mTileCache;
};

} // namespace inkwell
