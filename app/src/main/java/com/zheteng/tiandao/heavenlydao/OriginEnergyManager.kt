package com.zheteng.tiandao.heavenlydao

import com.zheteng.tiandao.core.config.MathFormulas
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents

/**
 * 天道本源能量管理器
 * 负责玩家作为“天道”唯一货币的收割结算、消耗审计与同步。
 * 遵循“养蛊与收割”逻辑：生灵逆天而行，陨落时能量归还苍天。
 */
object OriginEnergyManager {

    /**
     * 当前拥有的天道本源能量总额
     */
    @Volatile
    var currentEnergy: Long = 10000L // 创世初始赠送 10,000 点
        private set

    /**
     * 结算生灵陨落时的本源收割
     * @param majorLevel 陨落者的境界等级 (等级越高，收割越多)
     */
    fun harvestFromDeath(majorLevel: Int) {
        // 境界基础价值：炼气=100, 筑基=500, 结丹=2000, 元婴=10000 ...
        val victimTierBase = when (majorLevel) {
            1 -> 100
            2 -> 500
            3 -> 2000
            4 -> 10000
            5 -> 50000
            else -> 10
        }

        val levelMultiplier = majorLevel * 1.2f

        val gain = MathFormulas.calculateOriginEnergyGain(
            victimTierBase = victimTierBase,
            levelMultiplier = levelMultiplier,
            tribulationTax = 0 // 自然死亡无雷劫税
        )

        addEnergy(gain)
    }

    /**
     * 结算渡劫时的能量抽成 (雷劫税)
     * 无论修士渡劫成败，对抗雷劫爆发的能量均会被天道吸收。
     */
    fun harvestFromTribulation(realmLevel: Int) {
        val tax = realmLevel * 1000L
        addEnergy(tax)
    }

    /**
     * 消耗天道本源进行干预操作
     * @param amount 消耗数额
     * @return 扣减是否成功 (余额不足则返回 false)
     */
    fun spendEnergy(amount: Long): Boolean {
        if (amount < 0) return false

        synchronized(this) {
            if (currentEnergy >= amount) {
                currentEnergy -= amount

                // 广播变动，通知 UI 刷新
                EventBus.post(
                    SystemEvents.OriginEnergyChangedEvent(
                        currentEnergy = currentEnergy,
                        delta = -amount
                    )
                )
                return true
            }
        }
        return false
    }

    /**
     * 检查是否支付得起
     */
    fun canAfford(amount: Long): Boolean {
        return currentEnergy >= amount
    }

    /**
     * 内部加法逻辑，同步刷新 UI
     */
    private fun addEnergy(amount: Long) {
        if (amount <= 0) return

        synchronized(this) {
            currentEnergy += amount

            // 广播收割成功，UI 顶部资源栏监听此事件进行数值缓动渲染
            EventBus.post(
                SystemEvents.OriginEnergyChangedEvent(
                    currentEnergy = currentEnergy,
                    delta = amount
                )
            )
        }
    }

    /**
     * 开发者/调试模式接口：强行设定能量
     */
    fun debugSetEnergy(amount: Long) {
        currentEnergy = amount
        EventBus.post(SystemEvents.OriginEnergyChangedEvent(currentEnergy, 0))
    }
}