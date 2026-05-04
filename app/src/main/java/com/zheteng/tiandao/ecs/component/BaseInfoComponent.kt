package com.zheteng.tiandao.ecs.component

/**
 * 基础生理属性组件 (纯数据结构体)
 * 决定了生灵的寿命、种族底色、先天资质与气运。
 * 在底层推演中，当 [age] >= [maxLifespan] 时，直接触发坐化抹杀逻辑。
 */
data class BaseInfoComponent(
    /**
     * 实体全局唯一 ID
     */
    val entityId: Int,

    /**
     * 当前年龄 (单位：年)
     * 每个 Tick (月) 会累加，逢 12 进 1
     */
    var age: Int,

    /**
     * 寿元上限 (单位：年)
     * 结合大境界与种族决定的基础值。一旦 [age] 达到此值，强制触发坐化。
     */
    var maxLifespan: Int,

    /**
     * 族群类型枚举
     * 决定先天属性成长倍率（如人族悟性高、妖族肉身强、魔族吸灵快）。
     * 对应 [RaceType] 常量。
     */
    var raceType: Byte,

    /**
     * 灵根类型枚举
     * 决定基础吸取天地游离灵气的倍率。
     * 对应 [LinggenType] 常量。
     */
    var linggenType: Byte,

    /**
     * 气运值
     * 取值范围：-100(天谴) ~ 100(天命)。
     * 若该值被天道（玩家）强行锁死为 999，则触发“气运之子”锁血保底拦截机制。
     */
    var luckValue: Short,

    /**
     * 性格标签 (采用 16-bit 位掩码存储)
     * 供效用 AI 深度推演时评估行为权重。例如同时带有“贪婪”与“残忍”，劫杀概率极高。
     * 对应 [PersonalityTag] 常量。
     */
    var personalityTags: Short
) {
    /**
     * 提供内联的位运算辅助方法，避免在循环中创建多余对象。
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun hasPersonality(tag: Short): Boolean {
        return (this.personalityTags.toInt() and tag.toInt()) != 0
    }

    fun addPersonality(tag: Short) {
        this.personalityTags = (this.personalityTags.toInt() or tag.toInt()).toShort()
    }

    fun removePersonality(tag: Short) {
        this.personalityTags = (this.personalityTags.toInt() and tag.toInt().inv()).toShort()
    }

    /**
     * 常量字典：通过在 Companion Object 中收敛极简类型的映射，
     * 杜绝在 ECS 架构中使用高开销的 Enum Class 带来额外的内存分配。
     */
    companion object {
        // ==========================================
        // RaceType (族群分类)
        // ==========================================
        const val RACE_HUMAN: Byte = 0   // 人族 (基准)
        const val RACE_YAO: Byte = 1     // 妖族 (肉身强横，寿元极长)
        const val RACE_MO: Byte = 2      // 魔族 (极快，易走火入魔，可吞噬)
        const val RACE_LING: Byte = 3    // 灵族 (天生阵道，肉身孱弱)
        const val RACE_MAN: Byte = 4     // 蛮族 (免心魔，高物防)

        // ==========================================
        // LinggenType (灵根资质系数标识)
        // 具体乘区系数计算交由 MathFormulas 或底层配置表
        // ==========================================
        const val LINGGEN_NONE: Byte = 0 // 凡人 (无灵根)
        const val LINGGEN_WEI: Byte = 1  // 伪灵根 (系数 0.1)
        const val LINGGEN_ZHA: Byte = 2  // 杂灵根 (系数 0.5)
        const val LINGGEN_ZHEN: Byte = 3 // 真灵根 (系数 1.0)
        const val LINGGEN_DI: Byte = 4   // 地灵根 (系数 3.0)
        const val LINGGEN_TIAN: Byte = 5 // 天灵根 (系数 10.0)

        // ==========================================
        // PersonalityTag (性格标签位掩码 - 16位容量)
        // 使用位移运算 (Bit Shift)，极具性能优势
        // ==========================================
        const val TAG_NORMAL: Short = 0                   // 凡心 (无特殊标签)
        const val TAG_COWARD: Short = (1 shl 0).toShort() // 苟道 (极力避免冲突，闭关权重高)
        const val TAG_GREED: Short = (1 shl 1).toShort()  // 贪婪 (杀人夺宝动机评分加权)
        const val TAG_CRUEL: Short = (1 shl 2).toShort()  // 残忍 (战斗中不留活口，折磨致死率高)
        const val TAG_LOYAL: Short = (1 shl 3).toShort()  // 忠宗 (宗门战时绝不叛逃)
        const val TAG_PROUD: Short = (1 shl 4).toShort()  // 孤傲 (难以结交道侣，越阶挑战权重高)
        const val TAG_LUST: Short = (1 shl 5).toShort()   // 贪色 (极易结交道侣或双修鼎炉)

        // ==========================================
        // 特殊阈值常量
        // ==========================================
        const val LUCK_PROTAGONIST: Short = 999           // 气运之子锁血保底阈值
    }
}