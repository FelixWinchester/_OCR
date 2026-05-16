package com.example.ocrmsg.data

data class BatchItemResult(
    val fileName: String,
    val groundTruth: String,
    val ocrResult: OcrResult?,
    val deviceStats: DeviceStats?,
    val error: String?          = null
)

data class BatchSummary(
    val totalImages: Int,
    val successCount: Int,
    val errorCount: Int,
    val avgInferenceMs: Double,
    val avgTotalMs: Double,
    val avgCer: Double?,
    val avgWer: Double?,
    val avgPrecision: Double?,
    val avgRecall: Double?,
    val exactMatchCount: Int,
    val avgCpuLoad: Double,
    val avgRamUsedMb: Double
)