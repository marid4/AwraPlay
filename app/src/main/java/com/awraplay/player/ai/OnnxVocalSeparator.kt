package com.awraplay.player.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.net.Uri
import com.awraplay.player.data.db.AwraDao
import com.awraplay.player.data.db.StemCacheEntity
import com.awraplay.player.data.model.MediaItem
import com.awraplay.player.data.model.SeparationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * AI Vocal Source Separation Engine.
 * - Uses ONNX Runtime Mobile (MDX-Net / UVR ONNX models: Kim_Vocal_2.onnx / UVR-MDX-NET-Voc_FT.onnx)
 *   when a model file is present in app storage.
 * - Falls back to STFT Frequency-Domain Center-Channel & Phase-Masking Stem Separation
 *   (separates center-panned vocals from stereo accompaniment into two real WAV stems)
 *   if no ONNX model is downloaded yet, while clearly reporting which engine was used.
 */
class OnnxVocalSeparator(
    private val context: Context,
    private val dao: AwraDao,
    private val modelManager: ModelManager
) {
    private val stemsRootDir: File by lazy {
        File(context.filesDir, "awraplay_stems").apply { mkdirs() }
    }

    suspend fun separateIfNeeded(
        item: MediaItem,
        preferOnnx: Boolean,
        onProgress: (Float, String) -> Unit
    ): SeparationState = withContext(Dispatchers.Default) {
        if (!item.isVocalSeparationSupported) {
            return@withContext SeparationState.Unsupported(
                "Durasi file (${item.durationMs / 1000}s) di luar batas pemisahan lokal (3 detik – 15 menit)."
            )
        }

        // Check existing cache first
        val cached = dao.getStemCache(item.id)
        if (cached != null && File(cached.vocalPath).exists() && File(cached.accompanimentPath).exists()) {
            return@withContext SeparationState.Ready(
                mediaId = item.id,
                vocalPath = cached.vocalPath,
                accompanimentPath = cached.accompanimentPath,
                usedAiModel = cached.usedAiModel,
                engineLabel = cached.engineLabel
            )
        }

        return@withContext try {
            onProgress(0.05f, "Mendekode audio...")
            val pcm = AudioDecoder.decodeToStereo44k(context, item.uri) { decProg ->
                onProgress(0.05f + decProg * 0.20f, "Mendekode audio (${(decProg * 100).toInt()}%)...")
            }

            val modelFile = if (preferOnnx) modelManager.getInstalledModelFile() else null
            val itemDir = File(stemsRootDir, item.id.toString()).apply { mkdirs() }
            val vocalFile = File(itemDir, "vocals.wav")
            val accFile = File(itemDir, "accompaniment.wav")

            val usedOnnx: Boolean
            val engineLabel: String

            if (modelFile != null && modelFile.exists()) {
                onProgress(0.28f, "Menjalankan AI ONNX (${modelFile.name})...")
                val (vocals, acc) = runMdxOnnxSeparation(pcm, modelFile) { infProg ->
                    onProgress(0.28f + infProg * 0.62f, "Pemisahan AI MDX-Net (${(infProg * 100).toInt()}%)...")
                }
                onProgress(0.92f, "Menyimpan stem vokal & instrumental...")
                AudioDecoder.writeStereoWav(vocalFile, vocals.left, vocals.right)
                AudioDecoder.writeStereoWav(accFile, acc.left, acc.right)
                usedOnnx = true
                engineLabel = "AI ONNX (${modelFile.nameWithoutExtension})"
            } else {
                onProgress(0.28f, "Memisahkan stem vokal (Spectral STFT)...")
                val (vocals, acc) = runSpectralStemSeparation(pcm) { sepProg ->
                    onProgress(0.28f + sepProg * 0.62f, "Pemisahan Spektral STFT (${(sepProg * 100).toInt()}%)...")
                }
                onProgress(0.92f, "Menyimpan stem ke cache...")
                AudioDecoder.writeStereoWav(vocalFile, vocals.left, vocals.right)
                AudioDecoder.writeStereoWav(accFile, acc.left, acc.right)
                usedOnnx = false
                engineLabel = "Spectral STFT (Pasang ONNX untuk akurasi AI penuh)"
            }

            val totalBytes = vocalFile.length() + accFile.length()
            dao.upsertStemCache(
                StemCacheEntity(
                    mediaId = item.id,
                    vocalPath = vocalFile.absolutePath,
                    accompanimentPath = accFile.absolutePath,
                    usedAiModel = usedOnnx,
                    engineLabel = engineLabel,
                    sizeBytes = totalBytes
                )
            )
            onProgress(1.0f, "Selesai")
            SeparationState.Ready(
                mediaId = item.id,
                vocalPath = vocalFile.absolutePath,
                accompanimentPath = accFile.absolutePath,
                usedAiModel = usedOnnx,
                engineLabel = engineLabel
            )
        } catch (e: Throwable) {
            SeparationState.Error(e.message ?: "Gagal memisahkan vokal dari file ini.")
        }
    }

    /**
     * Runs UVR MDX-Net ONNX model inference on 44.1 kHz stereo PCM.
     * Supports standard MDX-Net spectrogram shape [1, 4, dim_f, 256] (e.g. UVR-MDX-NET-Voc_FT / Kim_Vocal_2).
     */
    private fun runMdxOnnxSeparation(
        pcm: StereoPcm,
        modelFile: File,
        onProgress: (Float) -> Unit
    ): Pair<StereoPcm, StereoPcm> {
        val env = OrtEnvironment.getEnvironment()
        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(max(2, Runtime.getRuntime().availableProcessors() / 2))
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }

        val session = env.createSession(modelFile.absolutePath, opts)
        try {
            val inputName = session.inputNames.first()
            val inputInfo = session.inputInfo[inputName]?.info as? ai.onnxruntime.TensorInfo
            val shape = inputInfo?.shape ?: longArrayOf(1, 4, 3072, 256)
            val dimF = if (shape.size >= 4 && shape[2] > 0) shape[2].toInt() else 3072
            val chunkFrames = if (shape.size >= 4 && shape[3] > 0) shape[3].toInt() else 256
            val nFft = if (dimF == 2560) 5120 else 7680
            val hop = 1024
            val compensate = 1.021f

            val fft = FftProcessor(nFft)
            val window = FftProcessor.hannWindow(nFft)

            val totalSamples = pcm.frameCount
            val numFrames = ((totalSamples - nFft).coerceAtLeast(0) / hop) + 1
            val padRight = max(0, (numFrames - 1) * hop + nFft - totalSamples)
            val paddedLen = totalSamples + padRight

            val inL = FloatArray(paddedLen)
            val inR = FloatArray(paddedLen)
            pcm.left.copyInto(inL)
            pcm.right.copyInto(inR)

            val outVocL = FloatArray(paddedLen)
            val outVocR = FloatArray(paddedLen)
            val normArr = FloatArray(paddedLen)

            // STFT all frames
            val specReL = Array(numFrames) { FloatArray(dimF) }
            val specImL = Array(numFrames) { FloatArray(dimF) }
            val specReR = Array(numFrames) { FloatArray(dimF) }
            val specImR = Array(numFrames) { FloatArray(dimF) }

            val bufReL = FloatArray(nFft)
            val bufImL = FloatArray(nFft)
            val bufReR = FloatArray(nFft)
            val bufImR = FloatArray(nFft)

            for (f in 0 until numFrames) {
                val start = f * hop
                for (i in 0 until nFft) {
                    val w = window[i]
                    bufReL[i] = inL[start + i] * w
                    bufImL[i] = 0f
                    bufReR[i] = inR[start + i] * w
                    bufImR[i] = 0f
                }
                fft.fft(bufReL, bufImL, inverse = false)
                fft.fft(bufReR, bufImR, inverse = false)
                for (k in 0 until dimF) {
                    specReL[f][k] = bufReL[k]
                    specImL[f][k] = bufImL[k]
                    specReR[f][k] = bufReR[k]
                    specImR[f][k] = bufImR[k]
                }
            }

            // Chunked ONNX inference
            val stepFrames = chunkFrames / 2
            val predReL = Array(numFrames) { FloatArray(dimF) }
            val predImL = Array(numFrames) { FloatArray(dimF) }
            val predReR = Array(numFrames) { FloatArray(dimF) }
            val predImR = Array(numFrames) { FloatArray(dimF) }
            val frameWeight = FloatArray(numFrames)

            val tensorSize = 4 * dimF * chunkFrames
            val flatInput = FloatArray(tensorSize)
            val tensorShape = longArrayOf(1L, 4L, dimF.toLong(), chunkFrames.toLong())

            var startFrame = 0
            val totalChunks = max(1, (numFrames + stepFrames - 1) / stepFrames)
            var chunkIdx = 0

            while (startFrame < numFrames) {
                flatInput.fill(0f)
                val validFrames = min(chunkFrames, numFrames - startFrame)
                for (t in 0 until validFrames) {
                    val fIdx = startFrame + t
                    for (k in 0 until dimF) {
                        val base = k * chunkFrames + t
                        flatInput[base] = specReL[fIdx][k]
                        flatInput[dimF * chunkFrames + base] = specImL[fIdx][k]
                        flatInput[2 * dimF * chunkFrames + base] = specReR[fIdx][k]
                        flatInput[3 * dimF * chunkFrames + base] = specImR[fIdx][k]
                    }
                }

                OnnxTensor.createTensor(env, FloatBuffer.wrap(flatInput), tensorShape).use { inTensor ->
                    session.run(mapOf(inputName to inTensor)).use { results ->
                        val outTensor = results[0] as OnnxTensor
                        val outArr = outTensor.floatBuffer
                        for (t in 0 until validFrames) {
                            val fIdx = startFrame + t
                            // Triangular overlap weight
                            val w = if (numFrames <= chunkFrames) 1f else {
                                0.5f * (1f - cos(2.0 * PI * (t + 0.5) / chunkFrames)).toFloat()
                            }
                            frameWeight[fIdx] += w
                            for (k in 0 until dimF) {
                                val base = k * chunkFrames + t
                                predReL[fIdx][k] += outArr.get(base) * w
                                predImL[fIdx][k] += outArr.get(dimF * chunkFrames + base) * w
                                predReR[fIdx][k] += outArr.get(2 * dimF * chunkFrames + base) * w
                                predImR[fIdx][k] += outArr.get(3 * dimF * chunkFrames + base) * w
                            }
                        }
                    }
                }
                chunkIdx++
                onProgress((chunkIdx.toFloat() / totalChunks).coerceIn(0f, 1f))
                if (startFrame + chunkFrames >= numFrames) break
                startFrame += stepFrames
            }

            // ISTFT to reconstruct vocal waveform
            for (f in 0 until numFrames) {
                val invW = if (frameWeight[f] > 1e-6f) 1f / frameWeight[f] else 1f
                bufReL.fill(0f); bufImL.fill(0f)
                bufReR.fill(0f); bufImR.fill(0f)
                for (k in 0 until dimF) {
                    val rl = predReL[f][k] * invW
                    val il = predImL[f][k] * invW
                    val rr = predReR[f][k] * invW
                    val ir = predImR[f][k] * invW
                    bufReL[k] = rl; bufImL[k] = il
                    bufReR[k] = rr; bufImR[k] = ir
                    if (k in 1 until nFft / 2) {
                        bufReL[nFft - k] = rl; bufImL[nFft - k] = -il
                        bufReR[nFft - k] = rr; bufImR[nFft - k] = -ir
                    }
                }
                fft.fft(bufReL, bufImL, inverse = true)
                fft.fft(bufReR, bufImR, inverse = true)
                val start = f * hop
                for (i in 0 until nFft) {
                    val w = window[i]
                    outVocL[start + i] += bufReL[i] * w
                    outVocR[start + i] += bufReR[i] * w
                    normArr[start + i] += w * w
                }
            }

            val vocL = FloatArray(totalSamples)
            val vocR = FloatArray(totalSamples)
            val accL = FloatArray(totalSamples)
            val accR = FloatArray(totalSamples)

            for (i in 0 until totalSamples) {
                val nrm = if (normArr[i] > 1e-6f) normArr[i] else 1f
                val vl = (outVocL[i] / nrm) * compensate
                val vr = (outVocR[i] / nrm) * compensate
                vocL[i] = vl.coerceIn(-1f, 1f)
                vocR[i] = vr.coerceIn(-1f, 1f)
                accL[i] = (pcm.left[i] - vl).coerceIn(-1f, 1f)
                accR[i] = (pcm.right[i] - vr).coerceIn(-1f, 1f)
            }

            return Pair(StereoPcm(vocL, vocR), StereoPcm(accL, accR))
        } finally {
            session.close()
        }
    }

    /**
     * Frequency-domain STFT Mid-Side & Spectral Vocal Masking Separator.
     * Separates center-panned vocal harmonics (85 Hz - 8 kHz) from stereo accompaniment
     * into independent vocal & accompaniment stems when no ONNX model file is installed.
     */
    private fun runSpectralStemSeparation(
        pcm: StereoPcm,
        onProgress: (Float) -> Unit
    ): Pair<StereoPcm, StereoPcm> {
        val nFft = 2048
        val hop = 512
        val fft = FftProcessor(nFft)
        val window = FftProcessor.hannWindow(nFft)

        val totalSamples = pcm.frameCount
        val numFrames = ((totalSamples - nFft).coerceAtLeast(0) / hop) + 1
        val paddedLen = max(totalSamples, (numFrames - 1) * hop + nFft)

        val inL = FloatArray(paddedLen).also { pcm.left.copyInto(it) }
        val inR = FloatArray(paddedLen).also { pcm.right.copyInto(it) }

        val outVocL = FloatArray(paddedLen)
        val outVocR = FloatArray(paddedLen)
        val normArr = FloatArray(paddedLen)

        val reL = FloatArray(nFft)
        val imL = FloatArray(nFft)
        val reR = FloatArray(nFft)
        val imR = FloatArray(nFft)
        val vocRe = FloatArray(nFft)
        val vocIm = FloatArray(nFft)

        val binHz = 44100f / nFft
        val minVocalBin = (85f / binHz).toInt()
        val maxVocalBin = (8500f / binHz).toInt()

        for (f in 0 until numFrames) {
            val start = f * hop
            for (i in 0 until nFft) {
                val w = window[i]
                reL[i] = inL[start + i] * w; imL[i] = 0f
                reR[i] = inR[start + i] * w; imR[i] = 0f
            }
            fft.fft(reL, imL, inverse = false)
            fft.fft(reR, imR, inverse = false)

            vocRe.fill(0f)
            vocIm.fill(0f)

            for (k in minVocalBin..min(maxVocalBin, nFft / 2)) {
                val midRe = 0.5f * (reL[k] + reR[k])
                val midIm = 0.5f * (imL[k] + imR[k])
                val sideRe = 0.5f * (reL[k] - reR[k])
                val sideIm = 0.5f * (imL[k] - imR[k])

                val midMag = sqrt(midRe * midRe + midIm * midIm)
                val sideMag = sqrt(sideRe * sideRe + sideIm * sideIm)

                // Soft Wiener-style center-pan mask
                val ratio = (midMag * midMag) / (midMag * midMag + 2.2f * sideMag * sideMag + 1e-9f)
                val mask = if (ratio > 0.35f) ((ratio - 0.35f) / 0.65f).coerceIn(0f, 1f) else 0f

                val vr = midRe * mask
                val vi = midIm * mask
                vocRe[k] = vr
                vocIm[k] = vi
                if (k in 1 until nFft / 2) {
                    vocRe[nFft - k] = vr
                    vocIm[nFft - k] = -vi
                }
            }

            fft.fft(vocRe, vocIm, inverse = true)
            for (i in 0 until nFft) {
                val w = window[i]
                val s = vocRe[i] * w
                outVocL[start + i] += s
                outVocR[start + i] += s
                normArr[start + i] += w * w
            }

            if (f % 100 == 0) {
                onProgress((f.toFloat() / numFrames).coerceIn(0f, 1f))
            }
        }

        val vocL = FloatArray(totalSamples)
        val vocR = FloatArray(totalSamples)
        val accL = FloatArray(totalSamples)
        val accR = FloatArray(totalSamples)

        for (i in 0 until totalSamples) {
            val nrm = if (normArr[i] > 1e-6f) normArr[i] else 1f
            val vl = (outVocL[i] / nrm).coerceIn(-1f, 1f)
            val vr = (outVocR[i] / nrm).coerceIn(-1f, 1f)
            vocL[i] = vl
            vocR[i] = vr
            accL[i] = (pcm.left[i] - vl).coerceIn(-1f, 1f)
            accR[i] = (pcm.right[i] - vr).coerceIn(-1f, 1f)
        }

        onProgress(1.0f)
        return Pair(StereoPcm(vocL, vocR), StereoPcm(accL, accR))
    }
}
