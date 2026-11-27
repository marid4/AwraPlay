package com.awraplay.player.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.awraplay.player.AwraPlayApp
import com.awraplay.player.ai.OnnxModelInfo
import com.awraplay.player.audio.StemSyncAudioEngine
import com.awraplay.player.data.db.FavoriteEntity
import com.awraplay.player.data.db.HistoryEntity
import com.awraplay.player.data.db.PlaylistEntity
import com.awraplay.player.data.db.PlaylistTrackEntity
import com.awraplay.player.data.model.AudioEffectsState
import com.awraplay.player.data.model.EqPreset
import com.awraplay.player.data.model.FolderGroup
import com.awraplay.player.data.model.LyricLine
import com.awraplay.player.data.model.MediaItem
import com.awraplay.player.data.model.MediaType
import com.awraplay.player.data.model.RepeatMode
import com.awraplay.player.data.model.SeparationState
import com.awraplay.player.data.model.SubtitleCue
import com.awraplay.player.data.model.VocalMode
import com.awraplay.player.data.prefs.AppSettings
import com.awraplay.player.player.AwraPlaybackService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AwraPlayViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AwraPlayApp
    private val dao = app.dao
    private val scanner = app.mediaScanner
    private val settingsStore = app.settingsDataStore
    val modelManager = app.modelManager
    val effectsController = app.effectsController

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings()
    )

    private val _audioList = MutableStateFlow<List<MediaItem>>(emptyList())
    val audioList: StateFlow<List<MediaItem>> = _audioList.asStateFlow()

    private val _videoList = MutableStateFlow<List<MediaItem>>(emptyList())
    val videoList: StateFlow<List<MediaItem>> = _videoList.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _currentIndex = MutableStateFlow(-1)
    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Vocal ON/OFF & Vocal Volume Slider (0%..100%)
    private val _vocalVolume = MutableStateFlow(1.0f)
    val vocalVolume: StateFlow<Float> = _vocalVolume.asStateFlow()
    private var lastNonZeroVocalVolume = 1.0f

    private val _vocalMode = MutableStateFlow(VocalMode.VOCAL_ON)
    val vocalMode: StateFlow<VocalMode> = _vocalMode.asStateFlow()

    private val _separationState = MutableStateFlow<SeparationState>(SeparationState.Idle)
    val separationState: StateFlow<SeparationState> = _separationState.asStateFlow()

    private val _stemsActive = MutableStateFlow(false)
    val stemsActive: StateFlow<Boolean> = _stemsActive.asStateFlow()

    // Lyrics & Subtitles
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _subtitles = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val subtitles: StateFlow<List<SubtitleCue>> = _subtitles.asStateFlow()

    private val _subtitlesEnabled = MutableStateFlow(true)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    // Sleep Timer
    private val _sleepTimerRemainingSec = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSec: StateFlow<Long?> = _sleepTimerRemainingSec.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Model Download State
    private val _modelDownloadProgress = MutableStateFlow<Float?>(null)
    val modelDownloadProgress: StateFlow<Float?> = _modelDownloadProgress.asStateFlow()
    private val _modelStatusMessage = MutableStateFlow<String?>(null)
    val modelStatusMessage: StateFlow<String?> = _modelStatusMessage.asStateFlow()

    // Room flows
    val playlists = dao.observePlaylists().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val favoriteIds = dao.observeFavoriteIds().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val historyEntities = dao.observeHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val stemCacheList = dao.observeStemCache().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allMediaMap: StateFlow<Map<Long, MediaItem>> = combine(_audioList, _videoList) { a, v ->
        (a + v).associateBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val folders: StateFlow<List<FolderGroup>> = combine(_audioList, _videoList) { a, v ->
        scanner.groupByFolder(a + v)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Media3 Controller (for Audio service) & Video ExoPlayer accessor
    private var mediaController: MediaController? = null
    private var separationJob: Job? = null

    val stemEngine = StemSyncAudioEngine(
        onSessionReady = { sessionId ->
            effectsController.attachToSession(sessionId)
        },
        onPositionUpdate = { pos ->
            if (_stemsActive.value && _currentMedia.value?.mediaType == MediaType.AUDIO) {
                _positionMs.value = pos
            }
        },
        onCompleted = {
            viewModelScope.launch {
                if (_stemsActive.value && _currentMedia.value?.mediaType == MediaType.AUDIO) {
                    handleTrackEnded()
                }
            }
        }
    )

    // Registered by VideoPlayerScreen when video is active
    var activeVideoPlayer: Player? = null
        set(value) {
            field = value
            if (value != null && _stemsActive.value) {
                value.volume = 0f
            }
        }

    init {
        connectMediaController()
        startPositionTicker()
        viewModelScope.launch {
            settings.collect { s ->
                _vocalVolume.value = s.vocalVolume
                stemEngine.setVocalVolume(s.vocalVolume)
                stemEngine.setMasterAndBalance(s.effects.masterVolume, s.effects.balance)
                effectsController.applyState(s.effects)
            }
        }
    }

    private fun connectMediaController() {
        val token = SessionToken(app, ComponentName(app, AwraPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            runCatching {
                mediaController = future.get().apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            if (_currentMedia.value?.mediaType == MediaType.AUDIO && !_stemsActive.value) {
                                _isPlaying.value = playing
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED && _currentMedia.value?.mediaType == MediaType.AUDIO) {
                                viewModelScope.launch { handleTrackEnded() }
                            }
                        }
                    })
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            while (true) {
                delay(150)
                val media = _currentMedia.value ?: continue
                if (media.mediaType == MediaType.VIDEO) {
                    activeVideoPlayer?.let { vp ->
                        _isPlaying.value = vp.isPlaying
                        val pos = vp.currentPosition.coerceAtLeast(0L)
                        _positionMs.value = pos
                        val dur = vp.duration.takeIf { it > 0 } ?: media.durationMs
                        _durationMs.value = dur
                        if (_stemsActive.value) {
                            if (vp.isPlaying) {
                                stemEngine.play()
                                stemEngine.syncToVideoPosition(pos)
                            } else {
                                stemEngine.pause()
                            }
                        }
                    }
                } else if (!_stemsActive.value) {
                    mediaController?.let { mc ->
                        _isPlaying.value = mc.isPlaying
                        _positionMs.value = mc.currentPosition.coerceAtLeast(0L)
                        _durationMs.value = mc.duration.takeIf { it > 0 } ?: media.durationMs
                    }
                }
            }
        }
    }

    fun scanAllMedia() {
        viewModelScope.launch {
            _isScanning.value = true
            _audioList.value = scanner.scanAudio()
            _videoList.value = scanner.scanVideos()
            _isScanning.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playMedia(item: MediaItem, queue: List<MediaItem> = listOf(item)) {
        separationJob?.cancel()
        stemEngine.stop()
        _stemsActive.value = false
        _separationState.value = SeparationState.Idle

        _currentQueue.value = queue.ifEmpty { listOf(item) }
        _currentIndex.value = _currentQueue.value.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        _currentMedia.value = item
        _durationMs.value = item.durationMs
        _positionMs.value = 0L

        // Reset vocal to ON if user hasn't explicitly kept Karaoke mode, or keep user's slider
        viewModelScope.launch {
            val prev = dao.getHistory(item.id)
            dao.upsertHistory(
                HistoryEntity(
                    mediaId = item.id,
                    lastPlayedAt = System.currentTimeMillis(),
                    playCount = (prev?.playCount ?: 0) + 1
                )
            )
            _lyrics.value = scanner.loadSidecarLyrics(item.path)
            if (item.mediaType == MediaType.VIDEO) {
                _subtitles.value = scanner.loadSidecarSubtitles(item.path)
            } else {
                _subtitles.value = emptyList()
            }
        }

        if (item.mediaType == MediaType.AUDIO) {
            playWithMediaController(item)
        }
        // Check if stems are already cached or vocalVolume < 0.99f
        viewModelScope.launch {
            val cached = dao.getStemCache(item.id)
            if (cached != null && File(cached.vocalPath).exists() && File(cached.accompanimentPath).exists()) {
                _separationState.value = SeparationState.Ready(
                    mediaId = item.id,
                    vocalPath = cached.vocalPath,
                    accompanimentPath = cached.accompanimentPath,
                    usedAiModel = cached.usedAiModel,
                    engineLabel = cached.engineLabel
                )
                if (_vocalVolume.value < 0.99f) {
                    activateStemEngine(cached.vocalPath, cached.accompanimentPath)
                }
            } else if (_vocalVolume.value < 0.99f && settings.value.autoSeparateInKaraoke) {
                startVocalSeparation(autoActivate = true)
            }
        }
    }

    private fun playWithMediaController(item: MediaItem) {
        val mc = mediaController ?: return
        val m3 = Media3Item.Builder()
            .setUri(item.uri)
            .setMediaId(item.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.artist)
                    .setAlbumTitle(item.album)
                    .setArtworkUri(item.albumArtUri)
                    .build()
            )
            .build()
        mc.setMediaItem(m3)
        mc.volume = settings.value.effects.masterVolume
        mc.setPlaybackSpeed(_playbackSpeed.value)
        mc.prepare()
        mc.play()
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        val media = _currentMedia.value ?: return
        if (media.mediaType == MediaType.VIDEO) {
            activeVideoPlayer?.let { vp ->
                if (vp.isPlaying) {
                    vp.pause()
                    stemEngine.pause()
                    _isPlaying.value = false
                } else {
                    vp.play()
                    if (_stemsActive.value) stemEngine.play()
                    _isPlaying.value = true
                }
            }
        } else if (_stemsActive.value) {
            if (_isPlaying.value) {
                stemEngine.pause()
                _isPlaying.value = false
            } else {
                stemEngine.play()
                _isPlaying.value = true
            }
        } else {
            mediaController?.let { mc ->
                if (mc.isPlaying) mc.pause() else mc.play()
            }
        }
    }

    fun seekTo(ms: Long) {
        val clamped = ms.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        _positionMs.value = clamped
        if (_currentMedia.value?.mediaType == MediaType.VIDEO) {
            activeVideoPlayer?.seekTo(clamped)
            if (_stemsActive.value) stemEngine.seekTo(clamped)
        } else if (_stemsActive.value) {
            stemEngine.seekTo(clamped)
        } else {
            mediaController?.seekTo(clamped)
        }
    }

    fun skipNext() {
        val q = _currentQueue.value
        if (q.isEmpty()) return
        val nextIdx = if (_shuffleEnabled.value && q.size > 1) {
            (q.indices - _currentIndex.value).random()
        } else {
            (_currentIndex.value + 1) % q.size
        }
        playMedia(q[nextIdx], q)
    }

    fun skipPrevious() {
        if (_positionMs.value > 3000L) {
            seekTo(0L)
            return
        }
        val q = _currentQueue.value
        if (q.isEmpty()) return
        val prevIdx = if (_currentIndex.value - 1 < 0) q.lastIndex else _currentIndex.value - 1
        playMedia(q[prevIdx], q)
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> seekTo(0L).also { if (!_isPlaying.value) togglePlayPause() }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                if (_currentIndex.value < _currentQueue.value.lastIndex) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaController?.setPlaybackSpeed(speed)
        activeVideoPlayer?.setPlaybackSpeed(speed)
        stemEngine.setPlaybackSpeed(speed)
    }

    // =========================================================================
    // SMART VOCAL ON/OFF & VOCAL VOLUME SLIDER (0% - 100%)
    // =========================================================================

    fun setVocalOn() {
        val target = if (lastNonZeroVocalVolume > 0.05f) lastNonZeroVocalVolume else 1.0f
        setVocalVolume(target)
    }

    fun setVocalOff() {
        if (_vocalVolume.value > 0.05f) {
            lastNonZeroVocalVolume = _vocalVolume.value
        }
        setVocalVolume(0.0f)
    }

    fun enterKaraokeMode() {
        if (_vocalVolume.value > 0.05f) {
            lastNonZeroVocalVolume = _vocalVolume.value
        }
        _vocalMode.value = VocalMode.KARAOKE
        setVocalVolume(0.0f)
    }

    fun setVocalVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        if (v > 0.05f) lastNonZeroVocalVolume = v
        _vocalVolume.value = v
        _vocalMode.value = when {
            v <= 0.01f -> VocalMode.VOCAL_OFF
            else -> VocalMode.VOCAL_ON
        }
        stemEngine.setVocalVolume(v)
        viewModelScope.launch { settingsStore.setVocalVolume(v) }

        val ready = _separationState.value as? SeparationState.Ready
        if (ready != null) {
            if (!_stemsActive.value && v < 0.99f) {
                activateStemEngine(ready.vocalPath, ready.accompanimentPath)
            }
        } else if (v < 0.99f && _separationState.value !is SeparationState.Separating) {
            startVocalSeparation(autoActivate = true)
        }
    }

    fun startVocalSeparation(autoActivate: Boolean = true) {
        val media = _currentMedia.value ?: return
        separationJob?.cancel()
        separationJob = viewModelScope.launch {
            _separationState.value = SeparationState.Separating(0.02f, "Menyiapkan pemisahan vokal...")
            val result = app.vocalSeparator.separateIfNeeded(
                item = media,
                preferOnnx = settings.value.preferAiOnnx
            ) { progress, stage ->
                _separationState.value = SeparationState.Separating(progress, stage)
            }
            _separationState.value = result
            if (result is SeparationState.Ready && autoActivate) {
                activateStemEngine(result.vocalPath, result.accompanimentPath)
            }
        }
    }

    private fun activateStemEngine(vocalPath: String, accPath: String) {
        val media = _currentMedia.value ?: return
        val currentPos = _positionMs.value
        val wasPlaying = _isPlaying.value

        stemEngine.loadStems(vocalPath, accPath, startMs = currentPos)
        stemEngine.setVocalVolume(_vocalVolume.value)
        stemEngine.setMasterAndBalance(settings.value.effects.masterVolume, settings.value.effects.balance)
        stemEngine.setPlaybackSpeed(_playbackSpeed.value)

        if (media.mediaType == MediaType.AUDIO) {
            mediaController?.pause()
            if (wasPlaying) stemEngine.play()
        } else {
            // Mute video's original audio track; video keeps playing smoothly while stemEngine drives audio
            activeVideoPlayer?.volume = 0f
            if (activeVideoPlayer?.isPlaying == true) stemEngine.play()
        }
        _stemsActive.value = true
    }

    // =========================================================================
    // AUDIO EFFECTS, LYRICS, SUBTITLES, PLAYLISTS, FAVORITES, SLEEP TIMER
    // =========================================================================

    fun updateEffects(transform: (AudioEffectsState) -> AudioEffectsState) {
        val updated = transform(settings.value.effects).copy(
            surroundSupported = effectsController.isSurroundSupported()
        )
        effectsController.applyState(updated)
        stemEngine.setMasterAndBalance(updated.masterVolume, updated.balance)
        if (!_stemsActive.value) {
            mediaController?.volume = updated.masterVolume
            activeVideoPlayer?.volume = updated.masterVolume
        }
        viewModelScope.launch { settingsStore.saveEffects(updated) }
    }

    fun selectEqPreset(preset: EqPreset) {
        updateEffects { it.copy(selectedPreset = preset, bandLevels = preset.bands10) }
    }

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch {
            if (dao.isFavorite(mediaId)) dao.removeFavorite(mediaId)
            else dao.addFavorite(FavoriteEntity(mediaId))
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { dao.insertPlaylist(PlaylistEntity(name = name.trim())) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { dao.deletePlaylist(playlistId) }
    }

    fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { dao.addTrackToPlaylist(PlaylistTrackEntity(playlistId, mediaId)) }
    }

    fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { dao.removeTrackFromPlaylist(playlistId, mediaId) }
    }

    fun observePlaylistTrackIds(playlistId: Long) = dao.observePlaylistTrackIds(playlistId)

    fun importLyricsUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { text ->
                _lyrics.value = scanner.parseLrc(text)
            }
        }
    }

    fun importSubtitleUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { text ->
                _subtitles.value = scanner.parseSrt(text)
                _subtitlesEnabled.value = true
            }
        }
    }

    fun toggleSubtitles() {
        _subtitlesEnabled.value = !_subtitlesEnabled.value
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSec.value = null
            return
        }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60L
            while (remaining > 0) {
                _sleepTimerRemainingSec.value = remaining
                delay(1000)
                remaining--
            }
            _sleepTimerRemainingSec.value = null
            if (_isPlaying.value) togglePlayPause()
        }
    }

    fun downloadOnnxModel(model: OnnxModelInfo) {
        viewModelScope.launch {
            _modelDownloadProgress.value = 0f
            _modelStatusMessage.value = "Mengunduh ${model.displayName}..."
            val res = modelManager.downloadModel(model) { prog, msg ->
                _modelDownloadProgress.value = prog
                _modelStatusMessage.value = msg
            }
            _modelDownloadProgress.value = null
            _modelStatusMessage.value = res.fold(
                onSuccess = {
                    settingsStore.setActiveModelName(it.name)
                    "Model ${it.name} berhasil dipasang!"
                },
                onFailure = { "Gagal mengunduh: ${it.message}" }
            )
        }
    }

    fun importOnnxModel(uri: Uri, name: String?) {
        viewModelScope.launch {
            val res = modelManager.importLocalOnnx(uri, name)
            _modelStatusMessage.value = res.fold(
                onSuccess = {
                    settingsStore.setActiveModelName(it.name)
                    "Model ${it.name} berhasil diimpor!"
                },
                onFailure = { "Gagal mengimpor: ${it.message}" }
            )
        }
    }

    fun clearStemCache() {
        viewModelScope.launch {
            File(app.filesDir, "awraplay_stems").deleteRecursively()
            dao.clearStemCache()
        }
    }

    fun setDarkTheme(dark: Boolean) = viewModelScope.launch { settingsStore.setDarkTheme(dark) }
    fun setPreferAiOnnx(prefer: Boolean) = viewModelScope.launch { settingsStore.setPreferAiOnnx(prefer) }
    fun setAutoSeparateInKaraoke(auto: Boolean) = viewModelScope.launch { settingsStore.setAutoSeparateInKaraoke(auto) }
    fun setDefaultVideoPip(enabled: Boolean) = viewModelScope.launch { settingsStore.setDefaultVideoPip(enabled) }

    override fun onCleared() {
        stemEngine.stop()
        effectsController.release()
        mediaController?.release()
        super.onCleared()
    }
}
