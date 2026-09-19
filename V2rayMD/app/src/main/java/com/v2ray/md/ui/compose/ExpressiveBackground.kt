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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

@Composable
fun ExpressiveBackground(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    // Darker blobs as requested (0.1f alpha instead of 0.2f)
    val blobColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

    val infiniteTransition = rememberInfiniteTransition(label = "blobTransition")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blobRotation"
    )

    // Pre-calculate paths once to avoid allocations and lag during recomposition/drawing
    val path1 = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 9,
            innerRadius = 0.7f,
            rounding = CornerRounding(0.3f)
        ).toPath().asComposePath()
    }
    val path2 = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 5,
            innerRadius = 0.6f,
            rounding = CornerRounding(0.4f)
        ).toPath().asComposePath()
    }
    val path3 = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.8f,
            rounding = CornerRounding(0.2f)
        ).toPath().asComposePath()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = minOf(canvasWidth, canvasHeight) * 0.65f

            // Blob 1: 9-sided cookie
            translate(left = canvasWidth * 0.2f, top = canvasHeight * 0.2f) {
                rotate(rotation) {
                    scale(radius, radius) {
                        drawPath(path = path1, color = blobColor)
                    }
                }
            }

            // Blob 2: pentagon
            translate(left = canvasWidth * 0.8f, top = canvasHeight * 0.75f) {
                rotate(-rotation * 1.5f) {
                    scale(radius * 0.9f, radius * 0.9f) {
                        drawPath(path = path2, color = blobColor)
                    }
                }
            }

            // Blob 3: very sunny (12-sided)
            translate(left = canvasWidth * 0.5f, top = canvasHeight * 0.5f) {
                rotate(rotation * 0.8f) {
                    scale(radius * 1.1f, radius * 1.1f) {
                        drawPath(path = path3, color = blobColor)
                    }
                }
            }
        }
    }
}
