package com.awraplay.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.awraplay.player.data.model.RepeatMode
import com.awraplay.player.ui.components.AudioEffectsSheet
import com.awraplay.player.ui.components.AudioSpectrumVisualizer
import com.awraplay.player.ui.components.VocalControlPanel
import com.awraplay.player.ui.components.formatDuration
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraBackgroundDark
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import com.awraplay.player.ui.viewmodel.AwraPlayViewModel

@Composable
fun NowPlayingScreen(
    vm: AwraPlayViewModel,
    onBack: () -> Unit,
    onOpenKaraoke: () -> Unit
) {
    val media by vm.currentMedia.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val positionMs by vm.positionMs.collectAsState()
    val durationMs by vm.durationMs.collectAsState()
    val vocalVolume by vm.vocalVolume.collectAsState()
    val separationState by vm.separationState.collectAsState()
    val shuffle by vm.shuffleEnabled.collectAsState()
    val repeat by vm.repeatMode.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val settings by vm.settings.collectAsState()

    var showEqSheet by remember { mutableStateOf(false) }

    val current = media
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada lagu yang diputar", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val isFav = favoriteIds.contains(current.id)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AwraPrimaryPurple.copy(alpha = 0.25f),
                        AwraBackgroundDark,
                        AwraBackgroundDark
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Kembali", tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AwraSecondaryBlue
                )
                IconButton(onClick = { vm.toggleFavorite(current.id) }) {
                    Icon(
                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) AwraAccentPink else Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Large Neon Halo Album Art
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(AwraPrimaryPurple, AwraSecondaryBlue, AwraAccentPink)),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .background(
                        Brush.radialGradient(listOf(AwraPrimaryPurple.copy(alpha = 0.45f), Color(0xFF181822)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(72.dp))
                AsyncImage(
                    model = current.albumArtUri,
                    contentDescription = current.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(Modifier.height(16.dp))

            // Animated Audio Spectrum Background Element
            AudioSpectrumVisualizer(
                isPlaying = isPlaying,
                barCount = 30,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = current.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = current.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8B8C8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            // Progress Slider
            val maxDur = durationMs.coerceAtLeast(1L).toFloat()
            Slider(
                value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
                onValueChange = { vm.seekTo(it.toLong()) },
                valueRange = 0f..maxDur,
                colors = SliderDefaults.colors(
                    thumbColor = AwraSecondaryBlue,
                    activeTrackColor = AwraPrimaryPurple,
                    inactiveTrackColor = Color(0xFF282838)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(positionMs), fontSize = 12.sp, color = Color(0xFFB8B8C8))
                Text(formatDuration(durationMs), fontSize = 12.sp, color = Color(0xFFB8B8C8))
            }

            Spacer(Modifier.height(8.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        "Shuffle",
                        tint = if (shuffle) AwraSecondaryBlue else Color(0xFF8E8EA0)
                    )
                }
                IconButton(onClick = { vm.skipPrevious() }) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AwraPrimaryPurple, AwraAccentPink))
                        )
                        .clickable { vm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = { vm.skipNext() }) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.cycleRepeatMode() }) {
                    Icon(
                        imageVector = if (repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeat != RepeatMode.OFF) AwraSecondaryBlue else Color(0xFF8E8EA0)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Equalizer + Karaoke Mode Quick Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1F1F2B))
                        .clickable { showEqSheet = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Equalizer, null, tint = AwraSecondaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Equalizer & FX", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AwraAccentPink, AwraPrimaryPurple))
                        )
                        .clickable {
                            vm.enterKaraokeMode()
                            onOpenKaraoke()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Karaoke Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Smart Vocal ON/OFF & Vocal Volume Slider (0% - 100%)
            VocalControlPanel(
                vocalVolume = vocalVolume,
                separationState = separationState,
                onVocalOn = { vm.setVocalOn() },
                onVocalOff = { vm.setVocalOff() },
                onVocalVolumeChange = { vm.setVocalVolume(it) },
                onStartSeparation = { vm.startVocalSeparation(autoActivate = true) }
            )

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showEqSheet) {
        AudioEffectsSheet(
            effectsState = settings.effects,
            bandLabels = vm.effectsController.bandFrequenciesLabel,
            onUpdateEffects = { vm.updateEffects(it) },
            onSelectPreset = { vm.selectEqPreset(it) },
            onDismiss = { showEqSheet = false }
        )
    }
}
