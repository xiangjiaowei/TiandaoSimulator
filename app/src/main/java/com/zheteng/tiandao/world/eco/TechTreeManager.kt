package com.zheteng.tiandao.world.eco

import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.component.BaseInfoComponent
import com.zheteng.tiandao.ecs.component.CultivationComponent
import com.zheteng.tiandao.ecs.core.EntityManager
import kotlin.math.max
import kotlin.random.Random

/**
 * 功法定义信息
 */
data class KnowledgeInfo(
    val id: Int,
    val name: String,
    val difficulty: Int,          // 推演难度基数 (1000 ~ 100000)
    var holderCount: Int = 0,     // 全图当前掌握此知识的活体 NPC 数量
    var isLost: Boolean = false   // 是否已失传
)

/**
 * 修仙百艺与功法传承管理器
 * 负责全局功法的推演进度监控、传承链条维护以及断代遗失判定。
 */
object TechTreeManager {

    /**
     * 全局知识注册表 (KnowledgeID -> Info)
     */
    private val knowledgeRegistry = mutableMapOf<Int, KnowledgeInfo>()

    /**
     * NPC 个人推演进度表 (EntityID -> (KnowledgeID -> Progress))
     * 为了性能，此处仅记录正在闭关推演高价值功法的高阶修士。
     */
    private val deductionProgressMap = mutableMapOf<Int, MutableMap<Int, Float>>()

    init {
        // 初始化修仙界基础百艺（这些是常识，基本不会失传）
        registerKnowledge(KnowledgeInfo(KNOWLEDGE_BASIC_ALCHEMY, "基础炼丹术", 1000, holderCount = 99999))
        registerKnowledge(KnowledgeInfo(KNOWLEDGE_BASIC_ARRAY, "基础阵法推演", 1000, holderCount = 99999))

        // 初始化稀有功法（开局可能只有极少数大能掌握）
        registerKnowledge(KnowledgeInfo(KNOWLEDGE_PILL_FOUNDATION, "筑基丹方", 5000, holderCount = 100))
        registerKnowledge(KnowledgeInfo(KNOWLEDGE_ULTIMATE_SWORD, "青元剑诀", 50000, holderCount = 1))
    }

    private fun registerKnowledge(info: KnowledgeInfo) {
        knowledgeRegistry[info.id] = info
    }

    /**
     * 每 Tick 驱动高悟性修士进行自主推演
     */
    fun onTickUpdate() {
        val activeIds = EntityManager.activeEntityIds
        val count = EntityManager.activeEntityCount
        val baseInfos = EntityManager.baseInfos
        val cultivations = EntityManager.cultivations

        for (i in 0 until count) {
            val entityId = activeIds[i]
            val base = baseInfos[entityId] ?: continue
            val cult = cultivations[entityId] ?: continue

            // 只有人族（悟性高）或高境界修士（元婴及以上）有概率开启自主推演
            if (base.raceType == BaseInfoComponent.RACE_HUMAN || cult.majorLevel >= CultivationComponent.REALM_YUANYING) {
                processIndividualDeduction(entityId, base, cult)
            }
        }
    }

    /**
     * 处理单体推演进度累加
     */
    private fun processIndividualDeduction(entityId: Int, base: BaseInfoComponent, cult: CultivationComponent) {
        // 模拟修士尝试推演“筑基丹方”
        val targetKnowledgeId = KNOWLEDGE_PILL_FOUNDATION
        val info = knowledgeRegistry[targetKnowledgeId] ?: return

        // 若该修士已掌握，则无需推演
        // (此处逻辑应配合 InventoryComponent 的已学功法列表，此处暂简化)

        val progressMap = deductionProgressMap.getOrPut(entityId) { mutableMapOf() }
        val currentProgress = progressMap.getOrDefault(targetKnowledgeId, 0f)

        // 推演公式：Progress = (Intelligence * Realm) / Difficulty * Inspiration
        // Intelligence 取灵根品质补正
        val intelligence = when(base.linggenType) {
            BaseInfoComponent.LINGGEN_TIAN -> 10.0f
            BaseInfoComponent.LINGGEN_DI -> 5.0f
            else -> 1.0f
        }

        val epsilon = Random.nextFloat() * 0.7f + 0.8f // 灵感波动因子 (0.8 ~ 1.5)
        val delta = (intelligence * cult.majorLevel) / info.difficulty.toFloat() * epsilon

        val newProgress = currentProgress + delta
        if (newProgress >= 1.0f) {
            // 推演成功！掌握该项知识
            info.holderCount++
            progressMap.remove(targetKnowledgeId)

            if (info.isLost) {
                info.isLost = false
                EventBus.post(SystemEvents.WorldLogEvent("【文明火种】失传已久的《${info.name}》竟被一名奇才闭关推演成功，重现世间！", SystemEvents.LogLevel.HEAVENLY))
            }
        } else {
            progressMap[targetKnowledgeId] = newProgress
        }
    }

    /**
     * 传承断代检定：当一名掌握知识的修士陨落时调用
     */
    fun notifyEntityDeath(entityId: Int, learnedKnowledgeIds: List<Int>, isSuddenDeath: Boolean) {
        for (kId in learnedKnowledgeIds) {
            val info = knowledgeRegistry[kId] ?: continue
            info.holderCount = max(0, info.holderCount - 1)

            // 如果这是全图最后一个掌握该知识的人
            if (info.holderCount <= 0) {
                // 判定是否掉落玉简：只有非突发死亡（如坐化）或 10% 概率触发
                val jadeSlipDropped = !isSuddenDeath || Random.nextFloat() < 0.1f

                if (!jadeSlipDropped) {
                    info.isLost = true
                    EventBus.post(SystemEvents.WorldLogEvent("【道统断绝】随着最后一位掌握者的陨落，《${info.name}》彻底消失在历史长河中...", SystemEvents.LogLevel.DANGER))
                }
            }
        }
        deductionProgressMap.remove(entityId)
    }

    fun getKnowledgeInfo(id: Int): KnowledgeInfo? = knowledgeRegistry[id]

    companion object {
        const val KNOWLEDGE_BASIC_ALCHEMY = 101
        const val KNOWLEDGE_BASIC_ARRAY   = 102
        const val KNOWLEDGE_PILL_FOUNDATION = 201
        const val KNOWLEDGE_ULTIMATE_SWORD  = 301
    }
}