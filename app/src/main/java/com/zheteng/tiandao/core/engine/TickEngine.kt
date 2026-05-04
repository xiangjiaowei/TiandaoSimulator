package com.zheteng.tiandao.core.engine

import com.zheteng.tiandao.core.config.WorldConstants
import com.zheteng.tiandao.core.eventbus.EventBus
import com.zheteng.tiandao.core.eventbus.SystemEvents
import com.zheteng.tiandao.ecs.system.AgingSystem
import com.zheteng.tiandao.ecs.system.CombatResolutionSystem
import com.zheteng.tiandao.ecs.system.CultivationSystem
import com.zheteng.tiandao.ecs.system.FactionAndWarSystem
import com.zheteng.tiandao.ecs.system.MovementAndGridSystem

/**
 * 全局 Tick 时间步进推进引擎
 * 脱离 Android UI 线程的纯粹逻辑心脏。负责维持大世界的时间流转与 System 调度。
 */
object TickEngine {

    @Volatile
    var isRunning: Boolean = false
        private set

    /**
     * 玩家在天道面板控制的时间流速倍率 (如 1, 10, 100)
     */
    @Volatile
    var speedMultiplier: Int = 1

    /**
     * 世界当前经历的总 Tick 数（纪元时间戳）
     */
    @Volatile
    var currentTick: Long = 0L
        private set

    private var engineThread: Thread? = null
    private var lastTimeNano: Long = 0L
    private var tickAccumulator: Double = 0.0

    /**
     * 启动天道引擎
     * @param initialTick 读档时传入的初始世界时间
     */
    fun startEngine(initialTick: Long = 0L) {
        if (isRunning) return

        currentTick = initialTick
        isRunning = true
        lastTimeNano = System.nanoTime()
        tickAccumulator = 0.0

        engineThread = Thread {
            engineLoop()
        }.apply {
            name = "Tiandao-Tick-Logic-Thread"
            // 赋予后台推演线程极高的优先级，确保十万实体算力倾斜
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /**
     * 暂停或停止引擎 (如玩家呼出系统设置菜单或存档时)
     */
    fun stopEngine() {
        isRunning = false
        try {
            engineThread?.join(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            engineThread = null
        }
    }

    /**
     * 核心死循环引擎 (极度压榨 CPU 且线程安全)
     */
    private fun engineLoop() {
        while (isRunning) {
            val currentTimeNano = System.nanoTime()
            // 换算为毫秒的浮点精度增量
            val deltaTimeMs = (currentTimeNano - lastTimeNano) / 1_000_000.0
            lastTimeNano = currentTimeNano

            update(deltaTimeMs)

            // 防止在 X1 正常速度下 CPU 空转拉满 100%，让出极短暂的线程时间片
            try {
                Thread.sleep(5)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    /**
     * 累加器时间推演
     */
    private fun update(deltaTimeMs: Double) {
        // 核心加速逻辑：不改变物理规则，只放大时间增量
        tickAccumulator += deltaTimeMs * speedMultiplier

        val baseInterval = WorldConstants.BASE_TICK_INTERVAL_MS.toDouble()

        // 当累积的时间足够进行一次物理推演时，消耗时间池。
        // 若玩家开了 X100 倍速，此 while 循环会瞬间执行多次，确保不丢失任何一次判定。
        while (tickAccumulator >= baseInterval) {
            executeOneGlobalTick()
            tickAccumulator -= baseInterval
        }
    }

    /**
     * 执行绝对单次宏观世界推演 (1 Tick)
     * 系统调用必须严格遵循前后依赖优先级。
     */
    private fun executeOneGlobalTick() {
        // 1. 寿元结算：老死或受天罚抹杀的实体被销毁，优先级最高，死人无需进行后续推演
        AgingSystem.onTickUpdate()

        // 2. 闭关修为获取与境界突破：幸存下来的修士吸纳灵气，结算卡瓶颈雷劫
        CultivationSystem.onTickUpdate()

        // 3. 寻路与网格状态：判断灵气枯竭进行宏观迁徙
        MovementAndGridSystem.onTickUpdate()

        // 4. 宗门状态推演：升仙大会招收弟子的年份判定，或大宗门发动灭门战
        FactionAndWarSystem.onTickUpdate()

        // 5. 战斗碰撞核算：寻仇追踪的直线截杀与同网格杀人夺宝胜率结算
        CombatResolutionSystem.onTickUpdate()

        // 时间推进
        currentTick++

        // 彻底读写分离解耦：向外投递时间推演事件。
        // UI 渲染层 (MainViewModel) 收到此事件后，才去底层读取数值并渲染，绝不阻塞当前逻辑线程。
        EventBus.post(SystemEvents.TickAdvancedEvent(currentTick))
    }
}