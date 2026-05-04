package com.zheteng.tiandao.core.config

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/**
 * 天道修仙模拟 - 全局统一数学推演公式库
 * 采用单例模式，所有高频调用的复杂运算在此收口。
 * 严禁在 ECS 的 Tick 循环中直接使用 Math.pow 等高耗损函数，必须查表或使用本类的极简近似。
 */
object MathFormulas {

    // ==========================================
    // 模块二：境界与成长数学模型
    // ==========================================

    /**
     * 境界最高等级：10个大境界 (炼气 到 渡劫)
     */
    const val MAX_MAJOR_LEVEL = 10

    /**
     * 每一大境界细分：9个小境界 (一层 到 九层大圆满)
     */
    const val MAX_MINOR_LEVEL = 9

    private const val BASE_EXP_REQUIREMENT = 1000L
    private const val MAJOR_LEVEL_MULTIPLIER = 10.0
    private const val MINOR_LEVEL_MULTIPLIER = 1.5

    /**
     * 预计算修为升级经验需求表。
     * [大境界 1-10][小境界 1-9]
     * 避免运行时几十万实体每 Tick 计算指数。
     */
    val EXP_REQUIREMENT_TABLE: Array<LongArray> = Array(MAX_MAJOR_LEVEL + 1) { LongArray(MAX_MINOR_LEVEL + 1) }

    init {
        // 预计算所有境界的突破修为阈值，存入内存表
        for (major in 1..MAX_MAJOR_LEVEL) {
            for (minor in 1..MAX_MINOR_LEVEL) {
                val req = BASE_EXP_REQUIREMENT *
                        MAJOR_LEVEL_MULTIPLIER.pow(major - 1.0) *
                        MINOR_LEVEL_MULTIPLIER.pow(minor - 1.0)
                EXP_REQUIREMENT_TABLE[major][minor] = req.toLong()
            }
        }
    }

    /**
     * 瓶颈突破成功率判定公式
     * @param baseProb 基础成功率 (如 0.15 代表 15%)
     * @param linggenMod 灵根补正系数 (天灵根极高，伪灵根极低)
     * @param pillBonus 丹药加成概率 (如筑基丹直接 +0.20)
     * @param bottleneckPenalty 连续失败导致的心魔惩罚次数
     * @param penaltyGamma 心魔惩罚递增常数
     * @return 最终突破成功率 [0.0, 1.0]
     */
    fun calculateBreakthroughProbability(
        baseProb: Float,
        linggenMod: Float,
        pillBonus: Float,
        bottleneckPenalty: Int,
        penaltyGamma: Float = 0.05f
    ): Float {
        val successRate = (baseProb * linggenMod) + pillBonus - (penaltyGamma * bottleneckPenalty)
        return successRate.coerceIn(0.0f, 1.0f)
    }

    /**
     * 天雷劫伤害核算公式
     * @param baseDamage 对应大境界的天雷基础伤害
     * @param heavenlyDaoModifier 天道（玩家）干预的雷劫强度倍率 (-0.5 到 +2.0)
     * @param racePenalty 种族天劫惩罚 (如妖族、魔族为 1.5 到 2.0)
     * @return 最终落雷绝对伤害值
     */
    fun calculateTribulationDamage(
        baseDamage: Int,
        heavenlyDaoModifier: Float,
        racePenalty: Float
    ): Int {
        val delta = 1.0f + heavenlyDaoModifier
        return (baseDamage * delta * racePenalty).toInt()
    }

    // ==========================================
    // 模块六：底层战斗核算与绝对边界控制
    // ==========================================

    /**
     * 境界战力绝对压制防破防公式 (计算护体真元绝对阈值)
     * 确保跨大境界时，低阶修士绝对无法破防，且反震自身。
     * @param defB 防守方基础防御力
     * @param realmLevelB 防守方大境界等级
     * @param realmLevelA 攻击方大境界等级
     * @return 绝对破防判定阈值
     */
    fun calculateDefenseThreshold(defB: Int, realmLevelB: Int, realmLevelA: Int): Int {
        val realmGap = max(0, realmLevelB - realmLevelA)
        return (defB * (1.0f + realmGap * 5.0f)).toInt()
    }

    /**
     * 攻击方最终穿透力计算
     * @param atkA 攻击方基础攻击力
     * @param weaponArmorPenetration 法宝/功法带来的护甲穿透系数
     */
    fun calculatePenetration(atkA: Int, weaponArmorPenetration: Float): Int {
        return (atkA * (1.0f + weaponArmorPenetration)).toInt()
    }

    /**
     * 综合战力评分核算 (CP Calculation)
     * 脱离渲染与单体碰撞，纯数据推演核心。
     * @param baseHP 基础气血
     * @param baseAtk 基础攻击
     * @param elementModifier 五行生克补正 (0.8被克, 1.0平, 1.2克制)
     * @param artifactMultiplier 古宝与法宝补正倍率
     * @param consumableBonus 一次性符箓/阵盘额外固定加成
     */
    fun calculateCombatPower(
        baseHP: Int,
        baseAtk: Int,
        elementModifier: Float,
        artifactMultiplier: Float,
        consumableBonus: Int
    ): Float {
        return (baseHP * 0.1f + baseAtk) * elementModifier * artifactMultiplier + consumableBonus
    }

