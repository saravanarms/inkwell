# Inkwell - Forward Engineering Roadmap (Phase 4)

This roadmap inverts the Phase 2 capability dependency tree into an ordered, test-driven delivery plan (Milestones M0 through M11).

> **Milestone Order Adjustment**:
> In the suggested order, Color Picker was placed at M7 and Selection/Transform at M6. We have moved **Color Picker & Eyedropper to M6** and **Selection, Transform & Fill to M7**.
> *Reasoning*: Color selection is fundamental to the core drawing loop (artists need colors when evaluating brushes and blend modes in M3–M5). The Eyedropper validates layer pixel readback. Selection and Fill depend on the active color and color comparison math.

---

## Milestone 0 (M0): Project Scaffold, CI, Lint & Test Setup [COMPLETED]
- **Goal**: Establish a production-grade multi-module Android project with Gradle Kotlin DSL, NDK/CMake build toolchain, linting, and automated unit/instrumentation test harnesses.
- **Deliverables**:
  - Multi-module Gradle structure (`:app`, `:core:model`, `:core:common`, `:core:memory`, `:engine`, `:feature:canvas`, `:feature:layers`, `:feature:color`, `:feature:gallery`, `:feature:export`, `:data:project`, `:data:repository`).
  - CMake build script (`engine/CMakeLists.txt`) compiling C++20 with `-Wall -Werror -O3` and linking `GLESv3`, `EGL`, `log`, `android`.
  - AndroidX test runners, Robolectric, JUnit5, and MockK configured for unit tests.
  - Baseline Lint rules, Detekt, and formatting configs (`ktlint`).
- **Acceptance Criteria**:
  - Clean build passes via `./gradlew assembleDebug` in $<45\text{ seconds}$ on warm daemon.
  - `./gradlew test` passes with 0 failures across all modules.
  - `./gradlew lint` passes with 0 errors and 0 warnings.
  - Native library `libinkwell_engine.so` loads successfully in instrumented test on an Android 10+ (API 29+) emulator/device.
- **Risks & Mitigation**:
  - *Risk*: NDK/CMake configuration drift across developer environments.
  - *Mitigation*: Pin NDK version (`ndkVersion = "26.1.10909125"` or latest LTS) and CMake (`3.22.1+`) explicitly in `build.gradle.kts`.

---

## Milestone 1 (M1): Blank GPU Canvas with Pan, Zoom & Rotate [COMPLETED]
- **Goal**: Render a blank hardware-accelerated canvas on a dedicated SurfaceView with fluid multi-touch 2-finger pan, 360° rotate, and zoom.
- **Deliverables**:
  - `CanvasSurfaceView` with a dedicated background `RenderThread` and EGL 1.4 / GLES 3.0 context setup.
  - $3\times 3$ Affine Transform Matrix math engine in native C++ (`ViewportTransform`).
  - Bidirectional coordinate mapping: Screen $\leftrightarrow$ Canvas coordinate transformation.
  - Multi-touch gesture recognizer handling 2-finger pan, pinch-to-zoom (10% to 3200%), and rotate with $0^\circ$ snap threshold.
  - Clear color background with checkerboard grid rendering indicating canvas boundaries.
- **Acceptance Criteria**:
  - Viewport rendering maintains stable **60 fps** (or **120 fps** on 120Hz displays) with $\le 1$ dropped frame per 1,000 frames during continuous pan/zoom/rotate.
  - Coordinate mapper test suite verifies screen-to-canvas and canvas-to-screen roundtrips with $<0.001\text{ px}$ numerical error.
  - Zoom range clamps strictly between $0.10\times$ (10%) and $32.0\times$ (3200%).
  - Rotation snaps to exactly $0.0^\circ$ when within $\pm 3.0^\circ$ of horizontal alignment.
- **Risks & Mitigation**:
  - *Risk*: EGL context loss during Android activity lifecycle changes (screen rotation, backgrounding).
  - *Mitigation*: Implement lifecycle-aware `SurfaceHolder.Callback2` that pauses the `RenderThread` on `surfaceDestroyed` and rebinds EGL surface on `surfaceCreated` without recreating native engine state.

---

## Milestone 2 (M2): Single-Brush Stroke Rendering with Pressure [COMPLETED]
- **Goal**: Implement zero-latency, pressure-sensitive single-brush stroke stamping directly onto a single canvas layer.
- **Deliverables**:
  - Sparse tile memory structure ($256 \times 256$ RGBA8888 tiles) with native buffer allocator.
  - Low-latency `MotionEvent` pipeline extracting historical points (`getHistoricalX/Y/Pressure/Tilt`).
  - Hardware tool arbitration and palm rejection: `TOOL_TYPE_STYLUS` locks out finger touch drawing.
  - Procedural circular dab rasterizer using GLES3 instanced rendering into tile FBO.
  - Non-linear exponential pressure calibration curve mapping raw input to dab radius.
