cmake_minimum_required(VERSION 3.22.1)

project("inkwell_engine" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-Wall -Wno-unused-variable -Wno-unused-parameter -O3)

include_directories(
    src/main/cpp/include
)

add_library(inkwell_engine SHARED
    src/main/cpp/src/TileGrid.cpp
    src/main/cpp/src/ViewportTransform.cpp
    src/main/cpp/src/CatmullRomSpline.cpp
    src/main/cpp/src/StreamlineFilter.cpp
    src/main/cpp/src/DabRasterizer.cpp
    src/main/cpp/src/LayerNode.cpp
    src/main/cpp/src/LayerManager.cpp
    src/main/cpp/src/TileTextureCache.cpp
    src/main/cpp/src/TileDelta.cpp
    src/main/cpp/src/NativeHistoryManager.cpp
    src/main/cpp/src/SelectionMask.cpp
    src/main/cpp/src/TransformSession.cpp
    src/main/cpp/src/FloodFill.cpp
    src/main/cpp/src/GLES3Renderer.cpp
    src/main/cpp/src/InkwellEngine.cpp
    src/main/cpp/jni/InkwellEngineJni.cpp
)

target_link_libraries(inkwell_engine
    PRIVATE
    android
    EGL
    GLESv3
    log
)
