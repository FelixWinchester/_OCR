package com.example.ocrmsg.ui.screen

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ocrmsg.ui.components.AppLogo
import com.example.ocrmsg.ui.components.ModelSelector
import com.example.ocrmsg.ui.components.StatsPanel
import com.example.ocrmsg.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = viewModel()
) {
    val context    = LocalContext.current
    val state      by vm.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    var showSheet  by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val source = ImageDecoder.createSource(context.contentResolver, it)
            vm.setBitmap(ImageDecoder.decodeBitmap(source))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) vm.onPhotoCaptured()
    }

    Scaffold(containerColor = BackgroundDark) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppLogo()
                ModelSelector(selected = state.selectedModel, onSelect = { vm.setModel(it) })
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDark)
                    .border(
                        1.dp,
                        if (state.bitmap != null) AccentCyan.copy(alpha = 0.5f) else SurfaceVariant,
                        RoundedCornerShape(24.dp)
                    )
                    .clickable(enabled = state.bitmap == null) { showSheet = true },
                contentAlignment = Alignment.Center
            ) {
                if (state.bitmap == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("＋", fontSize = 36.sp, color = AccentCyan)
                        Text("Выбрать изображение", fontSize = 15.sp, color = TextSecondary)
                        Text("jpg · png · webp", fontSize = 11.sp, color = TextMuted)
                    }
                } else {
                    Image(
                        bitmap = state.bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { vm.clearImage() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(BackgroundDark.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Удалить",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.groundTruth,
                onValueChange = { vm.setGroundTruth(it) },
                label = { Text("Эталонный текст (ground truth)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentCyan,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = AccentCyan
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (state.ocrResult != null) {
                val clipboardManager = LocalClipboardManager.current
                val recognizedText = state.ocrResult!!.recognizedText.ifBlank { "(текст не найден)" }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "РАСПОЗНАННЫЙ ТЕКСТ",
                                fontSize = 11.sp,
                                color = TextMuted,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceVariant)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(recognizedText))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Копировать",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Копировать", fontSize = 11.sp, color = AccentCyan)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp, max = 160.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = recognizedText,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { vm.recognize() },
                    enabled = state.bitmap != null && !state.isLoading,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = AccentCyan,
                        contentColor           = BackgroundDark,
                        disabledContainerColor = SurfaceVariant,
                        disabledContentColor   = TextMuted
                    ),
                    modifier = Modifier.weight(1f).height(54.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = BackgroundDark,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Распознать", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.ocrResult != null) {
                    IconButton(
                        onClick = { vm.saveToJson() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceVariant)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Сохранить JSON", tint = AccentCyan)
                    }
                }
            }

            state.savedPath?.let {
                Spacer(Modifier.height(8.dp))
                Text("✓ Сохранено: $it", fontSize = 11.sp, color = StatusGreen)
            }
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("✗ $it", fontSize = 11.sp, color = StatusRed)
            }

            Spacer(Modifier.height(16.dp))

            StatsPanel(ocrResult = state.ocrResult, deviceStats = state.deviceStats)

            Spacer(Modifier.height(24.dp))
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Источник изображения",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ListItem(
                        headlineContent = { Text("Галерея", color = TextPrimary) },
                        leadingContent  = { Icon(Icons.Default.Photo, null, tint = AccentCyan) },
                        colors = ListItemDefaults.colors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSheet = false; galleryLauncher.launch("image/*") }
                    )
                    ListItem(
                        headlineContent = { Text("Камера", color = TextPrimary) },
                        leadingContent  = { Icon(Icons.Default.CameraAlt, null, tint = AccentCyan) },
                        colors = ListItemDefaults.colors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showSheet = false
                                val uri = vm.prepareCameraUri(context)
                                cameraLauncher.launch(uri)
                            }
                    )
                }
            }
        }
    }
}