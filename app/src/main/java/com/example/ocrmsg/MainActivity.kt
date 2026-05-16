package com.example.ocrmsg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocrmsg.ui.screen.BatchScreen
import com.example.ocrmsg.ui.screen.MainScreen
import com.example.ocrmsg.ui.theme.AccentCyan
import com.example.ocrmsg.ui.theme.BackgroundDark
import com.example.ocrmsg.ui.theme.OCRmsgTheme
import com.example.ocrmsg.ui.theme.SurfaceDark
import com.example.ocrmsg.ui.theme.TextMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OCRmsgTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Default.Image, contentDescription = "Одиночный") },
                    label    = { Text("Одиночный", fontSize = 11.sp) },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = AccentCyan,
                        selectedTextColor   = AccentCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor      = AccentCyan.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Default.GridView, contentDescription = "Пакетный") },
                    label    = { Text("Пакетный", fontSize = 11.sp) },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = AccentCyan,
                        selectedTextColor   = AccentCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor      = AccentCyan.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> MainScreen(modifier = Modifier.padding(padding))
            1 -> BatchScreen(modifier = Modifier.padding(padding))
        }
    }
}