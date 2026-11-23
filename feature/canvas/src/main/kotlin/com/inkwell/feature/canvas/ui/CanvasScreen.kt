package com.inkwell.feature.canvas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.inkwell.feature.export.dispatcher.ExportDispatcher
import com.inkwell.feature.export.ui.ExportDialog
import java.io.File
import java.io.FileOutputStream
import com.inkwell.core.memory.MemoryBudgetManager
import com.inkwell.core.model.BrushPreset
import com.inkwell.core.model.CanvasSpec
import com.inkwell.core.model.Color as InkwellColor
import com.inkwell.core.model.LayerStack
import com.inkwell.core.model.PressureCurve
import com.inkwell.core.model.ToolMode
import com.inkwell.core.model.palette.PaletteManager
import com.inkwell.engine.NativeEngineBridge
import com.inkwell.feature.canvas.view.CanvasSurfaceView
import com.inkwell.feature.color.ui.ColorPickerSheet
import com.inkwell.feature.color.ui.EyedropperLoupe
import com.inkwell.feature.layers.ui.LayerDrawer
import kotlin.math.roundToInt

@Composable
fun CanvasScreen(
    spec: CanvasSpec = CanvasSpec.SQUARE_2K,
    onNavigateToGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val engineBridge = remember(spec) {
        NativeEngineBridge(
            width = spec.width,
            height = spec.height,
            tileSize = spec.tileSize
        ).apply {
            initialize()
        }
    }

    DisposableEffect(engineBridge) {
        onDispose {
            engineBridge.close()
        }
    }

    var activePreset by remember { mutableStateOf(BrushPreset.STUDIO_BRUSH_PEN) }
    var activeTool by remember { mutableStateOf(ToolMode.PAINT) }
    var brushSize by remember { mutableFloatStateOf(activePreset.maxSize) }
    var brushOpacity by remember { mutableFloatStateOf(activePreset.baseOpacity) }

    var activeColor by remember { mutableStateOf(InkwellColor.fromRgbInt(30, 30, 30)) }
    var previousColor by remember { mutableStateOf(InkwellColor.fromRgbInt(220, 45, 55)) }
    val paletteManager = remember { PaletteManager() }
    var showColorPicker by remember { mutableStateOf(false) }

    // Eyedropper state
    var isEyedropperActive by remember { mutableStateOf(false) }
    var eyedropperScreenX by remember { mutableFloatStateOf(0f) }
    var eyedropperScreenY by remember { mutableFloatStateOf(0f) }
    var eyedropperSampledColor by remember { mutableStateOf(InkwellColor.BLACK) }
    var eyedropperGridBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Selection & Transform state
    var isSelectionActive by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(SelectionMode.RECTANGLE) }
    var hasSelection by remember { mutableStateOf(false) }

    var isTransformActive by remember { mutableStateOf(false) }
    var transformBounds by remember { mutableStateOf(intArrayOf(0, 0, spec.width, spec.height)) }

    // ColorDrop Flood Fill state
    var isColorDropActive by remember { mutableStateOf(false) }
    var colorDropTolerance by remember { mutableFloatStateOf(0.20f) }

    // Export state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    var layerStack by remember { mutableStateOf(LayerStack()) }
    var showLayerDrawer by remember { mutableStateOf(false) }

    // Ergonomics, HUD visibility & Settings
    var isHudVisible by remember { mutableStateOf(true) }
    var isLeftHanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOnboardingGuide by remember { mutableStateOf(false) }
    var activePressureCurve by remember { mutableStateOf(PressureCurve.LINEAR) }
    var autosaveIntervalSeconds by remember { mutableStateOf(60) }
    val budgetManager = remember { MemoryBudgetManager(MemoryBudgetManager.STANDARD_TABLET_HEAP_MB) }

    // Sync brush and tool settings with native engine
    DisposableEffect(activePreset, activeTool, brushSize, brushOpacity, activeColor) {
        engineBridge.setToolMode(activeTool)
        engineBridge.setBrushParams(
            diameter = brushSize,
            r = activeColor.redInt,
            g = activeColor.greenInt,
            b = activeColor.blueInt,
            a = activeColor.alphaInt,
            opacity = brushOpacity,
            spacing = activePreset.spacing
        )
        engineBridge.setBrushHardness(activePreset.hardness)
        engineBridge.setStreamline(activePreset.streamline)
        onDispose {}
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CanvasSurfaceView(context).apply {
                    this.engineBridge = engineBridge
                    this.onEyedropperUpdate = { x, y, sampled, grid ->
                        isEyedropperActive = true
                        eyedropperScreenX = x
                        eyedropperScreenY = y
                        eyedropperSampledColor = sampled
                        eyedropperGridBytes = grid
                    }
                    this.onEyedropperEnd = { finalColor ->
                        isEyedropperActive = false
                        previousColor = activeColor
                        activeColor = finalColor
                    }
                    this.onFourFingerTapAction = {
                        isHudVisible = !isHudVisible
                    }
                }
            },
            update = { view ->
                view.engineBridge = engineBridge
                view.onEyedropperUpdate = { x, y, sampled, grid ->
                    isEyedropperActive = true
                    eyedropperScreenX = x
                    eyedropperScreenY = y
                    eyedropperSampledColor = sampled
                    eyedropperGridBytes = grid
                }
                view.onEyedropperEnd = { finalColor ->
                    isEyedropperActive = false
                    previousColor = activeColor
                    activeColor = finalColor
                }
                view.onFourFingerTapAction = {
                    isHudVisible = !isHudVisible
                }
            }
        )

        // Top HUD Bar: Canvas specs, tool indicator, layer drawer button
        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .background(Color(0xDD1E1E24), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (onNavigateToGallery != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2C2C35))
                                .clickable { onNavigateToGallery() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .semantics { contentDescription = "Back to Gallery" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("← Gallery", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text(
                        text = "${spec.width}x${spec.height}",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    // Tool Mode selector pills
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2C2C35), RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ToolPill("Paint", activeTool == ToolMode.PAINT) {
                            activeTool = ToolMode.PAINT
                        }
                        ToolPill("Smudge", activeTool == ToolMode.SMUDGE) {
                            activeTool = ToolMode.SMUDGE
                        }
                        ToolPill("Eraser", activeTool == ToolMode.ERASER) {
                            activeTool = ToolMode.ERASER
                        }
                    }

                    Text(
                        text = activePreset.name,
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    // Undo / Redo HUD buttons
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2C2C35), RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { engineBridge.undo() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .semantics { contentDescription = "Undo last stroke" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↶",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { engineBridge.redo() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .semantics { contentDescription = "Redo stroke" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↷",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Selection & Transform Action Buttons
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2C2C35), RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelectionActive) Color(0xFF3D5AFE) else Color.Transparent)
                                .clickable {
                                    isSelectionActive = !isSelectionActive
                                    if (isTransformActive) isTransformActive = false
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .semantics { contentDescription = "Selection Tool" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⬚", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTransformActive) Color(0xFF3D5AFE) else Color.Transparent)
                                .clickable {
                                    if (!isTransformActive) {
                                        if (engineBridge.beginTransform()) {
                                            transformBounds = engineBridge.getTransformBounds() ?: intArrayOf(0, 0, spec.width, spec.height)
                                            isTransformActive = true
                                            isSelectionActive = false
                                        }
                                    } else {
                                        engineBridge.commitTransform()
                                        isTransformActive = false
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .semantics { contentDescription = "Transform Tool" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✥", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Layers button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showLayerDrawer) Color(0xFF3D5AFE) else Color(0xFF2C2C35))
                            .clickable { showLayerDrawer = !showLayerDrawer }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .semantics { contentDescription = "Open Layer Stack" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Layers (${layerStack.layers.size})",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Active Color Swatch Circle Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(activeColor.toPackedRgba()))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showColorPicker = !showColorPicker }
                            .semantics { contentDescription = "Open Color Picker" }
                    )

                    // Export Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2C2C35))
                            .clickable { showExportDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .semantics { contentDescription = "Export Artwork" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↗ Export", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2C2C35))
                            .clickable { showSettingsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .semantics { contentDescription = "Settings and Preferences" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Gestures Guide Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2C2C35))
                            .clickable { showOnboardingGuide = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .semantics { contentDescription = "Gestures and Shortcuts Guide" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Restore HUD Pill (when HUD is hidden in immersive mode)
        if (!isHudVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xAA1E1E24))
                    .clickable { isHudVisible = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Restore Fullscreen HUD" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to show HUD (or 4-finger tap)",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }

        // Ergonomic Slider Panel (Left-handed vs Right-handed placement)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn() + slideInHorizontally { if (isLeftHanded) it else -it },
            exit = fadeOut() + slideOutHorizontally { if (isLeftHanded) it else -it },
            modifier = Modifier
                .align(if (isLeftHanded) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(
                    start = if (!isLeftHanded) 16.dp else 0.dp,
                    end = if (isLeftHanded) 16.dp else 0.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xDD1E1E24), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Size", color = Color.White, fontSize = 11.sp)
                    Text("${brushSize.roundToInt()}px", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 2f..120f,
                        modifier = Modifier.width(120.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF64B5F6)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Opacity", color = Color.White, fontSize = 11.sp)
                    Text("${(brushOpacity * 100).roundToInt()}%", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = brushOpacity,
                        onValueChange = { brushOpacity = it },
                        valueRange = 0.05f..1.0f,
                        modifier = Modifier.width(120.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF64B5F6)
                        )
                    )
                }
            }
        }

        // Right Floating Layer Drawer
        AnimatedVisibility(
            visible = showLayerDrawer,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            LayerDrawer(
                layerStack = layerStack,
                onSelectLayer = { id ->
                    layerStack = layerStack.selectLayer(id)
                    val idx = layerStack.layers.indexOfFirst { it.id == id }
                    if (idx != -1) {
                        engineBridge.setActiveLayer(idx + 1)
                    }
                },
                onAddLayer = {
                    val newName = "Layer ${layerStack.layers.size + 1}"
                    layerStack = layerStack.addLayer(newName)
                    engineBridge.addLayer(newName)
                },
                onDeleteLayer = { id ->
                    val idx = layerStack.layers.indexOfFirst { it.id == id }
                    if (idx != -1 && layerStack.layers.size > 1) {
                        layerStack = layerStack.deleteLayer(id)
                        engineBridge.deleteLayer(idx + 1)
                    }
                },
                onToggleVisibility = { id, visible ->
                    layerStack = layerStack.setVisibility(id, visible)
                    val idx = layerStack.layers.indexOfFirst { it.id == id }
                    if (idx != -1) {
                        engineBridge.setLayerVisible(idx + 1, visible)
                    }
                },
                onUpdateOpacity = { id, opacity ->
                    layerStack = layerStack.setOpacity(id, opacity)
                    val idx = layerStack.layers.indexOfFirst { it.id == id }
                    if (idx != -1) {
                        engineBridge.setLayerOpacity(idx + 1, opacity)
                    }
                },
                onUpdateBlendMode = { id, blendMode ->
                    layerStack = layerStack.setBlendMode(id, blendMode)
                    val idx = layerStack.layers.indexOfFirst { it.id == id }
                    if (idx != -1) {
                        engineBridge.setLayerBlendMode(idx + 1, blendMode.id)
                    }
                },
                onToggleAlphaLock = { id, locked ->
                    layerStack = layerStack.setAlphaLock(id, locked)
                    val idx = layerStack.layers.indexOfFirst { it.id == id }
                    if (idx != -1) {
                        engineBridge.setLayerAlphaLock(idx + 1, locked)
                    }
                },
                onToggleClippingMask = { id, mask ->
                    layerStack = layerStack.setClippingMask(id, mask)
                    val idx = layerStack.layers.indexOfFirst { it.id == id }
                    if (idx != -1) {
                        engineBridge.setLayerClippingMask(idx + 1, mask)
                    }
                },
                onMergeDown = { actualIndex ->
                    layerStack = layerStack.mergeDown(actualIndex)
                    engineBridge.mergeDown(actualIndex)
                },
                onClose = {
                    showLayerDrawer = false
                }
            )
        }

        // Floating Color Picker Sheet
        if (showColorPicker) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 20.dp)
            ) {
                ColorPickerSheet(
                    currentColor = activeColor,
                    previousColor = previousColor,
                    paletteManager = paletteManager,
                    onColorChanged = { newColor ->
                        previousColor = activeColor
                        activeColor = newColor
                    },
                    onClose = { showColorPicker = false }
                )
            }
        }

        // Interactive Eyedropper Magnifying Loupe Overlay
        if (isEyedropperActive) {
            EyedropperLoupe(
                screenX = eyedropperScreenX,
                screenY = eyedropperScreenY,
                sampledColor = eyedropperSampledColor,
                previousColor = activeColor,
                gridPixels = eyedropperGridBytes,
                gridSize = 15
            )
        }

        // Selection Action Toolbar
        if (isSelectionActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                SelectionToolbar(
                    mode = selectionMode,
                    hasSelection = hasSelection,
                    onModeChange = { selectionMode = it },
                    onInvert = {
                        engineBridge.invertSelection()
                        hasSelection = engineBridge.hasSelection()
                    },
                    onClear = {
                        engineBridge.clearSelection()
                        hasSelection = false
                    },
                    onTransform = {
                        isSelectionActive = false
                        if (engineBridge.beginTransform()) {
                            transformBounds = engineBridge.getTransformBounds() ?: intArrayOf(0, 0, spec.width, spec.height)
                            isTransformActive = true
                        }
                    },
                    onClose = { isSelectionActive = false }
                )
            }
        }

        // Transform Tool Overlay
        if (isTransformActive) {
            TransformOverlay(
                boundsX0 = transformBounds[0].toFloat(),
                boundsY0 = transformBounds[1].toFloat(),
                boundsX1 = transformBounds[2].toFloat(),
                boundsY1 = transformBounds[3].toFloat(),
                onTransformUpdate = { dx, dy, sx, sy, rot ->
                    val pivotX = (transformBounds[0] + transformBounds[2]) * 0.5f
                    val pivotY = (transformBounds[1] + transformBounds[3]) * 0.5f
                    engineBridge.updateTransform(dx, dy, sx, sy, rot, pivotX, pivotY)
                },
                onCommit = {
                    engineBridge.commitTransform()
                    isTransformActive = false
                },
                onCancel = {
                    engineBridge.cancelTransform()
                    isTransformActive = false
                }
            )
        }

        // ColorDrop Dynamic Tolerance Bar
        ColorDropThresholdBar(
            isVisible = isColorDropActive,
            tolerance = colorDropTolerance,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                canvasWidth = spec.width,
                canvasHeight = spec.height,
                isExporting = isExporting,
                exportProgress = exportProgress,
                onDismiss = { if (!isExporting) showExportDialog = false },
                onSaveToDevice = { options ->
                    scope.launch {
                        isExporting = true
                        exportProgress = 0f
                        try {
                            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                            val exportFile = File(exportDir, "Artwork_${System.currentTimeMillis()}.${options.format.extension}")
                            FileOutputStream(exportFile).use { fos ->
                                ExportDispatcher.exportCanvas(engineBridge, options, fos) { progress ->
                                    exportProgress = progress
                                }
                            }
                            showExportDialog = false
                        } catch (ignored: Exception) {
                        } finally {
                            isExporting = false
                        }
                    }
                },
                onShareArtwork = { options ->
                    scope.launch {
                        isExporting = true
                        exportProgress = 0f
                        try {
                            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                            val exportFile = File(exportDir, "Artwork_${System.currentTimeMillis()}.${options.format.extension}")
                            FileOutputStream(exportFile).use { fos ->
                                ExportDispatcher.exportCanvas(engineBridge, options, fos) { progress ->
                                    exportProgress = progress
                                }
                            }
                            val shareIntent = ExportDispatcher.dispatchShareSheet(context, exportFile, options.format.mimeType)
                            context.startActivity(shareIntent)
                            showExportDialog = false
                        } catch (ignored: Exception) {
                        } finally {
                            isExporting = false
                        }
                    }
                }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                isLeftHanded = isLeftHanded,
                onLeftHandedChange = { isLeftHanded = it },
                activePressureCurve = activePressureCurve,
                onPressureCurveChange = { curve ->
                    activePressureCurve = curve
                },
                autosaveIntervalSeconds = autosaveIntervalSeconds,
                onAutosaveIntervalChange = { autosaveIntervalSeconds = it },
                budgetManager = budgetManager,
                onShowGesturesGuide = {
                    showSettingsDialog = false
                    showOnboardingGuide = true
                },
                onDismissRequest = { showSettingsDialog = false }
            )
        }

        // Onboarding / Gestures Guide Overlay
        if (showOnboardingGuide) {
            OnboardingOverlay(
                onDismissRequest = { showOnboardingGuide = false }
            )
        }
    }
}

@Composable
private fun ToolPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3D5AFE) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