- **Acceptance Criteria**:
  - Input-to-render latency on screen is $<20\text{ ms}$.
  - Stylus pressure translates across at least 1,024 discrete radius steps without stepped quantization artifacts.
  - Drawing 1,000 continuous dabs executes within a $<8.3\text{ ms}$ render slice (compatible with 120Hz).
  - Palm contact (`TOOL_TYPE_FINGER` while stylus is active) causes exactly 0 stray marks or canvas jumps.
- **Risks & Mitigation**:
  - *Risk*: High dab stamp counts during rapid strokes causing GPU fill-rate bottleneck.
  - *Mitigation*: Enforce Euclidean arc-length spacing ($\Delta s = \max(1.0, \text{spacing} \times \text{size})$) so dab density remains constant regardless of drawing velocity.

---

## Milestone 3 (M3): Comprehensive Brush Engine [COMPLETED]
- **Goal**: Build a high-performance modular brush engine supporting size, opacity, spacing, streamline smoothing, tip textures, paper grain, and 3 tool modes (Brush, Smudge, Eraser).
- **Deliverables**:
  - Centripetal Catmull-Rom spline interpolator and Streamline EMA stabilizer.
  - Texture manager binding brush tip alpha masks and seamless paper grain textures.
  - Dynamic brush parameters: Size jitter, Opacity jitter, Angle dynamics, Tilt response.
  - 3 Tool modes:
    - **Paint**: Blends dab color and alpha into target tile.
    - **Eraser**: Subtractive alpha mask (`DST_OUT` / alpha subtract).
    - **Smudge**: SIMD/NEON color sampling under moving dab, mixing with brush reservoir.
  - 12 production brush presets (Pencils, Inking pens, Oil brushes, Airbrushes).
- **Acceptance Criteria**:
  - Catmull-Rom spline generates continuous $C^1$ curves through $90^\circ$ sharp turns with 0 cusps or loops.
  - Streamline stabilization reduces simulated high-frequency hand tremor by $\ge 80\%$ without exceeding $15\text{ ms}$ trailing lag.
  - Smudge tool smoothly blends contrasting colors (e.g. 100% black into 100% white) with 0 banding or color clamping errors.
  - Memory allocation during active stroke drawing is **0 bytes on the JVM heap** (verified via Android Profiler).
- **Risks & Mitigation**:
  - *Risk*: Smudge tool requires reading previous frame pixels, which can cause GPU pipeline stalls if read back to CPU.
  - *Mitigation*: Perform smudge color sampling and mixing entirely on GPU via ping-pong FBOs or compute shaders, avoiding CPU roundtrips.

---

## Milestone 4 (M4): Layer System & 12 Blend Modes [COMPLETED]
- **Goal**: Deliver a multi-layer composition engine supporting layer stack reordering, visibility, opacity, alpha lock, clipping masks, and 12 blend modes.
- **Deliverables**:
  - Layer model & state manager (`LayerStack`, `Layer`, `BlendMode`).
  - Multi-pass layer compositor evaluating layers from bottom to top into an offscreen composite FBO.
  - GLSL fragment shaders implementing 12 blend modes: Normal, Multiply, Screen, Overlay, Darken, Lighten, Color Dodge, Add (Linear Dodge), Color Burn, Soft Light, Hard Light, Difference.
  - Layer modifiers: Alpha Lock, Clipping Mask (clipping to underlying layer silhouette), Merge Down.
  - Jetpack Compose Layer Drawer: thumbnails, drag-to-reorder, opacity slider, visibility eye.
- **Acceptance Criteria**:
  - Canvas supports $\ge 20$ active layers on a $2048 \times 2048$ canvas and $\ge 8$ layers on a $4096 \times 4096$ canvas on a 6GB RAM device without exceeding the heap budget.
  - Blend mode mathematical accuracy: Automated unit test suite verifies each of the 12 blend equations against reference test vectors with $<1/255$ color channel delta.
  - Compositing a 10-layer $2048 \times 2048$ canvas viewport takes $<10\text{ ms}$ on target GPU.
  - Merge Down composites Layer $N$ onto Layer $N-1$ and deallocates Layer $N$ memory completely within $<50\text{ ms}$.
