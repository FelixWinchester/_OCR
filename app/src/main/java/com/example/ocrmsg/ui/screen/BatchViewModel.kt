package com.example.ocrmsg.ui.screen

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrmsg.data.BatchItemResult
import com.example.ocrmsg.data.BatchProcessor
import com.example.ocrmsg.data.BatchSummary
import com.example.ocrmsg.model.OcrModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BatchUiState(
    val items: List<Pair<Uri, String>> = emptyList(),
    val isRunning: Boolean             = false,
    val currentIndex: Int              = 0,
    val currentFileName: String        = "",
    val results: List<BatchItemResult> = emptyList(),
    val summary: BatchSummary?         = null,
    val savedPath: String?             = null,
    val error: String?                 = null,
    val csvLoaded: Boolean             = false,
    val csvInfo: String                = "",
    val selectedModel: OcrModel        = OcrModel.ML_KIT
)

class BatchViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(BatchUiState())
    val uiState: StateFlow<BatchUiState> = _uiState

    private var job: Job? = null

    // ── Модель ────────────────────────────────────────────────────────

    fun setModel(model: OcrModel) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    // ── CSV ───────────────────────────────────────────────────────────

    fun loadCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lines = context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.readLines()
                    ?: throw Exception("Не удалось открыть файл")

                val map = mutableMapOf<String, String>()
                val dataLines = if (lines.firstOrNull()
                        ?.lowercase()?.startsWith("filename") == true
                ) lines.drop(1) else lines

                dataLines.forEach { line ->
                    if (line.isBlank()) return@forEach
                    val commaIndex = line.indexOf(',')
                    if (commaIndex == -1) return@forEach
                    val fileName    = line.substring(0, commaIndex).trim()
                    val groundTruth = line.substring(commaIndex + 1).trim()
                    map[fileName] = groundTruth
                }

                val current = _uiState.value.items.toMutableList()
                var matched = 0
                val updated = current.map { (uri, gt) ->
                    val name  = uri.lastPathSegment?.substringAfterLast("/") ?: ""
                    val found = map[name] ?: map[name.substringBeforeLast(".")] ?: gt
                    if (found != gt) matched++
                    Pair(uri, found)
                }

                _uiState.value = _uiState.value.copy(
                    items     = updated,
                    csvLoaded = true,
                    csvInfo   = "CSV загружен: ${map.size} эталонов, совпало $matched из ${current.size}",
                    error     = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка загрузки CSV: ${e.message}"
                )
            }
        }
    }

    // ── Изображения ───────────────────────────────────────────────────

    fun addImages(uris: List<Uri>) {
        val current = _uiState.value.items.toMutableList()
        uris.forEach { uri ->
            if (current.none { it.first == uri }) current.add(Pair(uri, ""))
        }
        _uiState.value = _uiState.value.copy(items = current)
    }

    fun updateGroundTruth(index: Int, text: String) {
        val current = _uiState.value.items.toMutableList()
        if (index in current.indices) {
            current[index] = Pair(current[index].first, text)
            _uiState.value = _uiState.value.copy(items = current)
        }
    }

    fun removeImage(index: Int) {
        val current = _uiState.value.items.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(items = current)
        }
    }

    fun clearAll() {
        _uiState.value = BatchUiState()
    }

    // ── Запуск ────────────────────────────────────────────────────────

    fun start(context: Context) {
        val items = _uiState.value.items
        if (items.isEmpty()) return
        val model = _uiState.value.selectedModel

        job = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isRunning    = true,
                results      = emptyList(),
                summary      = null,
                savedPath    = null,
                error        = null,
                currentIndex = 0
            )

            val results = BatchProcessor.process(
                context    = context,
                items      = items,
                model      = model,
                onProgress = { current, _, fileName ->
                    _uiState.value = _uiState.value.copy(
                        currentIndex    = current,
                        currentFileName = fileName
                    )
                }
            )

            val summary = BatchProcessor.computeSummary(results)

            _uiState.value = _uiState.value.copy(
                isRunning = false,
                results   = results,
                summary   = summary
            )
        }
    }

    fun stop() {
        job?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    // ── Сохранение ────────────────────────────────────────────────────

    fun saveToJson() {
        val state = _uiState.value
        if (state.results.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat(
                    "yyyy-MM-dd_HH-mm-ss", Locale.getDefault()
                ).format(Date())

                val json = JSONObject().apply {
                    put("timestamp",    timestamp)
                    put("model",        state.selectedModel.displayName)
                    put("total_images", state.summary?.totalImages ?: 0)

                    state.summary?.let { s ->
                        put("summary", JSONObject().apply {
                            put("success_count",     s.successCount)
                            put("error_count",       s.errorCount)
                            put("avg_inference_ms",  s.avgInferenceMs)
                            put("avg_total_ms",      s.avgTotalMs)
                            put("avg_cer",           s.avgCer ?: "n/a")
                            put("avg_wer",           s.avgWer ?: "n/a")
                            put("avg_precision",     s.avgPrecision ?: "n/a")
                            put("avg_recall",        s.avgRecall ?: "n/a")
                            put("exact_match_count", s.exactMatchCount)
                            put("avg_cpu_load_pct",  s.avgCpuLoad)
                            put("avg_ram_used_mb",   s.avgRamUsedMb)
                        })
                    }

                    put("results", JSONArray().apply {
                        state.results.forEach { item ->
                            put(JSONObject().apply {
                                put("file",         item.fileName)
                                put("ground_truth", item.groundTruth)
                                put("error",        item.error ?: JSONObject.NULL)
                                item.ocrResult?.let { ocr ->
                                    put("ocr_text",    ocr.recognizedText)
                                    put("inference_ms",ocr.inferenceMs)
                                    put("total_ms",    ocr.totalMs)
                                    put("words",       ocr.wordCount)
                                    put("symbols",     ocr.symbolCount)
                                    put("cer",         ocr.cer ?: JSONObject.NULL)
                                    put("wer",         ocr.wer ?: JSONObject.NULL)
                                    put("exact_match", ocr.exactMatch ?: JSONObject.NULL)
                                    put("precision",   ocr.precision ?: JSONObject.NULL)
                                    put("recall",      ocr.recall ?: JSONObject.NULL)
                                }
                                item.deviceStats?.let { dev ->
                                    put("cpu_pct",      dev.cpuLoadPct)
                                    put("ram_used_mb",  dev.ramUsedMb)
                                    put("battery_temp", dev.batteryTempC)
                                }
                            })
                        }
                    })
                }

                val dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, "ocrmsg_batch_$timestamp.json")
                file.writeText(json.toString(2))

                _uiState.value = _uiState.value.copy(savedPath = file.absolutePath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun saveToCsv() {
        val state = _uiState.value
        if (state.results.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat(
                    "yyyy-MM-dd_HH-mm-ss", Locale.getDefault()
                ).format(Date())

                val sb = StringBuilder()
                sb.appendLine("file,ground_truth,ocr_text,inference_ms,total_ms,words,symbols,cer,wer,exact_match,precision,recall,cpu_pct,ram_used_mb,battery_temp")

                state.results.forEach { item ->
                    val ocr = item.ocrResult
                    val dev = item.deviceStats
                    fun String.csv() = "\"${replace("\"", "\"\"")}\""
                    sb.appendLine(listOf(
                        item.fileName.csv(),
                        item.groundTruth.csv(),
                        (ocr?.recognizedText ?: "").csv(),
                        ocr?.inferenceMs?.toString() ?: "",
                        ocr?.totalMs?.toString() ?: "",
                        ocr?.wordCount?.toString() ?: "",
                        ocr?.symbolCount?.toString() ?: "",
                        ocr?.cer?.let { "%.4f".format(it) } ?: "",
                        ocr?.wer?.let { "%.4f".format(it) } ?: "",
                        ocr?.exactMatch?.toString() ?: "",
                        ocr?.precision?.let { "%.4f".format(it) } ?: "",
                        ocr?.recall?.let { "%.4f".format(it) } ?: "",
                        dev?.cpuLoadPct?.toString() ?: "",
                        dev?.ramUsedMb?.toString() ?: "",
                        dev?.batteryTempC?.toString() ?: ""
                    ).joinToString(","))
                }

                val dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, "ocrmsg_batch_$timestamp.csv")
                file.writeText(sb.toString())

                _uiState.value = _uiState.value.copy(savedPath = file.absolutePath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка сохранения CSV: ${e.message}")
            }
        }
    }
}