package com.zheteng.tiandao.core.engine

import com.zheteng.tiandao.core.config.WorldConstants
import com.zheteng.tiandao.ecs.component.CultivationComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import kotlin.math.min

/**
 * 离线挂机收益与沧桑演化演算器
 * 当玩家重新进入游戏时，计算离线期间的时间流逝对整个世界的影响。
 */
object OfflineCalculator {

    /**
     * 计算离线演化
     * @param offlineTicks 离线换算后的总 Tick 数
     */
    fun calculateOfflineProgress(offlineTicks: Long) {
        if (offlineTicks <= 0) return

        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds
        val baseInfos = EntityManager.baseInfos
        val cultivations = EntityManager.cultivations
        val combats = EntityManager.combats

        // 离线期间的性能保护：限制最大演化跨度（例如最多演化 1000 年，即 12000 Ticks）
        val safeTicks = min(offlineTicks, 12000L)

        for (i in 0 until entityCount) {
            val id = entityIds[i]
            val base = baseInfos[id] ?: continue
            val cult = cultivations[id] ?: continue
            val combat = combats[id] ?: continue

            // 1. 寿元结算
            val ageIncr = (safeTicks / 12).toInt() // 每 12 Ticks 为一岁
            if (ageIncr > 0) {
                base.age += ageIncr
                if (base.age >= base.maxLifespan && base.luckValue != 999) {
                    // 寿元耗尽，执行离线坐化逻辑（此处简单标记死亡，实际需调用 EntityManager 销毁）
                    EntityManager.destroyEntity(id)
                    continue
                }
            }

            // 2. 修为期望增长
            // 离线状态下不进行复杂的网格灵气扣减，改用基础吸收期望
            val baseExpGainPerTick = when (cult.majorLevel.toInt()) {
                CultivationComponent.REALM_LIANQI -> 5L
                CultivationComponent.REALM_ZHuji -> 20L
                CultivationComponent.REALM_JIEDAN -> 100L
                else -> 500L
            }

            // 离线修为 = 基础值 * 时间 * 灵根补正 (0.5 ~ 2.0)
            val linggenMod = (base.linggenType.toFloat() / 3f).coerceIn(0.5f, 2.0f)
            val totalExpGain = (baseExpGainPerTick * safeTicks * linggenMod).toLong()

            cult.currentExp += totalExpGain

            // 离线期间不处理雷劫，修为到达瓶颈后强制锁定，等待玩家上线手动引导突破
            // 这样可以防止玩家上线发现心爱的“气运之子”在离线时渡劫失败被劈死
            // val maxExp = ... (从 MathFormulas 获取)
            // if (cult.currentExp >= maxExp) cult.isInBottleneck = true
        }

        // 3. 更新全局时间
        TickEngine.currentTick += safeTicks
    }
}