package com.awraplay.player.ai

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

data class StereoPcm(
    val left: FloatArray,
    val right: FloatArray,
    val sampleRate: Int = 44100
) {
    val frameCount: Int get() = left.size
}

/**
 * Decodes audio files or audio tracks from video files into 44.1 kHz Stereo PCM
 * using Android's native MediaExtractor + MediaCodec without altering the original file.
 */
object AudioDecoder {

    const val TARGET_SAMPLE_RATE = 44100

    fun decodeToStereo44k(
        context: Context,
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): StereoPcm {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                audioFormat = fmt
                break
            }
        }
        if (audioTrackIndex < 0 || audioFormat == null) {
            extractor.release()
            throw IllegalStateException("Track audio tidak ditemukan pada file ini.")
        }

        extractor.selectTrack(audioTrackIndex)
        val mime = audioFormat.getString(MediaFormat.KEY_MIME)!!
        val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
            audioFormat.getLong(MediaFormat.KEY_DURATION)
        } else 0L

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(audioFormat, null, null, 0)
        codec.start()

        val rawLeft = ArrayList<Float>(44100 * 60)
        val rawRight = ArrayList<Float>(44100 * 60)
        var sourceSampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf: ByteBuffer = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime
                            if (durationUs > 0) {
                                onProgress((pts.toFloat() / durationUs.toFloat()).coerceIn(0f, 0.99f))
                            }
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sourceSampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                    }
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val shorts = ShortArray(shortBuf.remaining())
                            shortBuf.get(shorts)

                            if (channelCount == 1) {
                                for (s in shorts) {
                                    val f = s / 32768.0f
                                    rawLeft.add(f)
                                    rawRight.add(f)
                                }
                            } else {
                                var i = 0
                                while (i + channelCount - 1 < shorts.size) {
                                    rawLeft.add(shorts[i] / 32768.0f)
                                    rawRight.add(shorts[i + 1] / 32768.0f)
                                    i += channelCount
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        onProgress(1.0f)
        val leftArr = rawLeft.toFloatArray()
        val rightArr = rawRight.toFloatArray()

        return if (sourceSampleRate == TARGET_SAMPLE_RATE) {
            StereoPcm(leftArr, rightArr, TARGET_SAMPLE_RATE)
        } else {
            StereoPcm(
                resampleLinear(leftArr, sourceSampleRate, TARGET_SAMPLE_RATE),
                resampleLinear(rightArr, sourceSampleRate, TARGET_SAMPLE_RATE),
                TARGET_SAMPLE_RATE
            )
        }
    }

    private fun resampleLinear(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (input.isEmpty() || srcRate == dstRate) return input
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val outLen = (input.size / ratio).toInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i * ratio
            val idx0 = srcPos.toInt()
            val frac = (srcPos - idx0).toFloat()
            val idx1 = min(idx0 + 1, input.size - 1)
            out[i] = input[idx0] * (1f - frac) + input[idx1] * frac
        }
        return out
    }

    fun writeStereoWav(file: File, left: FloatArray, right: FloatArray, sampleRate: Int = TARGET_SAMPLE_RATE) {
        val numSamples = min(left.size, right.size)
        val dataSize = numSamples * 4 // 2 channels * 2 bytes (16-bit)
        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { out ->
            // RIFF header
            out.writeBytes("RIFF")
            out.writeIntLE(36 + dataSize)
            out.writeBytes("WAVE")
            // fmt subchunk
            out.writeBytes("fmt ")
            out.writeIntLE(16)
            out.writeShortLE(1) // PCM
            out.writeShortLE(2) // Stereo
            out.writeIntLE(sampleRate)
            out.writeIntLE(sampleRate * 4)
            out.writeShortLE(4) // Block align
            out.writeShortLE(16) // Bits per sample
            // data subchunk
            out.writeBytes("data")
            out.writeIntLE(dataSize)

            val chunk = ByteArray(4096)
            var pos = 0
            var idx = 0
            while (idx < numSamples) {
                val l = (left[idx].coerceIn(-1f, 1f) * 32767f).toInt()
                val r = (right[idx].coerceIn(-1f, 1f) * 32767f).toInt()
                chunk[pos++] = (l and 0xFF).toByte()
                chunk[pos++] = ((l shr 8) and 0xFF).toByte()
                chunk[pos++] = (r and 0xFF).toByte()
                chunk[pos++] = ((r shr 8) and 0xFF).toByte()
                idx++
                if (pos == chunk.size || idx == numSamples) {
                    out.write(chunk, 0, pos)
                    pos = 0
                }
            }
        }
    }

    private fun DataOutputStream.writeIntLE(v: Int) {
        write(v and 0xFF)
        write((v shr 8) and 0xFF)
        write((v shr 16) and 0xFF)
        write((v shr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(v: Int) {
        write(v and 0xFF)
        write((v shr 8) and 0xFF)
    }
}
