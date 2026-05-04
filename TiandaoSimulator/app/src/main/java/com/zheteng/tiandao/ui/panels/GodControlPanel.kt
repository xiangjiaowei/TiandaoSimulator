package com.zheteng.tiandao.ui.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zheteng.tiandao.heavenlydao.GodModeApi

@Composable
fun GodControlPanel(originEnergy: Double) {
    var targetEntityId by remember { mutableStateOf("") }
    var tribulationIntensity by remember { mutableStateOf(1f) }

    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "天道干预枢纽", style = MaterialTheme.typography.titleMedium)
            Text(text = "当前可用本源能量: $originEnergy", color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(16.dp))

            // 目标输入与滑块 (纯数据绑定)
            OutlinedTextField(
                value = targetEntityId,
                onValueChange = { targetEntityId = it },
                label = { Text("目标修士 ID") }
            )

            Text(text = "雷劫强度 (指数级消耗): ${tribulationIntensity.toInt()}")
            Slider(
                value = tribulationIntensity,
                onValueChange = { tribulationIntensity = it },
                valueRange = 1f..10f
            )

            Button(
                onClick = {
                    val id = targetEntityId.toIntOrNull()
                    if (id != null) {
                        // 纯指令下发，杜绝 UI 层包含扣除能量的逻辑
                        GodModeApi.castHeavenlyTribulation(id, tribulationIntensity.toDouble())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("降下九霄神雷")
            }
        }
    }
}