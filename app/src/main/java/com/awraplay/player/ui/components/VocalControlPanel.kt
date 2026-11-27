package com.awraplay.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awraplay.player.data.model.SeparationState
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import kotlin.math.roundToInt

@Composable
fun VocalControlPanel(
    vocalVolume: Float,
    separationState: SeparationState,
    onVocalOn: () -> Unit,
    onVocalOff: () -> Unit,
    onVocalVolumeChange: (Float) -> Unit,
    onStartSeparation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVocalOn = vocalVolume > 0.05f
    val pct = (vocalVolume * 100f).roundToInt()

    val onScale by animateFloatAsState(if (isVocalOn) 1.02f else 0.98f, tween(250), label = "onScale")
    val offScale by animateFloatAsState(if (!isVocalOn) 1.02f else 0.98f, tween(250), label = "offScale")
    val onBg by animateColorAsState(
        if (isVocalOn) AwraPrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
        tween(250), label = "onBg"
    )
    val offBg by animateColorAsState(
        if (!isVocalOn) AwraAccentPink else MaterialTheme.colorScheme.surfaceVariant,
        tween(250), label = "offBg"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = AwraSecondaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "SMART VOCAL AI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AwraSecondaryBlue
                    )
                }

                val modeDesc = when {
                    pct == 0 -> "0% • Instrumental / Karaoke"
                    pct <= 30 -> "$pct% • Vokal Sangat Pelan"
                    pct <= 60 -> "$pct% • Seimbang"
                    pct <= 85 -> "$pct% • Vokal Dominan"
                    else -> "$pct% • Vokal Asli"
                }
                Text(
                    text = modeDesc,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (pct == 0) AwraAccentPink else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Big [ VOCAL ON ]  [ VOCAL OFF ] Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(onScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isVocalOn) Brush.horizontalGradient(listOf(AwraPrimaryPurple, AwraSecondaryBlue))
                            else Brush.horizontalGradient(listOf(onBg, onBg))
                        )
                        .clickable { onVocalOn() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Vocal On",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "VOCAL ON",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(offScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (!isVocalOn) Brush.horizontalGradient(listOf(AwraAccentPink, AwraPrimaryPurple))
                            else Brush.horizontalGradient(listOf(offBg, offBg))
                        )
                        .clickable { onVocalOff() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.MicOff,
                            contentDescription = "Vocal Off",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "VOCAL OFF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Vocal Volume Slider 0% - 100%
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Volume Vokal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Slider(
                    value = vocalVolume,
                    onValueChange = onVocalVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = AwraAccentPink,
                        activeTrackColor = AwraPrimaryPurple,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Quick Preset Percentages: 0%, 25%, 50%, 75%, 100%
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0, 25, 50, 75, 100).forEach { presetPct ->
                    val selected = kotlin.math.abs(pct - presetPct) <= 3
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) AwraPrimaryPurple.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) AwraPrimaryPurple else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onVocalVolumeChange(presetPct / 100f) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "$presetPct%",
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) AwraSecondaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // AI Separation Status Indicator
            when (separationState) {
                is SeparationState.Idle -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onStartSeparation() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GraphicEq, null, tint = AwraSecondaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Ketuk untuk pisahkan vokal & musik sekarang",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "PROSES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AwraSecondaryBlue
                        )
                    }
                }
                is SeparationState.Separating -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = separationState.stage,
                                style = MaterialTheme.typography.labelMedium,
                                color = AwraSecondaryBlue
                            )
                            Text(
                                text = "${(separationState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AwraAccentPink
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { separationState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AwraAccentPink,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                is SeparationState.Ready -> {
                    Text(
                        text = "Stem Siap • ${separationState.engineLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AwraSecondaryBlue
                    )
                }
                is SeparationState.Unsupported -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = AwraAccentPink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = separationState.reason,
                            style = MaterialTheme.typography.labelMedium,
                            color = AwraAccentPink
                        )
                    }
                }
                is SeparationState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = AwraAccentPink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = separationState.message,
                            style = MaterialTheme.typography.labelMedium,
                            color = AwraAccentPink
                        )
                    }
                }
            }
        }
    }
}
