package com.awraplay.player.ai

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure-Kotlin FFT supporting both power-of-2 (Radix-2 Cooley-Tukey)
 * and arbitrary even sizes (Bluestein's algorithm for n_fft = 6144 / 7680 / 5120).
 */
class FftProcessor(val n: Int) {

    private val isPow2 = (n and (n - 1)) == 0 && n > 0
    private val m: Int = if (isPow2) n else Integer.highestOneBit(2 * n - 1) shl 1

    // Bluestein precomputed tables when n is not a power of 2
    private val chirpRe = FloatArray(n)
    private val chirpIm = FloatArray(n)
    private val bFftRe = FloatArray(if (isPow2) 0 else m)
    private val bFftIm = FloatArray(if (isPow2) 0 else m)

    init {
        if (!isPow2) {
            for (k in 0 until n) {
                val angle = PI * k.toDouble() * k.toDouble() / n.toDouble()
                chirpRe[k] = cos(angle).toFloat()
                chirpIm[k] = (-sin(angle)).toFloat()
            }
            bFftRe[0] = chirpRe[0]
            bFftIm[0] = -chirpIm[0]
            for (k in 1 until n) {
                bFftRe[k] = chirpRe[k]
                bFftIm[k] = -chirpIm[k]
                bFftRe[m - k] = chirpRe[k]
                bFftIm[m - k] = -chirpIm[k]
            }
            fftPow2(bFftRe, bFftIm, inverse = false)
        }
    }

    fun fft(re: FloatArray, im: FloatArray, inverse: Boolean = false) {
        if (isPow2) {
            fftPow2(re, im, inverse)
            return
        }
        val aRe = FloatArray(m)
        val aIm = FloatArray(m)
        if (!inverse) {
            for (k in 0 until n) {
                val xr = re[k]
                val xi = im[k]
                val cr = chirpRe[k]
                val ci = chirpIm[k]
                aRe[k] = xr * cr - xi * ci
                aIm[k] = xr * ci + xi * cr
            }
        } else {
            // Conjugate input, run forward, conjugate & scale output
            for (k in 0 until n) {
                val xr = re[k]
                val xi = -im[k]
                val cr = chirpRe[k]
                val ci = chirpIm[k]
                aRe[k] = xr * cr - xi * ci
                aIm[k] = xr * ci + xi * cr
            }
        }
        fftPow2(aRe, aIm, inverse = false)
        for (k in 0 until m) {
            val ar = aRe[k]
            val ai = aIm[k]
            val br = bFftRe[k]
            val bi = bFftIm[k]
            aRe[k] = ar * br - ai * bi
            aIm[k] = ar * bi + ai * br
        }
        fftPow2(aRe, aIm, inverse = true)
        val invN = 1.0f / n
        for (k in 0 until n) {
            val ar = aRe[k]
            val ai = aIm[k]
            val cr = chirpRe[k]
            val ci = chirpIm[k]
            val yr = ar * cr - ai * ci
            val yi = ar * ci + ai * cr
            if (!inverse) {
                re[k] = yr
                im[k] = yi
            } else {
                re[k] = yr * invN
                im[k] = -yi * invN
            }
        }
    }

    private fun fftPow2(re: FloatArray, im: FloatArray, inverse: Boolean) {
        val len = re.size
        var j = 0
        for (i in 1 until len) {
            var bit = len shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var step = 2
        val sign = if (inverse) 1.0 else -1.0
        while (step <= len) {
            val half = step shr 1
            val theta = sign * 2.0 * PI / step
            val wStepRe = cos(theta)
            val wStepIm = sin(theta)
            var i = 0
            while (i < len) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until half) {
                    val evenIdx = i + k
                    val oddIdx = evenIdx + half
                    val or = re[oddIdx]
                    val oi = im[oddIdx]
                    val tRe = (wRe * or - wIm * oi).toFloat()
                    val tIm = (wRe * oi + wIm * or).toFloat()
                    re[oddIdx] = re[evenIdx] - tRe
                    im[oddIdx] = im[evenIdx] - tIm
                    re[evenIdx] += tRe
                    im[evenIdx] += tIm
                    val nextWRe = wRe * wStepRe - wIm * wStepIm
                    wIm = wRe * wStepIm + wIm * wStepRe
                    wRe = nextWRe
                }
                i += step
            }
            step = step shl 1
        }
        if (inverse) {
            val scale = 1.0f / len
            for (i in 0 until len) {
                re[i] *= scale
                im[i] *= scale
            }
        }
    }

    companion object {
        fun hannWindow(size: Int): FloatArray {
            return FloatArray(size) { i ->
                (0.5 * (1.0 - cos(2.0 * PI * i / size))).toFloat()
            }
        }
    }
}
