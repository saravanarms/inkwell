package com.inkwell.feature.canvas.ui

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.inkwell.core.memory.DeviceRamTier
import com.inkwell.core.memory.MemoryBudgetManager
import com.inkwell.core.model.PressureCurve

/**
 * Settings and Preferences Dialog configuring ergonomics, pressure response,
 * autosave frequency, and real-time hardware memory diagnostics.
 */
@Composable
fun SettingsDialog(
    isLeftHanded: Boolean,
    onLeftHandedChange: (Boolean) -> Unit,
    activePressureCurve: PressureCurve,
    onPressureCurveChange: (PressureCurve) -> Unit,
    autosaveIntervalSeconds: Int,
    onAutosaveIntervalChange: (Int) -> Unit,
    budgetManager: MemoryBudgetManager,
    onShowGesturesGuide: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .border(1.dp, Color(0xFF38383A), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings & Preferences",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2E))
                            .clickable(onClick = onDismissRequest)
                            .semantics { contentDescription = "Close Settings" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp)
                    }
                }

                // 1. Ergonomics: Left-Handed Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Left-Handed Mode",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Moves size/opacity sliders to right side",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isLeftHanded,
                        onCheckedChange = onLeftHandedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0A84FF),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF3A3A3C)
                        ),
                        modifier = Modifier.semantics { contentDescription = "Left-handed mode toggle" }
                    )
                }

                // 2. Stylus Pressure Curve Presets
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Stylus Pressure Response",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PressureCurveChip("Linear", activePressureCurve == PressureCurve.LINEAR) {
                            onPressureCurveChange(PressureCurve.LINEAR)
                        }
                        PressureCurveChip("Soft", activePressureCurve == PressureCurve.SOFT) {
                            onPressureCurveChange(PressureCurve.SOFT)
                        }
                        PressureCurveChip("Firm", activePressureCurve == PressureCurve.FIRM) {
                            onPressureCurveChange(PressureCurve.FIRM)
                        }
                    }
                }

                // 3. Autosave Interval Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Autosave Frequency",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutosaveChip("30s", autosaveIntervalSeconds == 30) { onAutosaveIntervalChange(30) }
                        AutosaveChip("60s (Default)", autosaveIntervalSeconds == 60) { onAutosaveIntervalChange(60) }
                        AutosaveChip("120s", autosaveIntervalSeconds == 120) { onAutosaveIntervalChange(120) }
                    }
                }

                // 4. Hardware Memory & RAM Diagnostics
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF242426), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "System Diagnostics",
                        color = Color(0xFF64B5F6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DiagnosticRow("Hardware Tier", budgetManager.ramTier.displayName)
                    DiagnosticRow("Large Heap Class", "${budgetManager.largeHeapClassMb} MB")
                    DiagnosticRow("Max Canvas Dimension", "${budgetManager.maxAllowedDimension} × ${budgetManager.maxAllowedDimension} px")
                    DiagnosticRow("Safety Ceiling", "${budgetManager.maxProcessSafetyCeilingBytes / (1024 * 1024)} MB (75% Heap)")
                }

                // 5. Onboarding / Gestures Guide Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF38383A))
                        .clickable(onClick = onShowGesturesGuide)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📖 Gestures & Shortcuts Guide",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PressureCurveChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0A84FF) else Color(0xFF3A3A3C))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .semantics { contentDescription = "Pressure curve $name" },
        contentAlignment = Alignment.Center
    ) {
        Text(text = name, color = Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AutosaveChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0A84FF) else Color(0xFF3A3A3C))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics { contentDescription = "Autosave interval $label" },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
