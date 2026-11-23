package com.inkwell.feature.color.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkwell.core.model.Color
import com.inkwell.core.model.palette.PaletteManager

enum class ColorPickerTab {
    DISC, SLIDERS, VALUES, PALETTES
}

/**
 * Professional floating color picker panel supporting Disc, Sliders, Values,
 * and custom Swatch Palettes.
 */
@Composable
fun ColorPickerSheet(
    currentColor: Color,
    previousColor: Color,
    paletteManager: PaletteManager,
    onColorChanged: (Color) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ColorPickerTab.DISC) }

    Box(
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ComposeColor(0xFA1E1E24))
            .border(1.dp, ComposeColor(0x33FFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: Dual Color Comparison Swatch (Previous vs Current)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color",
                    color = ComposeColor.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Comparison Swatch Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeColor(0xFF2C2C35))
                        .border(1.dp, ComposeColor(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    // Previous color (tap to revert)
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 24.dp)
                            .background(ComposeColor(previousColor.toPackedRgba()))
                            .clickable { onColorChanged(previousColor) }
                    )
                    // Current active color
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 24.dp)
                            .background(ComposeColor(currentColor.toPackedRgba()))
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(0xFF2C2C35))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", color = ComposeColor.LightGray, fontSize = 11.sp)
                }
            }

            // Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeColor(0xFF282832), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabPill("Disc", activeTab == ColorPickerTab.DISC) { activeTab = ColorPickerTab.DISC }
                TabPill("Sliders", activeTab == ColorPickerTab.SLIDERS) { activeTab = ColorPickerTab.SLIDERS }
                TabPill("Values", activeTab == ColorPickerTab.VALUES) { activeTab = ColorPickerTab.VALUES }
                TabPill("Palettes", activeTab == ColorPickerTab.PALETTES) { activeTab = ColorPickerTab.PALETTES }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    ColorPickerTab.DISC -> {
                        ColorWheel(
                            color = currentColor,
                            onColorChanged = onColorChanged
                        )
                    }
                    ColorPickerTab.SLIDERS -> {
                        ColorSliders(
                            color = currentColor,
                            onColorChanged = onColorChanged
                        )
                    }
                    ColorPickerTab.VALUES -> {
                        ColorValues(
                            color = currentColor,
                            onColorChanged = onColorChanged
                        )
                    }
                    ColorPickerTab.PALETTES -> {
                        PaletteView(
                            paletteManager = paletteManager,
                            currentColor = currentColor,
                            onColorSelected = onColorChanged
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Bottom Persistent Quick-Access Swatch Bar (from active palette)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ComposeColor(0xFF282832))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val swatches = paletteManager.activePalette.swatches.take(8)
                swatches.forEach { swatch ->
                    val isSelected = swatch.toHex() == currentColor.toHex()
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ComposeColor(swatch.toPackedRgba()))
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, ComposeColor.White, CircleShape)
                                } else {
                                    Modifier.border(1.dp, ComposeColor(0x33FFFFFF), CircleShape)
                                }
                            )
                            .clickable { onColorChanged(swatch) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ComposeColor(0xFF3D5AFE) else ComposeColor.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) ComposeColor.White else ComposeColor.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
