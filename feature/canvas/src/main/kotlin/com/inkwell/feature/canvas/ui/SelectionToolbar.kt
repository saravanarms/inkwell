package com.inkwell.feature.canvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SelectionMode {
    RECTANGLE, LASSO
}

@Composable
fun SelectionToolbar(
    mode: SelectionMode,
    hasSelection: Boolean,
    onModeChange: (SelectionMode) -> Unit,
    onInvert: () -> Unit,
    onClear: () -> Unit,
    onTransform: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xDD1E1E24))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Selectors
            Row(
                modifier = Modifier
                    .background(Color(0xFF2C2C35), RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ActionPill("Rect", mode == SelectionMode.RECTANGLE) { onModeChange(SelectionMode.RECTANGLE) }
                ActionPill("Lasso", mode == SelectionMode.LASSO) { onModeChange(SelectionMode.LASSO) }
            }

            // Operations
            ActionPill("Invert", false, onClick = onInvert)
            ActionPill("Clear", false, enabled = hasSelection, onClick = onClear)
            ActionPill("Transform", false, enabled = hasSelection, onClick = onTransform)

            // Close
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3D5AFE) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (!enabled) Color(0xFF555560) else if (isSelected) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
