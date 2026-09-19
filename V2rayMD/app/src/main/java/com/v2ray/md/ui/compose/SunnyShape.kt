package com.v2ray.md.ui.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

class SunnyShape(
    private val points: Int = 10,
    private val depth: Float = 0.08f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(centerX, centerY)
        val innerRadius = radius * (1f - depth)

        var angle = -Math.PI / 2.0 // Start at top
        val angleIncrement = Math.PI / points

        path.moveTo(
            x = centerX + (radius * cos(angle)).toFloat(),
            y = centerY + (radius * sin(angle)).toFloat()
        )

        for (i in 1..(points * 2)) {
            angle += angleIncrement
            val currentRadius = if (i % 2 == 0) radius else innerRadius
            path.lineTo(
                x = centerX + (currentRadius * cos(angle)).toFloat(),
                y = centerY + (currentRadius * sin(angle)).toFloat()
            )
        }
        path.close()
        return Outline.Generic(path)
    }
}