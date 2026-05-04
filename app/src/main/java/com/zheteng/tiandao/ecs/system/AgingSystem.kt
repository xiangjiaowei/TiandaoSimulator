package com.zheteng.tiandao.ecs.system

import com.zheteng.tiandao.core.config.WorldConstants
import com.zheteng.tiandao.core.engine.TickEngine
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.component.BaseInfoComponent
import com.zheteng.tiandao.ecs.component.CultivationComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import com.zheteng.tiandao.world.grid.GridManager

/**
 * 寿元结算系统
 * 负责大世界生灵的年龄自增与自然老死（坐化）判定。
 * 优先级最高：一旦实体被判定老死，立刻抹除，阻止其参与后续的修为获取或战斗。
 */
object AgingSystem {

    /**
     * 由 TickEngine 每一 Tick (月) 高频调用
     */
    fun onTickUpdate() {
        val currentTick = TickEngine.currentTick

        // 【性能优化核心】由于 age 是按年计算的，我们不需要每个月(Tick)都去遍历几十万实体进行加法。
        // 只有在逢 12 的倍数（即跨年）时，才执行一次全图年龄自增。
        val isNewYear = (currentTick % 12L == 0L)
        if (!isNewYear) return

        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds
        val baseInfos = EntityManager.baseInfos

        // 【安全删除核心】必须倒序遍历。
        // 因为 EntityManager.destroyEntity() 会将数组末尾的元素移动到当前被删除的索引处。
        // 倒序遍历能确保刚被移动过来的末尾元素（在之前的循环中已经处理过）不会被漏处理或重复处理。
        for (i in entityCount - 1 downTo 0) {
            val entityId = entityIds[i]
            val baseInfo = baseInfos[entityId] ?: continue

            // 骨龄 +1
            baseInfo.age++

            // 寿元枯竭判定
            if (baseInfo.age >= baseInfo.maxLifespan) {
                // 气运之子保底拦截：即使老死，天道也会强行续命
                if (baseInfo.luckValue == BaseInfoComponent.LUCK_PROTAGONIST) {
                    baseInfo.maxLifespan += 10 // 强行延寿十年
                    EventBus.post(
                        SystemEvents.WorldLogEvent(
                            "【天道庇护】气运之子在寿元将尽之际，偶食奇异朱果，竟强行延寿十年！",
                            SystemEvents.LogLevel.HEAVENLY
                        )
                    )
                    continue
                }

                // 正常寿终正寝处理逻辑
                executeDeath(entityId, baseInfo)
            }
        }
    }

    /**
     * 处决死亡实体并结算“鲸落”能量反哺
     */
    private fun executeDeath(entityId: Int, baseInfo: BaseInfoComponent) {
        val combatComp = EntityManager.combats[entityId]
        val transformComp = EntityManager.transforms[entityId]
        val cultComp = EntityManager.cultivations[entityId]

        // 1. 大能坐化，天地异象广播 (结丹期及以上)
        if (cultComp != null && cultComp.majorLevel >= CultivationComponent.REALM_JIEDAN) {
            EventBus.post(
                SystemEvents.WorldLogEvent(
                    "【天地同悲】一位大能寿元已尽，坐化于天地之间，其毕生修为开始反哺灵脉！",
                    SystemEvents.LogLevel.DANGER
                )
            )
        }

        // 2. 能量反哺机制 (一鲸落万物生)
        if (combatComp != null && transformComp != null) {
            val returnRatio = WorldConstants.DEATH_QI_RETURN_RATIO
            val returnedQi = (combatComp.boundQi * returnRatio).toLong()

            if (returnedQi > 0) {
                // 将固化的灵气直接注入其死亡坐标所在的网格中
                GridManager.addActiveQiToCell(transformComp.gridX, transformComp.gridY, returnedQi)
            }
        }

        // 3. 彻底从 ECS 内存数组中抹杀，触发 ID 回收
        EntityManager.destroyEntity(entityId)
    }
}