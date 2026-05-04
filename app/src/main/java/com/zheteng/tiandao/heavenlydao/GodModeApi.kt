package com.zheteng.tiandao.heavenlydao

import com.zheteng.tiandao.core.config.WorldConstants
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.component.BaseInfoComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import com.zheteng.tiandao.world.grid.GridManager

/**
 * 天道干预控制台底层修改接口
 * 提供玩家重塑世界法则、干预生灵命运的绝对权限。
 * 所有操作必须消耗天道本源能量，并直接修改底层内存中的组件数据。
 */
object GodModeApi {

    /**
     * 【宏观·局部灵气调控】
     * 强行注入或抽干某个区域的游离灵气。
     * 注入高浓度灵气通常会导致周边宗门发觉异象并举宗迁徙抢夺。
     */
    fun alterRegionalQi(gridX: Short, gridY: Short, radius: Int, multiplier: Float) {
        val cost = 5000L // 调控法则基础消耗
        if (!OriginEnergyManager.spendEnergy(cost)) return

        val cells = GridManager.getCellsInRange(gridX, gridY, radius)
        for (cell in cells) {
            cell.localActiveQi = (cell.localActiveQi * multiplier).toLong()
        }

        EventBus.post(
            SystemEvents.WorldLogEvent(
                "【天道敕令】坐标($gridX, $gridY)附近灵气发生剧烈紊乱，天地异象频现！",
                SystemEvents.LogLevel.HEAVENLY
            )
        )
    }

    /**
     * 【微观·拨弄命运】
     * 强行修改指定实体的灵根资质。
     * 例如将一名凡人提拔为“天灵根”，使其成为重点培养的“位面之子”。
     */
    fun modifyEntityLinggen(entityId: Int, newLinggenType: Byte) {
        val cost = WorldConstants.COST_MODIFY_LINGGEN
        if (!OriginEnergyManager.spendEnergy(cost)) return

        val baseInfo = EntityManager.baseInfos[entityId] ?: return
        val oldType = baseInfo.linggenType
        baseInfo.linggenType = newLinggenType

        EventBus.post(
            SystemEvents.WorldLogEvent(
                "【天命更易】实体ID:$entityId 的先天灵根被强行重塑，因果线发生剧烈偏移！",
                SystemEvents.LogLevel.HEAVENLY
            )
        )
    }

    /**
     * 【宏观·降下灾祸】
     * 强行提升某区域的危险度，诱发妖兽潮或魔气灌体。
     * 用于清理区域内过剩的修士，实现能量回收。
     */
    fun triggerRegionalBeastHorde(gridX: Short, gridY: Short, radius: Int) {
        val cost = WorldConstants.COST_TRIGGER_BEAST_HORDE
        if (!OriginEnergyManager.spendEnergy(cost)) return

        val cells = GridManager.getCellsInRange(gridX, gridY, radius)
        for (cell in cells) {
            cell.dangerLevel = 100 // 强制拉满危险度
        }

        EventBus.post(
            SystemEvents.WorldLogEvent(
                "【灾厄降临】恐怖兽潮在坐标($gridX, $gridY)附近爆发，血气漫天！",
                SystemEvents.LogLevel.DANGER
            )
        )
    }

    /**
     * 【天谴·雷罚抹杀】
     * 无视境界判定，降下九霄神雷直接抹杀特定实体。
     * 能量消耗与目标的生命值成正比，处决老怪代价昂贵。
     */
    fun smiteEntity(entityId: Int) {
        val combat = EntityManager.combats[entityId] ?: return
        val cost = (combat.maxHp * WorldConstants.COST_SMITE_MULTIPLIER).toLong()

        if (!OriginEnergyManager.spendEnergy(cost)) return

        // 直接抹杀，跳过战斗系统和气运保底（天道要你死，不得不死）
        EntityManager.destroyEntity(entityId)

        EventBus.post(
            SystemEvents.WorldLogEvent(
                "【天罚抹杀】九霄神雷降世，实体ID:$entityId 已在雷光中灰飞烟灭！",
                SystemEvents.LogLevel.HEAVENLY
            )
        )
    }

    /**
     * 【法则·种族枷锁】
     * 在底层规则层面限制某个种族的上限。
     * 目前作为逻辑占位，后续可接入全局规则校验系统。
     */
    fun lockRaceProgression(raceType: Byte, maxAllowedTier: Byte) {
        val cost = 20000L
        if (!OriginEnergyManager.spendEnergy(cost)) return

        // 此处应修改全局 GlobalRules 对象，影响 CultivationSystem 的 P_success 判定
        EventBus.post(
            SystemEvents.WorldLogEvent(
                "【法则锁死】天道降下禁制，特定族群从此突破无望，上限已定！",
                SystemEvents.LogLevel.HEAVENLY
            )
        )
    }
}