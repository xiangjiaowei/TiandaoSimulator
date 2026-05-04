package com.zheteng.tiandao.heavenlydao

import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.component.*
import com.zheteng.tiandao.ecs.core.EntityManager
import com.zheteng.tiandao.world.eco.TechTreeManager
import kotlin.random.Random

/**
 * 奇遇事件定义
 */
data class MiracleEvent(
    val id: Int,
    val name: String,
    val weight: Int,
    val action: (entityId: Int) -> Unit
)

/**
 * 奇遇与保底事件池
 * 专门为 [BaseInfoComponent.LUCK_PROTAGONIST] 级别的实体提供逻辑补偿。
 */
object MiracleEventPool {

    private val eventPool = mutableListOf<MiracleEvent>()

    init {
        // 事件：坠崖不死，反得神丹
        eventPool.add(MiracleEvent(1, "坠崖奇遇", 100) { id ->
            val inv = EntityManager.inventories[id]
            inv?.addStackableItem(InventoryComponent.ITEM_PILL_FOUNDATION, 1)
            EventBus.post(SystemEvents.WorldLogEvent("【气运爆发】实体 $id 坠入万丈深渊，竟在崖底枯骨中寻得一枚筑基丹！", SystemEvents.LogLevel.HEAVENLY))
        })

        // 事件：残魂附体，加速修炼
        eventPool.add(MiracleEvent(2, "金手指觉醒", 50) { id ->
            val cult = EntityManager.cultivations[id]
            if (cult != null) {
                cult.currentExp += 5000 // 获得大量修为
                EventBus.post(SystemEvents.WorldLogEvent("【金手指】实体 $id 意外开启神秘戒指，在神秘老者的指点下修为一日千里！", SystemEvents.LogLevel.HEAVENLY))
            }
        })

        // 事件：古宝认主
        eventPool.add(MiracleEvent(3, "古宝认主", 30) { id ->
            val inv = EntityManager.inventories[id]
            // 此处模拟获得高阶玉简，实际开发中需对应 TechTreeManager 的知识 ID
            inv?.uniqueArtifactIds?.add(TechTreeManager.KNOWLEDGE_ULTIMATE_SWORD)
            EventBus.post(SystemEvents.WorldLogEvent("【大机缘】一道流光划破长空，失传已久的《青元剑诀》竟主动向实体 $id 认主！", SystemEvents.LogLevel.HEAVENLY))
        })
    }

    /**
     * 尝试为指定实体触发奇遇
     * 只有符合条件的“气运之子”或特定高气运 NPC 才有资格进入此方法。
     */
    fun tryTrigger(entityId: Int) {
        val base = EntityManager.baseInfos[entityId] ?: return

        // 校验气运阈值，非主角级别的实体触发概率极低
        val threshold = if (base.luckValue == BaseInfoComponent.LUCK_PROTAGONIST) 0.05f else 0.0001f

        if (Random.nextFloat() < threshold) {
            val event = pickRandomEvent()
            event.action(entityId)
        }
    }

    /**
     * 加权随机抽取事件
     */
    private fun pickRandomEvent(): MiracleEvent {
        val totalWeight = eventPool.sumOf { it.weight }
        var roll = Random.nextInt(totalWeight)

        for (event in eventPool) {
            roll -= event.weight
            if (roll < 0) return event
        }
        return eventPool[0]
    }
}