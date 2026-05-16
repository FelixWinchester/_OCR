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
    val inferenceMs: Long,
    val preprocessMs: Long,
    val totalMs: Long,
    val blockCount: Int,
    val lineCount: Int,
    val wordCount: Int,
    val symbolCount: Int,
    val avgConfidence: Float,
    val hasLatinChars: Boolean,
    val hasDigits: Boolean,
    val hasSpecialChars: Boolean,
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val imageSizeKb: Int,
    val cer: Float?          = null,
    val wer: Float?          = null,
    val exactMatch: Boolean? = null,
    val precision: Float?    = null,
    val recall: Float?       = null
)

object OcrRepository {

    // ── ML Kit ────────────────────────────────────────────────────────

    suspend fun runMlKit(bitmap: Bitmap): OcrResult {
        val totalStart = System.currentTimeMillis()

        val preprocessStart = System.currentTimeMillis()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val preprocessMs = System.currentTimeMillis() - preprocessStart

        val inferenceStart = System.currentTimeMillis()
        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        val inferenceMs = System.currentTimeMillis() - inferenceStart
        val totalMs     = System.currentTimeMillis() - totalStart

        val allText     = visionText.text
        val wordCount   = allText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val lineCount   = visionText.textBlocks.sumOf { it.lines.size }
        val symbolCount = allText.replace("\\s".toRegex(), "").length

        val avgConfidence = if (visionText.textBlocks.isEmpty()) 0f
        else visionText.textBlocks.map { block ->
            val len = block.text.replace("\\s".toRegex(), "").length.toFloat()
            (len / (len + 2f)).coerceIn(0f, 1f)
        }.average().toFloat()

        val hasDigits       = allText.any { it.isDigit() }
        val hasLatinChars   = allText.any { it.isLetter() && it.code < 128 }
        val hasSpecialChars = allText.any { !it.isLetterOrDigit() && !it.isWhitespace() }

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

    // ── Tesseract ─────────────────────────────────────────────────────

    fun runTesseract(context: Context, bitmap: Bitmap): OcrResult {
        val totalStart = System.currentTimeMillis()

        val preprocessStart = System.currentTimeMillis()
        // Шаг 1: конвертируем hardware → software
        val softwareBitmap = toSoftware(bitmap)
        // Шаг 2: конвертируем в grayscale (уже software)
        val grayBitmap = toGrayscale(softwareBitmap)
        // Освобождаем промежуточный если создавали новый
        if (softwareBitmap !== bitmap) softwareBitmap.recycle()
        val preprocessMs = System.currentTimeMillis() - preprocessStart

        val inferenceStart = System.currentTimeMillis()
        val allText = TesseractHelper.recognize(context, grayBitmap)
        val inferenceMs = System.currentTimeMillis() - inferenceStart
        val totalMs     = System.currentTimeMillis() - totalStart

        grayBitmap.recycle()

        val wordCount   = allText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val lineCount   = allText.lines().filter { it.isNotBlank() }.size
        val symbolCount = allText.replace("\\s".toRegex(), "").length

        val hasDigits       = allText.any { it.isDigit() }
        val hasLatinChars   = allText.any { it.isLetter() && it.code < 128 }
        val hasSpecialChars = allText.any { !it.isLetterOrDigit() && !it.isWhitespace() }

        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val imageSizeKb = stream.size() / 1024

        return OcrResult(
            recognizedText  = allText.trim(),
            inferenceMs     = inferenceMs,
            preprocessMs    = preprocessMs,
            totalMs         = totalMs,
            blockCount      = lineCount,
            lineCount       = lineCount,
            wordCount       = wordCount,
            symbolCount     = symbolCount,
            avgConfidence   = 0f,
            hasLatinChars   = hasLatinChars,
            hasDigits       = hasDigits,
            hasSpecialChars = hasSpecialChars,
            imageWidthPx    = bitmap.width,
            imageHeightPx   = bitmap.height,
            imageSizeKb     = imageSizeKb
        )
    }

    // ── Вспомогательные ──────────────────────────────────────────────

    // Конвертация hardware bitmap → software ARGB_8888
    private fun toSoftware(bitmap: Bitmap): Bitmap {
        if (bitmap.config != Bitmap.Config.HARDWARE) return bitmap
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    // Конвертация в grayscale — принимает только software bitmap
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint  = android.graphics.Paint()
        val matrix = android.graphics.ColorMatrix()
        matrix.setSaturation(0f)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    fun applyGroundTruth(result: OcrResult, groundTruth: String): OcrResult {
        return result.copy(
            cer        = MetricsCalculator.cer(groundTruth, result.recognizedText),
            wer        = MetricsCalculator.wer(groundTruth, result.recognizedText),
            exactMatch = MetricsCalculator.exactMatch(groundTruth, result.recognizedText),
            precision  = MetricsCalculator.wordPrecision(groundTruth, result.recognizedText),
            recall     = MetricsCalculator.wordRecall(groundTruth, result.recognizedText)
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
                put("text",           ocrResult.recognizedText)
                put("preprocess_ms",  ocrResult.preprocessMs)
                put("inference_ms",   ocrResult.inferenceMs)
                put("total_ms",       ocrResult.totalMs)
                put("blocks",         ocrResult.blockCount)
                put("lines",          ocrResult.lineCount)
                put("words",          ocrResult.wordCount)
                put("symbols",        ocrResult.symbolCount)
                put("avg_confidence", ocrResult.avgConfidence)
                put("has_latin",      ocrResult.hasLatinChars)
                put("has_digits",     ocrResult.hasDigits)
                put("has_special",    ocrResult.hasSpecialChars)
                put("image_width_px", ocrResult.imageWidthPx)
                put("image_height_px",ocrResult.imageHeightPx)
                put("image_size_kb",  ocrResult.imageSizeKb)
            })
            if (ocrResult.cer != null) {
                put("metrics", JSONObject().apply {
                    put("cer",         ocrResult.cer)
                    put("wer",         ocrResult.wer)
                    put("exact_match", ocrResult.exactMatch)
                    put("precision",   ocrResult.precision)
                    put("recall",      ocrResult.recall)
                })
            }
            put("device", JSONObject().apply {
                put("android_version", deviceStats.androidVersion)
                put("device_model",    deviceStats.deviceModel)
                put("cpu_load_pct",    deviceStats.cpuLoadPct)
                put("ram_used_mb",     deviceStats.ramUsedMb)
                put("ram_total_mb",    deviceStats.ramTotalMb)
                put("battery_temp_c",  deviceStats.batteryTempC)
            })
        }

        val dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "ocrmsg_$timestamp.json")
        file.writeText(json.toString(2))

        return file.absolutePath
    }
}