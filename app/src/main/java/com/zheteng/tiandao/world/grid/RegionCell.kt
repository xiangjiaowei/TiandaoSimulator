package com.zheteng.tiandao.world.grid

/**
 * 区域网格单元数据 (纯数据结构体)
 * 代表 N x M 大世界矩阵中的一个绝对单元格。
 * 不涉及任何 UI 渲染，只维护环境级别的数值。
 */
data class RegionCell(
    /**
     * X 坐标
     */
    val cellX: Short,

    /**
     * Y 坐标
     */
    val cellY: Short,

    /**
     * 当前区块的游离灵气总量 (Local Active Qi)
     * 该区块内所有修士闭关打坐，都在竞争抽取此数值。
     * 若被抽干至枯竭阈值以下，将引发大规模的宗门迁徙或灭门夺脉战。
     */
    var localActiveQi: Long = 0L,

    /**
     * 占领此地的宗门 ID
     * 对应 FactionComponent 中的 sectId。如果为 0，则代表此地为三不管的法外之地（无主之地）。
     */
    var sectOwnerId: Int = 0,

    /**
     * 环境凶险度 (取值 0 ~ 100)
     * 决定了此区域触发妖兽潮的底层概率，以及散修在此地历练（Exploring 状态）时的基础暴毙率。
     * 天道（玩家）可通过干预法则强行拉高此数值来清理过剩的底层人口。
     */
    var dangerLevel: Byte = 0
) {

    /**
     * 尝试从此网格中抽取天地灵气 (线程安全由于 TickEngine 的单线程绝对控制)
     * @param requestedAmount 实体请求吸纳的理论灵气值
     * @return 实际成功抽取的灵气值（如果网格灵脉贫瘠，可能不足额甚至返回 0）
     */
    fun consumeQi(requestedAmount: Long): Long {
        if (localActiveQi <= 0L) return 0L

        return if (localActiveQi >= requestedAmount) {
            localActiveQi -= requestedAmount
            requestedAmount
        } else {
            // 榨干最后一丝灵气
            val actual = localActiveQi
            localActiveQi = 0L
            actual
        }
    }

    /**
     * 向此网格注入灵气
     * 触发场景：大能在此地陨落引发“一鲸落万物生”，或者天道降下“灵气复苏”祥瑞事件。
     * @param amount 注入的灵气量
     */
    fun addQi(amount: Long) {
        if (amount > 0L) {
            localActiveQi += amount
        }
    }

    /**
     * 快速评估本网格的灵气是否处于枯竭状态
     * @param threshold 判定为枯竭的基准线（由世界常数或灵脉品阶决定，此处提供默认值 1000）
     */
    fun isQiExhausted(threshold: Long = 1000L): Boolean {
        return localActiveQi < threshold
    }
}