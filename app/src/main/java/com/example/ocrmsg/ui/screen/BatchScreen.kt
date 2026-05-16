package com.example.ocrmsg.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ocrmsg.data.BatchItemResult
import com.example.ocrmsg.model.OcrModel
import com.example.ocrmsg.ui.theme.*

private enum class ResultsView { CARDS, TABLE }

@Composable
fun BatchScreen(
    modifier: Modifier = Modifier,
    vm: BatchViewModel = viewModel()
) {
    val context = LocalContext.current
    val state   by vm.uiState.collectAsStateWithLifecycle()
    var resultsView    by remember { mutableStateOf(ResultsView.CARDS) }
    var showSaveMenu   by remember { mutableStateOf(false) }
    var showModelMenu  by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> -> vm.addImages(uris) }

    val csvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.loadCsv(context, it) } }

    Scaffold(containerColor = BackgroundDark) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Заголовок + выбор модели ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ПАКЕТНЫЙ РЕЖИМ", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = AccentCyan, letterSpacing = 3.sp)
                    Text("Загрузи изображения и CSV с эталонами",
                        fontSize = 12.sp, color = TextSecondary)
                }

                // Выбор модели
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceVariant)
                            .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(state.selectedModel.displayName, fontSize = 13.sp, color = AccentCyan)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
                            tint = AccentCyan, modifier = Modifier.size(16.dp))
                    }
                    // невидимая кнопка поверх
                    IconButton(
                        onClick = { showModelMenu = true },
                        modifier = Modifier.matchParentSize()
                    ) {}
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false }
                    ) {
                        OcrModel.entries.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Text(model.displayName,
                                        color = if (model == state.selectedModel)
                                            AccentCyan else TextSecondary)
                                },
                                onClick = { vm.setModel(model); showModelMenu = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Кнопки управления ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !state.isRunning,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = SurfaceVariant,
                        contentColor           = TextPrimary,
                        disabledContainerColor = SurfaceVariant,
                        disabledContentColor   = TextMuted
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) { Text("+ Фото", fontSize = 13.sp) }

                Button(
                    onClick = { csvPicker.launch("text/*") },
                    enabled = !state.isRunning,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = if (state.csvLoaded)
                            AccentCyan.copy(alpha = 0.2f) else SurfaceVariant,
                        contentColor           = if (state.csvLoaded) AccentCyan else TextPrimary,
                        disabledContainerColor = SurfaceVariant,
                        disabledContentColor   = TextMuted
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV", fontSize = 13.sp)
                }

                Button(
                    onClick = { if (state.isRunning) vm.stop() else vm.start(context) },
                    enabled = state.items.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = if (state.isRunning) StatusRed else AccentCyan,
                        contentColor           = BackgroundDark,
                        disabledContainerColor = SurfaceVariant,
                        disabledContentColor   = TextMuted
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(
                        if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isRunning) "Стоп" else "Старт", fontSize = 13.sp)
                }
            }

            // ── Кнопки результатов ────────────────────────────────────
            if (state.results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            resultsView = if (resultsView == ResultsView.CARDS)
                                ResultsView.TABLE else ResultsView.CARDS
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (resultsView == ResultsView.TABLE)
                                    AccentCyan.copy(alpha = 0.2f) else SurfaceVariant
                            )
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Таблица",
                            tint = AccentCyan)
                    }

                    Box {
                        IconButton(
                            onClick = { showSaveMenu = true },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceVariant)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Сохранить",
                                tint = AccentCyan)
                        }
                        DropdownMenu(
                            expanded = showSaveMenu,
                            onDismissRequest = { showSaveMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Сохранить JSON", color = TextPrimary) },
                                onClick = { vm.saveToJson(); showSaveMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Сохранить CSV", color = AccentCyan) },
                                onClick = { vm.saveToCsv(); showSaveMenu = false }
                            )
                        }
                    }

                    Button(
                        onClick = { vm.clearAll() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariant,
                            contentColor   = StatusRed
                        ),
                        modifier = Modifier.height(46.dp)
                    ) { Text("Очистить", fontSize = 12.sp) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── CSV инфо ──────────────────────────────────────────────
            if (state.csvLoaded && state.csvInfo.isNotBlank()) {
                Text("✓ ${state.csvInfo}", fontSize = 11.sp, color = StatusGreen)
                Spacer(Modifier.height(8.dp))
            }

            // ── Прогресс ──────────────────────────────────────────────
            if (state.isRunning) {
                val progress = if (state.items.isEmpty()) 0f
                else state.currentIndex.toFloat() / state.items.size.toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${state.currentIndex} / ${state.items.size}",
                            fontSize = 12.sp, color = AccentCyan)
                        Text(state.currentFileName, fontSize = 11.sp, color = TextSecondary)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = AccentCyan, trackColor = SurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Сводка ────────────────────────────────────────────────
            state.summary?.let { s ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("СВОДКА · ${state.selectedModel.displayName}",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 2.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryItem("Всего",   "${s.totalImages}")
                        SummaryItem("Успешно", "${s.successCount}", StatusGreen)
                        SummaryItem("Ошибок",  "${s.errorCount}",
                            if (s.errorCount > 0) StatusRed else TextSecondary)
                        SummaryItem("Совпало", "${s.exactMatchCount}", AccentCyan)
                    }
                    HorizontalDivider(color = SurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryItem("Avg инф.", "${s.avgInferenceMs.toLong()} мс")
                        SummaryItem("Avg CER",
                            s.avgCer?.let { "${"%.1f".format(it * 100)}%" } ?: "—",
                            metricColor(s.avgCer))
                        SummaryItem("Avg WER",
                            s.avgWer?.let { "${"%.1f".format(it * 100)}%" } ?: "—",
                            metricColor(s.avgWer))
                        SummaryItem("Avg CPU", "${s.avgCpuLoad.toInt()}%")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            state.savedPath?.let {
                Text("✓ Сохранено: $it", fontSize = 11.sp, color = StatusGreen)
                Spacer(Modifier.height(8.dp))
            }
            state.error?.let {
                Text("✗ $it", fontSize = 11.sp, color = StatusRed)
                Spacer(Modifier.height(8.dp))
            }

            // ── Контент ───────────────────────────────────────────────
            when {
                state.items.isEmpty() && state.results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Нет изображений", fontSize = 14.sp, color = TextMuted)
                            Text(
                                "1. Выбери модель сверху справа\n2. Нажми «+ Фото»\n3. Нажми «CSV» для эталонов\n4. Нажми «Старт»",
                                fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                state.results.isNotEmpty() && resultsView == ResultsView.TABLE -> {
                    ResultsTable(results = state.results, modifier = Modifier.weight(1f))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.results.isNotEmpty()) {
                            itemsIndexed(state.results) { _, item -> ResultItem(item) }
                        } else {
                            itemsIndexed(state.items) { index, (uri, groundTruth) ->
                                QueueItem(
                                    index               = index,
                                    uri                 = uri,
                                    groundTruth         = groundTruth,
                                    enabled             = !state.isRunning,
                                    onGroundTruthChange = { vm.updateGroundTruth(index, it) },
                                    onRemove            = { vm.removeImage(index) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ── Таблица ───────────────────────────────────────────────────

@Composable
private fun ResultsTable(results: List<BatchItemResult>, modifier: Modifier = Modifier) {
    val columns = listOf("Файл","Инф.мс","Симв.","Слов","CER%","WER%","Prec%","Rec%","Match")
    val widths  = listOf(120.dp,70.dp,60.dp,60.dp,60.dp,60.dp,60.dp,60.dp,60.dp)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(SurfaceVariant)
                .padding(vertical = 8.dp)
        ) {
            columns.forEachIndexed { i, col ->
                TableCell(col, widths[i], AccentCyan, isBold = true)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            itemsIndexed(results) { index, item ->
                val ocr = item.ocrResult
                val bg  = if (index % 2 == 0) SurfaceDark else CardDark
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(bg)
                        .padding(vertical = 8.dp)
                ) {
                    TableCell(item.fileName, widths[0], TextSecondary)
                    TableCell(ocr?.inferenceMs?.toString() ?: "ERR", widths[1],
                        if (item.error != null) StatusRed else AccentCyan)
                    TableCell(ocr?.symbolCount?.toString() ?: "—", widths[2])
                    TableCell(ocr?.wordCount?.toString()   ?: "—", widths[3])
                    TableCell(ocr?.cer?.let { "${"%.1f".format(it*100)}" } ?: "—",
                        widths[4], metricColor(ocr?.cer?.toDouble()))
                    TableCell(ocr?.wer?.let { "${"%.1f".format(it*100)}" } ?: "—",
                        widths[5], metricColor(ocr?.wer?.toDouble()))
                    TableCell(ocr?.precision?.let { "${"%.1f".format(it*100)}" } ?: "—",
                        widths[6], AccentCyan)
                    TableCell(ocr?.recall?.let { "${"%.1f".format(it*100)}" } ?: "—",
                        widths[7], AccentCyan)
                    TableCell(
                        when (ocr?.exactMatch) { true -> "✓"; false -> "✗"; null -> "—" },
                        widths[8],
                        when (ocr?.exactMatch) { true -> StatusGreen; false -> StatusRed; null -> TextMuted }
                    )
                }
            }

            item {
                val successful  = results.filter { it.ocrResult != null }
                val withMetrics = successful.filter { it.ocrResult?.cer != null }
                if (successful.isNotEmpty()) {
                    val avgInf  = successful.map { it.ocrResult!!.inferenceMs }.average()
                    val avgSymb = successful.map { it.ocrResult!!.symbolCount.toDouble() }.average()
                    val avgWord = successful.map { it.ocrResult!!.wordCount.toDouble() }.average()
                    val avgCer  = withMetrics.map { it.ocrResult!!.cer!!.toDouble() }.average().takeIf { withMetrics.isNotEmpty() }
                    val avgWer  = withMetrics.map { it.ocrResult!!.wer!!.toDouble() }.average().takeIf { withMetrics.isNotEmpty() }
                    val avgPrec = withMetrics.map { it.ocrResult!!.precision!!.toDouble() }.average().takeIf { withMetrics.isNotEmpty() }
                    val avgRec  = withMetrics.map { it.ocrResult!!.recall!!.toDouble() }.average().takeIf { withMetrics.isNotEmpty() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(SurfaceVariant)
                            .padding(vertical = 8.dp)
                    ) {
                        TableCell("СРЕДНЕЕ", widths[0], AccentCyan, isBold = true)
                        TableCell("${avgInf.toLong()}", widths[1], AccentCyan, isBold = true)
                        TableCell("${avgSymb.toInt()}", widths[2], isBold = true)
                        TableCell("${avgWord.toInt()}", widths[3], isBold = true)
                        TableCell(avgCer?.let { "${"%.1f".format(it*100)}" } ?: "—",
                            widths[4], metricColor(avgCer), isBold = true)
                        TableCell(avgWer?.let { "${"%.1f".format(it*100)}" } ?: "—",
                            widths[5], metricColor(avgWer), isBold = true)
                        TableCell(avgPrec?.let { "${"%.1f".format(it*100)}" } ?: "—",
                            widths[6], AccentCyan, isBold = true)
                        TableCell(avgRec?.let { "${"%.1f".format(it*100)}" } ?: "—",
                            widths[7], AccentCyan, isBold = true)
                        TableCell("—", widths[8], isBold = true)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TableCell(text: String, width: Dp, color: Color = TextSecondary, isBold: Boolean = false) {
    Text(
        text = text, fontSize = 11.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width).padding(horizontal = 4.dp)
    )
}

@Composable
private fun QueueItem(
    index: Int, uri: Uri, groundTruth: String, enabled: Boolean,
    onGroundTruthChange: (String) -> Unit, onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}. ${uri.lastPathSegment?.substringAfterLast("/") ?: "image"}",
                fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f)
            )
            if (enabled) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Удалить",
                        tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
        OutlinedTextField(
            value = groundTruth, onValueChange = onGroundTruthChange, enabled = enabled,
            label = { Text("Эталон", color = TextSecondary, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(), maxLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AccentCyan,
                unfocusedBorderColor = SurfaceVariant,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
                cursorColor          = AccentCyan
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun ResultItem(item: BatchItemResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp,
                if (item.error != null) StatusRed.copy(alpha = 0.4f)
                else AccentCyan.copy(alpha = 0.2f),
                RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(item.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (item.error != null) {
            Text("✗ ${item.error}", fontSize = 11.sp, color = StatusRed)
        } else {
            item.ocrResult?.let { ocr ->
                Text(
                    ocr.recognizedText.take(80).let {
                        if (ocr.recognizedText.length > 80) "$it…" else it
                    },
                    fontSize = 12.sp, color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("${ocr.inferenceMs}мс")
                    ocr.cer?.let { MiniStat("CER ${"%.1f".format(it*100)}%", metricColor(it.toDouble())) }
                    ocr.wer?.let { MiniStat("WER ${"%.1f".format(it*100)}%", metricColor(it.toDouble())) }
                    ocr.exactMatch?.let {
                        MiniStat(if (it) "✓ Match" else "✗ Diff", if (it) StatusGreen else StatusRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color = AccentCyan) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun MiniStat(text: String, color: Color = TextSecondary) {
    Text(text, fontSize = 11.sp, color = color)
}

private fun metricColor(value: Double?): Color {
    if (value == null) return TextSecondary
    val pct = (value * 100).toInt()
    return when { pct <= 10 -> StatusGreen; pct <= 30 -> StatusAmber; else -> StatusRed }
}