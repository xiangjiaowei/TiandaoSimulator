package com.zheteng.tiandao.world.social

/**
 * 有向图因果关系边
 */
data class KarmaEdge(
    val sourceId: Int,      // 发起方实体 ID (如：寻仇的老祖)
    val targetId: Int,      // 目标方实体 ID (如：杀害徒弟的凶手)
    val relationType: Int,  // 关系类型
    var intensity: Int      // 羁绊/仇恨强度值
)

/**
 * 人际关系图谱管理器
 * 负责维护修仙界高阶大能之间的因果联系。
 * 底层采用高性能列表存储，仅对结丹期及以上修士开放。
 */
object KarmaGraph {

    // 存储所有的关系边
    private val edges = mutableListOf<KarmaEdge>()

    /**
     * 建立或强化因果关系
     */
    fun addRelation(sourceId: Int, targetId: Int, type: Int, intensity: Int) {
        // 先检查是否已有相同方向、相同类型的关系，若有则累加强度
        val existing = edges.find { it.sourceId == sourceId && it.targetId == targetId && it.relationType == type }
        if (existing != null) {
            existing.intensity += intensity
        } else {
            edges.add(KarmaEdge(sourceId, targetId, type, intensity))
        }
    }

    /**
     * 获取指定实体的所有对外因果关系 (例如：找出所有仇人)
     */
    fun getRelationsFrom(sourceId: Int): List<KarmaEdge> {
        return edges.filter { it.sourceId == sourceId }
    }

    /**
     * 获取指向该实体的所有关系 (例如：看看谁在惦记我)
     */
    fun getRelationsTo(targetId: Int): List<KarmaEdge> {
        return edges.filter { it.targetId == targetId }
    }

    /**
     * 斩断因果 (如：仇敌陨落或道侣坐化)
     */
    fun removeRelationsOf(entityId: Int) {
        // 删除所有作为发起方或接收方的边
        edges.removeIf { it.sourceId == entityId || it.targetId == entityId }
    }

    /**
     * 衰减全图仇恨值
     * 时间是治愈一切的良药，每个 Tick 仇恨值会自然下降，直到降为 0 后自动斩断因果。
     */
    fun decayAllRelations(decayRate: Int = 1) {
        val iterator = edges.iterator()
        while (iterator.hasNext()) {
            val edge = iterator.next()
            edge.intensity -= decayRate
            if (edge.intensity <= 0) {
                iterator.remove()
            }
        }
    }

    companion object {
        // ==========================================
        // RelationType (关系类型枚举)
        // ==========================================

        /** 道侣 (生死契约，一方遇袭，另一方概率降临支援) */
        const val RELATION_TAO_COMPANION: Int = 1

        /** 师徒 (护短机制，徒弟陨落自动向师父生成针对凶手的血仇边) */
        const val RELATION_MASTER_DISCIPLE: Int = 2

        /** 血仇 (神识寻仇的唯一触发条件，老祖级别修士会不惜代价追杀到底) */
        const val RELATION_BLOOD_FEUD: Int = 3

        /** 金兰/好友 */
        const val RELATION_BROTHERHOOD: Int = 4
    }
}