package com.example.ocrmsg.data

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

object TesseractHelper {

    private const val LANG = "rus"

    fun init(context: Context): String {
        val tessDir = File(context.filesDir, "tessdata")
        if (!tessDir.exists()) tessDir.mkdirs()

        val dataFile = File(tessDir, "$LANG.traineddata")
        if (!dataFile.exists()) {
            context.assets.open("tessdata/$LANG.traineddata").use { input ->
                FileOutputStream(dataFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        return context.filesDir.absolutePath
    }

    fun recognize(context: Context, bitmap: Bitmap): String {
        val dataPath = init(context)
        val api = TessBaseAPI()

        // Конвертируем hardware bitmap в software — Tesseract не поддерживает hardware bitmaps
        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        return try {
            if (!api.init(dataPath, LANG)) {
                throw Exception("Tesseract не инициализирован — проверь tessdata файл")
            }
            api.setImage(softwareBitmap)
            api.utF8Text ?: ""
        } finally {
            api.recycle()
            // Освобождаем копию только если создавали новую
            if (softwareBitmap !== bitmap) softwareBitmap.recycle()
        }
    }
}
