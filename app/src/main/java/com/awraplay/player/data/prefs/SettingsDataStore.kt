package com.awraplay.player.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.awraplay.player.data.model.AudioEffectsState
import com.awraplay.player.data.model.EqPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "awraplay_settings")

data class AppSettings(
    val isDarkTheme: Boolean = true,
    val vocalVolume: Float = 1.0f,
    val preferAiOnnx: Boolean = true,
    val autoSeparateInKaraoke: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultVideoPip: Boolean = true,
    val activeModelName: String = "UVR-MDX-NET-Voc_FT.onnx",
    val effects: AudioEffectsState = AudioEffectsState()
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val VOCAL_VOLUME = floatPreferencesKey("vocal_volume")
        val PREFER_AI_ONNX = booleanPreferencesKey("prefer_ai_onnx")
        val AUTO_SEPARATE_KARAOKE = booleanPreferencesKey("auto_separate_karaoke")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val VIDEO_PIP = booleanPreferencesKey("video_pip")
        val ACTIVE_MODEL_NAME = stringPreferencesKey("active_model_name")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val TREBLE = intPreferencesKey("treble")
        val REVERB = intPreferencesKey("reverb")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val BALANCE = floatPreferencesKey("balance")
        val MASTER_VOLUME = floatPreferencesKey("master_volume")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val presetName = prefs[Keys.EQ_PRESET] ?: EqPreset.FLAT.name
        val preset = EqPreset.entries.find { it.name == presetName } ?: EqPreset.FLAT
        val bandsStr = prefs[Keys.EQ_BANDS]
        val bands = bandsStr?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 10 } ?: preset.bands10

        AppSettings(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            vocalVolume = prefs[Keys.VOCAL_VOLUME] ?: 1.0f,
            preferAiOnnx = prefs[Keys.PREFER_AI_ONNX] ?: true,
            autoSeparateInKaraoke = prefs[Keys.AUTO_SEPARATE_KARAOKE] ?: true,
            defaultPlaybackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            defaultVideoPip = prefs[Keys.VIDEO_PIP] ?: true,
            activeModelName = prefs[Keys.ACTIVE_MODEL_NAME] ?: "UVR-MDX-NET-Voc_FT.onnx",
            effects = AudioEffectsState(
                enabled = prefs[Keys.EQ_ENABLED] ?: true,
                selectedPreset = preset,
                bandLevels = bands,
                bassBoostStrength = prefs[Keys.BASS_BOOST] ?: 0,
                trebleStrength = prefs[Keys.TREBLE] ?: 0,
                reverbPreset = prefs[Keys.REVERB] ?: 0,
                virtualizerStrength = prefs[Keys.VIRTUALIZER] ?: 0,
                balance = prefs[Keys.BALANCE] ?: 0f,
                masterVolume = prefs[Keys.MASTER_VOLUME] ?: 1f
            )
        )
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun setVocalVolume(volume: Float) {
        context.dataStore.edit { it[Keys.VOCAL_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setPreferAiOnnx(prefer: Boolean) {
        context.dataStore.edit { it[Keys.PREFER_AI_ONNX] = prefer }
    }

    suspend fun setAutoSeparateInKaraoke(auto: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SEPARATE_KARAOKE] = auto }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun setDefaultVideoPip(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIDEO_PIP] = enabled }
    }

    suspend fun setActiveModelName(name: String) {
        context.dataStore.edit { it[Keys.ACTIVE_MODEL_NAME] = name }
    }

    suspend fun saveEffects(effects: AudioEffectsState) {
        context.dataStore.edit {
            it[Keys.EQ_ENABLED] = effects.enabled
            it[Keys.EQ_PRESET] = effects.selectedPreset.name
            it[Keys.EQ_BANDS] = effects.bandLevels.joinToString(",")
            it[Keys.BASS_BOOST] = effects.bassBoostStrength
            it[Keys.TREBLE] = effects.trebleStrength
            it[Keys.REVERB] = effects.reverbPreset
            it[Keys.VIRTUALIZER] = effects.virtualizerStrength
            it[Keys.BALANCE] = effects.balance
            it[Keys.MASTER_VOLUME] = effects.masterVolume
        }
    }
}
