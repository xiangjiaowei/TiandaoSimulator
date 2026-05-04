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
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 底层极简斗法与绝对边界控制系统
 * 负责处理同网格内散修抢夺资源的遭遇战，以及境界压制、破防判定与战死爆包回收逻辑。
 */
object CombatResolutionSystem {

    /**
     * 空间哈希数组：索引为网格的 1D ID，值为占据该网格的 EntityId。
     * 用于 O(N) 复杂度解决全图碰撞配对，避免 O(N^2) 嵌套循环卡死主线程。
     */
    private var cellOccupants = IntArray(0)

    fun onTickUpdate() {
        val width = GridManager.width.toInt()
        val height = GridManager.height.toInt()
        val totalCells = width * height
        if (totalCells <= 0) return

        // 动态扩容与 O(N) 极速清理哈希表
        if (cellOccupants.size < totalCells) {
            cellOccupants = IntArray(totalCells) { -1 }
        } else {
            // 使用底层 System.arraycopy 或原生循环极速重置为 -1
            for (i in 0 until totalCells) {
                cellOccupants[i] = -1
            }
        }

        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds
        val transforms = EntityManager.transforms

        // 倒序遍历，因为斗法随时会抹杀实体
        for (i in entityCount - 1 downTo 0) {
            val entityId = entityIds[i]
            val transform = transforms[entityId] ?: continue

            // 只有处于“历练/游荡”状态的修士才会主动触发杀人夺宝（闭关者享有临时隐匿豁免）
            if (transform.actionState != TransformComponent.STATE_EXPLORING) continue

            val x = transform.gridX.toInt()
            val y = transform.gridY.toInt()

            // 越界保护
            if (x < 0 || x >= width || y < 0 || y >= height) continue

            val cellIndex = y * width + x
            val existingOccupantId = cellOccupants[cellIndex]

            if (existingOccupantId == -1) {
                // 网格无人，占山为王
                cellOccupants[cellIndex] = entityId
            } else {
                // 冤家路窄，触发同网格遭遇战！
                resolveCombat(entityId, existingOccupantId, transform)

                // 斗法非常惨烈，无论谁胜谁负，该网格在本 Tick 的“占位”清空，避免发生连环血案导致一回合死绝
                cellOccupants[cellIndex] = -1
            }
        }
    }

