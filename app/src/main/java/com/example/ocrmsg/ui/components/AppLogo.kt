package com.example.ocrmsg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocrmsg.ui.theme.AccentCyan
import com.example.ocrmsg.ui.theme.BackgroundDark
import com.example.ocrmsg.ui.theme.TextSecondary

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Иконка-плашка
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentCyan),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OCR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = BackgroundDark,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "OCR",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
                letterSpacing = 3.sp
            )
            Text(
                text = "Text Recognition Tool",
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        }
    }
}