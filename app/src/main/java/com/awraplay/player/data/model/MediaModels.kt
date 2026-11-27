package com.awraplay.player.data.model

import android.net.Uri

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class VocalMode {
    VOCAL_ON,
    VOCAL_OFF,
    KARAOKE
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val path: String,
    val folderName: String,
    val folderPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mediaType: MediaType,
    val albumArtUri: Uri? = null,
    val width: Int = 0,
    val height: Int = 0
) {
    val isVocalSeparationSupported: Boolean
        get() = durationMs in 3_000L..900_000L // 3 sec to 15 min local files
}

sealed interface SeparationState {
    data object Idle : SeparationState
    data class Separating(val progress: Float, val stage: String) : SeparationState
    data class Ready(
        val mediaId: Long,
        val vocalPath: String,
        val accompanimentPath: String,
        val usedAiModel: Boolean,
        val engineLabel: String
    ) : SeparationState
    data class Unsupported(val reason: String) : SeparationState
    data class Error(val message: String) : SeparationState
}

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

enum class EqPreset(
    val displayName: String,
    val bands10: List<Int> // millibels (-1500..1500)
) {
    FLAT("Flat", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    POP("Pop", listOf(-150, 200, 450, 500, 300, 0, -150, -100, 150, 250)),
    ROCK("Rock", listOf(500, 350, 150, -100, -200, 50, 250, 450, 550, 600)),
    JAZZ("Jazz", listOf(350, 250, 100, 200, -150, -150, 0, 200, 350, 400)),
    CLASSICAL("Classical", listOf(450, 350, 200, 100, -100, -100, 0, 200, 350, 450)),
    EDM("EDM", listOf(650, 550, 300, 0, -200, 0, 250, 450, 550, 650)),
    ACOUSTIC("Acoustic", listOf(350, 250, 150, 100, 200, 150, 250, 350, 300, 200)),
    KARAOKE("Karaoke", listOf(250, 200, 100, -150, -250, -200, 100, 300, 400, 450)),
    CUSTOM("Custom", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
}

data class AudioEffectsState(
    val enabled: Boolean = true,
    val selectedPreset: EqPreset = EqPreset.FLAT,
    val bandLevels: List<Int> = EqPreset.FLAT.bands10,
    val bassBoostStrength: Int = 0,     // 0..1000
    val trebleStrength: Int = 0,        // 0..1000
    val reverbPreset: Int = 0,          // 0..6 (PresetReverb)
    val virtualizerStrength: Int = 0,   // 0..1000
    val surroundSupported: Boolean = false,
    val balance: Float = 0f,            // -1f (Left) .. +1f (Right)
    val masterVolume: Float = 1f        // 0f .. 1f
)

data class FolderGroup(
    val name: String,
    val path: String,
    val itemCount: Int,
    val items: List<MediaItem>
)
