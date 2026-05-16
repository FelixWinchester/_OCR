package com.example.ocrmsg.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.ocrmsg.model.OcrModel

object BatchProcessor {

    suspend fun process(
        context: Context,
        items: List<Pair<Uri, String>>,
        model: OcrModel = OcrModel.ML_KIT,
        onProgress: (current: Int, total: Int, fileName: String) -> Unit
    ): List<BatchItemResult> {
        val results = mutableListOf<BatchItemResult>()

        items.forEachIndexed { index, (uri, groundTruth) ->
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "image_$index"
            onProgress(index + 1, items.size, fileName)

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    results.add(BatchItemResult(
                        fileName    = fileName,
                        groundTruth = groundTruth,
                        ocrResult   = null,
                        deviceStats = null,
                        error       = "Не удалось декодировать изображение"
                    ))
                    return@forEachIndexed
                }

                val ocrResult = when (model) {
                    OcrModel.ML_KIT    -> OcrRepository.runMlKit(bitmap)
                    OcrModel.TESSERACT -> OcrRepository.runTesseract(context, bitmap)
                }

                val finalResult = if (groundTruth.isNotBlank()) {
                    OcrRepository.applyGroundTruth(ocrResult, groundTruth)
                } else {
                    ocrResult
                }

                val deviceStats = DeviceStatsCollector.collect(context)

                results.add(BatchItemResult(
                    fileName    = fileName,
                    groundTruth = groundTruth,
                    ocrResult   = finalResult,
                    deviceStats = deviceStats
                ))

            } catch (e: Exception) {
                results.add(BatchItemResult(
                    fileName    = fileName,
                    groundTruth = groundTruth,
                    ocrResult   = null,
                    deviceStats = null,
                    error       = e.message ?: "Неизвестная ошибка"
                ))
            }
        }

        return results
    }

    fun computeSummary(results: List<BatchItemResult>): BatchSummary {
        val successful  = results.filter { it.ocrResult != null && it.error == null }
        val withMetrics = successful.filter { it.ocrResult?.cer != null }

        return BatchSummary(
            totalImages     = results.size,
            successCount    = successful.size,
            errorCount      = results.count { it.error != null },
            avgInferenceMs  = successful.map { it.ocrResult!!.inferenceMs.toDouble() }.average().takeIf { it.isFinite() } ?: 0.0,
            avgTotalMs      = successful.map { it.ocrResult!!.totalMs.toDouble() }.average().takeIf { it.isFinite() } ?: 0.0,
            avgCer          = withMetrics.map { it.ocrResult!!.cer!!.toDouble() }.average().takeIf { it.isFinite() },
            avgWer          = withMetrics.map { it.ocrResult!!.wer!!.toDouble() }.average().takeIf { it.isFinite() },
            avgPrecision    = withMetrics.map { it.ocrResult!!.precision!!.toDouble() }.average().takeIf { it.isFinite() },
            avgRecall       = withMetrics.map { it.ocrResult!!.recall!!.toDouble() }.average().takeIf { it.isFinite() },
            exactMatchCount = withMetrics.count { it.ocrResult?.exactMatch == true },
            avgCpuLoad      = successful.mapNotNull { it.deviceStats?.cpuLoadPct?.toDouble() }.average().takeIf { it.isFinite() } ?: 0.0,
            avgRamUsedMb    = successful.mapNotNull { it.deviceStats?.ramUsedMb?.toDouble() }.average().takeIf { it.isFinite() } ?: 0.0
        )
    }
}