package com.zheteng.tiandao.ecs.system

import com.zheteng.tiandao.core.config.MathFormulas
import com.zheteng.tiandao.core.config.WorldConstants
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.component.BaseInfoComponent
import com.zheteng.tiandao.ecs.component.CombatComponent
import com.zheteng.tiandao.ecs.component.CultivationComponent
import com.zheteng.tiandao.ecs.component.TransformComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import com.zheteng.tiandao.world.grid.GridManager
import kotlin.random.Random

/**
 * 境界与修为演化系统
 * 负责处理生灵的灵气吸纳、经验增长、瓶颈卡点以及残酷的雷劫判定。
 */
object CultivationSystem {

    fun onTickUpdate() {
        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds

        // 核心数组引用缓存在局部变量，避免循环内重复寻址
        val baseInfos = EntityManager.baseInfos
        val cultivations = EntityManager.cultivations
        val transforms = EntityManager.transforms

        // 倒序遍历，因为突破失败遭遇雷劫陨落的实体会被 Swap And Pop 抹杀
        for (i in entityCount - 1 downTo 0) {
            val entityId = entityIds[i]

            val cultComp = cultivations[entityId] ?: continue
            val baseInfo = baseInfos[entityId] ?: continue
            val transform = transforms[entityId] ?: continue

            if (cultComp.isInBottleneck) {
                // 处于瓶颈期，不吸纳灵气，直接进行突破概率检定
                processBreakthrough(entityId, cultComp, baseInfo, transform)
            } else {
                // 正常修炼吸纳灵气
                processCultivation(cultComp, baseInfo, transform)
            }
        }
    }

    /**
     * 处理灵气吸纳与经验转化
     */
    private fun processCultivation(
        cult: CultivationComponent,
        baseInfo: BaseInfoComponent,
        transform: TransformComponent
    ) {
        // 1. 获取灵根基础吸取倍率
        val linggenMultiplier = when (baseInfo.linggenType) {
            BaseInfoComponent.LINGGEN_NONE -> 0.0f
            BaseInfoComponent.LINGGEN_WEI -> 0.1f
            BaseInfoComponent.LINGGEN_ZHA -> 0.5f
            BaseInfoComponent.LINGGEN_ZHEN -> 1.0f
            BaseInfoComponent.LINGGEN_DI -> 3.0f
            BaseInfoComponent.LINGGEN_TIAN -> 10.0f
            else -> 0.0f
        }

        if (linggenMultiplier <= 0f) return

        // 2. 状态补正：如果处于历练或战斗状态，修炼效率大幅下降
        var stateMultiplier = 1.0f
        if (transform.actionState != TransformComponent.STATE_CULTIVATING) {
            stateMultiplier = 0.2f
        }

        // 3. 计算本 Tick 理论上能吸取的灵气量基数
        val baseDemand = 10L // 炼气一层的基础需求常量
        val realmMultiplier = cult.majorLevel.toLong()
        val requestedQi = (baseDemand * realmMultiplier * linggenMultiplier * stateMultiplier).toLong()

        if (requestedQi <= 0L) return

        // 4. 从当前所在的网格中抽取真实游离灵气（如果网格灵气枯竭，则抽不到）
        val actualGainedQi = GridManager.consumeQiFromCell(transform.gridX, transform.gridY, requestedQi)

        if (actualGainedQi > 0L) {
            // 魔族自带吸灵极快天赋，获得额外补正
            val raceBonus = if (baseInfo.raceType == BaseInfoComponent.RACE_MO) 1.5f else 1.0f

            // 转化为修为
            cult.currentExp += (actualGainedQi * raceBonus).toLong()

            // 5. 校验是否触碰瓶颈
            val safeMajor = cult.majorLevel.toInt().coerceAtMost(MathFormulas.MAX_MAJOR_LEVEL)
            val safeMinor = cult.minorLevel.toInt().coerceAtMost(MathFormulas.MAX_MINOR_LEVEL)
            val maxExpReq = MathFormulas.EXP_REQUIREMENT_TABLE[safeMajor][safeMinor]

            if (cult.currentExp >= maxExpReq) {
                cult.currentExp = maxExpReq // 锁死上限
                cult.isInBottleneck = true
            }
        }
    }

