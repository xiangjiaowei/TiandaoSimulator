package com.zheteng.tiandao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.heavenlydao.OriginEnergyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // 1. 世界日志流 (最多保留 100 条)
    private val _worldLogs = MutableStateFlow<List<String>>(emptyList())
    val worldLogs = _worldLogs.asStateFlow()

    // 2. 天道能量状态
    private val _originEnergy = MutableStateFlow(10000L)
    val originEnergy = _originEnergy.asStateFlow()

    init {
        // 订阅底层事件
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is SystemEvents.WorldLogEvent -> {
                        val current = _worldLogs.value.toMutableList()
                        current.add(0, "[${event.level}] ${event.message}")
                        if (current.size > 100) current.removeAt(current.size - 1)
                        _worldLogs.value = current
                    }
                    is SystemEvents.OriginEnergyChangedEvent -> {
                        _originEnergy.value = event.currentEnergy
                    }
                }
            }
        }
    }
}