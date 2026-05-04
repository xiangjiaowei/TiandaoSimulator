package com.zheteng.tiandao.world.social

import com.zheteng.tiandao.core.config.MathFormulas
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.component.BaseInfoComponent
import com.zheteng.tiandao.ecs.component.CultivationComponent
import com.zheteng.tiandao.ecs.component.TransformComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import kotlin.math.sign
import kotlin.random.Random

/**
 * 神识寻仇与因果追踪系统
 * 负责解析 [KarmaGraph] 中的血仇关系，并驱动高阶老怪对仇家进行跨区域打击。
 */
object VengeanceSystem {

    /**
     * 由 TickEngine 每一 Tick (月) 调度
     */
    fun onTickUpdate() {
        val activeIds = EntityManager.activeEntityIds
        val count = EntityManager.activeEntityCount

        val cultivations = EntityManager.cultivations
        val transforms = EntityManager.transforms
        val baseInfos = EntityManager.baseInfos

        // 遍历所有活跃实体，寻找具有寻仇动机的老怪
        for (i in 0 until count) {
            val trackerId = activeIds[i]
            val trackerCult = cultivations[trackerId] ?: continue

            // 只有结丹期及以上老怪拥有足够的神识强度进行全图追踪
            if (trackerCult.majorLevel < CultivationComponent.REALM_JIEDAN) continue

            // 获取该老怪所有的血仇边
            val feuds = KarmaGraph.getRelationsFrom(trackerId).filter {
                it.relationType == KarmaGraph.RELATION_BLOOD_FEUD
            }

            if (feuds.isEmpty()) continue

            // 寻找最恨的那一个仇人进行追踪
            val targetEdge = feuds.maxByOrNull { it.intensity } ?: continue
            val targetId = targetEdge.targetId

            // 校验目标是否还活着
            val targetTransform = transforms[targetId] ?: continue
            val targetCult = cultivations[targetId] ?: continue
            val trackerTransform = transforms[trackerId] ?: continue

            // 计算神识追踪成功率 (基于境界压制与对方的敛气手段)
            // 目前 targetConcealment 暂定为 0.0，后续接入法宝系统后由 InventoryComponent 判定
            val trackProb = MathFormulas.calculateTrackingProbability(
                levelTracker = trackerCult.majorLevel.toInt(),
                levelTarget = targetCult.majorLevel.toInt(),
                targetConcealment = 0.0f
            )

            if (Random.nextFloat() <= trackProb) {
                // 追踪成功！
                executeTracking(trackerId, trackerTransform, targetId, targetTransform)
            } else {
                // 追踪失败，老怪在原位愤怒咆哮，状态切回闭关或游荡
                if (trackerTransform.actionState == TransformComponent.STATE_TRACKING_ENEMY) {
                    trackerTransform.actionState = TransformComponent.STATE_CULTIVATING
                }
            }
        }

        // 每一 Tick 自然衰减全图因果值，让陈年旧怨随时间消散
        KarmaGraph.decayAllRelations(decayRate = 1)
    }

    /**
     * 执行追踪逻辑：
     * 若境界压制极大，直接瞬移（瞬移至目标网格）；
     * 若境界相仿，则向目标坐标逼近。
     */
    private fun executeTracking(
        trackerId: Int,
        trackerTrans: TransformComponent,
        targetId: Int,
        targetTrans: TransformComponent
    ) {
        val trackerCult = EntityManager.cultivations[trackerId] ?: return
        val targetCult = EntityManager.cultivations[targetId] ?: return

        val realmDiff = trackerCult.majorLevel - targetCult.majorLevel

        if (realmDiff >= 2) {
            // 【神通：缩地成寸】境界压制 2 层以上，直接降临仇家所在网格！
            trackerTrans.updatePosition(targetTrans.gridX, targetTrans.gridY)
            trackerTrans.actionState = TransformComponent.STATE_TRACKING_ENEMY

            EventBus.post(
                SystemEvents.WorldLogEvent(
                    "【神识锁定】一位老怪破空而出，已将仇家气息牢牢锁定！",
                    SystemEvents.LogLevel.DANGER
                )
            )
        } else {
            // 【虹光飞行】境界相仿，每 Tick 向目标移动 1-2 个网格
            val dx = (targetTrans.gridX - trackerTrans.gridX).coerceIn(-2, 2)
            val dy = (targetTrans.gridY - trackerTrans.gridY).coerceIn(-2, 2)

            trackerTrans.updatePosition(
                (trackerTrans.gridX + dx).toShort(),
                (trackerTrans.gridY + dy).toShort()
            )
            trackerTrans.actionState = TransformComponent.STATE_TRACKING_ENEMY
        }
    }
}