    /**
     * 斗法核算核心引擎 (纯数据推演，包含护体真元绝对防御判定)
     */
    private fun resolveCombat(challengerId: Int, defenderId: Int, transform: TransformComponent) {
        val combatA = EntityManager.combats[challengerId] ?: return
        val combatB = EntityManager.combats[defenderId] ?: return

        val cultA = EntityManager.cultivations[challengerId] ?: return
        val cultB = EntityManager.cultivations[defenderId] ?: return

        val baseA = EntityManager.baseInfos[challengerId] ?: return
        val baseB = EntityManager.baseInfos[defenderId] ?: return

        // 1. 计算双方综合战力评估 (CP)
        val cpA = MathFormulas.calculateCombatPower(combatA.maxHp, combatA.baseAtk, 1.0f, 1.0f, 0)
        val cpB = MathFormulas.calculateCombatPower(combatB.maxHp, combatB.baseAtk, 1.0f, 1.0f, 0)

        // 2. 胜率模型构建
        var winProbA = MathFormulas.calculateWinRate(cpA, cpB)

        // 【天道暗箱】气运之子保底锁最低胜率
        if (baseA.luckValue == BaseInfoComponent.LUCK_PROTAGONIST) winProbA = max(winProbA, 0.8f)
        if (baseB.luckValue == BaseInfoComponent.LUCK_PROTAGONIST) winProbA = min(winProbA, 0.2f)

        // 3. 掷骰子决定攻防之势
        val roll = Random.nextFloat()
        val aWins = roll <= winProbA

        val winnerId = if (aWins) challengerId else defenderId
        val loserId = if (aWins) defenderId else challengerId

        val combatWinner = if (aWins) combatA else combatB
        val combatLoser = if (aWins) combatB else combatA

        val cultWinner = if (aWins) cultA else cultB
        val cultLoser = if (aWins) cultB else cultA

        val baseLoser = if (aWins) baseB else baseA

        // 4. 【绝对边界判定】Winner 对 Loser 发出致命一击，计算穿透力是否打破大境界防守阈值
        val weaponPenetration = 0.0f // 暂无装备系统
        val penetration = MathFormulas.calculatePenetration(combatWinner.baseAtk, weaponPenetration)
        val threshold = MathFormulas.calculateDefenseThreshold(
            defB = combatLoser.baseDef,
            realmLevelB = cultLoser.majorLevel.toInt(),
            realmLevelA = cultWinner.majorLevel.toInt()
        )

        // 判定伤害类型
        if (penetration < threshold * 0.2f) {
            // 【反震重伤】跨大境界强杀老怪，法器崩碎，Winner 自身遭严重反噬！
            val reboundDamage = (combatLoser.baseDef * 0.5f).toInt()
            combatWinner.hp -= reboundDamage

            // 如果胜方被反震死，直接抹杀
            if (combatWinner.hp <= 0 && EntityManager.baseInfos[winnerId]?.luckValue != BaseInfoComponent.LUCK_PROTAGONIST) {
                executeDeathAndLoot(survivorId = loserId, deadId = winnerId, transform)
            }
        } else if (penetration <= threshold) {
            // 【勉强破防/刮痧】造成基础伤害的 10%
            val scratchDamage = max(1, ((combatWinner.baseAtk - combatLoser.baseDef) * 0.1f).toInt())
            combatLoser.hp -= scratchDamage

            // 双方各自扣减战损，变为重伤逃遁状态，无人阵亡
            EntityManager.transforms[winnerId]?.actionState = TransformComponent.STATE_HEAVY_WOUND
            EntityManager.transforms[loserId]?.actionState = TransformComponent.STATE_HEAVY_WOUND

        } else {
            // 【碾压击杀】绝对破防，Loser 遭到致命重创
            val fullDamage = max(1, combatWinner.baseAtk - (combatLoser.baseDef / 2))
            combatLoser.hp -= fullDamage

            // 判定 Loser 死亡
            if (combatLoser.hp <= 0) {
                if (baseLoser.luckValue == BaseInfoComponent.LUCK_PROTAGONIST) {
                    // 气运之子残血遁入虚空
                    combatLoser.hp = 1
                    EntityManager.transforms[loserId]?.actionState = TransformComponent.STATE_HEAVY_WOUND
                    EventBus.post(SystemEvents.WorldLogEvent("【机缘巧合】气运之子在死斗中落败，竟触发了神秘古符，化作血光遁走！", SystemEvents.LogLevel.HEAVENLY))
                } else {
                    // 彻底处决并结算储物袋
                    executeDeathAndLoot(survivorId = winnerId, deadId = loserId, transform)
                }
            }
        }
    }

    /**
     * 执行死亡结算、战利品剥夺与灵气反哺 (鲸落)
     */
    private fun executeDeathAndLoot(survivorId: Int, deadId: Int, transform: TransformComponent) {
        val combatDead = EntityManager.combats[deadId] ?: return
        val invSurvivor = EntityManager.inventories[survivorId]
        val invDead = EntityManager.inventories[deadId]
        val cultDead = EntityManager.cultivations[deadId]

        // 1. 储物袋爆率结算
        if (invSurvivor != null && invDead != null) {
            // 胜者掠夺死者的灵石 (扣除 30% 斗法损耗)
            val deadStones = invDead.stackableItems[InventoryComponent.ITEM_SPIRIT_STONE_LOW] ?: 0
            if (deadStones > 0) {
                val lootStones = (deadStones * (1.0f - WorldConstants.RUIN_LOOT_DESTRUCT_RATIO)).toInt()
                invSurvivor.addStackableItem(InventoryComponent.ITEM_SPIRIT_STONE_LOW, lootStones)
            }
            // (其余法宝与功法玉简转移逻辑后续扩充)
        }

        // 2. 广播大能陨落
        if (cultDead != null && cultDead.majorLevel >= CultivationComponent.REALM_JIEDAN) {
            EventBus.post(
                SystemEvents.WorldLogEvent(
                    "【修仙界震动】一位结丹期以上老怪在斗法中不幸陨落，其储物袋与毕生心血皆被洗劫一空！",
                    SystemEvents.LogLevel.DANGER
                )
            )
        }

        // 3. 鲸落：体内真元反哺网格天地
        val returnRatio = WorldConstants.DEATH_QI_RETURN_RATIO
        val returnedQi = (combatDead.boundQi * returnRatio).toLong()
        if (returnedQi > 0) {
            GridManager.addActiveQiToCell(transform.gridX, transform.gridY, returnedQi)
        }

        // 4. 彻底从 ECS 内存数组中抹杀实体
        EntityManager.destroyEntity(deadId)
    }
}