#include "GLES3Renderer.h"
#include <android/log.h>

#define LOG_TAG "InkwellGLES3"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace inkwell {

static const char* CANVAS_VERT_SHADER = R"glsl(#version 300 es
layout(location = 0) in vec2 aPosition;

uniform mat4 uProjectionMatrix;
uniform vec2 uCanvasDim;

out vec2 vCanvasCoord;

void main() {
    vCanvasCoord = aPosition * uCanvasDim;
    gl_Position = uProjectionMatrix * vec4(vCanvasCoord, 0.0, 1.0);
}
)glsl";

static const char* CANVAS_FRAG_SHADER = R"glsl(#version 300 es
precision highp float;

in vec2 vCanvasCoord;
uniform vec2 uCanvasDim;

out vec4 fragColor;

void main() {
    float borderWidth = 2.0;
    if (vCanvasCoord.x <= borderWidth || vCanvasCoord.x >= (uCanvasDim.x - borderWidth) ||
        vCanvasCoord.y <= borderWidth || vCanvasCoord.y >= (uCanvasDim.y - borderWidth)) {
        fragColor = vec4(0.12, 0.12, 0.15, 1.0);
        return;
    }

    vec2 checkPos = floor(vCanvasCoord / 32.0);
    float check = mod(checkPos.x + checkPos.y, 2.0);

    vec3 colA = vec3(1.0, 1.0, 1.0);
    vec3 colB = vec3(0.92, 0.92, 0.94);
    vec3 finalColor = mix(colA, colB, check);

    fragColor = vec4(finalColor, 1.0);
}
)glsl";

static const char* TILE_VERT_SHADER = R"glsl(#version 300 es
layout(location = 0) in vec2 aPosition;

uniform mat4 uProjectionMatrix;
uniform vec2 uTileOrigin;

out vec2 vTexCoord;

void main() {
    vTexCoord = aPosition;
    vec2 worldPos = uTileOrigin + (aPosition * 256.0);
    gl_Position = uProjectionMatrix * vec4(worldPos, 0.0, 1.0);
}
)glsl";

static const char* TILE_FRAG_SHADER = R"glsl(#version 300 es
precision mediump float;

in vec2 vTexCoord;
uniform sampler2D uTileTexture;
uniform float uLayerOpacity;
uniform int uBlendMode;

out vec4 fragColor;

void main() {
    vec4 tex = texture(uTileTexture, vTexCoord);
    fragColor = vec4(tex.rgb, tex.a * uLayerOpacity);
}
)glsl";

GLES3Renderer::GLES3Renderer() = default;

GLES3Renderer::~GLES3Renderer() {
    destroy();
}

bool GLES3Renderer::initialize(ANativeWindow* window) {
    if (mInitialized) {
        destroy();
    }

    if (!initEGL(window)) {
        LOGE("Failed to initialize EGL");
        return false;
    }

    if (!initShaders()) {
        LOGE("Failed to initialize OpenGL shaders");
        destroyEGL();
        return false;
    }

    mInitialized = true;
    LOGI("GLES3Renderer successfully initialized with surface size %dx%d",
         mSurfaceWidth, mSurfaceHeight);
    return true;
}

void GLES3Renderer::destroy() {
    if (!mInitialized) return;

    mTileCache.destroy();
    destroyShaders();
    destroyEGL();

    mInitialized = false;
    LOGI("GLES3Renderer destroyed");
}

void GLES3Renderer::resize(int32_t width, int32_t height) {
    mSurfaceWidth = width;
    mSurfaceHeight = height;
    if (mInitialized && mDisplay != EGL_NO_DISPLAY && mSurface != EGL_NO_SURFACE) {
        glViewport(0, 0, width, height);
    }
}

void GLES3Renderer::renderFrame(const ViewportTransform& viewport, const LayerManager* layerManager) {
    if (!mInitialized || mDisplay == EGL_NO_DISPLAY || mSurface == EGL_NO_SURFACE) {
        return;
    }

    glClearColor(0.118f, 0.118f, 0.141f, 1.0f); // #1E1E24
    glClear(GL_COLOR_BUFFER_BIT);

    glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);

    // 1. Draw canvas checkerboard quad
    glUseProgram(mCanvasProgram);

    std::array<float, 16> projMatrix;
    viewport.getGLProjectionMatrix(projMatrix);
    glUniformMatrix4fv(mCanvasMatrixLoc, 1, GL_FALSE, projMatrix.data());

    glUniform2f(mCanvasDimLoc,
                static_cast<float>(viewport.getCanvasWidth()),
                static_cast<float>(viewport.getCanvasHeight()));

    glBindVertexArray(mQuadVao);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    // 2. Multi-layer composition from bottom to top
    if (layerManager) {
        glUseProgram(mTileProgram);
        glUniform1i(mTileSamplerLoc, 0);

        for (const auto& layer : layerManager->getLayers()) {
            if (layer->isVisible()) {
                mTileCache.updateAndRenderLayer(
                    viewport,
                    layer->getTileGrid(),
                    layer->getOpacity(),
                    layer->getBlendMode(),
                    mTileProgram,
                    mTileMatrixLoc,
                    mTileOriginLoc,
                    mTileOpacityLoc,
                    mTileBlendModeLoc
                );
            }
        }
    }

    glBindVertexArray(0);

    // 3. Swap buffers
    eglSwapBuffers(mDisplay, mSurface);
}

