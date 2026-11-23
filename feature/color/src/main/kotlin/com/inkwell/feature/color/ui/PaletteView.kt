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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

@Composable
fun PaletteView(
    paletteManager: PaletteManager,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val activePalette = remember(paletteManager.activePaletteId, refreshTrigger) {
        paletteManager.activePalette
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Palette Header / Dropdown Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ComposeColor(0xFF2C2C35))
                        .clickable { expandedDropdown = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = activePalette.name,
                        color = ComposeColor.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "▼", color = ComposeColor.LightGray, fontSize = 9.sp)
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.background(ComposeColor(0xFF23232B))
                ) {
                    paletteManager.palettes.forEach { palette ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = palette.name,
                                    color = if (palette.id == activePalette.id) ComposeColor(0xFF64B5F6) else ComposeColor.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (palette.id == activePalette.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                paletteManager.selectPalette(palette.id)
                                refreshTrigger++
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Quick Add Current Color Swatch Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ComposeColor(0xFF3D5AFE))
                    .clickable {
                        paletteManager.addSwatch(activePalette.id, currentColor)
                        refreshTrigger++
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Add Swatch",
                    color = ComposeColor.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Swatch Grid (5 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(ComposeColor(0xFF1E1E24), RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            itemsIndexed(activePalette.swatches) { index, swatchColor ->
                val isSelected = swatchColor.toHex() == currentColor.toHex()
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ComposeColor(swatchColor.toPackedRgba()))
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, ComposeColor.White, RoundedCornerShape(6.dp))
                            } else {
                                Modifier.border(1.dp, ComposeColor(0x33FFFFFF), RoundedCornerShape(6.dp))
                            }
                        )
                        .clickable { onColorSelected(swatchColor) }
                )
            }
        }
    }
}
