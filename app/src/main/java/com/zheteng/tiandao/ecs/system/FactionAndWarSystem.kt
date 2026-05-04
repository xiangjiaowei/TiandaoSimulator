package com.zheteng.tiandao.ecs.system

import com.zheteng.tiandao.core.config.WorldConstants
import com.zheteng.tiandao.core.engine.TickEngine
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.component.BaseInfoComponent
import com.zheteng.tiandao.ecs.component.CultivationComponent
import com.zheteng.tiandao.ecs.component.FactionComponent
import com.zheteng.tiandao.ecs.component.InventoryComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import com.zheteng.tiandao.world.grid.GridManager
import kotlin.random.Random

/**
 * 宗门阵营与社会关系拓扑推演系统
 * 负责执行宗门的周期性割韭菜逻辑（升仙大会、血色试炼），以及处理宗门间的外交与战争状态。
 */
object FactionAndWarSystem {

    /**
     * 由 TickEngine 每 Tick 调度执行
     */
    fun onTickUpdate() {
        val currentTick = TickEngine.currentTick

        // 1. 升仙大会 (凡人收割期) - 每 120 Ticks 触发
        if (currentTick > 0L && currentTick % WorldConstants.SECT_RECRUIT_INTERVAL_TICKS == 0L) {
            processAscensionAssembly()
        }

        // 2. 血色禁地试炼 (筑基丹主药收割与绞肉机) - 每 600 Ticks 触发
        if (currentTick > 0L && currentTick % WorldConstants.SECT_TRIAL_INTERVAL_TICKS == 0L) {
            processBloodCoreTrial()
        }

        // 3. 宗门外交状态机与宏观战争演算
        // 此部分将与底层的 Room 数据库 (SectDbEntity) 深度挂钩，在此暂留推演钩子。
        // ProcessSectDiplomacyAndWar()
    }

    /**
     * 升仙大会：底层人口红利收割
     * 遍历符合条件的凡人，强制更改其 FactionComponent，剥夺自由身。
     */
    private fun processAscensionAssembly() {
        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds

        val baseInfos = EntityManager.baseInfos
        val factions = EntityManager.factions
        val transforms = EntityManager.transforms

        var recruitedCount = 0

        for (i in 0 until entityCount) {
            val entityId = entityIds[i]
            val base = baseInfos[entityId] ?: continue

            // 筛选条件：10~15 岁，具有灵根（哪怕是伪灵根）
            if (base.age in 10..15 && base.linggenType > BaseInfoComponent.LINGGEN_NONE) {
                val faction = factions[entityId] ?: continue

                // 仅对当前没有宗门的凡人/散修生效
                if (faction.isRogueCultivator()) {
                    val transform = transforms[entityId] ?: continue
                    val cell = GridManager.getCell(transform.gridX, transform.gridY)

                    // 如果该凡人所在网格处于某宗门统治之下，强制入宗沦为底层外门弟子
                    if (cell != null && cell.sectOwnerId != FactionComponent.SECT_NONE) {
                        faction.sectId = cell.sectOwnerId
                        faction.position = FactionComponent.POS_OUTER
                        recruitedCount++
                    }
                }
            }
        }

        // 宏观播报
        if (recruitedCount > 0) {
            EventBus.post(
                SystemEvents.WorldLogEvent(
                    "【升仙大会】天下各大宗门广开山门，共计收编了 $recruitedCount 名身具灵根的凡童入宗修行。",
                    SystemEvents.LogLevel.INFO
                )
            )
        }
    }

    /**
     * 血色禁地试炼：极高死亡率的筑基资源收割绞肉机
     */
    private fun processBloodCoreTrial() {
        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds

        val cultivations = EntityManager.cultivations
        val factions = EntityManager.factions
        val inventories = EntityManager.inventories
        val baseInfos = EntityManager.baseInfos

        var deathCount = 0
        var survivorCount = 0

        // 倒序遍历，因为试炼会产生大批量死亡并触发 EntityManager 的 Swap and Pop
        for (i in entityCount - 1 downTo 0) {
            val entityId = entityIds[i]
            val faction = factions[entityId] ?: continue

            // 强制参与条件：各大宗门的外门弟子
            if (faction.position != FactionComponent.POS_OUTER || faction.isRogueCultivator()) continue

            val cult = cultivations[entityId] ?: continue

            // 且必须是修为停滞在炼气大圆满（急需筑基丹破境）的炮灰
            if (cult.majorLevel == CultivationComponent.REALM_LIANQI && cult.isInBottleneck) {
                val base = baseInfos[entityId]

                // 【天道暗箱】气运之子进入秘境不仅不死，必定满载而归
                if (base?.luckValue == BaseInfoComponent.LUCK_PROTAGONIST) {
                    survivorCount++
                    val inv = inventories[entityId]
                    inv?.addStackableItem(InventoryComponent.ITEM_PILL_FOUNDATION, 3)
                    EventBus.post(
                        SystemEvents.WorldLogEvent(
                            "【血色试炼】气运之子在禁地秘境中大杀四方，不仅斩杀敌对天骄，更夺得极品灵药满载而归！",
                            SystemEvents.LogLevel.HEAVENLY
                        )
                    )
                    continue
                }

                // 正常修士过生还检定 (基础死亡率 70%)
                if (Random.nextFloat() < WorldConstants.SECT_TRIAL_BASE_DEATH_RATE) {
                    deathCount++
                    // 试炼陨落直接抹杀
                    EntityManager.destroyEntity(entityId)
                } else {
                    survivorCount++
                    // 幸存者以鲜血带回主药，系统分配给其一枚筑基丹
                    val inv = inventories[entityId]
                    inv?.addStackableItem(InventoryComponent.ITEM_PILL_FOUNDATION, 1)
                }
            }
        }

        if (deathCount > 0 || survivorCount > 0) {
            EventBus.post(
                SystemEvents.WorldLogEvent(
                    "【血色禁地】五十年一度的残酷试炼落下帷幕。各宗炼气弟子死伤惨重，共计陨落 $deathCount 人，仅 $survivorCount 人携灵药生还！",
                    SystemEvents.LogLevel.DANGER
                )
            )
        }
    }
}