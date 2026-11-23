package com.inkwell.feature.color.ui

import androidx.compose.foundation.background
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
import kotlin.math.roundToInt

private enum class SliderMode {
    RGB, HSV
}

@Composable
fun ColorSliders(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(SliderMode.RGB) }

    val hsv = remember(color) { color.toHsv() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode selector pills
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(ComposeColor(0xFF2C2C35), RoundedCornerShape(12.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModePill("RGB", mode == SliderMode.RGB) { mode = SliderMode.RGB }
            ModePill("HSV", mode == SliderMode.HSV) { mode = SliderMode.HSV }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (mode == SliderMode.RGB) {
            SliderRow(
                label = "R",
                value = color.redInt.toFloat(),
                valueRange = 0f..255f,
                displayValue = "${color.redInt}",
                accentColor = ComposeColor(0xFFFF5252),
                onValueChange = {
                    onColorChanged(Color.fromRgbInt(it.roundToInt(), color.greenInt, color.blueInt, color.alphaInt))
                }
            )

            SliderRow(
                label = "G",
                value = color.greenInt.toFloat(),
                valueRange = 0f..255f,
                displayValue = "${color.greenInt}",
                accentColor = ComposeColor(0xFF69F0AE),
                onValueChange = {
                    onColorChanged(Color.fromRgbInt(color.redInt, it.roundToInt(), color.blueInt, color.alphaInt))
                }
            )

            SliderRow(
                label = "B",
                value = color.blueInt.toFloat(),
                valueRange = 0f..255f,
                displayValue = "${color.blueInt}",
                accentColor = ComposeColor(0xFF448AFF),
                onValueChange = {
                    onColorChanged(Color.fromRgbInt(color.redInt, color.greenInt, it.roundToInt(), color.alphaInt))
                }
            )
        } else {
            SliderRow(
                label = "H",
                value = hsv[0],
                valueRange = 0f..360f,
                displayValue = "${hsv[0].roundToInt()}°",
                accentColor = ComposeColor(0xFFFFD740),
                onValueChange = {
                    onColorChanged(Color.fromHsv(it, hsv[1], hsv[2], color.alpha))
                }
            )

            SliderRow(
                label = "S",
                value = hsv[1] * 100f,
                valueRange = 0f..100f,
                displayValue = "${(hsv[1] * 100f).roundToInt()}%",
                accentColor = ComposeColor(0xFFE040FB),
                onValueChange = {
                    onColorChanged(Color.fromHsv(hsv[0], it / 100f, hsv[2], color.alpha))
                }
            )

            SliderRow(
                label = "V",
                value = hsv[2] * 100f,
                valueRange = 0f..100f,
                displayValue = "${(hsv[2] * 100f).roundToInt()}%",
                accentColor = ComposeColor(0xFFFFFFFF),
                onValueChange = {
                    onColorChanged(Color.fromHsv(hsv[0], hsv[1], it / 100f, color.alpha))
                }
            )
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ComposeColor(0xFF3D5AFE) else ComposeColor.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) ComposeColor.White else ComposeColor.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accentColor: ComposeColor,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(20.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ComposeColor.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = ComposeColor(0xFF3E3E4A)
            )
        )

        Text(
            text = displayValue,
            color = ComposeColor.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(42.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
