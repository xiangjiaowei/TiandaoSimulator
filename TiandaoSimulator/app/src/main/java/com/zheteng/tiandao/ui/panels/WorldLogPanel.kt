package com.zheteng.tiandao.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorldLogPanel(logs: List<String>) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF1E1E1E))
        .padding(8.dp)
    ) {
        Text(
            text = "世界线推演日志 (实时)",
            color = Color.Green,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))

        // 数据驱动的被动列表，列表项增减自动触发重组，无主线程重绘负担
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs) { logMsg ->
                Text(
                    text = "> $logMsg",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}