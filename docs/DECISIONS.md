# Decisions Log (ADR)

## ADR-001: Project Scope & Definition of Done Baseline
- **Date**: 2026-11-21
- **Status**: Approved
- **Context**: Setting the target product specification, hardware requirements, and performance envelopes for "Inkwell" v1.0.
- **Decision**:
  - Target Android 10 (API 29) as minimum SDK with 64-bit ARM architecture.
  - Prioritize tablet form factors with active digitizer/stylus (Wacom EMR / Samsung S-Pen, USI 2.0).
  - Scope v1.0 to raster painting essentials: tiled canvas (up to 4096x4096), 60+ fps brush engine with pressure/tilt, layer stack with 12 blend modes, 50+ undo steps via tile delta journaling, and PNG/JPEG export.
  - Explicit non-goals: vector graphics, 3D painting, cloud account lock-in, and generative AI.

---

## ADR-002: Rendering Architecture & Graphics API Selection
- **Date**: 2026-11-21
- **Status**: Approved
- **Context**: Choosing the graphics API for the drawing surface among Android Canvas/Skia, Vulkan, and OpenGL ES.
- **Decision**:
  - Adopt **OpenGL ES 3.2** (with GLES 3.0 baseline compatibility).
  - Use a dedicated `SurfaceView` with background rendering thread and EGL context, completely decoupled from Jetpack Compose UI rendering.

---

## ADR-003: Core Engine Language & NDK Hybrid Boundary
- **Date**: 2026-11-21
- **Status**: Approved
- **Context**: Balancing developer velocity, UI expressiveness, and zero-GC performance for high-frequency stroke rendering.
- **Decision**:
  - UI Chrome, ViewModels, business logic, and file management implemented in **Kotlin + Jetpack Compose**.
  - Stroke interpolation, Catmull-Rom spline evaluation, tile grid storage, smudge color-mixing, and GLES3 rendering implemented in **C++20 via Android NDK**.
  - Lean JNI boundary: JNI is called once per input event packet with raw coordinate/pressure arrays, never per dab or per pixel ($<0.01\text{ ms}$ overhead).

---

## ADR-004: Canvas Tiling & Memory Budget Strategy
- **Date**: 2026-11-21
- **Status**: Approved
- **Context**: Enabling arbitrary canvas sizes up to $4096 \times 4096$ with 20+ layers on devices with 4GB–8GB RAM without Out-Of-Memory (OOM) crashes.
- **Decision**:
  - Partition canvas into $256 \times 256$ RGBA8888 tiles (256 KB each).
  - Use sparse allocation: unpainted tiles consume 0 memory.
  - Maintain a pre-allocated pool of direct native memory buffers to prevent GC thrashing.
  - Cap in-memory uncompressed tile footprint to 60% of `ActivityManager.getLargeMemoryClass()`, using in-memory LZ4 compression for inactive tiles.

---

## ADR-005: Undo/Redo via Tile-Diff Journaling
- **Date**: 2026-11-21
- **Status**: Approved
- **Context**: Supporting 50+ undo steps without running out of RAM or incurring slow undo response times.
- **Decision**:
  - Combine the Command Pattern with **Tile-Delta Snapshots**.
  - Record only modified tile coordinates per stroke, saving LZ4-compressed pre-stroke pixel data.
  - Typical stroke costs $\sim 300\text{ KB}$; 50 undo steps occupy $\sim 15\text{ MB} - 30\text{ MB}$.
  - Tile restore takes $<2\text{ ms}$, ensuring $<50\text{ ms}$ undo latency.

---

## ADR-006: Native Project File Format (`.ink`)
- **Date**: 2026-11-21
- **Status**: Approved
- **Context**: Defining a reliable, non-destructive, crash-safe project file container.
- **Decision**:
  - Use a ZIP package container containing `manifest.json`, `canvas.json`, `thumbnail.png`, per-layer metadata, and LZ4-compressed tile binary chunks (`tiles.lz4`).
  - Implement crash recovery via atomic write: write to `.ink.tmp` $\to$ `fsync()` $\to$ atomic rename to `.ink`.

---

## ADR-007: Forward Milestone Sequence & Color System Placement
- **Date**: 2026-11-21
- **Status**: Approved
- **Context**: Ordering milestones M0 through M11 to minimize risk and ensure natural progression.
- **Decision**:
  - Place **Color Picker, Palettes & Eyedropper at M6** immediately following Layers (M4) and Undo/Redo (M5).
  - Place **Selection, Transform & Flood Fill at M7**.
- **Reasoning**:
  - Color selection is essential to test paint brushes, smudging, and layer blend modes.
  - Eyedropper directly validates layer framebuffer pixel readback.
  - Selection and Flood Fill depend on the active color and color tolerance logic.
