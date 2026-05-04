package com.zheteng.tiandao.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.zheteng.tiandao.ui.panels.GodControlPanel
import com.zheteng.tiandao.ui.panels.SectTopologyView
import com.zheteng.tiandao.ui.panels.WorldLogPanel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: 在这里或通过前台 Service 唤醒底层的 TickEngine 引擎
        // TickEngine.start()

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val uiState by viewModel.uiState.collectAsState()
                    val logs by viewModel.worldLogs.collectAsState()

                    // 大战略游戏常见的面板布局结构
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 左侧：天道控制与宗门拓扑 (占 60% 屏幕宽度)
                        Column(modifier = Modifier.weight(0.6f)) {
                            GodControlPanel(originEnergy = uiState.originEnergy)
                            Box(modifier = Modifier.weight(1f)) {
                                SectTopologyView()
                            }
                        }

                        // 右侧：实时世界日志推演 (占 40% 屏幕宽度)
                        Box(modifier = Modifier.weight(0.4f)) {
                            WorldLogPanel(logs = logs)
                        }
                    }
                }
            }
        }
    }
}