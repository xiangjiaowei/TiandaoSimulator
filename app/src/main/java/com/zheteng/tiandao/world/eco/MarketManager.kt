package com.zheteng.tiandao.world.eco

import com.zheteng.tiandao.core.config.MathFormulas
import com.zheteng.tiandao.ecs.component.CultivationComponent
import com.zheteng.tiandao.ecs.component.InventoryComponent
import com.zheteng.tiandao.ecs.component.TransformComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import com.zheteng.tiandao.world.grid.GridManager
import kotlin.math.max

/**
 * 坊市数据模型
 */
data class MarketData(
    val gridX: Short,
    val gridY: Short,
    val sectOwnerId: Int,
    /** 物资 ID 映射到当前的实时单价 */
    val priceMap: MutableMap<Int, Int> = mutableMapOf(),
    /** 物资 ID 映射到当前的坊市库存量 */
    val supplyMap: MutableMap<Int, Int> = mutableMapOf()
)

/**
 * 大世界坊市物价动态平衡管理器
 * 负责全图商业节点的生成、物资供需核算以及物价波动推演。
 */
object MarketManager {

    /** 全图活跃坊市列表 (通常位于二级以上宗门驻地) */
    private val activeMarkets = mutableListOf<MarketData>()

    /** 基础物资锚定价格表 (ItemID -> BasePrice) */
    private val basePriceTable = mapOf(
        InventoryComponent.ITEM_SPIRIT_STONE_LOW to 1,
        InventoryComponent.ITEM_PILL_FOUNDATION to 500,
        InventoryComponent.ITEM_PILL_GOLDEN_CORE to 5000,
        InventoryComponent.ITEM_FRUIT_LIFESPAN to 20000
    )

    /**
     * 由 TickEngine 定期调用 (例如每 12 Tick 核算一次物价)
     */
    fun onTickUpdate() {
        refreshMarketsFromWorld()
        updateAllMarketPrices()
    }

    /**
     * 扫描全图，根据宗门占领情况动态开设或关闭坊市
     */
    private fun refreshMarketsFromWorld() {
        activeMarkets.clear()
        val width = GridManager.width
        val height = GridManager.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val cell = GridManager.getCell(x.toShort(), y.toShort()) ?: continue
                // 只有被宗门占领的区域才会开设坊市
                if (cell.sectOwnerId != 0) {
                    activeMarkets.add(MarketData(cell.cellX, cell.cellY, cell.sectOwnerId))
                }
            }
        }
    }

    /**
     * 核心物价演化算法：基于局部网格的供需关系核算
     */
    private fun updateAllMarketPrices() {
        val entityCount = EntityManager.activeEntityCount
        val entityIds = EntityManager.activeEntityIds
        val transforms = EntityManager.transforms
        val cultivations = EntityManager.cultivations

        for (market in activeMarkets) {
            // 1. 统计当前坊市辐射范围 (Radius=3) 内的潜在需求
            // 以“筑基丹”为例：统计区域内处于炼气大圆满且卡瓶颈的修士数量
            var foundationPillDemand = 0

            for (i in 0 until entityCount) {
                val eId = entityIds[i]
                val trans = transforms[eId] ?: continue

                // 检查实体是否在坊市附近
                if (Math.abs(trans.gridX - market.gridX) <= 3 && Math.abs(trans.gridY - market.gridY) <= 3) {
                    val cult = cultivations[eId] ?: continue
                    // 需求判定：卡在炼气大圆满
                    if (cult.majorLevel == CultivationComponent.REALM_LIANQI && cult.isInBottleneck) {
                        foundationPillDemand++
                    }
                }
            }

            // 2. 获取当前坊市的库存 (Supply)
            // 在纯沙盒推演中，库存由宗门生产力和玩家投放决定
            val currentSupply = market.supplyMap.getOrDefault(InventoryComponent.ITEM_PILL_FOUNDATION, 5)

            // 3. 调用数学公式计算最终波动价格
            val basePrice = basePriceTable[InventoryComponent.ITEM_PILL_FOUNDATION] ?: 500
            val dynamicPrice = MathFormulas.calculateDynamicPrice(
                basePrice = basePrice,
                localDemand = foundationPillDemand,
                localSupply = currentSupply,
                volatility = 1.0f // 可接入天道干预倍率
            )

            market.priceMap[InventoryComponent.ITEM_PILL_FOUNDATION] = dynamicPrice

            // 4. 模拟库存的自然消耗：每轮核算，坊市会自动卖出少量库存给 NPC
            if (currentSupply > 0) {
                val sold = max(1, currentSupply / 10)
                market.supplyMap[InventoryComponent.ITEM_PILL_FOUNDATION] = currentSupply - sold
            }
        }
    }

    /**
     * 获取指定坐标附近的坊市物资价格
     */
    fun getLocalPrice(gridX: Short, gridY: Short, itemId: Int): Int {
        val nearestMarket = activeMarkets.minByOrNull {
            Math.abs(it.gridX - gridX) + Math.abs(it.gridY - gridY)
        }
        return nearestMarket?.priceMap?.get(itemId) ?: basePriceTable[itemId] ?: 0
    }

    /**
     * 外部干预接口：向特定坊市注入物资 (例如宗门产出或玩家投放)
     */
    fun injectSupply(gridX: Short, gridY: Short, itemId: Int, amount: Int) {
        val market = activeMarkets.find { it.gridX == gridX && it.gridY == gridY }
        market?.let {
            val current = it.supplyMap.getOrDefault(itemId, 0)
            it.supplyMap[itemId] = current + amount
        }
    }
}