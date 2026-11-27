package com.awraplay.player.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awraplay.player.ui.components.AudioSpectrumVisualizer
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraBackgroundDark
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(500))
        scale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        delay(1100)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AwraPrimaryPurple.copy(alpha = 0.28f),
                        AwraBackgroundDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AwraPrimaryPurple, AwraSecondaryBlue, AwraAccentPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                AudioSpectrumVisualizer(
                    isPlaying = true,
                    barCount = 7,
                    modifier = Modifier
                        .width(66.dp)
                        .height(52.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "AwraPlay",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your Music, Your Voice, Your Karaoke.",
                style = MaterialTheme.typography.bodyMedium,
                color = AwraSecondaryBlue
            )
            Spacer(Modifier.height(28.dp))
            AudioSpectrumVisualizer(
                isPlaying = true,
                barCount = 22,
                modifier = Modifier
                    .width(200.dp)
                    .height(32.dp)
            )
        }
    }
}
