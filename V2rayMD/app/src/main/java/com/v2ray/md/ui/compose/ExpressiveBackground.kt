package com.v2ray.md.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveBackground(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    // Use only primary color for a monochromatic monet aesthetic as requested. User requested 20% opacity.
    val blobColor1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val blobColor2 = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val blobColor3 = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    val infiniteTransition = rememberInfiniteTransition(label = "blobTransition")
    
    val progress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobProgress1"
    )

    val progress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobProgress2"
    )
    
    val progress3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobProgress3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val radius = minOf(width, height) * 0.6f

            // Blob 1
            val center1 = Offset(
                x = width * 0.2f + width * 0.6f * progress1,
                y = height * 0.2f + height * 0.4f * progress2
            )
            drawCircle(
                color = blobColor1,
                radius = radius,
                center = center1
            )

            // Blob 2
            val center2 = Offset(
                x = width * 0.8f - width * 0.5f * progress2,
                y = height * 0.7f - height * 0.3f * progress3
            )
            drawCircle(
                color = blobColor2,
                radius = radius * 0.9f,
                center = center2
            )

            // Blob 3
            val center3 = Offset(
                x = width * 0.5f + width * 0.3f * progress3,
                y = height * 0.5f - height * 0.4f * progress1
            )
            drawCircle(
                color = blobColor3,
                radius = radius * 1.1f,
                center = center3
            )
        }
    }
}
