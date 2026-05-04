package com.zheteng.tiandao.core.eventbus

/**
 * 全局系统事件载荷集
 * 采用密封类 (Sealed Class) 统一管理，便于 UI 层在使用 when 表达式时进行穷举匹配。
 */
sealed class SystemEvents {

    /**
     * 时间步进事件
     * 由 TickEngine 在每次推演循环结束时固定抛出，UI 层利用此事件更新游戏内年份。
     * @param currentTick 当前世界的绝对纪元时间戳
     */
    data class TickAdvancedEvent(val currentTick: Long) : SystemEvents()

    /**
     * 大世界日志广播事件
     * 用于驱动 UI 层的“九宫格信息流大事件渲染”。
     * @param message 日志文本，如：“【惊天噩耗】黄枫谷护宗大阵被攻破，道统断绝！”
     * @param level 日志严重程度枚举，用于前端决定渲染的颜色。
     */
    data class WorldLogEvent(
        val message: String,
        val level: LogLevel = LogLevel.INFO
    ) : SystemEvents()

    /**
     * 大境界突破天象事件
     * 结丹期及以上的老怪突破时抛出，用于全图广播或 UI 弹窗警告。
     */
    data class EntityBreakthroughEvent(
        val entityId: Int,
        val newMajorRealm: Byte,
        val gridX: Short,
        val gridY: Short
    ) : SystemEvents()

    /**
     * 宗门覆灭事件
     * 宗门护阵被破或公款归零、底蕴耗尽时抛出。
     */
    data class SectDestroyedEvent(
        val destroyedSectId: Int,
        val attackerSectId: Int // 如果是自然衰亡或天罚抹杀，可传 0
    ) : SystemEvents()

    /**
     * 古宝出世/持宝人暴露事件
     * 对应策划案：怀璧其罪。暴露后会引发全图引怪红框警告。
     */
    data class ArtifactExposedEvent(
        val ownerEntityId: Int,
        val artifactId: Int,
        val gridX: Short,
        val gridY: Short,
        val exposureRadius: Int
    ) : SystemEvents()

    /**
     * 天道法则异变：末法时代降临事件
     * 当全局游离灵气跌破 10% 阈值时触发。
     */
    object MofaEraTriggeredEvent : SystemEvents()

    /**
     * 天道本源能量变动事件
     * 玩家收割修士或干预世界消耗能量后抛出，UI 顶部资源栏监听此事件进行数值缓动。
     */
    data class OriginEnergyChangedEvent(
        val currentEnergy: Long,
        val delta: Long // 变动差值，正数为收割入账，负数为玩家消耗
    ) : SystemEvents()

    // ==========================================
    // 辅助枚举
    // ==========================================
    enum class LogLevel {
        INFO,       // 普通传闻 (白色/淡蓝色)
        WARNING,    // 坊市物价暴涨/局部摩擦 (黄色)
        DANGER,     // 兽潮/灭门战/大能追杀 (红色)
        HEAVENLY    // 天道干预/雷罚/祥瑞 (紫色/金色)
    }
}