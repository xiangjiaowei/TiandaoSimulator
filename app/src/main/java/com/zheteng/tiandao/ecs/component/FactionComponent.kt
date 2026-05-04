package com.zheteng.tiandao.ecs.component

/**
 * 宗门阵营与社会剥削阶级组件 (纯数据结构体)
 * 维系修仙界金字塔剥削关系的核心数据。
 * 记录修士的宗门归属、地位层级以及宗门贡献度。
 */
data class FactionComponent(
    /**
     * 实体全局唯一 ID
     */
    val entityId: Int,

    /**
     * 归属的宗门 ID
     * 对应 SectDbEntity 的主键。如果值为 0，则代表该实体是无依无靠、四处游荡的散修。
     */
    var sectId: Int,

    /**
     * 在宗门内的阶级地位
     * 决定了在宗门周期性事件中的待遇（是被剥削的炮灰，还是享受供奉的高层）。
     * 对应 [SectPosition] 常量。
     */
    var position: Byte,

    /**
     * 宗门贡献点
     * 底层弟子通过上交灵草、矿石或参与宗门战获得。
     * 达到一定数值且满足修为门槛时，可提升 [position] 阶级或兑换功法玉简。
     */
    var contribution: Int
) {
    /**
     * 判断该实体是否为无宗门的散修
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun isRogueCultivator(): Boolean {
        return this.sectId == SECT_NONE
    }

    companion object {
        /** 无宗门 / 散修标识 */
        const val SECT_NONE: Int = 0

        // ==========================================
        // SectPosition (宗门阶级地位枚举)
        // 绝对的权力金字塔，下层必须无条件服从系统发起的宏观指令
        // ==========================================

        /** 散修 (无社会保障，极易成为高阶老怪掠夺寿命与真元的血食) */
        const val POS_ROGUE: Byte = 0

        /** 杂役弟子 (纯粹的劳动力，负责底层开荒与灵矿挖掘，死亡率极高) */
        const val POS_SERVANT: Byte = 1

        /** 外门弟子 (修仙界的底层基石，每600Tick强制投入血色秘境充当炮灰) */
        const val POS_OUTER: Byte = 2

        /** 内门弟子 (免去杂役，享有所在区块网格的灵气低保分配权) */
        const val POS_INNER: Byte = 3

        /** 核心/真传弟子 (天灵根或老祖后代，优先享受宗门全损耗炼制出的高阶丹药) */
        const val POS_CORE: Byte = 4

        /** 执事/长老 (结丹期以上，拥有调动宗门底层炮灰发起局部摩擦战的权限) */
        const val POS_ELDER: Byte = 5

        /** 掌门 (宗门世俗代理人，负责决定宗门的宏观外交状态流转) */
        const val POS_LEADER: Byte = 6

        /** 太上长老/老祖 (宗门的绝对核威慑。此 ID 一旦判定死亡，系统将向周围宗门广播灭门危机) */
        const val POS_PATRIARCH: Byte = 7
    }
}