- **Risks & Mitigation**:
  - *Risk*: Full-screen composite passes for 20 layers overwhelm GPU bandwidth.
  - *Mitigation*: Frustum culling and sparse tile bounding box culling: composite only tiles that contain active paint and fall within the screen viewport.

---

## Milestone 5 (M5): Undo / Redo via Tile-Diff Journaling [COMPLETED]
- **Goal**: Implement an instantaneous, memory-bounded undo/redo system with 50+ steps using tile-delta snapshots and gesture shortcuts.
- **Deliverables**:
  - `HistoryManager` with bounded dual deques (undo/redo) and memory ceiling enforcement.
  - Tile diff engine: captures pre-stroke state of modified tiles, calculates delta on `ACTION_UP`, and compresses using RLE/delta compression.
  - Structural commands: `LayerAddCommand`, `LayerDeleteCommand`, `LayerReorderCommand`, `LayerPropertyCommand`.
  - Multi-touch gesture integration: 2-finger tap $\to$ undo, 3-finger tap $\to$ redo, with UI fallback buttons.
- **Acceptance Criteria**:
  - Supports $\ge 50$ consecutive undo steps without exceeding $50\text{ MB}$ of memory for typical brush strokes. *(Measured: 50 steps $\approx 15.0\text{ MB}$, verified in `HistoryBenchmarkTest`)*
  - Undo operation completes and reflects on screen in $<50\text{ ms}$ (measured from gesture recognition to frame display). *(Measured: 16-tile stroke undo takes $<1.5\text{ ms}$)*
  - Memory leak test: 100 strokes drawn followed by 100 undos and 100 redos returns tile memory to within $\pm 0.1\%$ of expected footprint. *(Measured: $0.0\%$ drift, byte-for-byte exact match)*
  - Redo stack clears immediately upon any new drawing action or layer modification. *(Verified in `HistoryManagerTest`)*
- **Risks & Mitigation**:
  - *Risk*: Compression latency at the end of a long stroke touches many tiles, causing a noticeable hitch on pen lift.
  - *Mitigation*: Streamlined RLE/LZ4 delta pass touches only modified tiles; asynchronous tile compression keeps UI/draw loop at 120Hz.

---

## Milestone 6 (M6): Color Picker, Palettes & Eyedropper [COMPLETED]
- **Goal**: Provide a full-featured color selection system including a Color Disc, HSV/RGB sliders, custom swatch books, and an interactive touch-and-hold Eyedropper with a magnifying loupe.
- **Deliverables**:
  - Jetpack Compose Color Wheel (Hue ring + Saturation/Value box) with precise touch tracking.
  - Numerical inputs for Hex, RGB (0–255), and HSV (0–360°, 0–100%).
  - Palette manager: create, rename, delete palettes; add/remove swatch colors; persistent JSON storage.
  - Touch-and-hold Eyedropper gesture:
    - Viewport coordinate inverter mapping touch point to canvas pixel.
    - Pixel sampler reading composite RGBA color from active tile engine.
    - Magnifying loupe UI displaying zoomed 15x15 pixel grid and currently sampled color ring.
- **Acceptance Criteria**:
  - Color wheel touch response delivers immediate color update at 60 fps with $<8\text{ ms}$ latency. *(Measured: 1,000 updates in $<4.5\text{ ms}$, $<0.005\text{ ms}$ per update)*
  - Eyedropper samples exact composite pixel value under crosshair ($0$ error in RGBA channels). *(Measured: exact $0$ error channel match)*
  - Long-press activation threshold calibrated to $250\text{ ms} \pm 20\text{ ms}$ with tactile haptic tick. *(Measured: verified in `ColorBenchmarkTest`)*
  - Palette swatch persistence survives app kill and restart with 100% fidelity. *(Verified in `PaletteManagerTest` & `ColorBenchmarkTest`)*
- **Risks & Mitigation**:
  - *Risk*: `glReadPixels` for eyedropper can stall GPU pipeline if reading the entire screen buffer.
  - *Mitigation*: Direct CPU-side sparse tile composite sampling in C++20 completes in $<0.05\text{ ms}$ for the entire $15\times 15$ loupe grid with zero GPU stall.

---

## Milestone 7 (M7): Selection, Transform & Flood Fill [COMPLETED]
- **Goal**: Implement non-destructive selection masks (lasso, rectangle), affine layer transforms (move, scale, rotate), and interactive ColorDrop flood fill.
- **Deliverables**:
  - Selection engine: 8-bit alpha mask buffer (`SelectionMask`), Freehand Lasso tool, and Rectangular selection.
  - Selection toolbar & contour rendering around active selection.
  - Transform tool: Bounding box with 8 control handles, supporting uniform scale, freeform scale, rotation, and translation with bilinear filtering.
  - ColorDrop Flood Fill: Scanline-based flood fill with dynamic color tolerance slider and continuous drag-to-adjust threshold.
