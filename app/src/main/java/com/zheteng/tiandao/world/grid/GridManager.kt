package com.zheteng.tiandao.world.grid

import kotlin.math.max
import kotlin.math.min

/**
 * 大世界网格矩阵管理器 (全局单例)
 * 采用扁平化的一维连续数组来模拟 N x M 二维地图，确保 O(1) 的极致寻址性能。
 * 负责整个修仙界游离灵气的分布、坐标越界校验以及宏观区域查询。
 */
object GridManager {

    /**
     * 大世界网格的宽度 (X 轴容量)
     */
    var width: Short = 0
        private set

    /**
     * 大世界网格的高度 (Y 轴容量)
     */
    var height: Short = 0
        private set

    /**
     * 核心连续内存块：存储所有网格单元
     */
    private lateinit var cells: Array<RegionCell>

    /**
     * 天道创世：初始化大世界网格并分配基础灵气
     * 游戏在首次启动或生成新世界时调用此接口。
     *
     * @param w 网格宽度 (建议值如 100)
     * @param h 网格高度 (建议值如 100)
     * @param initialTotalQi 世界初始拥有的游离灵气总量，将被平分至所有网格
     */
    fun initializeWorld(w: Short, h: Short, initialTotalQi: Long) {
        this.width = w
        this.height = h

        val totalCells = w.toInt() * h.toInt()
        val baseQiPerCell = if (totalCells > 0) initialTotalQi / totalCells else 0L

        // 初始化连续数组
        cells = Array(totalCells) { index ->
            val cx = (index % w).toShort()
            val cy = (index / w).toShort()
            RegionCell(
                cellX = cx,
                cellY = cy,
                localActiveQi = baseQiPerCell
            )
        }
    }

    /**
     * O(1) 绝对寻址获取对应坐标的网格实体。
     * 若坐标越界，则返回 null。
     */
    fun getCell(x: Short, y: Short): RegionCell? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        val index = y.toInt() * width.toInt() + x.toInt()
        return cells[index]
    }

    /**
     * 暴露给 CultivationSystem 高频调用的灵气抽取接口
     */
    fun consumeQiFromCell(x: Short, y: Short, requestedAmount: Long): Long {
        val cell = getCell(x, y) ?: return 0L
        return cell.consumeQi(requestedAmount)
    }

    /**
     * 暴露给 AgingSystem (鲸落) 与 GodModeApi (降下祥瑞) 的灵气注入接口
     */
    fun addActiveQiToCell(x: Short, y: Short, amount: Long) {
        val cell = getCell(x, y) ?: return
        cell.addQi(amount)
    }

    /**
     * 获取指定坐标周围的相邻网格 (用于底层散修游荡或短距离灵脉寻找)
     * 不在循环中 new 数组，而是填充到传入的列表中，压榨 GC 性能。
     */
    fun getAdjacentCells(x: Short, y: Short, outList: MutableList<RegionCell>) {
        outList.clear()

        // 上下左右四向邻接
        getCell(x, (y - 1).toShort())?.let { outList.add(it) }
        getCell(x, (y + 1).toShort())?.let { outList.add(it) }
        getCell((x - 1).toShort(), y)?.let { outList.add(it) }
        getCell((x + 1).toShort(), y)?.let { outList.add(it) }
    }

    /**
     * 获取指定半径范围内的所有网格
     * 用于天道施加“局部法则变异”或极品古宝出世时的引怪暴露范围。
     */
    fun getCellsInRange(centerX: Short, centerY: Short, radius: Int): List<RegionCell> {
        val result = mutableListOf<RegionCell>()
        if (radius <= 0) {
            getCell(centerX, centerY)?.let { result.add(it) }
            return result
        }

        val startX = max(0, centerX - radius)
        val endX = min(width - 1, centerX + radius)
        val startY = max(0, centerY - radius)
        val endY = min(height - 1, centerY + radius)

        for (y in startY..endY) {
            for (x in startX..endX) {
                val index = y * width.toInt() + x
                result.add(cells[index])
            }
        }
        return result
    }

    /**
     * 统计全图当前的游离灵气总量 (Active Qi)
     * 用于评估是否触发【末法时代】。
     */
    fun getTotalActiveQi(): Long {
        var total = 0L
        for (i in cells.indices) {
            total += cells[i].localActiveQi
        }
        return total
    }
}