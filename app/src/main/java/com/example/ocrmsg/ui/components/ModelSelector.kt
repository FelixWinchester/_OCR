package com.example.ocrmsg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocrmsg.model.OcrModel
import com.example.ocrmsg.ui.theme.AccentCyan
import com.example.ocrmsg.ui.theme.SurfaceVariant
import com.example.ocrmsg.ui.theme.TextSecondary

@Composable
fun ModelSelector(
    selected: OcrModel,
    onSelect: (OcrModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceVariant)
                .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = selected.displayName,
                fontSize = 13.sp,
                color = AccentCyan
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OcrModel.entries.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = model.displayName,
                            color = if (model == selected) AccentCyan else TextSecondary
                        )
                    },
                    onClick = {
                        onSelect(model)
                        expanded = false
                    }
                )
            }
        }
    }
}