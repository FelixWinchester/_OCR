package com.example.ocrmsg.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CameraHelper {

    fun createImageUri(context: Context): Uri {
        val dir = File(context.cacheDir, "images").also { it.mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

