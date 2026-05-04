package com.zheteng.tiandao.ecs.component

/**
 * 坐标与状态组件 (纯数据结构体)
 * 决定了该生灵在二维网格地图中的绝对位置，以及当前正在执行的核心行为（状态机）。
 * 便于区域统计、寻脉迁徙和遭遇战触发。
 */
data class TransformComponent(
    /**
     * 实体全局唯一 ID
     */
    val entityId: Int,

    /**
     * 当前所在的网格 X 坐标
     */
    var gridX: Short,

    /**
     * 当前所在的网格 Y 坐标
     */
    var gridY: Short,

    /**
     * 当前的行为状态 (极简状态机)
     * 对应 [ActionState] 常量。底层各个 System 会根据此状态决定是否处理该实体。
     */
    var actionState: Byte
) {
    /**
     * 快速更新坐标的辅助函数，避免直接创建新对象
     */
    fun updatePosition(newX: Short, newY: Short) {
        this.gridX = newX
        this.gridY = newY
    }

    companion object {
        // ==========================================
        // ActionState (行为状态机常量)
        // 决定了实体在当前 Tick 中的逻辑分支
        // ==========================================

        /**
         * 闭关 / 打坐吸纳灵气
         * 此状态下处于隐匿安全期，全力获取修为，较难触发外部遭遇战。
         */
        const val STATE_CULTIVATING: Byte = 0

        /**
         * 外出历练 / 游荡搜寻物资
         * 此状态下极易与同一网格内的其他历练实体发生碰撞（抢夺机缘）。
         */
        const val STATE_EXPLORING: Byte = 1

        /**
         * 重伤逃遁 / 闭死关
         * 斗法失败或强行破阵后的虚弱期，战力大幅下降，无法主动移动。
         */
        const val STATE_HEAVY_WOUND: Byte = 2

        /**
         * 交战中
         * 被 CombatSystem 捕获锁定，正在进行生死斗法推演，无法被其他事件打断。
         */
        const val STATE_IN_COMBAT: Byte = 3

        /**
         * 神识寻仇追踪
         * 高阶老怪触发血仇机制时进入此状态，每 Tick 向仇人坐标直线逼近，无视灵脉。
         */
        const val STATE_TRACKING_ENEMY: Byte = 4
    }
}