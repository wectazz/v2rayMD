package com.v2ray.md.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

@Composable
fun ExpressiveBackground(modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    // Darker blobs as requested (0.1f alpha)
    val blobColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

    // Pre-calculate paths once to avoid allocations
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
    // Blob 3 (Bottom Left): exact very-sunny.svg artwork, normalized to unit
    // space so position and scale below stay exactly as before.
    val path3 = remember {
        PathParser().parsePathString(VERY_SUNNY_PATH).toPath().apply {
            val bounds = getBounds()
            val unit = 2f / maxOf(bounds.width, bounds.height)
            transform(
                Matrix().apply {
                    this[0, 0] = unit
                    this[1, 1] = unit
                    this[0, 3] = -bounds.center.x * unit
                    this[1, 3] = -bounds.center.y * unit
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = minOf(canvasWidth, canvasHeight) * 0.45f

            // Blob 1: 9-sided cookie (Top Left)
            translate(left = canvasWidth * 0.15f, top = canvasHeight * 0.15f) {
                scale(radius, radius, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(path = path1, color = blobColor)
                }
            }

            // Blob 2: pentagon (Middle Right)
            translate(left = canvasWidth * 0.85f, top = canvasHeight * 0.5f) {
                scale(radius * 0.9f, radius * 0.9f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(path = path2, color = blobColor)
                }
            }

            // Blob 3: very sunny (12-sided) (Bottom Left)
            translate(left = canvasWidth * 0.2f, top = canvasHeight * 0.85f) {
                scale(radius * 1.1f, radius * 1.1f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                    drawPath(path = path3, color = blobColor)
                }
            }
        }
    }
}
