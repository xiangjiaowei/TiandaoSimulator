package com.zheteng.tiandao.core.config

/**
 * 全局配置常量表
 * 包含大纲要求的族群先天属性修正倍率
 */
object WorldConstants {
    // 时间基准
    const val TICK_PER_YEAR = 12 // 每 12 Tick 为一年[cite: 1]

    // 能量阈值
    const val END_OF_QI_ERA_THRESHOLD = 0.1f // 末法时代触发阈值 (10%)[cite: 1]
    const val WHALE_FALL_RETURN_RATE = 0.8f  // 鲸落灵气反哺比例 (80%)[cite: 1]

    // 族群倍率表 (Exp_mod, HP_mod, Life_mod)[cite: 1]
    val RACE_MODIFIERS = mapOf(
        0 to RaceMod(1.0f, 1.0f, 1.0f), // 人族：平衡[cite: 1]
        1 to RaceMod(0.6f, 3.5f, 5.0f), // 妖族：寿长血厚，修炼极慢[cite: 1]
        2 to RaceMod(1.5f, 1.2f, 1.0f), // 魔族：吞噬效率高，极易入魔[cite: 1]
        3 to RaceMod(2.5f, 0.6f, 3.0f), // 灵族：天骄资质，肉身孱弱[cite: 1]
        4 to RaceMod(0.2f, 5.0f, 1.5f)  // 蛮族：修炼迟缓，金身不坏[cite: 1]
    )

    data class RaceMod(val exp: Float, val hp: Float, val life: Float)

    // 天道干预定价[cite: 1]
    const val COST_MODIFY_LINGGEN = 5000L
    const val COST_TRIGGER_BEAST_HORDE = 10000L
    const val COST_GENERATE_ARTIFACT = 20000L
    const val COST_SMITE_MULTIPLIER = 10 // 目标 HP 的 10 倍[cite: 1]
}