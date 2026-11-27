package com.awraplay.player.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

/**
 * Sample-accurate dual-stem AudioTrack mixer for Vocal ON/OFF & Vocal Volume Slider (0%..100%).
 * Mixes pre-separated `accompaniment.wav` + `vocals.wav` in real time, keeping exact
 * synchronization with either standalone audio playback or ExoPlayer video playback.
 */
class StemSyncAudioEngine(
    private val onSessionReady: (Int) -> Unit = {},
    private val onPositionUpdate: (Long) -> Unit = {},
    private val onCompleted: () -> Unit = {}
) {
    private val sampleRate = 44100
    private val wavHeaderBytes = 44L
    private val bytesPerFrame = 4 // 16-bit stereo

    @Volatile private var isPlaying = false
    @Volatile private var isReleased = false
    @Volatile private var targetVocalGain = 1.0f
    @Volatile private var currentVocalGain = 1.0f
    @Volatile private var masterVolume = 1.0f
    @Volatile private var balance = 0.0f // -1f Left .. +1f Right
    @Volatile private var playbackSpeed = 1.0f

    @Volatile private var seekRequestedFrame: Long = -1L
    @Volatile private var currentFrame: Long = 0L
    @Volatile private var totalFrames: Long = 0L

    private var vocalFilePath: String? = null
    private var accFilePath: String? = null
    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null

    val audioSessionId: Int
        get() = audioTrack?.audioSessionId ?: 0

    val durationMs: Long
        get() = (totalFrames * 1000L) / sampleRate

    val currentPositionMs: Long
        get() = (currentFrame * 1000L) / sampleRate

    fun loadStems(vocalPath: String, accompanimentPath: String, startMs: Long = 0L) {
        stop()
        val vf = File(vocalPath)
        val af = File(accompanimentPath)
        if (!vf.exists() || !af.exists()) return

        vocalFilePath = vocalPath
        accFilePath = accompanimentPath
        val dataBytes = min(vf.length(), af.length()) - wavHeaderBytes
        totalFrames = (dataBytes / bytesPerFrame).coerceAtLeast(1L)
        currentFrame = ((startMs * sampleRate) / 1000L).coerceIn(0L, totalFrames - 1)
        isReleased = false
    }

    fun setVocalVolume(volume: Float) {
        targetVocalGain = volume.coerceIn(0f, 1f)
    }

    fun setMasterAndBalance(volume: Float, bal: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        balance = bal.coerceIn(-1f, 1f)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        runCatching {
            audioTrack?.let { track ->
                val params = track.playbackParams
                params.speed = playbackSpeed
                track.playbackParams = params
            }
        }
    }

    fun seekTo(positionMs: Long) {
        if (totalFrames <= 0) return
        val target = ((positionMs * sampleRate) / 1000L).coerceIn(0L, totalFrames - 1)
        seekRequestedFrame = target
        currentFrame = target
    }

    fun syncToVideoPosition(videoPosMs: Long) {
        if (totalFrames <= 0) return
        val driftMs = abs(videoPosMs - currentPositionMs)
        if (driftMs > 40L) {
            seekTo(videoPosMs)
        }
    }

    fun play() {
        if (vocalFilePath == null || accFilePath == null) return
        if (isPlaying) return
        isPlaying = true

        if (renderThread?.isAlive == true) return
        startRenderLoop()
    }

    fun pause() {
        isPlaying = false
        runCatching { audioTrack?.pause() }
    }

    fun stop() {
        isPlaying = false
        isReleased = true
        runCatching { renderThread?.join(300) }
        renderThread = null
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
    }

    private fun startRenderLoop() {
        isReleased = false
        renderThread = thread(start = true, name = "AwraStemMixer") {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(8192)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            onSessionReady(track.audioSessionId)
            setPlaybackSpeed(playbackSpeed)
            track.play()

            val vocRaf = RandomAccessFile(vocalFilePath, "r")
            val accRaf = RandomAccessFile(accFilePath, "r")

            val framesPerChunk = 1024
            val chunkBytes = framesPerChunk * bytesPerFrame
            val vocBuf = ByteArray(chunkBytes)
            val accBuf = ByteArray(chunkBytes)
            val mixBuf = ByteArray(chunkBytes)

            try {
                var posOffset = wavHeaderBytes + currentFrame * bytesPerFrame
                vocRaf.seek(posOffset)
                accRaf.seek(posOffset)

                while (!isReleased) {
                    if (!isPlaying) {
                        Thread.sleep(20)
                        continue
                    }

                    val pendingSeek = seekRequestedFrame
                    if (pendingSeek >= 0L) {
                        seekRequestedFrame = -1L
                        currentFrame = pendingSeek
                        posOffset = wavHeaderBytes + pendingSeek * bytesPerFrame
                        vocRaf.seek(posOffset)
                        accRaf.seek(posOffset)
                        track.flush()
                    }

                    val vRead = vocRaf.read(vocBuf)
                    val aRead = accRaf.read(accBuf)
                    val bytesRead = min(vRead, aRead)

                    if (bytesRead <= 0) {
                        isPlaying = false
                        onCompleted()
                        break
                    }

                    val framesRead = bytesRead / bytesPerFrame
                    val leftGain = if (balance > 0f) 1f - balance else 1f
                    val rightGain = if (balance < 0f) 1f + balance else 1f

                    var i = 0
                    while (i + 3 < bytesRead) {
                        // Smooth gain ramp to avoid clicks when toggling Vocal ON/OFF
                        currentVocalGain += (targetVocalGain - currentVocalGain) * 0.004f

                        val vL = ((vocBuf[i].toInt() and 0xFF) or (vocBuf[i + 1].toInt() shl 8)).toShort()
                        val vR = ((vocBuf[i + 2].toInt() and 0xFF) or (vocBuf[i + 3].toInt() shl 8)).toShort()
                        val aL = ((accBuf[i].toInt() and 0xFF) or (accBuf[i + 1].toInt() shl 8)).toShort()
                        val aR = ((accBuf[i + 2].toInt() and 0xFF) or (accBuf[i + 3].toInt() shl 8)).toShort()

                        val mL = ((aL + vL * currentVocalGain) * masterVolume * leftGain)
                            .toInt().coerceIn(-32768, 32767)
                        val mR = ((aR + vR * currentVocalGain) * masterVolume * rightGain)
                            .toInt().coerceIn(-32768, 32767)

                        mixBuf[i] = (mL and 0xFF).toByte()
                        mixBuf[i + 1] = ((mL shr 8) and 0xFF).toByte()
                        mixBuf[i + 2] = (mR and 0xFF).toByte()
                        mixBuf[i + 3] = ((mR shr 8) and 0xFF).toByte()
                        i += 4
                    }

                    track.write(mixBuf, 0, framesRead * bytesPerFrame)
                    currentFrame += framesRead
                    onPositionUpdate(currentPositionMs)
                }
            } finally {
                runCatching { vocRaf.close() }
                runCatching { accRaf.close() }
            }
        }
    }
}