bool GLES3Renderer::initEGL(ANativeWindow* window) {
    mWindow = window;
    mDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (mDisplay == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed: 0x%x", eglGetError());
        return false;
    }

    EGLint major = 0, minor = 0;
    if (!eglInitialize(mDisplay, &major, &minor)) {
        LOGE("eglInitialize failed: 0x%x", eglGetError());
        return false;
    }

    const EGLint configAttribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 0,
        EGL_STENCIL_SIZE, 0,
        EGL_NONE
    };

    EGLint numConfigs = 0;
    if (!eglChooseConfig(mDisplay, configAttribs, &mConfig, 1, &numConfigs) || numConfigs <= 0) {
        LOGE("eglChooseConfig failed: 0x%x", eglGetError());
        return false;
    }

    const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };

    mContext = eglCreateContext(mDisplay, mConfig, EGL_NO_CONTEXT, contextAttribs);
    if (mContext == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed: 0x%x", eglGetError());
        return false;
    }

    mSurface = eglCreateWindowSurface(mDisplay, mConfig, mWindow, nullptr);
    if (mSurface == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed: 0x%x", eglGetError());
        return false;
    }

    if (!eglMakeCurrent(mDisplay, mSurface, mSurface, mContext)) {
        LOGE("eglMakeCurrent failed: 0x%x", eglGetError());
        return false;
    }

    eglQuerySurface(mDisplay, mSurface, EGL_WIDTH, &mSurfaceWidth);
    eglQuerySurface(mDisplay, mSurface, EGL_HEIGHT, &mSurfaceHeight);

    return true;
}

void GLES3Renderer::destroyEGL() {
    if (mDisplay != EGL_NO_DISPLAY) {
        eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (mSurface != EGL_NO_SURFACE) {
            eglDestroySurface(mDisplay, mSurface);
            mSurface = EGL_NO_SURFACE;
        }
        if (mContext != EGL_NO_CONTEXT) {
            eglDestroyContext(mDisplay, mContext);
            mContext = EGL_NO_CONTEXT;
        }
        eglTerminate(mDisplay);
        mDisplay = EGL_NO_DISPLAY;
    }

    if (mWindow != nullptr) {
        ANativeWindow_release(mWindow);
        mWindow = nullptr;
    }
}

bool GLES3Renderer::initShaders() {
    mCanvasProgram = createProgram(CANVAS_VERT_SHADER, CANVAS_FRAG_SHADER);
    if (mCanvasProgram == 0) return false;

    mCanvasMatrixLoc = glGetUniformLocation(mCanvasProgram, "uProjectionMatrix");
    mCanvasDimLoc = glGetUniformLocation(mCanvasProgram, "uCanvasDim");

    mTileProgram = createProgram(TILE_VERT_SHADER, TILE_FRAG_SHADER);
    if (mTileProgram == 0) return false;

    mTileMatrixLoc = glGetUniformLocation(mTileProgram, "uProjectionMatrix");
    mTileOriginLoc = glGetUniformLocation(mTileProgram, "uTileOrigin");
    mTileOpacityLoc = glGetUniformLocation(mTileProgram, "uLayerOpacity");
    mTileBlendModeLoc = glGetUniformLocation(mTileProgram, "uBlendMode");
    mTileSamplerLoc = glGetUniformLocation(mTileProgram, "uTileTexture");

    const float quadVertices[] = {
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    };

    glGenVertexArrays(1, &mQuadVao);
    glGenBuffers(1, &mQuadVbo);

    glBindVertexArray(mQuadVao);
    glBindBuffer(GL_ARRAY_BUFFER, mQuadVbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(quadVertices), quadVertices, GL_STATIC_DRAW);

    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 2 * sizeof(float), nullptr);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);

    return true;
}

void GLES3Renderer::destroyShaders() {
    if (mQuadVbo != 0) {
        glDeleteBuffers(1, &mQuadVbo);
        mQuadVbo = 0;
    }
    if (mQuadVao != 0) {
        glDeleteVertexArrays(1, &mQuadVao);
        mQuadVao = 0;
    }
    if (mCanvasProgram != 0) {
        glDeleteProgram(mCanvasProgram);
        mCanvasProgram = 0;
    }
    if (mTileProgram != 0) {
        glDeleteProgram(mTileProgram);
        mTileProgram = 0;
    }
}

GLuint GLES3Renderer::compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    if (shader == 0) return 0;

    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 1) {
            std::vector<char> infoLog(infoLen);
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog.data());
            LOGE("Error compiling shader: %s", infoLog.data());
        }
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

GLuint GLES3Renderer::createProgram(const char* vertexSource, const char* fragmentSource) {
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
    if (!vertexShader) return 0;

    GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (!fragmentShader) {
        glDeleteShader(vertexShader);
        return 0;
    }

    GLuint program = glCreateProgram();
    if (!program) return 0;

    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);

    GLint linked = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (!linked) {
        GLint infoLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 1) {
            std::vector<char> infoLog(infoLen);
            glGetProgramInfoLog(program, infoLen, nullptr, infoLog.data());
            LOGE("Error linking program: %s", infoLog.data());
        }
        glDeleteProgram(program);
        program = 0;
    }

    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
    return program;
}

} // namespace inkwell
