# Inkwell - Digital Painting App for Android

Inkwell is an original, professional-grade digital painting application for Android tablets and phones, engineered with low-latency graphics, active stylus support, an advanced raster brush engine, and a multi-layer composition pipeline.

## Architecture Highlights
- **UI Chrome**: 100% Jetpack Compose for reactive, modular UI components.
- **Rendering Surface**: Dedicated hardware-accelerated OpenGL ES 3.2 rendering pipeline via an Android `SurfaceView`.
- **Brush Engine**: Low-latency, zero-GC native C++20 engine with Catmull-Rom spline interpolation, Streamline stabilization, and procedural dabs.
- **Canvas Storage**: Sparse $256 \times 256$ RGBA8888 tile grid with bounded memory budgets and in-memory LZ4 compression.
- **Undo / Redo**: Sub-50ms tile-diff delta snapshots supporting 50+ undo steps.
- **Project Persistence**: Non-destructive atomic `.ink` container package with crash recovery journaling.

## Documentation
- [Definition of Done (v1.0)](docs/DEFINITION_OF_DONE.md)
- [Capability Dependency Tree](docs/CAPABILITY_DEPENDENCY_TREE.md)
- [System Architecture](docs/ARCHITECTURE.md)
- [Engineering Roadmap](docs/ROADMAP.md)
- [Decisions Log (ADR)](docs/DECISIONS.md)
- [Changelog](docs/CHANGELOG.md)

## Building the Project
- **Minimum SDK**: Android 10 (API 29)
- **Target SDK**: Android 15 (API 35)
- **JDK**: Java 17+
- **NDK**: r26b+ (C++20)
- **CMake**: 3.22.1+

```bash
./gradlew assembleDebug
./gradlew test
```
