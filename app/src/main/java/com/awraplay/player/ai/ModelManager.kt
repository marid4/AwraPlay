package com.awraplay.player.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class OnnxModelInfo(
    val fileName: String,
    val displayName: String,
    val downloadUrl: String,
    val approxSizeMb: String
)

class ModelManager(private val context: Context) {

    private val modelsDir: File by lazy {
        File(context.filesDir, "awraplay_models").apply { mkdirs() }
    }

    val availableModels = listOf(
        OnnxModelInfo(
            fileName = "UVR-MDX-NET-Voc_FT.onnx",
            displayName = "UVR MDX-Net Vocal FT (Direkomendasikan)",
            downloadUrl = "https://huggingface.co/k2-fsa/sherpa-onnx-models/resolve/main/source-separation-models/UVR-MDX-NET-Voc_FT.onnx",
            approxSizeMb = "66.8 MB"
        ),
        OnnxModelInfo(
            fileName = "Kim_Vocal_2.onnx",
            displayName = "Kim Vocal 2 (MDX-Net)",
            downloadUrl = "https://huggingface.co/aaatmy/uvr-mdx-net-onnx/resolve/main/Kim_Vocal_2.onnx",
            approxSizeMb = "66.8 MB"
        )
    )

    fun getInstalledModelFile(): File? {
        return modelsDir.listFiles()?.firstOrNull { it.extension.equals("onnx", ignoreCase = true) && it.length() > 1_000_000 }
    }

    fun getInstalledModels(): List<File> {
        return modelsDir.listFiles()?.filter { it.extension.equals("onnx", ignoreCase = true) } ?: emptyList()
    }

    suspend fun downloadModel(
        model: OnnxModelInfo,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dest = File(modelsDir, model.fileName)
            val tmp = File(modelsDir, "${model.fileName}.part")
            val conn = (URL(model.downloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 20_000
                readTimeout = 30_000
            }
            conn.connect()
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode} saat mengunduh model.")
            }
            val total = conn.contentLengthLong.coerceAtLeast(1L)
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buf = ByteArray(16_384)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        downloaded += read
                        val prog = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        onProgress(prog, "${downloaded / (1024 * 1024)} MB / ${total / (1024 * 1024)} MB")
                    }
                }
            }
            if (dest.exists()) dest.delete()
            tmp.renameTo(dest)
            dest
        }
    }

    suspend fun importLocalOnnx(uri: Uri, displayName: String?): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val name = (displayName?.takeIf { it.endsWith(".onnx", true) } ?: "custom_vocal_model.onnx")
            val dest = File(modelsDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Tidak dapat membuka file model.")
            dest
        }
    }

    fun deleteAllModels() {
        modelsDir.listFiles()?.forEach { it.delete() }
    }
}
