package com.example.ocrmsg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocrmsg.data.DeviceStats
import com.example.ocrmsg.data.OcrResult
import com.example.ocrmsg.ui.theme.*

@Composable
fun StatsPanel(
    ocrResult: OcrResult?,
    deviceStats: DeviceStats?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Бенчмарк модели ──────────────────────────────────────
        StatCard(title = "БЕНЧМАРК МОДЕЛИ") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Препроц.",  ocrResult?.let { "${it.preprocessMs} мс" } ?: "—", AccentCyan)
                StatItem("Инференс", ocrResult?.let { "${it.inferenceMs} мс" } ?: "—", AccentCyan)
                StatItem("Итого",    ocrResult?.let { "${it.totalMs} мс" } ?: "—", AccentCyan)
            }

            StatDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Блоков",   ocrResult?.blockCount?.toString() ?: "—")
                StatItem("Строк",    ocrResult?.lineCount?.toString() ?: "—")
                StatItem("Слов",     ocrResult?.wordCount?.toString() ?: "—")
                StatItem("Символов", ocrResult?.symbolCount?.toString() ?: "—")
            }

            StatDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val confPct = ocrResult?.let { (it.avgConfidence * 100).toInt() }
                val confColor = when {
                    confPct == null -> TextSecondary
                    confPct >= 80   -> StatusGreen
                    confPct >= 50   -> StatusAmber
                    else            -> StatusRed
                }
                StatItem("Уверенность", confPct?.let { "$it%" } ?: "—", confColor)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlagChip("Abc", ocrResult?.hasLatinChars == true)
                    FlagChip("123", ocrResult?.hasDigits == true)
                    FlagChip("!@#", ocrResult?.hasSpecialChars == true)
                }
            }

            StatDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    "Разрешение",
                    ocrResult?.let { "${it.imageWidthPx}×${it.imageHeightPx}" } ?: "—"
                )
                StatItem(
                    "Размер файла",
                    ocrResult?.let { "${it.imageSizeKb} КБ" } ?: "—"
                )
            }

            // ── Метрики качества — только если введён эталон ─────
            if (ocrResult?.cer != null) {
                StatDivider()

                Text(
                    text = "МЕТРИКИ КАЧЕСТВА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cerPct = ((ocrResult.cer) * 100).toInt()
                    val werPct = ((ocrResult.wer ?: 0f) * 100).toInt()

                    StatItem("CER",       "${cerPct}%",  metricColor(cerPct))
                    StatItem("WER",       "${werPct}%",  metricColor(werPct))
                    StatItem("Precision", "${((ocrResult.precision ?: 0f) * 100).toInt()}%", AccentCyan)
                    StatItem("Recall",    "${((ocrResult.recall ?: 0f) * 100).toInt()}%",    AccentCyan)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (ocrResult.exactMatch == true) StatusGreen.copy(alpha = 0.15f)
                                else StatusRed.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (ocrResult.exactMatch == true) "✓ Match" else "✗ Diff",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ocrResult.exactMatch == true) StatusGreen else StatusRed
                        )
                    }
                }
            }
        }

        // ── Устройство ────────────────────────────────────────────
        StatCard(title = "УСТРОЙСТВО") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("CPU",
                    deviceStats?.let { "${it.cpuLoadPct}%" } ?: "—",
                    cpuColor(deviceStats?.cpuLoadPct))
                StatItem("RAM используется",
                    deviceStats?.let { "${it.ramUsedMb} МБ" } ?: "—")
                StatItem("RAM всего",
                    deviceStats?.let { "${it.ramTotalMb} МБ" } ?: "—")
                StatItem("Темп.",
                    deviceStats?.let { "${it.batteryTempC}°C" } ?: "—",
                    tempColor(deviceStats?.batteryTempC))
            }

            StatDivider()

            if (deviceStats != null) {
                Text(
                    text = deviceStats.deviceModel,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = deviceStats.androidVersion,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            } else {
                Text(
                    text = "Запустите распознавание для сбора данных",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

// ── Вспомогательные компоненты ────────────────────────────────

@Composable
private fun StatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceVariant, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        content()
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SurfaceVariant)
    )
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color = AccentCyan
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun FlagChip(label: String, active: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) AccentCyan.copy(alpha = 0.15f) else SurfaceVariant)
            .border(
                1.dp,
                if (active) AccentCyan.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) AccentCyan else TextMuted
        )
    }
}

private fun metricColor(pct: Int): Color = when {
    pct <= 10 -> StatusGreen
    pct <= 30 -> StatusAmber
    else      -> StatusRed
}

private fun cpuColor(pct: Int?): Color = when {
    pct == null -> TextSecondary
    pct >= 80   -> StatusRed
    pct >= 50   -> StatusAmber
    else        -> StatusGreen
}

private fun tempColor(temp: Float?): Color = when {
    temp == null -> TextSecondary
    temp >= 45f  -> StatusRed
    temp >= 35f  -> StatusAmber
    else         -> StatusGreen
}