package com.zheteng.tiandao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.heavenlydao.OriginEnergyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 纯数据形态的全局 UI 状态字典
 * Compose 层仅依赖此对象进行 Dirty Flag 差异比对重绘
 */
data class WorldUiState(
    val originEnergy: Double = 0.0,
    val activeEntitiesCount: Int = 0,
    val isApocalypseEra: Boolean = false // 末法时代标识
)

class MainViewModel : ViewModel() {

    // 核心面板状态
    private val _uiState = MutableStateFlow(WorldUiState())
    val uiState: StateFlow<WorldUiState> = _uiState.asStateFlow()

    // 世界日志流，作为 Ring Buffer 限制最大容量，严防 OOM 内存泄漏
    private val _worldLogs = MutableStateFlow<List<String>>(emptyList())
    val worldLogs: StateFlow<List<String>> = _worldLogs.asStateFlow()

    private val MAX_LOG_SIZE = 100

    init {
        // 启动协程，在后台线程 (Dispatchers.Default) 挂起监听天道万物运转的声音
        viewModelScope.launch(Dispatchers.Default) {
            EventBus.events.collect { event ->
                handleSystemEvent(event)
            }
        }
    }

    /**
     * 将底层的离散事件翻译为 UI 状态
     */
    private fun handleSystemEvent(event: SystemEvents.TiandaoEvent) {
        when (event) {
            is SystemEvents.LogEvent -> {
                appendLog(event.message)
            }

            is SystemEvents.WhaleFallEvent -> {
                // 收到陨落事件，主动同步一次天道本源数值
                _uiState.value = _uiState.value.copy(
                    originEnergy = OriginEnergyManager.currentOriginEnergy
                )

                // 翻译底层数据为人类可读的史书记录
                val msg = "【天地同悲】实体[${event.entityId}]陨落，" +
                        "其${String.format("%.1f", event.returnedQi)}灵气反哺网格[${event.gridId}]"
                appendLog(msg)
            }

            is SystemEvents.TribulationEvent -> {
                val msg = "【天罚降世】九霄神雷锁定了实体[${event.targetEntityId}]，毁灭强度：${event.intensity}"
                appendLog(msg)

                // 降下雷劫必定消耗了本源，同步更新UI
                _uiState.value = _uiState.value.copy(
                    originEnergy = OriginEnergyManager.currentOriginEnergy
                )
            }

            is SystemEvents.SpiritualRainEvent -> {
                val msg = "【天降甘霖】网格[${event.gridId}]迎来灵气复苏，浓度上升：${event.amount}"
                appendLog(msg)
                _uiState.value = _uiState.value.copy(
                    originEnergy = OriginEnergyManager.currentOriginEnergy
                )
            }
            // 后续可在此拦截更多事件，例如：SystemEvents.BreakthroughEvent (境界突破)
        }
    }

    /**
     * 线程安全的日志追加方法
     */
    private fun appendLog(message: String) {
        // 由于在 Default 调度器执行，这里做复制操作，更新 state，触发 Compose 重组
        val newLogs = _worldLogs.value.toMutableList().apply {
            add(0, message) // 最新日志插在最顶端
            if (size > MAX_LOG_SIZE) {
                removeLast()
            }
        }
        _worldLogs.value = newLogs
    }
}