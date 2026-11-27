package com.awraplay.player.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.awraplay.player.data.model.MediaType
import com.awraplay.player.ui.components.AudioEffectsSheet
import com.awraplay.player.ui.components.VocalControlPanel
import com.awraplay.player.ui.components.formatDuration
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraBackgroundDark
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import com.awraplay.player.ui.viewmodel.AwraPlayViewModel

@Composable
fun KaraokeScreen(
    vm: AwraPlayViewModel,
    onBack: () -> Unit,
    onOpenVideoPlayer: () -> Unit
) {
    val media by vm.currentMedia.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val positionMs by vm.positionMs.collectAsState()
    val durationMs by vm.durationMs.collectAsState()
    val vocalVolume by vm.vocalVolume.collectAsState()
    val separationState by vm.separationState.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val settings by vm.settings.collectAsState()

    var showEqSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val lrcPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.importLyricsUri(it) }
    }

    val activeLyricIndex = remember(positionMs, lyrics) {
        lyrics.indexOfLast { it.timestampMs <= positionMs }
    }

    LaunchedEffect(activeLyricIndex) {
        if (activeLyricIndex >= 0) {
            runCatching { listState.animateScrollToItem((activeLyricIndex - 1).coerceAtLeast(0)) }
        }
    }

    val isVocalOn = vocalVolume > 0.05f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF4222B8),
                        Color(0xFF24145C),
                        AwraBackgroundDark
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar + Vocal Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "Kembali", tint = Color.White)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isVocalOn) AwraPrimaryPurple else AwraAccentPink
                ) {
                    Text(
                        text = if (isVocalOn) "VOCAL ON (${(vocalVolume * 100).toInt()}%)" else "VOCAL OFF • KARAOKE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Row {
                    IconButton(onClick = { lrcPicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Rounded.Lyrics, "Muat Lirik .lrc", tint = Color.White)
                    }
                    IconButton(onClick = { showEqSheet = true }) {
                        Icon(Icons.Rounded.Equalizer, "Audio FX", tint = Color.White)
                    }
                }
            }

            // Song / Video Card Header
            media?.let { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AwraPrimaryPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (item.mediaType == MediaType.VIDEO) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                            null, tint = Color.White
                        )
                        AsyncImage(
                            model = item.albumArtUri,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.artist,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.mediaType == MediaType.VIDEO) {
                        Button(
                            onClick = onOpenVideoPlayer,
                            colors = ButtonDefaults.buttonColors(containerColor = AwraSecondaryBlue)
                        ) {
                            Text("Video", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Synced Lyrics Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (lyrics.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Lyrics,
                            null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Lirik (.lrc) belum tersedia untuk lagu ini",
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { lrcPicker.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = AwraPrimaryPurple)
                        ) {
                            Text("Pilih File Lirik (.lrc)")
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(lyrics) { idx, line ->
                            val isActive = idx == activeLyricIndex
                            Text(
                                text = line.text,
                                fontSize = if (isActive) 22.sp else 17.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.42f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.seekTo(line.timestampMs) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Progress & Play/Pause Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { vm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF24145C),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
                        onValueChange = { vm.seekTo(it.toLong()) },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = AwraAccentPink,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(positionMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(formatDuration(durationMs), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Main Vocal ON/OFF & 0-100% Slider
            VocalControlPanel(
                vocalVolume = vocalVolume,
                separationState = separationState,
                onVocalOn = { vm.setVocalOn() },
                onVocalOff = { vm.setVocalOff() },
                onVocalVolumeChange = { vm.setVocalVolume(it) },
                onStartSeparation = { vm.startVocalSeparation(autoActivate = true) }
            )
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