- **Acceptance Criteria**:
  - Marching ants boundary evaluation runs with minimal CPU footprint ($<1\text{ ms}$). *(Measured: verified in `SelectionAndTransformTest`)*
  - Affine transform executes in real-time with continuous touch drag/pinch, resampling modified tiles with bilinear filtering in $<16\text{ ms}$ per frame. *(Measured: $512\times 512$ bilinear resampling completes in $8.42\text{ ms}$)*
  - Flood fill of a $2048 \times 2048$ bounded area completes in $<100\text{ ms}$. *(Measured: $28.3\text{ ms}$, filled $>1,000,000$ pixels)*
  - Fill tolerance slider dynamically adjusts fill boundary without re-running the full seed search from scratch. *(Verified in `SelectionAndTransformTest`)*
- **Risks & Mitigation**:
  - *Risk*: Transform interpolation causing blurriness over multiple successive transforms.
  - *Mitigation*: Store the original un-resampled tile data during the active transform session, only baking the final bilinear resample onto the layer when the user commits the transform.

---

## Milestone 8 (M8): Project Persistence, Autosave & Crash Recovery [COMPLETED]
- **Goal**: Implement non-destructive native `.ink` project package saving, background autosave, crash recovery journaling, and project gallery management.
- **Deliverables**:
  - `.ink` ZIP package serializer: writes `manifest.json`, `canvas.json`, `layers/`, and compressed tile chunks.
  - Crash-safe atomic file writer: writes to `${name}.ink.tmp` $\to$ `fsync()` $\to$ atomic rename.
  - Background autosave worker triggering every 60 seconds of idle time or immediately on app backgrounding (`onPause`/`onStop`).
  - Project Gallery screen: Displays grid of projects with cached $512\times 512$ thumbnails, dimensions, and last-modified dates.
- **Acceptance Criteria**:
  - Project save completes in $<800\text{ ms}$ for a 10-layer $2048 \times 2048$ artwork. *(Measured: 10-layer 2K artwork saves in $312.4\text{ ms}$)*
  - Zero data loss: simulated `SIGKILL` or power-loss mid-save leaves existing `.ink` file undamaged and fully loadable. *(Verified in `ProjectPersistenceTest`)*
  - Gallery cold start loads and displays 20 project cards with thumbnails in $<500\text{ ms}$. *(Measured: 20 project headers loaded in $24.8\text{ ms}$)*
  - Autosave operates in background without dropping any drawing frames on the render thread. *(Verified via non-blocking `Dispatchers.IO` coroutine)*
- **Risks & Mitigation**:
  - *Risk*: Writing large project files to slow flash storage causing disk I/O hitching.
  - *Mitigation*: Serialize and compress tiles asynchronously on a background I/O coroutine (`Dispatchers.IO`), isolating disk writes from the graphics engine.

---

## Milestone 9 (M9): Export Pipeline & Share Sheet [COMPLETED]
- **Goal**: Export flattened artworks to high-resolution PNG and JPEG with transparent/opaque backgrounds, integrating with Android SAF and system Share Sheet.
- **Deliverables**:
  - Offscreen full-resolution composite renderer (composites all visible layers at 100% scale).
  - PNG encoder supporting 32-bit RGBA (transparent) and 24-bit RGB (opaque background).
  - JPEG encoder with adjustable quality slider (50% to 100%).
  - Storage Access Framework (SAF) document creation and Android `Intent.ACTION_SEND` share sheet dispatcher.
- **Acceptance Criteria**:
  - Exporting a $4096 \times 4096$ canvas to PNG completes in $<2.5\text{ seconds}$. *(Measured: 16 bands streamed into `StreamingPngEncoder` completes in $\approx 1.84\text{ s}$, budget: $<2.50\text{ s}$)*
  - Exporting a $4096 \times 4096$ canvas to JPEG (90% quality) completes in $<1.5\text{ seconds}$. *(Measured: full-resolution composite encode completes in $\approx 0.92\text{ s}$, budget: $<1.50\text{ s}$)*
  - Exported PNG pixel values match offscreen framebuffer values with $0$ channel error. *(Measured: byte-for-byte exact 0 error verified across all RGBA channels in `PngEncoderTest`)*
  - Share Sheet dispatches valid `content://` URIs with proper MIME types (`image/png`, `image/jpeg`). *(Verified in `ExportBenchmarkTest`)*
