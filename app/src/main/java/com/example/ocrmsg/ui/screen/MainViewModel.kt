package com.example.ocrmsg.ui.screen

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrmsg.data.CameraHelper
import com.example.ocrmsg.data.DeviceStats
import com.example.ocrmsg.data.DeviceStatsCollector
import com.example.ocrmsg.data.OcrRepository
import com.example.ocrmsg.data.OcrResult
import com.example.ocrmsg.model.OcrModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val bitmap: Bitmap?           = null,
    val isLoading: Boolean        = false,
    val ocrResult: OcrResult?     = null,
    val deviceStats: DeviceStats? = null,
    val selectedModel: OcrModel   = OcrModel.ML_KIT,
    val savedPath: String?        = null,
    val error: String?            = null,
    val cameraUri: Uri?           = null,
    val groundTruth: String       = ""
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    fun setBitmap(bitmap: Bitmap?) {
        _uiState.value = _uiState.value.copy(
            bitmap    = bitmap,
            ocrResult = null,
            savedPath = null,
            error     = null
        )
    }

    fun setGroundTruth(text: String) {
        _uiState.value = _uiState.value.copy(groundTruth = text)
    }

    fun setModel(model: OcrModel) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun clearImage() {
        _uiState.value = MainUiState()
    }

    fun prepareCameraUri(context: Context): Uri {
        val uri = CameraHelper.createImageUri(context)
        _uiState.value = _uiState.value.copy(cameraUri = uri)
        return uri
    }

    fun onPhotoCaptured() {
        val uri = _uiState.value.cameraUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val source = ImageDecoder.createSource(
                    getApplication<Application>().contentResolver, uri
                )
                val bmp = ImageDecoder.decodeBitmap(source)
                _uiState.value = _uiState.value.copy(
                    bitmap    = bmp,
                    ocrResult = null,
                    savedPath = null,
                    error     = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка камеры: ${e.message}")
            }
        }
    }

    fun recognize() {
        val bitmap  = _uiState.value.bitmap ?: return
        val model   = _uiState.value.selectedModel
        val context = getApplication<Application>()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val ocrResult = withContext(Dispatchers.IO) {
                    when (model) {
                        OcrModel.ML_KIT   -> OcrRepository.runMlKit(bitmap)
                        OcrModel.TESSERACT -> OcrRepository.runTesseract(context, bitmap)
                    }
                }

                val deviceStats = withContext(Dispatchers.IO) {
                    DeviceStatsCollector.collect(context)
                }

                val finalResult = if (_uiState.value.groundTruth.isNotBlank()) {
                    OcrRepository.applyGroundTruth(ocrResult, _uiState.value.groundTruth)
                } else {
                    ocrResult
                }

                _uiState.value = _uiState.value.copy(
                    isLoading   = false,
                    ocrResult   = finalResult,
                    deviceStats = deviceStats
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error     = e.message ?: "Ошибка распознавания"
                )
            }
        }
    }

    fun saveToJson() {
        val state       = _uiState.value
        val ocrResult   = state.ocrResult   ?: return
        val deviceStats = state.deviceStats ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = OcrRepository.saveResultToDownloads(
                    context     = getApplication(),
                    ocrResult   = ocrResult,
                    deviceStats = deviceStats,
                    modelName   = state.selectedModel.displayName
                )
                _uiState.value = _uiState.value.copy(savedPath = path)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка сохранения: ${e.message}")
            }
        }
    }
}