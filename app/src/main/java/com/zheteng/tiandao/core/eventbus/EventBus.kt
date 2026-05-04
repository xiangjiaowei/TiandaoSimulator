package com.zheteng.tiandao.core.eventbus

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局轻量级事件总线 (基于 Kotlin 协程 SharedFlow)
 * 彻底解耦底层高频逻辑推演与表层 UI 渲染的纽带。
 */
object EventBus {

    /**
     * 核心事件流。
     * - replay = 0: 新订阅者不需要接收历史旧事件。
     * - extraBufferCapacity = 64: 预留缓冲池，应对 X100 倍速下的事件爆发。
     * - onBufferOverflow = BufferOverflow.DROP_OLDEST:
     *   【核心防御机制】如果 UI 线程发生严重卡顿（无法及时渲染），新产生的事件会直接覆盖老事件。
     *   宁可丢失几条 UI 的渲染日志，也绝不让底层 Tick 推演引擎发生任何阻塞！
     */
    private val _events = MutableSharedFlow<Any>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * 暴露给外部（如 UI 层 MainActivity/MainViewModel）用于订阅事件的只读流。
     * UI 层在 lifecycleScope.launch { EventBus.events.collect { ... } } 中进行监听。
     */
    val events = _events.asSharedFlow()

    /**
     * 投递事件。
     * 由底层 Tick 逻辑线程高频调用。使用 tryEmit 发送非阻塞事件。
     * @param event 任意定义的事件载荷（Data Class）
     */
    fun post(event: Any) {
        // tryEmit 不会挂起当前线程，完美契合 TickEngine 中对极致性能的要求
        _events.tryEmit(event)
    }
}