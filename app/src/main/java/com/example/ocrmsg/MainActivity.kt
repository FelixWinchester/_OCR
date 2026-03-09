package com.example.ocrmsg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ocrmsg.ui.screen.MainScreen
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