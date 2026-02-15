package com.example.ocrmsg

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocrmsg.ui.theme.OCRmsgTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OCRmsgTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {

    val context = LocalContext.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val source = ImageDecoder.createSource(context.contentResolver, it)
            bitmap = ImageDecoder.decodeBitmap(source)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Карточка выбора изображения
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { showSheet = true },
                contentAlignment = Alignment.Center
            ) {

                if (bitmap == null) {
                    Text(
                        text = "Нажмите для выбора изображения",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Кнопка запуска
            Button(
                onClick = { /* позже подключим OCR */ },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Text("Начать распознавание", fontSize = 18.sp)
            }
        }

        // Bottom Sheet
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {

                    ListItem(
                        headlineContent = { Text("Галерея") },
                        leadingContent = {
                            Icon(Icons.Default.Photo, null)
                        },
                        modifier = Modifier.clickable {
                            showSheet = false
                            galleryLauncher.launch("image/*")
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Камера") },
                        leadingContent = {
                            Icon(Icons.Default.CameraAlt, null)
                        },
                        modifier = Modifier.clickable {
                            showSheet = false
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Отмена") },
                        modifier = Modifier.clickable {
                            showSheet = false
                        }
                    )
                }
            }
        }
    }
}
