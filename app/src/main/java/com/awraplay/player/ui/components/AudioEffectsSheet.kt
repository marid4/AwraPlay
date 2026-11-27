package com.awraplay.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awraplay.player.data.model.AudioEffectsState
import com.awraplay.player.data.model.EqPreset
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    effectsState: AudioEffectsState,
    bandLabels: List<String>,
    onUpdateEffects: ((AudioEffectsState) -> AudioEffectsState) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reverbNames = listOf("Off", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header + Master Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AwraPlay Audio FX",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "10-Band Equalizer & Efek Ruang",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = effectsState.enabled,
                    onCheckedChange = { enabled -> onUpdateEffects { it.copy(enabled = enabled) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AwraPrimaryPurple
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            // Preset Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqPreset.entries.filter { it != EqPreset.CUSTOM }.forEach { preset ->
                    val selected = effectsState.selectedPreset == preset
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AwraPrimaryPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 10-Band Vertical Equalizer Card (matching reference design)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bandLabels.forEachIndexed { idx, label ->
                        val levelMb = effectsState.bandLevels.getOrElse(idx) { 0 }
                        val dbText = String.format("%+.1f", levelMb / 100f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = dbText,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            VerticalSlider(
                                value = levelMb.toFloat(),
                                valueRange = -1500f..1500f,
                                onValueChange = { newVal ->
                                    val updatedBands = effectsState.bandLevels.toMutableList()
                                    if (idx < updatedBands.size) {
                                        updatedBands[idx] = newVal.toInt()
                                    }
                                    onUpdateEffects {
                                        it.copy(
                                            selectedPreset = EqPreset.CUSTOM,
                                            bandLevels = updatedBands
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(140.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2x2 Effect Cards (Bass Boost, Treble Control, Reverb, Surround)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EffectCard(
                    title = "Bass Boost",
                    valueLabel = "${effectsState.bassBoostStrength / 10}%",
                    progress = effectsState.bassBoostStrength / 1000f,
                    accentColor = AwraPrimaryPurple,
                    onValueChange = { v -> onUpdateEffects { it.copy(bassBoostStrength = (v * 1000).toInt()) } },
                    modifier = Modifier.weight(1f)
                )
                EffectCard(
                    title = "Treble Control",
                    valueLabel = "${effectsState.trebleStrength / 10}%",
                    progress = effectsState.trebleStrength / 1000f,
                    accentColor = AwraSecondaryBlue,
                    onValueChange = { v -> onUpdateEffects { it.copy(trebleStrength = (v * 1000).toInt()) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EffectCard(
                    title = "Surround",
                    valueLabel = if (effectsState.surroundSupported) "${effectsState.virtualizerStrength / 10}%" else "${effectsState.virtualizerStrength / 10}% (Auto)",
                    progress = effectsState.virtualizerStrength / 1000f,
                    accentColor = AwraAccentPink,
                    onValueChange = { v -> onUpdateEffects { it.copy(virtualizerStrength = (v * 1000).toInt()) } },
                    modifier = Modifier.weight(1f)
                )

                // Reverb Cycle Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .clickable {
                            val next = (effectsState.reverbPreset + 1) % reverbNames.size
                            onUpdateEffects { it.copy(reverbPreset = next) }
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Reverb",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = reverbNames.getOrElse(effectsState.reverbPreset) { "Off" },
                            style = MaterialTheme.typography.labelMedium,
                            color = AwraSecondaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ketuk untuk ubah",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Master Volume & Left-Right Balance
            Text(
                text = "Volume Master (${(effectsState.masterVolume * 100).toInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = effectsState.masterVolume,
                onValueChange = { v -> onUpdateEffects { it.copy(masterVolume = v) } },
                colors = SliderDefaults.colors(
                    thumbColor = AwraSecondaryBlue,
                    activeTrackColor = AwraPrimaryPurple
                )
            )

            val balLabel = when {
                effectsState.balance < -0.05f -> "Kiri ${(-effectsState.balance * 100).toInt()}%"
                effectsState.balance > 0.05f -> "Kanan ${(effectsState.balance * 100).toInt()}%"
                else -> "Seimbang (Center)"
            }
            Text(
                text = "Balance L/R ($balLabel)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = effectsState.balance,
                valueRange = -1f..1f,
                onValueChange = { b -> onUpdateEffects { it.copy(balance = b) } },
                colors = SliderDefaults.colors(
                    thumbColor = AwraAccentPink,
                    activeTrackColor = AwraSecondaryBlue
                )
            )
        }
    }
}

@Composable
private fun EffectCard(
    title: String,
    valueLabel: String,
    progress: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Slider(
                value = progress,
                onValueChange = onValueChange,
                modifier = Modifier.height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = AwraPrimaryPurple,
            inactiveTrackColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .rotate(270f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = -(placeable.width - placeable.height) / 2,
                        y = (placeable.width - placeable.height) / 2
                    )
                }
            }
    )
}
