package com.zheteng.tiandao.core.eventbus

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局非阻塞事件总线
 * 专为极客级高频 ECS 架构打造的无锁分发中心。
 */
object EventBus {

    // 缓冲池容量：应对 1 个 Tick 瞬间爆发上万次杀戮事件的极端情况
    private const val BUS_BUFFER_CAPACITY = 10000

    /**
     * 核心发射器。
     * 配置策略：
     * 1. replay = 0：不缓存历史事件给新的订阅者（错过了就是错过了，符合时间流逝的残酷性）。
     * 2. extraBufferCapacity：提供 1 万个事件的缓冲池。
     * 3. onBufferOverflow = BufferOverflow.DROP_OLDEST：当消费端（如 UI 渲染）卡顿导致缓冲池满时，
     *    直接丢弃最老的事件，**绝对不挂起/阻塞**发射端（ECS System）。
     */
    private val _events = MutableSharedFlow<SystemEvents.TiandaoEvent>(
        replay = 0,
        extraBufferCapacity = BUS_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * 对外暴露的只读数据流，供 ViewModel 和 Logger 订阅。
     */
    val events: SharedFlow<SystemEvents.TiandaoEvent> = _events.asSharedFlow()

    /**
     * 高频无锁投递接口。
     * System 层调用此方法抛出事件。使用 tryEmit 确保完全非阻塞。
     */
    fun post(event: SystemEvents.TiandaoEvent) {
        // tryEmit 会尝试将事件塞入缓冲池，如果池子满了（触发丢弃策略）也必定立即返回 true，不阻塞线程。
        _events.tryEmit(event)
    }
}