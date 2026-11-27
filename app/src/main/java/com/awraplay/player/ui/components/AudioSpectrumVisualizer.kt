package com.awraplay.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioSpectrumVisualizer(
    isPlaying: Boolean,
    barCount: Int = 28,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
) {
    val transition = rememberInfiniteTransition(label = "spectrum")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val totalW = size.width
        val totalH = size.height
        val slotW = totalW / barCount
        val barW = (slotW * 0.58f).coerceAtLeast(3f)
        val brush = Brush.horizontalGradient(
            listOf(AwraPrimaryPurple, AwraSecondaryBlue, AwraAccentPink)
        )

        for (i in 0 until barCount) {
            val norm = i.toFloat() / barCount
            val envelope = sin(norm * PI).toFloat()
            val wave = if (isPlaying) {
                0.22f + 0.78f * abs(sin(phase + i * 0.45f) * cosTerm(phase * 0.7f, i))
            } else {
                0.15f + 0.10f * envelope
            }
            val barH = (totalH * wave * (0.45f + 0.55f * envelope)).coerceAtLeast(6f)
            val x = i * slotW + (slotW - barW) / 2f
            val y = (totalH - barH) / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

private fun cosTerm(p: Float, idx: Int): Float {
    return kotlin.math.cos(p + idx * 0.27f)
}
