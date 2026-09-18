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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

@Composable
fun ExpressiveBackground(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    // Increase alpha to make shapes pop more
    val blobColor1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    val blobColor2 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
    val blobColor3 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)

    val infiniteTransition = rememberInfiniteTransition(label = "blobTransition")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blobMorph"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blobRotation"
    )

    val shapeA = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 5,
            innerRadius = 0.5f,
            rounding = CornerRounding(0.3f),
            innerRounding = CornerRounding(0.3f)
        )
    }

    val shapeB = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 6,
            innerRadius = 0.6f,
            rounding = CornerRounding(0.4f),
            innerRounding = CornerRounding(0.4f)
        )
    }

    val shapeCookie = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.85f,
            rounding = CornerRounding(0.15f),
            innerRounding = CornerRounding(0.15f)
        )
    }

    val morph1 = remember(shapeA, shapeB) { Morph(shapeA, shapeB) }
    val morph2 = remember(shapeB, shapeCookie) { Morph(shapeB, shapeCookie) }
    val morph3 = remember(shapeCookie, shapeA) { Morph(shapeCookie, shapeA) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .alpha(1f) // Fully opaque here, controlled by the colors above
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val scale = minOf(canvasWidth, canvasHeight) * 0.85f

            val path1 = morph1.toPath(progress).asComposePath()
            
            translate(left = canvasWidth * 0.3f, top = canvasHeight * 0.2f) {
                val matrix = Matrix()
                matrix.scale(scale, scale)
                matrix.rotateZ(rotation)
                path1.transform(matrix)
                
                drawPath(path = path1, color = blobColor1)
            }

            val path2 = morph2.toPath(1f - progress).asComposePath()
            
            translate(left = canvasWidth * 0.7f, top = canvasHeight * 0.8f) {
                val matrix = Matrix()
                matrix.scale(scale * 0.9f, scale * 0.9f)
                matrix.rotateZ(-rotation * 1.2f)
                path2.transform(matrix)
                
                drawPath(path = path2, color = blobColor2)
            }

            val path3 = morph3.toPath(Math.abs(progress - 0.5f) * 2f).asComposePath()
            
            translate(left = canvasWidth * 0.5f, top = canvasHeight * 0.5f) {
                val matrix = Matrix()
                matrix.scale(scale * 0.7f, scale * 0.7f)
                matrix.rotateZ(rotation * 0.5f)
                path3.transform(matrix)
                
                drawPath(path = path3, color = blobColor3)
            }
        }
    }
}
