package com.zheteng.tiandao.heavenlydao

import kotlin.random.Random

/**
 * 奇遇与气运结算池
 * 负责在修士面临致命打击时，进行最后一次“逆天改命”的判定。
 */
object MiracleEventPool {

    enum class MiracleType {
        NONE,
        ANCIENT_REMNANT_SOUL, // 触发：戒指残魂觉醒抵御致命一击
        CLIFF_SURVIVAL,       // 触发：绝地求生，坠落深渊偶得秘籍
        SUDDEN_EPIPHANY       // 触发：生死间顿悟，强行突破境界
    }

    /**
     * 致死伤害判定时的气运检定
     */
    fun evaluateLethalDamageMiracle(karmicLuck: Int): MiracleType {
        // 硬核养蛊：气运低于 90 的普通修士，连触发奇遇的资格都没有
        if (karmicLuck < 90) return MiracleType.NONE

        // 极限生还率算法：最高不超过 50%
        val triggerChance = (karmicLuck - 90) * 0.05
        if (Random.nextDouble() < triggerChance) {
            val validMiracles = MiracleType.entries.filter { it != MiracleType.NONE }
            return validMiracles.random()
        }

        return MiracleType.NONE
    }
}