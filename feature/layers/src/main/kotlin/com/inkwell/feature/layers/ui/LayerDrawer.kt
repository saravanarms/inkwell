package com.inkwell.feature.layers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkwell.core.model.BlendMode
import com.inkwell.core.model.Layer
import com.inkwell.core.model.LayerStack
import kotlin.math.roundToInt

@Composable
fun LayerDrawer(
    layerStack: LayerStack,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    onToggleVisibility: (String, Boolean) -> Unit,
    onUpdateOpacity: (String, Float) -> Unit,
    onUpdateBlendMode: (String, BlendMode) -> Unit,
    onToggleAlphaLock: (String, Boolean) -> Unit,
    onToggleClippingMask: (String, Boolean) -> Unit,
    onMergeDown: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(Color(0xF01E1E24), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layers (${layerStack.layers.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddLayer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+ Add", fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C35))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Layers List (Rendered top-to-bottom: reverse order of index)
            val reversedLayers = layerStack.layers.reversed()

            LazyColumn(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(reversedLayers) { visualIndex, layer ->
                    val actualIndex = layerStack.layers.size - 1 - visualIndex
                    val isSelected = layer.id == layerStack.activeLayerId

                    LayerCard(
                        layer = layer,
                        isSelected = isSelected,
                        canMergeDown = actualIndex > 0,
                        onSelect = { onSelectLayer(layer.id) },
                        onToggleVisibility = { onToggleVisibility(layer.id, !layer.isVisible) },
                        onUpdateOpacity = { onUpdateOpacity(layer.id, it) },
                        onUpdateBlendMode = { onUpdateBlendMode(layer.id, it) },
                        onToggleAlphaLock = { onToggleAlphaLock(layer.id, !layer.isAlphaLocked) },
                        onToggleClippingMask = { onToggleClippingMask(layer.id, !layer.isClippingMask) },
                        onMergeDown = { onMergeDown(actualIndex) },
                        onDelete = { onDeleteLayer(layer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerCard(
    layer: Layer,
    isSelected: Boolean,
    canMergeDown: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onUpdateBlendMode: (BlendMode) -> Unit,
    onToggleAlphaLock: () -> Unit,
    onToggleClippingMask: () -> Unit,
    onMergeDown: () -> Unit,
    onDelete: () -> Unit
) {
    var showBlendMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF2A2A38) else Color(0xFF23232C))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) Color(0xFF3D5AFE) else Color(0xFF33333E),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visibility Eye Toggle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (layer.isVisible) Color(0xFF3D5AFE) else Color(0xFF3E3E4A))
                    .clickable(onClick = onToggleVisibility),
                contentAlignment = Alignment.Center
            ) {
                Text(if (layer.isVisible) "👁" else "—", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = layer.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                // Blend mode picker pill
                Box {
                    Text(
                        text = "${layer.blendMode.displayName} ▼",
                        color = Color(0xFF64B5F6),
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { showBlendMenu = true }
                    )

                    DropdownMenu(
                        expanded = showBlendMenu,
                        onDismissRequest = { showBlendMenu = false },
                        modifier = Modifier.background(Color(0xFF23232C))
                    ) {
                        for (mode in BlendMode.entries) {
                            DropdownMenuItem(
                                text = { Text(mode.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    onUpdateBlendMode(mode)
                                    showBlendMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "${(layer.opacity * 100).roundToInt()}%",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.height(10.dp))

            // Opacity Slider
            Slider(
                value = layer.opacity,
                onValueChange = onUpdateOpacity,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF3D5AFE)
                )
            )

            // Layer Modifiers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModifierPill("Alpha Lock", layer.isAlphaLocked, onToggleAlphaLock)
                ModifierPill("Clip Mask", layer.isClippingMask, onToggleClippingMask)
                if (canMergeDown) {
                    ModifierPill("Merge", false, onMergeDown)
                }
                ModifierPill("Delete", false, onDelete)
            }
        }
    }
}

@Composable
private fun ModifierPill(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0xFF3D5AFE) else Color(0xFF33333E))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color.LightGray,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