    /**
     * 斗法胜率计算器 (Lanchester 法则变体)
     * 呈现高 CP 对低 CP 的碾压，但保留极小概率的爆冷。
     * @param cpA 攻击方综合战力
     * @param cpB 防守方综合战力
     * @return 攻击方获胜的概率 [0.0, 1.0]
     */
    fun calculateWinRate(cpA: Float, cpB: Float): Float {
        if (cpA <= 0f) return 0f
        if (cpB <= 0f) return 1f
        val cpAPow = cpA.pow(1.5f)
        val cpBPow = cpB.pow(1.5f)
        return cpAPow / (cpAPow + cpBPow)
    }

    // ==========================================
    // 模块七：离线挂机宏观数学期望
    // ==========================================

    /**
     * 离线挂机收益期望值算法 (避免暴力穷举)
     * 采用离散逻辑斯谛增长模型变体，瞬间结算离线百年期间某个网格区域的生灵繁衍与死亡。
     * @param popStart 离线前底层修士初始人口数量
     * @param capacity (K) 该区域灵气浓度支持的最大环境容量
     * @param growthRate (r) 该区域基础繁衍与突破综合速率系数 (基于种族与灵脉品阶)
     * @param tickCount (N) 经过的离线 Tick 总数
     * @return 离线演算结束后的最终人口存活数量
     */
    fun calculateOfflinePopulation(
        popStart: Int,
        capacity: Int,
        growthRate: Float,
        tickCount: Int
    ): Int {
        if (popStart <= 0 || capacity <= 0) return 0
        if (popStart >= capacity) return capacity // 已达环境饱和

        val rate = exp(-growthRate * tickCount)
        val denominator = 1.0f + ((capacity - popStart).toFloat() / popStart.toFloat()) * rate
        return (capacity / denominator).toInt()
    }

    // ==========================================
    // 模块三：宗门与人际拓扑数学模型
    // ==========================================

    /**
     * 宗门底蕴评估公式 (Heritage Score)
     * 宗门 AI 在决定吞并或外交时进行综合国力评估。
     * @param veinGrade 灵脉品阶 (1-5)
     * @param treasuryStones 宗门公款 (下品灵石总数)
     * @param totalElderCombatPower 宗门所有结丹期及以上高阶修士的战力总和
     * @return 宗门综合底蕴分
     */
    fun calculateSectHeritageScore(
        veinGrade: Int,
        treasuryStones: Long,
        totalElderCombatPower: Float
    ): Float {
        return veinGrade * 10000.0f + treasuryStones * 0.1f + totalElderCombatPower
    }

    /**
     * 神识寻仇追踪成功率算法
     * @param levelTracker 寻仇者（高阶老祖）大境界
     * @param levelTarget 被追踪者大境界
     * @param targetConcealment 被追踪者隐匿功法系数
     * @return 索敌成功概率 [0.0, 1.0]
     */
    fun calculateTrackingProbability(
        levelTracker: Int,
        levelTarget: Int,
        targetConcealment: Float
    ): Float {
        val baseProb = (levelTracker - levelTarget) * 0.2f
        return (baseProb - targetConcealment).coerceIn(0.0f, 1.0f)
    }

    // ==========================================
    // 模块五：坊市物价与奇物规则
    // ==========================================

    /**
     * 坊市物价动态平衡波动逻辑
     * @param basePrice 物品基础锚定价格
     * @param localDemand 该区域内对此物资有需求的修士总数
     * @param localSupply 坊市流通中的该物资存量
     * @param volatility 波动系数（天道可干预）
     * @return 实时收购价格
     */
    fun calculateDynamicPrice(
        basePrice: Int,
        localDemand: Int,
        localSupply: Int,
        volatility: Float = 1.0f
    ): Int {
        val supplySafe = localSupply + 1
        val priceMultiplier = 1.0f + (localDemand - localSupply).toFloat() / supplySafe.toFloat()
        val finalMultiplier = max(0.1f, priceMultiplier * volatility) // 最低跌至 1折
        return (basePrice * finalMultiplier).toInt()
    }

    /**
     * 唯一性古宝持宝人坐标暴露判定半径
     * @param artifactPowerRating 古宝评级分数 (如玄天斩灵剑为9999)
     * @param ownerRealmLevel 持宝人当前大境界
     * @param concealmentStat 持宝人隐匿功法系数 (至少为 1.0)
     * @return 暴露引怪的网格半径
     */
    fun calculateArtifactExposureRadius(
        artifactPowerRating: Int,
        ownerRealmLevel: Int,
        concealmentStat: Float
    ): Int {
        val safeConcealment = max(1.0f, concealmentStat)
        val safeLevel = ownerRealmLevel + 1
        return (artifactPowerRating / (safeConcealment * safeLevel)).toInt()
    }

    // ==========================================
    // 模块一与四：宏观系统
    // ==========================================

    /**
     * 天道本源能量（玩家剥削资源）收割公式
     * @param victimTierBase 死者境界基础价值
     * @param levelMultiplier 具体等级价值乘区
     * @param tribulationTax 遭遇雷劫时的抽成数值
     * @return 天道单次收割入账能量值
     */
    fun calculateOriginEnergyGain(
        victimTierBase: Int,
        levelMultiplier: Float,
        tribulationTax: Int
    ): Long {
        val basicGain = victimTierBase * levelMultiplier * 0.5f
        return basicGain.toLong() + tribulationTax
    }
}