    /**
     * 处理瓶颈突破检定
     */
    private fun processBreakthrough(
        entityId: Int,
        cult: CultivationComponent,
        baseInfo: BaseInfoComponent,
        transform: TransformComponent
    ) {
        // 已达此界绝对巅峰，无法继续突破
        if (cult.majorLevel >= MathFormulas.MAX_MAJOR_LEVEL && cult.minorLevel >= MathFormulas.MAX_MINOR_LEVEL) return

        val isMajorBreakthrough = (cult.minorLevel >= MathFormulas.MAX_MINOR_LEVEL)

        // 小境界突破相对容易，大境界突破极难
        val baseSuccessRate = if (isMajorBreakthrough) 0.05f else 0.40f

        // 灵根越高，突破越容易
        val linggenMod = when (baseInfo.linggenType) {
            BaseInfoComponent.LINGGEN_TIAN -> 3.0f
            BaseInfoComponent.LINGGEN_DI -> 1.5f
            BaseInfoComponent.LINGGEN_ZHEN -> 1.0f
            else -> 0.5f
        }

        // 实际开发中这里需读取 InventoryComponent 判断是否持有/消耗筑基丹等破境丹药
        // 目前沙盒宏观推演暂计 pillBonus = 0f
        val pillBonus = 0f

        val finalProb = MathFormulas.calculateBreakthroughProbability(
            baseProb = baseSuccessRate,
            linggenMod = linggenMod,
            pillBonus = pillBonus,
            bottleneckPenalty = cult.bottleneckPenalty
        )

        // 掷骰子检定
        if (Random.nextFloat() <= finalProb) {
            // ==========================================
            // 突破成功逻辑
            // ==========================================
            var survive = true
            var newMajor = cult.majorLevel

            // 如果是大境界突破，且达到元婴期，强制触发天雷劫
            if (isMajorBreakthrough) {
                newMajor = (cult.majorLevel + 1).toByte()
                if (newMajor >= CultivationComponent.REALM_YUANYING) {
                    survive = executeTribulation(entityId, newMajor, baseInfo)
                }
            }

            if (survive) {
                // 更新境界
                if (isMajorBreakthrough) {
                    cult.majorLevel = newMajor
                    cult.minorLevel = 1

                    // 广播大能突破异象
                    if (cult.majorLevel >= CultivationComponent.REALM_JIEDAN) {
                        EventBus.post(
                            SystemEvents.EntityBreakthroughEvent(
                                entityId = entityId,
                                newMajorRealm = cult.majorLevel,
                                gridX = transform.gridX,
                                gridY = transform.gridY
                            )
                        )
                    }
                } else {
                    cult.minorLevel++
                }

                // 重置瓶颈状态与心魔惩罚
                cult.isInBottleneck = false
                cult.currentExp = 0L
                cult.bottleneckPenalty = 0

                // 突破后重塑肉身，恢复满血并更新固化灵气 (鲸落底蕴)
                val combatComp = EntityManager.combats[entityId]
                if (combatComp != null) {
                    combatComp.maxHp = (combatComp.maxHp * 1.5f).toInt()
                    combatComp.healToMax()
                    // 每次突破，其固化的天道灵气大幅增长
                    combatComp.boundQi += (cult.majorLevel * 10000L)
                }
            }
        } else {
            // ==========================================
            // 突破失败逻辑
            // ==========================================
            cult.bottleneckPenalty++

            // 突破失败会导致反噬，强行转入重伤状态
            transform.actionState = TransformComponent.STATE_HEAVY_WOUND

            val combatComp = EntityManager.combats[entityId]
            if (combatComp != null) {
                val damage = (combatComp.maxHp * 0.3f).toInt()
                combatComp.hp -= damage
                if (combatComp.hp <= 0 && baseInfo.luckValue != BaseInfoComponent.LUCK_PROTAGONIST) {
                    // 走火入魔爆体而亡
                    executeDeath(entityId, transform, combatComp)
                }
            }
        }
    }

    /**
     * 执行天雷劫核算
     * @return 是否在雷劫中存活
     */
    private fun executeTribulation(entityId: Int, newMajorRealm: Byte, baseInfo: BaseInfoComponent): Boolean {
        val combatComp = EntityManager.combats[entityId] ?: return true

        // 天劫基础伤害指数暴增
        val baseDamage = newMajorRealm.toInt() * 5000
        val racePenalty = if (baseInfo.raceType == BaseInfoComponent.RACE_YAO || baseInfo.raceType == BaseInfoComponent.RACE_MO) 1.5f else 1.0f

        // TODO: 后续接入天道操作台 (GodModeApi) 读取玩家设定的雷罚倍率
        val heavenlyDaoModifier = 0.0f

        val finalDamage = MathFormulas.calculateTribulationDamage(baseDamage, heavenlyDaoModifier, racePenalty)

        // 简易抗雷判定：自身 HP + 护甲 (暂未接法宝护盾)
        if (combatComp.hp >= finalDamage) {
            combatComp.hp -= finalDamage
            return true
        } else {
            // 气运之子保底机制
            if (baseInfo.luckValue == BaseInfoComponent.LUCK_PROTAGONIST) {
                combatComp.hp = 1 // 锁血
                EventBus.post(
                    SystemEvents.WorldLogEvent(
                        "【逆天改命】气运之子在九霄神雷下肉身尽毁，竟有一缕残魂附着于神秘吊坠中逃过一劫！",
                        SystemEvents.LogLevel.HEAVENLY
                    )
                )
                return true
            }

            // 正常灰飞烟灭
            val transform = EntityManager.transforms[entityId]
            if (transform != null) {
                executeDeath(entityId, transform, combatComp)
                EventBus.post(
                    SystemEvents.WorldLogEvent(
                        "【天威难测】一位修士未能扛过天雷劫，在雷光中灰飞烟灭！",
                        SystemEvents.LogLevel.WARNING
                    )
                )
            }
            return false
        }
    }

    /**
     * 抹杀走火入魔或渡劫失败的实体，并执行鲸落
     */
    private fun executeDeath(entityId: Int, transform: TransformComponent, combat: CombatComponent) {
        val returnRatio = WorldConstants.DEATH_QI_RETURN_RATIO
        val returnedQi = (combat.boundQi * returnRatio).toLong()
        if (returnedQi > 0) {
            GridManager.addActiveQiToCell(transform.gridX, transform.gridY, returnedQi)
        }
        EntityManager.destroyEntity(entityId)
    }
}