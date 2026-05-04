package com.zheteng.tiandao.core.eventbus

/**
 * 核心事件协议簇
 * 记录天道沙盒中发生的一切关键节点。
 */
sealed class SystemEvents {
    // 所有天道事件的基类
    abstract class TiandaoEvent

    /**
     * 纯文本推演日志
     * 用于低价值或纯描述性的流式文本反馈
     */
    data class LogEvent(val message: String) : TiandaoEvent()

    /**
     * 鲸落反哺事件 (能量绝对守恒节点)
     * @param entityId 陨落修士的实体 ID (Int)
     * @param gridId 陨落所在的网格 ID
     * @param returnedQi 被天道抽成后，归还给大自然的灵气量
     */
    data class WhaleFallEvent(
        val entityId: Int,
        val gridId: Int,
        val returnedQi: Double
    ) : TiandaoEvent()

    /**
     * 天罚降世事件
     * @param targetEntityId 被天道锁定的倒霉蛋
     * @param intensity 雷劫的毁灭指数
     */
    data class TribulationEvent(
        val targetEntityId: Int,
        val intensity: Double
    ) : TiandaoEvent()

    /**
     * 天降甘霖/灵气复苏事件
     * @param gridId 受益网格 ID
     * @param amount 无中生有的灵气量
     */
    data class SpiritualRainEvent(
        val gridId: Int,
        val amount: Double
    ) : TiandaoEvent()

    // 预留扩充口：例如宗门覆灭、跨大境界突破等核心事件
    // data class BreakthroughEvent(val entityId: Int, val newTier: Int) : TiandaoEvent()
    // data class SectDestroyedEvent(val sectId: Int, val killerEntityId: Int) : TiandaoEvent()
}