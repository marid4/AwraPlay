package com.awraplay.player.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.awraplay.player.data.model.AudioEffectsState
import kotlin.math.abs

/**
 * Real-time Android AudioFX controller (10-band EQ mapping, BassBoost, Treble,
 * PresetReverb, Virtualizer Surround, and Master/Balance tracking).
 */
class AudioEffectsController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var reverb: PresetReverb? = null
    private var virtualizer: Virtualizer? = null

    private var currentSessionId: Int = 0
    private var lastState: AudioEffectsState = AudioEffectsState()

    val bandFrequenciesLabel = listOf(
        "31Hz", "62Hz", "125Hz", "250Hz", "500Hz",
        "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    )
    private val uiCenterHz = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId
        runCatching {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = lastState.enabled }
        }
        runCatching {
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = lastState.enabled }
        }
        runCatching {
            reverb = PresetReverb(0, audioSessionId).apply { enabled = lastState.enabled }
        }
        runCatching {
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = lastState.enabled }
        }
        applyState(lastState)
    }

    fun isSurroundSupported(): Boolean {
        return runCatching { virtualizer?.strengthSupported == true }.getOrDefault(false)
    }

    fun applyState(state: AudioEffectsState) {
        lastState = state
        val enabled = state.enabled

        // 10-band UI -> device native EQ bands + Treble boost on >= 4kHz
        equalizer?.let { eq ->
            runCatching {
                eq.enabled = enabled
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0].toInt()
                val maxLevel = eq.bandLevelRange[1].toInt()
                val trebleBoostMb = ((state.trebleStrength / 1000f) * 900f).toInt()

                for (b in 0 until numBands) {
                    val centerHz = eq.getCenterFreq(b.toShort()) / 1000
                    // Find nearest UI band
                    var nearestIdx = 0
                    var bestDist = Int.MAX_VALUE
                    for (i in uiCenterHz.indices) {
                        val d = abs(centerHz - uiCenterHz[i])
                        if (d < bestDist) {
                            bestDist = d
                            nearestIdx = i
                        }
                    }
                    var targetLevel = state.bandLevels.getOrElse(nearestIdx) { 0 }
                    if (centerHz >= 4000) {
                        targetLevel += trebleBoostMb
                    }
                    eq.setBandLevel(b.toShort(), targetLevel.coerceIn(minLevel, maxLevel).toShort())
                }
            }
        }

        bassBoost?.let { bb ->
            runCatching {
                bb.enabled = enabled && state.bassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(state.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
        }

        reverb?.let { rv ->
            runCatching {
                rv.enabled = enabled && state.reverbPreset > 0
                rv.preset = state.reverbPreset.coerceIn(0, 6).toShort()
            }
        }

        virtualizer?.let { vz ->
            runCatching {
                vz.enabled = enabled && state.virtualizerStrength > 0
                if (vz.strengthSupported) {
                    vz.setStrength(state.virtualizerStrength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { reverb?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        reverb = null
        virtualizer = null
        currentSessionId = 0
    }
}
