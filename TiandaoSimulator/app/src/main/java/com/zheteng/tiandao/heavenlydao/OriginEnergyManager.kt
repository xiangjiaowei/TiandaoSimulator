package com.zheteng.tiandao.heavenlydao

import com.zheteng.tiandao.core.config.WorldConstants
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents

/**
 * 天道本源管理器 - 绝对法则守护者
 * 严格执行“能量守恒”，监控沙盒世界中灵气的流动与回收。
 */
object OriginEnergyManager {
    // 天道（玩家）当前收割的本源能量
    @Volatile
    var currentOriginEnergy: Double = 0.0
        private set

    /**
     * 触发“鲸落法则”
     * 当修士陨落时，强制执行能量剥离。
     */
    fun processWhaleFall(entityId: Int, totalQi: Double, gridId: Int) {
        // 天道无情：强制抽成 (例如：15%)
        val heavenlyTax = totalQi * WorldConstants.HEAVENLY_TAX_RATE

        // 天道反哺：剩余能量必须精确回归对应的地理网格，拒绝全局灵气膨胀
        val returnedQi = totalQi - heavenlyTax

        synchronized(this) {
            currentOriginEnergy += heavenlyTax
        }

        // 遵循 ECS 读写分离，本模块不直接操作网格内存，仅抛出底层事件
        EventBus.post(SystemEvents.WhaleFallEvent(entityId, gridId, returnedQi))
    }

    /**
     * 神明干预的能量扣除
     */
    fun tryConsumeEnergy(cost: Double): Boolean {
        synchronized(this) {
            if (currentOriginEnergy >= cost) {
                currentOriginEnergy -= cost
                return true
            }
            return false
        }
    }
}