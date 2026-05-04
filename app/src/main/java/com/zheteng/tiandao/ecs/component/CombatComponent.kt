package com.zheteng.tiandao.ecs.component

/**
 * 战斗属性组件 (纯数据结构体)
 * 记录生灵在战斗推演中所需的绝对数值屏障参数，以及死亡后反哺天地的能量本源。
 * 仅在触发斗法判定时被 CombatResolutionSystem 读取和修改。
 */
data class CombatComponent(
    /**
     * 实体全局唯一 ID
     */
    val entityId: Int,

    /**
     * 当前气血值 (HP)
     * 扣至 0 以下且未触发气运保底时，判定肉身死亡。
     */
    var hp: Int,

    /**
     * 最大气血上限 (Max HP)
     * 由大境界基数乘以种族补正（如妖族极高）得出。
     */
    var maxHp: Int,

    /**
     * 基础攻击力 (Base Attack)
     * 决定了能否击穿低阶修士的绝对防御屏障。
     */
    var baseAtk: Int,

    /**
     * 护体真元防御力 (Base Defense)
     * 结合境界差距，用于计算绝对破防阈值。低于阈值的攻击强制为刮痧或反震。
     */
    var baseDef: Int,

    /**
     * 五行主修功法属性 (Elemental Affinity)
     * 决定斗法时的五行生克补正乘区。
     * 对应 [ElementAttribute] 常量。
     */
    var elementalAffinity: Byte,

    /**
     * 体内固化真元总量 (Bound Qi)
     * 境界越高，锁死在体内的天地灵气越多。
     * 一旦该实体陨落，此值的 80% 将直接化为游离灵气，注入其死亡所在网格，形成“一鲸落万物生”的宏观地貌演化。
     */
    var boundQi: Long
) {
    /**
     * 提供内联的恢复满血辅助方法
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun healToMax() {
        this.hp = this.maxHp
    }

    companion object {
        // ==========================================
        // ElementAttribute (五行属性枚举)
        // 用于战斗推演中的五行克制网状演算 (金克木、木克土等)
        // ==========================================
        const val ELEMENT_NONE: Byte = 0  // 无属性/凡人/杂散功法
        const val ELEMENT_METAL: Byte = 1 // 金 (主杀伐，穿透高)
        const val ELEMENT_WOOD: Byte = 2  // 木 (主生息，恢复快)
        const val ELEMENT_WATER: Byte = 3 // 水 (主绵长，真元厚)
        const val ELEMENT_FIRE: Byte = 4  // 火 (主爆裂，伤害高)
        const val ELEMENT_EARTH: Byte = 5 // 土 (主厚重，防御极高)
    }
}