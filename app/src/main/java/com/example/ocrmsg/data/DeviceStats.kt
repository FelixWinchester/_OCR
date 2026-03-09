package com.example.ocrmsg.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

data class DeviceStats(
    val androidVersion: String,
    val deviceModel: String,
    val cpuLoadPct: Int,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val batteryTempC: Float
)

object DeviceStatsCollector {

    fun collect(context: Context): DeviceStats {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val ramTotalMb = memInfo.totalMem / 1024 / 1024
        val ramUsedMb  = (memInfo.totalMem - memInfo.availMem) / 1024 / 1024

        // Температура батареи
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTempC = tempRaw / 10f

        // CPU — читаем /proc/stat (работает без root на большинстве устройств)
        val cpuLoad = readCpuLoad()

        return DeviceStats(
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel    = "${Build.MANUFACTURER} ${Build.MODEL}",
            cpuLoadPct     = cpuLoad,
            ramUsedMb      = ramUsedMb,
            ramTotalMb     = ramTotalMb,
            batteryTempC   = batteryTempC
        )
    }

    private fun readCpuLoad(): Int {
        return try {
            val lines1 = File("/proc/stat").readLines()
            val parts1 = lines1[0].trim().split("\\s+".toRegex())
            val idle1  = parts1[4].toLong()
            val total1 = parts1.drop(1).sumOf { it.toLong() }

            Thread.sleep(200)

            val lines2 = File("/proc/stat").readLines()
            val parts2 = lines2[0].trim().split("\\s+".toRegex())
            val idle2  = parts2[4].toLong()
            val total2 = parts2.drop(1).sumOf { it.toLong() }

            val deltaIdle  = idle2 - idle1
            val deltaTotal = total2 - total1

            if (deltaTotal == 0L) 0
            else ((1.0 - deltaIdle.toDouble() / deltaTotal) * 100).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            0
        }
    }
}

// нужен import для File внутри object
private val File = java.io.File::class.java.let { java.io.File("/proc/stat") }.let {
    object {
        operator fun invoke(path: String) = java.io.File(path)
    }
}