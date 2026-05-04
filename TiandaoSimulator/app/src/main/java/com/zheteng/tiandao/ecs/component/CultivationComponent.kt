package com.zheteng.tiandao.ecs.component

/**
 * 境界与修为属性组件 (纯数据结构体)
 * 记录大境界、小境界、当前积累的修为值，以及卡瓶颈的状态。
 * 底层推演只负责纯粹的数值累加，不包含任何显示逻辑。
 */
data class CultivationComponent(
    /**
     * 实体全局唯一 ID
     */
    val entityId: Int,

    /**
     * 当前大境界
     * 1 炼气 ~ 10 渡劫。决定了寿元基数与绝对战力压制。
     * 对应 [MajorRealm] 常量。
     */
    var majorLevel: Byte,

    /**
     * 当前小境界
     * 1 层 ~ 9 层 (大圆满)。
     */
    var minorLevel: Byte,

    /**
     * 当前累积修为 (Exp)
     * 每个 Tick 闭关或打坐吸纳天地灵气后累加此值。
     */
    var currentExp: Long,

    /**
     * 是否处于瓶颈期
     * 当 currentExp 达到当前境界的 maxExp 时，标记为 true。
     * 此时修为不再增加，直到触发跨界突破检定。
     */
    var isInBottleneck: Boolean,

    /**
     * 连续突破失败导致的心魔惩罚次数
     * 每次突破失败此值累加，大幅降低下一次突破的成功率，必须依靠特定高阶丹药消除。
     */
    var bottleneckPenalty: Int
) {
    /**
     * 常量字典：定义修仙界的经典大境界体系。
     * 使用 Byte 极简类型代替枚举，降低几十万实体同屏推演时的堆内存开销。
     */
    companion object {
        // ==========================================
        // MajorRealm (大境界枚举)
        // ==========================================
        const val REALM_MORTAL: Byte = 0       // 凡人 (尚未引气入体)
        const val REALM_LIANQI: Byte = 1       // 炼气期 (底层炮灰)
        const val REALM_ZHUJI: Byte = 2        // 筑基期 (中坚力量)
        const val REALM_JIEDAN: Byte = 3       // 结丹期 (高阶分水岭，拥有独立社交运算图谱)
        const val REALM_YUANYING: Byte = 4     // 元婴期 (老怪，肉身毁坏可元婴出窍)
        const val REALM_HUASHEN: Byte = 5      // 化神期 (人间绝巅，可吸干一州灵气)
        const val REALM_LIANXU: Byte = 6       // 炼虚期 (法则接触)
        const val REALM_HETI: Byte = 7         // 合体期 (法相天地)
        const val REALM_DACHENG: Byte = 8      // 大乘期 (界面无敌)
        const val REALM_DUJIE: Byte = 9        // 渡劫期 (迎接近地飞升天劫)
        const val REALM_ASCENDED: Byte = 10    // 飞升/真仙 (当前大世界位面承载极限)
    }
}