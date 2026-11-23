package com.inkwell.feature.export.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.window.Dialog
import com.inkwell.feature.export.dispatcher.ExportFormat
import com.inkwell.feature.export.dispatcher.ExportOptions
import kotlin.math.roundToInt

@Composable
fun ExportDialog(
    canvasWidth: Int,
    canvasHeight: Int,
    isExporting: Boolean,
    exportProgress: Float,
    onDismiss: () -> Unit,
    onSaveToDevice: (ExportOptions) -> Unit,
    onShareArtwork: (ExportOptions) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var transparentBg by remember { mutableStateOf(false) }
    var jpegQuality by remember { mutableFloatStateOf(90f) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E24))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export Artwork",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.LightGray, fontSize = 12.sp)
                    }
                }

                Text(
                    text = "$canvasWidth x $canvasHeight px",
                    color = Color(0xFF64B5F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Format Selector Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF282832), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormatPill("PNG", selectedFormat == ExportFormat.PNG, Modifier.weight(1f)) {
                        selectedFormat = ExportFormat.PNG
                    }
                    FormatPill("JPEG", selectedFormat == ExportFormat.JPEG, Modifier.weight(1f)) {
                        selectedFormat = ExportFormat.JPEG
                    }
                }

                // Options based on format
                if (selectedFormat == ExportFormat.PNG) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Transparent Background", color = Color.White, fontSize = 13.sp)
                            Text("Omit white canvas fill", color = Color.Gray, fontSize = 10.sp)
                        }

                        Switch(
                            checked = transparentBg,
                            onCheckedChange = { transparentBg = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF3D5AFE)
                            )
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("JPEG Quality", color = Color.White, fontSize = 13.sp)
                            Text("${jpegQuality.roundToInt()}%", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = jpegQuality,
                            onValueChange = { jpegQuality = it },
                            valueRange = 50f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF3D5AFE)
                            )
                        )
                    }
                }

                // Progress Bar (if exporting)
                if (isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2C2C35))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(exportProgress.coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF3D5AFE))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val currentOptions = ExportOptions(
                        format = selectedFormat,
                        transparentBackground = if (selectedFormat == ExportFormat.PNG) transparentBg else false,
                        jpegQuality = jpegQuality.roundToInt()
                    )

                    // Share Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2C2C35))
                            .clickable(enabled = !isExporting) { onShareArtwork(currentOptions) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Share", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Save Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3D5AFE))
                            .clickable(enabled = !isExporting) { onSaveToDevice(currentOptions) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save File", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3D5AFE) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