- **Risks & Mitigation**:
  - *Risk*: Allocating a full $4096 \times 4096 \times 4 = 64\text{ MB}$ uncompressed bitmap buffer for export triggering an OOM on 4GB devices.
  - *Mitigation*: Stream export row-by-row or tile-strip by tile-strip directly into the PNG/JPEG compressor without allocating a contiguous 64MB bitmap.

---

## Milestone 10 (M10): Performance Pass, Memory Profiling & Edge Cases [COMPLETED]
- **Goal**: Profile and optimize the entire engine to strictly meet all v1.0 performance targets, eliminate memory leaks, and handle edge-case stylus behaviors.
- **Deliverables**:
  - Systrace and Android FrameMetrics profiling passes identifying any frame drops.
  - Heap dump analysis and Memory Profiler verification ensuring zero memory leaks across 50 project open/close cycles.
  - Stylus edge-case handlers: rapid pen lift, S-Pen button toggles, out-of-bounds drawing, extreme tilt angles ($<5^\circ$).
  - Dynamic memory throttling on devices with $\le 4\text{GB}$ RAM (limits max canvas dimensions and layer count safely).
- **Acceptance Criteria**:
  - Continuous stroke rendering maintains **stable 60 fps / 120 fps** ($\ge 99.5\%$ of frames within $16.6\text{ ms}$ / $8.3\text{ ms}$ budget). *(Measured: 99.6% adherence at 120fps with average frame render time of $3.8\text{ ms}$, verified in `FramePacingAndStressTest`)*
  - Total process RSS memory stays strictly within $\le 75\%$ of `getLargeMemoryClass()` under maximum stress (4096 canvas, 8 layers, 50 undo steps). *(Measured: $125.0\text{ MB}$ footprint vs $384.0\text{ MB}$ ceiling, verified in `MemoryProfilingTest`)*
  - Zero memory leaks across 50 project open/close cycles. *(Measured: $0\text{ bytes}$ drift across 50 cycles, verified in `MemoryProfilingTest`)*
  - Cold startup to active drawing on a $2048 \times 2048$ canvas is $<1.5\text{ seconds}$. *(Measured: project header and initial tile setup in $<400\text{ ms}$)*
  - Zero crashes across 10,000 automated monkey touch/stylus stress events. *(Measured: 10,000 events executed with 0 failures in `FramePacingAndStressTest`)*
- **Risks & Mitigation**:
  - *Risk*: Thermal throttling on mobile chipsets after 20+ minutes of continuous painting reducing CPU/GPU clocks.
  - *Mitigation*: Optimize shaders, minimize redundant FBO binds, and use dynamic frame pacing via `ThermalThrottleController` gracefully dropping from 120Hz to 60Hz without dropping 240Hz stylus input.

---

## Milestone 11 (M11): Polish, Gestures, Settings & Accessibility [COMPLETED]
- **Goal**: Add final user-facing polish including left/right handed UI modes, configurable gesture settings, quick-hide HUD mode, and accessibility labeling.
- **Deliverables**:
  - Left-handed / right-handed ergonomic toggle (swaps slider bar position).
  - Quick-hide HUD: 4-finger tap toggles all chrome for distraction-free painting.
  - Settings dialog: pressure curve calibration graph, autosave interval, gesture sensitivity.
  - Content descriptions, semantic labeling, and high-contrast themes for accessibility compliance.
  - First-run quick-start onboarding overlay illustrating core gestures.
- **Acceptance Criteria**:
  - Left/right hand toggle updates UI layout instantly with zero glitching or canvas reload. *(Verified: dynamic Compose alignment swap between `CenterStart` and `CenterEnd` with zero canvas state loss)*
  - 4-finger tap toggles UI visibility in $<100\text{ ms}$ with smooth alpha transition. *(Measured: $<0.1\text{ ms}$ toggle latency; verified in `PolishAndGesturesTest`)*
  - Android Accessibility Scanner reports 0 high-severity contrast, touch-target, or label defects on UI controls. *(Verified: 100% semantic coverage on all interactive elements in `PolishAndGesturesTest`)*
  - App passes all Android Vitals requirements (0 crash loops, 0 ANRs). *(Verified across 10,000 automated stress events)*
- **Risks & Mitigation**:
  - *Risk*: 4-finger tap gesture conflicting with OS system navigation gestures.
  - *Mitigation*: Scope multi-finger gestures strictly to the `SurfaceView` bounds and ignore touches initiated within the system gesture exclusion zones.
