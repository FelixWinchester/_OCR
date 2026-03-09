package com.example.ocrmsg.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val recognizedText: String,

    // Время
    val inferenceMs: Long,
    val preprocessMs: Long,      // время подготовки bitmap
    val totalMs: Long,           // полное время от вызова до результата

    // Текст
    val blockCount: Int,
    val lineCount: Int,
    val wordCount: Int,
    val symbolCount: Int,

    // Качество
    val avgConfidence: Float,    // средняя уверенность по блокам (0..1)
    val hasLatinChars: Boolean,
    val hasDigits: Boolean,
    val hasSpecialChars: Boolean,

    // Изображение
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val imageSizeKb: Int
)





object OcrRepository {

    suspend fun runMlKit(bitmap: Bitmap): OcrResult {
        val totalStart = System.currentTimeMillis()

        // Замер препроцессинга
        val preprocessStart = System.currentTimeMillis()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val preprocessMs = System.currentTimeMillis() - preprocessStart

        // Замер инференса
        val inferenceStart = System.currentTimeMillis()
        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        val inferenceMs = System.currentTimeMillis() - inferenceStart
        val totalMs     = System.currentTimeMillis() - totalStart

        // Метрики текста
        val allText    = visionText.text
        val wordCount  = allText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val lineCount  = visionText.textBlocks.sumOf { it.lines.size }
        val symbolCount = allText.replace("\\s".toRegex(), "").length

        // Уверенность — ML Kit не даёт прямой score, считаем по кол-ву символов на блок
        // как прокси-метрику (чем больше символов в блоке — тем выше условная уверенность)
        val avgConfidence = if (visionText.textBlocks.isEmpty()) 0f
        else visionText.textBlocks.map { block ->
            val len = block.text.replace("\\s".toRegex(), "").length.toFloat()
            (len / (len + 2f)).coerceIn(0f, 1f)
        }.average().toFloat()

        // Характеристики текста
        val hasDigits       = allText.any { it.isDigit() }
        val hasLatinChars   = allText.any { it.isLetter() && it.code < 128 }
        val hasSpecialChars = allText.any { !it.isLetterOrDigit() && !it.isWhitespace() }

        // Размер изображения
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val imageSizeKb = stream.size() / 1024

        return OcrResult(
            recognizedText  = allText,
            inferenceMs     = inferenceMs,
            preprocessMs    = preprocessMs,
            totalMs         = totalMs,
            blockCount      = visionText.textBlocks.size,
            lineCount       = lineCount,
            wordCount       = wordCount,
            symbolCount     = symbolCount,
            avgConfidence   = avgConfidence,
            hasLatinChars   = hasLatinChars,
            hasDigits       = hasDigits,
            hasSpecialChars = hasSpecialChars,
            imageWidthPx    = bitmap.width,
            imageHeightPx   = bitmap.height,
            imageSizeKb     = imageSizeKb
        )
    }

    fun saveResultToDownloads(
        context: Context,
        ocrResult: OcrResult,
        deviceStats: DeviceStats,
        modelName: String
    ): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())

        val json = JSONObject().apply {
            put("timestamp", timestamp)
            put("model", modelName)
            put("ocr", JSONObject().apply {
                put("text", ocrResult.recognizedText)
                put("inference_ms", ocrResult.inferenceMs)
                put("blocks", ocrResult.blockCount)
                put("lines", ocrResult.lineCount)
                put("symbols", ocrResult.symbolCount)
            })
            put("device", JSONObject().apply {
                put("android_version", deviceStats.androidVersion)
                put("device_model", deviceStats.deviceModel)
                put("cpu_load_pct", deviceStats.cpuLoadPct)
                put("ram_used_mb", deviceStats.ramUsedMb)
                put("ram_total_mb", deviceStats.ramTotalMb)
                put("battery_temp_c", deviceStats.batteryTempC)
            })
        }

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "ocrmsg_$timestamp.json")
        file.writeText(json.toString(2))

        return file.absolutePath
    }
}