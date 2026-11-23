# Inkwell - Definition of Done (v1.0)

## 1. Target Devices & System Baseline
- **Primary Tier (Tablets)**: Android tablets with active digitizers and pressure-sensitive styluses (Samsung Galaxy Tab S7/S8/S9 with S-Pen, Pixel Tablet with USI stylus, Xiaomi Pad 6, Lenovo Tab P11/P12).
- **Secondary Tier (Phones)**: Android phones with capacitive touch or integrated stylus (Samsung Galaxy S-Ultra / Note series).
- **Minimum OS Version**: Android 10 (API level 29) - 64-bit ARM (`arm64-v8a`) baseline.
- **Target OS Version**: Android 15 (API level 35).
- **Input Hardware**: Active stylus with continuous pressure sensitivity (minimum 4096 levels reported via Android MotionEvent), tilt angle, hover cursor detection, physical barrel button shortcuts, and hardware-level palm rejection.

---

## 2. Core User Journey
1. **App Launch**: Instant cold start (<1.5s) into the **Gallery View**, showing existing projects with cached high-resolution thumbnails, canvas dimensions, color profile, and last-edited timestamps.
2. **Canvas Creation**: User selects from standard presets (Screen Size, 4K UHD 3840x2160, Square 2048x2048, A4/Letter 300 DPI) or specifies custom dimensions (up to 4096 x 4096 px). The UI displays real-time memory estimation and maximum safe layer count.
3. **Drawing Surface (Canvas)**:
   - Fullscreen immersive UI with collapsible side controls (brush size, opacity sliders, undo/redo buttons) configurable for left- or right-handed artists.
   - Smooth fluid pan (infinite canvas positioning), 360° rotation, and zoom (10% to 3200%) with snap-to-zero tactile feedback.
   - Ultra-low latency stroke rendering with real-time pressure response, tilt-based shading, and configurable stroke smoothing (Streamline / Catmull-Rom).
   - Intuitive touch gestures: 2-finger tap to undo, 3-finger tap to redo, 2-finger pinch/drag to navigate viewport, 4-finger tap to hide all UI chrome.
4. **Tool & Layer Manipulation**:
   - Seamless switching between Brush, Smudge, and Eraser using the same tip dynamics.
   - Non-destructive Layer drawer supporting layer creation, deletion, reordering, opacity adjustment, visibility toggles, alpha lock, clipping masks, merge down, and 12 standard blend modes.
   - Color selection via Color Disc, HSV/RGB sliders, custom palette swatch books, and instant touch-and-hold Eyedropper with sample loupe.
5. **Project Persistence & Export**:
   - Zero-data-loss background autosave during drawing into an atomic, transactional project bundle (`.ink`).
   - Clean export dialog supporting flattened PNG (with optional transparent background) and high-quality JPEG.
   - Direct integration with Android Share Sheet and system Storage Access Framework (SAF).

---

## 3. Feature Matrix

### MUST (v1.0 - Core Release)
- **Canvas & Viewport**:
  - Arbitrary canvas dimensions up to 4096 x 4096 px.
  - Infinite virtual canvas navigation (pan, smooth 360° rotation, pinch-zoom 10%–3200%).
  - Tiled memory management (256x256 or 512x512 tiles) with GPU texture paging and RAM budgeting.
- **Input & Stylus Pipeline**:
  - Sub-pixel stroke interpolation using `MotionEvent` historical batches.
  - Non-linear pressure curve calibration (soft to hard response).
  - Tilt support (angle and altitude) for calligraphic and broad-edge shading.
  - Strict palm rejection (touch events suppressed while stylus is active or in proximity).
  - Hover pointer indicator showing brush tip outline and diameter.
- **Brush Engine**:
  - Procedural dab stamp rasterizer with configurable tip shapes and paper grain textures.
  - Core dynamics: Size jitter, Opacity jitter, Spacing (1%–100%), Angle dynamics.
  - Stroke stabilizer / Streamline smoothing (Catmull-Rom spline interpolation).
  - 3 primary tool modes: **Brush** (paint dab), **Smudge** (color mixing and pigment dragging), and **Eraser** (subtractive mask).
  - Curated set of 12 built-in production brushes across pencils, inks, painterly oils, and airbrushes.
- **Layer Architecture**:
  - Up to 20+ layers on 2048x2048 canvas; up to 8–12 layers on 4096x4096 (RAM-dependent).
  - Operations: Add, Delete, Duplicate, Reorder, Rename, Visibility toggle, Opacity slider (0–100%).
  - Alpha Lock (restricts painting to existing pixel alpha).
  - Clipping Mask (clips layer visibility to underlying layer silhouette).
  - Merge Down & Flatten.
  - 12 Blend Modes: Normal, Multiply, Screen, Overlay, Darken, Lighten, Color Dodge, Add (Linear Dodge), Color Burn, Soft Light, Hard Light, Difference.
