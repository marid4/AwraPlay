package com.awraplay.player.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.awraplay.player.ui.components.AudioEffectsSheet
import com.awraplay.player.ui.components.VocalControlPanel
import com.awraplay.player.ui.components.formatDuration
import com.awraplay.player.ui.theme.AwraAccentPink
import com.awraplay.player.ui.theme.AwraPrimaryPurple
import com.awraplay.player.ui.theme.AwraSecondaryBlue
import com.awraplay.player.ui.viewmodel.AwraPlayViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    vm: AwraPlayViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentMedia by vm.currentMedia.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val positionMs by vm.positionMs.collectAsState()
    val durationMs by vm.durationMs.collectAsState()
    val vocalVolume by vm.vocalVolume.collectAsState()
    val separationState by vm.separationState.collectAsState()
    val stemsActive by vm.stemsActive.collectAsState()
    val subtitles by vm.subtitles.collectAsState()
    val subtitlesEnabled by vm.subtitlesEnabled.collectAsState()
    val playbackSpeed by vm.playbackSpeed.collectAsState()
    val settings by vm.settings.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showVocalDrawer by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var gestureHudText by remember { mutableStateOf<String?>(null) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var brightness by remember {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }

    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.importSubtitleUri(it) }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            vm.effectsController.attachToSession(audioSessionId)
        }
    }

    LaunchedEffect(currentMedia?.id) {
        currentMedia?.let { media ->
            exoPlayer.setMediaItem(Media3Item.fromUri(media.uri))
            exoPlayer.volume = if (stemsActive) 0f else settings.effects.masterVolume
            exoPlayer.setPlaybackSpeed(playbackSpeed)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    LaunchedEffect(stemsActive) {
        exoPlayer.volume = if (stemsActive) 0f else settings.effects.masterVolume
    }

    DisposableEffect(Unit) {
        vm.activeVideoPlayer = exoPlayer
        onDispose {
            vm.activeVideoPlayer = null
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val activeSubtitleText = remember(positionMs, subtitles, subtitlesEnabled) {
        if (!subtitlesEnabled) null
        else subtitles.firstOrNull { positionMs in it.startMs..it.endMs }?.text
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2f) {
                            vm.seekTo((positionMs - 10_000L).coerceAtLeast(0L))
                            gestureHudText = "-10 detik"
                        } else {
                            vm.seekTo(positionMs + 10_000L)
                            gestureHudText = "+10 detik"
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { gestureHudText = null },
                    onDragCancel = { gestureHudText = null }
                ) { change, dragAmount ->
                    change.consume()
                    val isVertical = kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)
                    if (isVertical) {
                        val delta = -dragAmount.y / size.height
                        if (change.position.x < size.width / 2f) {
                            // Left side: Brightness
                            brightness = (brightness + delta).coerceIn(0.05f, 1f)
                            activity?.window?.let { win ->
                                val lp: WindowManager.LayoutParams = win.attributes
                                lp.screenBrightness = brightness
                                win.attributes = lp
                            }
                            gestureHudText = "Kecerahan ${(brightness * 100).toInt()}%"
                        } else {
                            // Right side: Volume
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val step = if (delta > 0) 1 else -1
                            if (kotlin.math.abs(dragAmount.y) > 12f) {
                                val next = (curVol + step).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                gestureHudText = "Volume ${(next * 100 / maxVol.coerceAtLeast(1))}%"
                            }
                        }
                    } else {
                        // Horizontal: Seek
                        val seekDeltaMs = (dragAmount.x / size.width * 60_000f).toLong()
                        vm.seekTo(positionMs + seekDeltaMs)
                        gestureHudText = formatDuration(vm.positionMs.value)
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Synced Subtitle Overlay
        if (!activeSubtitleText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showControls) 120.dp else 32.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activeSubtitleText,
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Gesture HUD
        gestureHudText?.let { hud ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Text(
                    text = hud,
                    color = AwraSecondaryBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        // Player Controls Overlay
        AnimatedVisibility(visible = showControls, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, "Kembali", tint = Color.White)
                        }
                        Text(
                            text = currentMedia?.title ?: "Video Player",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        IconButton(onClick = { showVocalDrawer = !showVocalDrawer }) {
                            Icon(
                                Icons.Rounded.Mic,
                                "Vocal ON/OFF",
                                tint = if (vocalVolume < 0.99f) AwraAccentPink else Color.White
                            )
                        }
                        IconButton(onClick = {
                            if (subtitles.isEmpty()) subtitlePicker.launch(arrayOf("*/*"))
                            else vm.toggleSubtitles()
                        }) {
                            Icon(
                                Icons.Rounded.Subtitles,
                                "Subtitle",
                                tint = if (subtitlesEnabled && subtitles.isNotEmpty()) AwraSecondaryBlue else Color.White
                            )
                        }
                        IconButton(onClick = { showEqSheet = true }) {
                            Icon(Icons.Rounded.Equalizer, "Audio FX", tint = Color.White)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                            IconButton(onClick = {
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                                activity.enterPictureInPictureMode(params)
                            }) {
                                Icon(Icons.Rounded.PictureInPictureAlt, "PiP", tint = Color.White)
                            }
                        }
                    }
                }

                // Center Play/Pause Button (Neon Pink per reference)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(AwraAccentPink)
                        .clickable { vm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Bottom Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatDuration(positionMs), color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
                            onValueChange = { vm.seekTo(it.toLong()) },
                            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = AwraAccentPink,
                                activeTrackColor = AwraAccentPink
                            )
                        )
                        Text(formatDuration(durationMs), color = Color.White, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed Cycle
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable {
                                    val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                    vm.setPlaybackSpeed(speeds[nextIdx])
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Speed, null, tint = AwraSecondaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${playbackSpeed}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Inline Vocal Quick Toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (vocalVolume < 0.99f) AwraAccentPink else AwraPrimaryPurple
                                )
                                .clickable { showVocalDrawer = !showVocalDrawer }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Vokal ${(vocalVolume * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = {
                            isFullscreen = !isFullscreen
                            activity?.requestedOrientation = if (isFullscreen) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        }) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Slide-up Vocal Control Panel inside Video Player
        if (showVocalDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Tutup Panel Vokal ✕",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .clickable { showVocalDrawer = false }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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
