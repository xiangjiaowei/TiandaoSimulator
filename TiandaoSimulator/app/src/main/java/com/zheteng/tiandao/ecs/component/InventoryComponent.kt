package com.zheteng.tiandao.ecs.component

/**
 * 虚拟储物袋与物品组件 (纯数据结构体)
 * 记录生灵携带的资源、丹药、法宝与功法玉简。
 * 纯数值 ID 映射，拒绝实例化庞大的 Item 对象树，确保内存与 GC 稳定。
 */
data class InventoryComponent(
    /**
     * 实体全局唯一 ID
     */
    val entityId: Int,

    /**
     * 储物袋容量上限 (格子数)
     * 取决于修士装备的储物法器品阶。战胜对手后若容量不足，多余的战利品将掉落在网格中。
     */
    var capacity: Int,

    /**
     * 堆叠类物资字典 (ItemID -> Count)
     * 用于存储下品灵石、各阶灵草、消耗型丹药（如筑基丹）、符箓等。
     */
    val stackableItems: MutableMap<Int, Int>,

    /**
     * 非堆叠/唯一类物品列表 (存储全局唯一的 Artifact/Equipment ID)
     * 用于存储法宝、阵盘、功法传承玉简、太古奇宝（如虚天鼎）。
     * 若列表中包含高品阶古宝，在动用时将触发 MathFormulas.calculateArtifactExposureRadius 引来高阶老怪追杀。
     */
    val uniqueArtifactIds: MutableList<Int>
) {
    /**
     * 添加堆叠类物品
     */
    fun addStackableItem(itemId: Int, count: Int) {
        if (count <= 0) return
        val currentCount = stackableItems[itemId] ?: 0
        stackableItems[itemId] = currentCount + count
    }

    /**
     * 扣除堆叠类物品
     * @return 是否扣除成功（余额是否充足）
     */
    fun consumeStackableItem(itemId: Int, count: Int): Boolean {
        if (count <= 0) return true
        val currentCount = stackableItems[itemId] ?: 0
        if (currentCount < count) return false

        if (currentCount == count) {
            stackableItems.remove(itemId)
        } else {
            stackableItems[itemId] = currentCount - count
        }
        return true
    }

    /**
     * 获取当前占据的格子总数（近似算法：每个堆叠 ID 占一格，每个独特古宝占一格）
     */
    fun getUsedCapacity(): Int {
        return stackableItems.size + uniqueArtifactIds.size
    }

    /**
     * 判断储物袋是否已满
     */
    fun isFull(): Boolean {
        return getUsedCapacity() >= capacity
    }

    companion object {
        // ==========================================
        // 核心硬通货常量字典 (Hardcoded Item IDs)
        // 坊市交易与天道法则核算时的通用等价物基准
        // ==========================================

        /** 下品灵石 (修仙界基础货币，维持大阵、底层交易的核心) */
        const val ITEM_SPIRIT_STONE_LOW: Int = 1001

        /** 中品灵石 */
        const val ITEM_SPIRIT_STONE_MID: Int = 1002

        /** 高品灵石 (战略资源，常用于跨大洲传送阵或元婴级交易) */
        const val ITEM_SPIRIT_STONE_HIGH: Int = 1003

        /** 筑基丹 (引发底层血色禁地屠杀的万恶之源) */
        const val ITEM_PILL_FOUNDATION: Int = 2001

        /** 降尘丹 (结丹期保底圣药) */
        const val ITEM_PILL_GOLDEN_CORE: Int = 2002

        /** 寿元果 (延长寿命，最高权重吸引老怪拼命的奇物) */
        const val ITEM_FRUIT_LIFESPAN: Int = 3001
    }
}