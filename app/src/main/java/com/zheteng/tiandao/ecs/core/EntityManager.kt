package com.zheteng.tiandao.ecs.core

import com.zheteng.tiandao.ecs.component.*
import java.util.ArrayDeque

/**
 * ECS 核心调度中枢：实体管理器
 * 负责分发唯一 ID、管理组件生命周期，并维护高性能的活跃实体遍历队列。
 * 绝对禁止在此处使用高开销的集合类（如 HashMap），全盘采用连续数组以极致利用 CPU 缓存线。
 */
object EntityManager {

    // 初始容量预设为 10 万个并发生灵，避免运行时频繁扩容。
    private const val INITIAL_CAPACITY = 100_000
    private var currentCapacity = INITIAL_CAPACITY

    // ID 生成与对象池回收
    private var nextEntityId = 1
    private val recycledIds = ArrayDeque<Int>()

    // ==========================================
    // 连续内存块：组件存储数组 (索引直接对应 entityId)
    // ==========================================
    var baseInfos = arrayOfNulls<BaseInfoComponent>(currentCapacity)
    var cultivations = arrayOfNulls<CultivationComponent>(currentCapacity)
    var transforms = arrayOfNulls<TransformComponent>(currentCapacity)
    var combats = arrayOfNulls<CombatComponent>(currentCapacity)
    var factions = arrayOfNulls<FactionComponent>(currentCapacity)
    var inventories = arrayOfNulls<InventoryComponent>(currentCapacity)

    // ==========================================
    // 高性能迭代器支持 (供 System 每 Tick 遍历)
    // ==========================================
    /** 紧凑的活跃实体 ID 连续数组，System 遍历时的核心目标，防止 Cache Miss */
    var activeEntityIds = IntArray(currentCapacity)

    /** 记录当前活跃实体的总数 */
    var activeEntityCount = 0

    /** 稀疏数组：记录 entityId 在 activeEntityIds 数组中的位置，用于实现 O(1) 的删除 */
    private var entityToIndex = IntArray(currentCapacity) { -1 }

    /**
     * 创建一个全新的空白实体，并分配全局唯一的 ID。
     * @return 分配到的 entityId
     */
    fun createEntity(): Int {
        val id = if (recycledIds.isNotEmpty()) {
            recycledIds.removeFirst()
        } else {
            val newId = nextEntityId++
            ensureCapacity(newId)
            newId
        }

        // 将新 ID 注册到活跃紧凑数组中
        activeEntityIds[activeEntityCount] = id
        entityToIndex[id] = activeEntityCount
        activeEntityCount++

        return id
    }

    /**
     * 抹杀实体：彻底销毁该实体并回收 ID (触发对象置空，交由 GC 回收组件)
     * 使用尾部交换法 (Swap and Pop)，确保 O(1) 删除且维持 activeEntityIds 紧凑。
     */
    fun destroyEntity(entityId: Int) {
        val index = entityToIndex[entityId]
        if (index == -1) return // 已经被销毁或不存在

        // 1. O(1) 从活跃数组中移除：用最后一个元素覆盖当前位置
        val lastId = activeEntityIds[activeEntityCount - 1]
        activeEntityIds[index] = lastId
        entityToIndex[lastId] = index

        // 2. 清理自身映射
        entityToIndex[entityId] = -1
        activeEntityCount--

        // 3. 剥离所有组件引用，避免内存泄漏
        baseInfos[entityId] = null
        cultivations[entityId] = null
        transforms[entityId] = null
        combats[entityId] = null
        factions[entityId] = null
        inventories[entityId] = null

        // 4. 回收 ID 供后续复用，防止无限自增导致数组越界
        recycledIds.addLast(entityId)
    }

    /**
     * 扩容机制 (Double Capacity)
     * 当修士大爆发导致容量不足时，触发底层内存复制扩容。此操作开销较大，所以初始容量设置很高。
     */
    private fun ensureCapacity(requiredId: Int) {
        if (requiredId < currentCapacity) return

        val newCapacity = currentCapacity * 2

        baseInfos = baseInfos.copyOf(newCapacity)
        cultivations = cultivations.copyOf(newCapacity)
        transforms = transforms.copyOf(newCapacity)
        combats = combats.copyOf(newCapacity)
        factions = factions.copyOf(newCapacity)
        inventories = inventories.copyOf(newCapacity)

        activeEntityIds = activeEntityIds.copyOf(newCapacity)

        val newEntityToIndex = IntArray(newCapacity) { -1 }
        System.arraycopy(entityToIndex, 0, newEntityToIndex, 0, currentCapacity)
        entityToIndex = newEntityToIndex

        currentCapacity = newCapacity
    }
}