package com.inkwell.feature.canvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Onboarding and Gestures Quick-Start Guide modal presenting
 * the natural multi-touch gestures of Inkwell.
 */
@Composable
fun OnboardingOverlay(
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161618))
                .border(1.dp, Color(0xFF38383A), RoundedCornerShape(24.dp))
                .padding(26.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Welcome to Inkwell",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Master the natural gesture workflow for digital painting",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                // Gesture items list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GestureRow(
                        icon = "✌️",
                        title = "2-Finger Tap",
                        desc = "Instantly undo your last stroke or layer edit"
                    )
                    GestureRow(
                        icon = "🤟",
                        title = "3-Finger Tap",
                        desc = "Redo the previously undone action"
                    )
                    GestureRow(
                        icon = "🖐️",
                        title = "4-Finger Tap",
                        desc = "Toggle distraction-free full-screen immersive canvas"
                    )
                    GestureRow(
                        icon = "🤏",
                        title = "Pinch & Rotate",
                        desc = "Fluidly navigate the canvas with 360° rotation snap"
                    )
                    GestureRow(
                        icon = "👁️",
                        title = "Touch & Hold (250ms)",
                        desc = "Activate 15× magnified reticle eyedropper loupe"
                    )
                }

                // Dismiss Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0A84FF))
                        .clickable(onClick = onDismissRequest)
                        .padding(vertical = 14.dp)
                        .semantics { contentDescription = "Get started and close guide" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Got it! Let's Paint",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureRow(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222226), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C32)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
}
