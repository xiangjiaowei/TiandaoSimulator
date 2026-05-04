package com.zheteng.tiandao.heavenlydao

import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents

/**
 * 天道干预接口 API
 * 玩家所有的上帝视角操作入口。只产生 Event，不包含具体结算逻辑。
 */
object GodModeApi {

    /**
     * 降下天罚 / 雷劫
     * @param targetEntityId 目标修士实体 ID
     * @param intensity 雷劫强度指数
     */
    fun castHeavenlyTribulation(targetEntityId: Int, intensity: Double) {
        // 干预代价：呈指数级递增，限制玩家滥用天罚
        val cost = Math.pow(intensity, 1.5) * WorldConstants.TRIBULATION_COST_MULTIPLIER

        if (OriginEnergyManager.tryConsumeEnergy(cost)) {
            EventBus.post(SystemEvents.TribulationEvent(targetEntityId, intensity))
        } else {
            EventBus.post(SystemEvents.LogEvent("【警告】天道本源枯竭，无法降下毁灭雷劫！"))
        }
    }

    /**
     * 降下甘霖 / 灵气复苏
     */
    fun bestowSpiritualRain(gridId: Int, amount: Double) {
        // 无中生有打破守恒定律，需要消耗双倍的天道本源
        val cost = amount * 2.0
        if (OriginEnergyManager.tryConsumeEnergy(cost)) {
            EventBus.post(SystemEvents.SpiritualRainEvent(gridId, amount))
        }
    }
}