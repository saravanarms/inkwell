package com.inkwell.feature.color.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkwell.core.model.Color
import kotlin.math.roundToInt

@Composable
fun ColorValues(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hexText by remember(color) { mutableStateOf(color.toHex(includeAlpha = false)) }
    val hsv = remember(color) { color.toHsv() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hex Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ComposeColor(0xFF2C2C35))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HEX",
                color = ComposeColor.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            BasicTextField(
                value = hexText,
                onValueChange = { input ->
                    hexText = input
                    try {
                        val parsed = Color.fromHex(input)
                        onColorChanged(parsed)
                    } catch (ignored: Exception) {
                    }
                },
                textStyle = TextStyle(
                    color = ComposeColor.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                singleLine = true,
                cursorBrush = SolidColor(ComposeColor(0xFF64B5F6)),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )
        }

        // RGB Breakdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ValueCard("R", "${color.redInt}", ComposeColor(0xFFFF5252), Modifier.weight(1f))
            ValueCard("G", "${color.greenInt}", ComposeColor(0xFF69F0AE), Modifier.weight(1f))
            ValueCard("B", "${color.blueInt}", ComposeColor(0xFF448AFF), Modifier.weight(1f))
        }

        // HSV Breakdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ValueCard("H", "${hsv[0].roundToInt()}°", ComposeColor(0xFFFFD740), Modifier.weight(1f))
            ValueCard("S", "${(hsv[1] * 100).roundToInt()}%", ComposeColor(0xFFE040FB), Modifier.weight(1f))
            ValueCard("V", "${(hsv[2] * 100).roundToInt()}%", ComposeColor.White, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ValueCard(
    label: String,
    value: String,
    accentColor: ComposeColor,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ComposeColor(0xFF23232B))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = ComposeColor.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