- **History & Undo/Redo**:
  - Minimum 50 undo steps.
  - Tile-diff delta compression (stores only modified tile coordinate deltas per stroke).
  - Instantaneous undo response (<50ms).
  - Multi-touch gesture shortcuts (2-finger tap undo, 3-finger tap redo).
- **Color System**:
  - Color Disc with inner saturation/value triangle or square.
  - Numerical HSV, RGB, and Hexadecimal input.
  - Custom palette creator and swatch storage.
  - Quick-access touch-hold Eyedropper with pixel magnification loupe.
- **File System & Export**:
  - Native `.ink` project package with atomic write / crash recovery journal.
  - Export to PNG (opaque and 32-bit transparent RGBA).
  - Export to JPEG with quality slider (50%–100%).
  - Android Share Sheet and SAF file saving.
- **UI & UX Chrome**:
  - Clean, distraction-free Jetpack Compose UI.
  - Left-handed and right-handed UI slider layout toggles.
  - Quick-hide UI canvas mode.

### SHOULD (v1.1 - Fast Follow)
- Selection Tools: Freehand Lasso, Rectangle, Ellipse, Automatic Magic Wand with adjustable color tolerance threshold.
- Transform Tools: Move, Uniform Scale, Freeform Scale, Rotate, Flip Horizontal/Vertical with bilinear/bicubic resampler.
- Flood Fill: ColorDrop bucket fill with continuous drag-threshold adjustment.
- Layer Grouping (folders) and Layer Blend Mode Previews.
- Custom Brush Studio: Import custom PNG tip shapes and grain patterns, brush export/import format.
- PSD-lite Export and Import (multi-layer, opacity, and blend modes compatible with desktop suites).
- QuickShape: Draw-and-hold stroke detection for perfect circles, squares, polygons, and straight lines.

### LATER (v2.0+ - Advanced Ecosystem)
- Time-lapse video recorder and MP4/GIF export.
- Non-destructive Color Adjustments (Curves, HSL, Brightness/Contrast, Color Balance, Gaussian Blur).
- Vector Text Engine with typography formatting and rasterization.
- Frame-by-frame 2D Animation Assist with onion skinning and timeline playback.
- CMYK color preview and ICC color profile conversions.
- Cloud backup & cross-device synchronization (Google Drive / Nextcloud).

---

## 4. Performance Targets & Budgets
| Metric | Target Specification | Measurement Technique |
| :--- | :--- | :--- |
| **Stroke Frame Rate** | Stable 60 fps (16.6ms frame budget), 90/120 fps on supported 120Hz displays | Android Choreographer / FrameMetrics API |
| **Input Latency** | < 20ms motion-to-photon | High-speed 240fps camera / Systrace `MotionEvent` to Surface presentation |
| **Maximum Canvas** | 4096 x 4096 px @ 300 DPI | Memory allocation test across 4GB/6GB/8GB devices |
| **Layer Capacity** | ≥ 20 layers @ 2048x2048, ≥ 8 layers @ 4096x4096 | Tile memory budget allocator verification |
| **Undo Steps** | ≥ 50 steps | Automated test applying 100 strokes and verifying memory cap & restore fidelity |
| **Undo Latency** | < 50ms per step | Benchmark timer on single-stroke undo step |
| **Cold Startup** | < 1.5 seconds to Gallery | Android Macrobenchmark cold startup test |
| **Project Open** | < 800ms for 2048x2048 project with 10 layers | Wall-clock file deserialization + texture staging |
| **Memory Ceiling** | Strict adherence to `ActivityManager.getLargeMemoryClass()` (capped at ~512MB-1GB RSS) | Android Profiler / Heap allocation limits |

---

## 5. Explicit Non-Goals
1. **Vector Drawing**: No bezier pen tools, control point editing, or SVG path editing. Inkwell is strictly a high-performance raster painting application.
2. **3D Model Painting & UV Unwrapping**: No 3D meshes, OBJ/FBX imports, or PBR shader pipelines.
3. **Mandatory Cloud Accounts / DRM**: No proprietary login walls, subscriptions, or telemetry trackers. Inkwell operates 100% offline with user-owned local storage.
4. **Generative AI Tools**: No text-to-image synthesis, inpainting servers, or cloud AI integrations. The focus is exclusively on authentic human artistic technique and responsive tactile brushwork.
5. **Cross-Platform Compromises**: We will not compromise Android rendering performance or stylus fidelity with generic cross-platform web or Flutter abstractions. The core engine is built natively for Android graphics and input hardware.
