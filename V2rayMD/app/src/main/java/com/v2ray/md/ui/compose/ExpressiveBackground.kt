package com.v2ray.md.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
    // Blob 3 (Bottom Left): exact very-sunny.svg artwork. The baked points are
    // parsed once and normalized to unit space here with plain moveTo/lineTo
    // (same pipeline as the rendering badge shapes), so position and scale
    // below stay exactly as before.
    val path3 = remember {
        val xs = ArrayList<Float>(320)
        val ys = ArrayList<Float>(320)
        VERY_SUNNY_PATH.split(' ').forEach { token ->
            if (token.startsWith("M") || token.startsWith("L")) {
                val xy = token.drop(1).split(',')
                xs.add(xy[0].toFloat())
                ys.add(xy[1].toFloat())
            }
        }
        val minX = xs.min()
        val maxX = xs.max()
        val minY = ys.min()
        val maxY = ys.max()
        val unit = 2f / maxOf(maxX - minX, maxY - minY)
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        androidx.compose.ui.graphics.Path().apply {
            xs.forEachIndexed { index, x ->
                val px = (x - centerX) * unit
                val py = (ys[index] - centerY) * unit
                if (index == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
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
