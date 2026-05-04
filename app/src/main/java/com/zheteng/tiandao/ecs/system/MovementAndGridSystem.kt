package com.zheteng.tiandao.ecs.system

import com.zheteng.tiandao.ecs.component.FactionComponent
import com.zheteng.tiandao.ecs.component.TransformComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import com.zheteng.tiandao.world.grid.GridManager
import com.zheteng.tiandao.world.grid.RegionCell
import kotlin.math.max

/**
 * 寻路迁徙与网格状态系统
 * 负责底层散修的趋利避害逻辑：自动检测周围网格的灵气浓度，并向高浓度区域流窜。
 */
object MovementAndGridSystem {

    /**
     * 单线程复用缓存列表，绝对禁止在 Tick 循环中 new 任何 List 对象！
     */
    private val adjacentCache = ArrayList<RegionCell>(4)

    /**
     * 由 TickEngine 每一 Tick (月) 高频调用
     */
    fun onTickUpdate() {
        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds

        val transforms = EntityManager.transforms
        val factions = EntityManager.factions

        for (i in entityCount - 1 downTo 0) {
            val entityId = entityIds[i]

            val transform = transforms[entityId] ?: continue

            // 处于交战或重伤状态的修士无法移动
            if (transform.actionState == TransformComponent.STATE_IN_COMBAT ||
                transform.actionState == TransformComponent.STATE_HEAVY_WOUND) {
                continue
            }

            // 处于神识寻仇追踪状态的修士，由专门的 VengeanceSystem 覆写坐标，这里跳过
            if (transform.actionState == TransformComponent.STATE_TRACKING_ENEMY) {
                continue
            }

            val faction = factions[entityId] ?: continue

            // 【核心生态逻辑】：只有散修（sectId == 0）会盲目逐灵气而居。
            // 宗门修士的迁徙由宏观的 FactionAndWarSystem 决定（举宗搬迁）。
            if (faction.isRogueCultivator()) {
                processRogueMigration(transform)
            }
        }
    }

    /**
     * 散修迁徙判定算法
     * 规则：如果发现相邻网格灵气高于当前网格的 1.5倍（且具有一定基数），立即执行坐标变更。
     */
    private fun processRogueMigration(transform: TransformComponent) {
        val currentX = transform.gridX
        val currentY = transform.gridY

        val currentCell = GridManager.getCell(currentX, currentY) ?: return
        val currentQi = currentCell.localActiveQi

        // 获取相邻四个网格放入复用缓存池
        GridManager.getAdjacentCells(currentX, currentY, adjacentCache)

        if (adjacentCache.isEmpty()) return

        var bestCell: RegionCell? = null
        var maxAdjQi = -1L

        // 寻找四个方向中灵气最充沛的网格
        for (i in 0 until adjacentCache.size) {
            val adjCell = adjacentCache[i]
            if (adjCell.localActiveQi > maxAdjQi) {
                maxAdjQi = adjCell.localActiveQi
                bestCell = adjCell
            }
        }

        // 迁徙阈值公式：相邻网格灵气 > 当前网格灵气 * 1.5，并且相邻网格灵气至少 > 500 (防止为了个位数灵气来回抖动跑酷)
        val threshold = max((currentQi * 1.5f).toLong(), 500L)

        if (bestCell != null && maxAdjQi > threshold) {
            // 执行迁徙！
            transform.updatePosition(bestCell.cellX, bestCell.cellY)

            // 散修在迁徙过程中，状态强制切换为【游荡/历练】，极易在新区域引发与原住民的抢地盘斗法
            transform.actionState = TransformComponent.STATE_EXPLORING
        } else {
            // 灵气相对充裕，留在此地继续闭关打坐
            transform.actionState = TransformComponent.STATE_CULTIVATING
        }
    }
}