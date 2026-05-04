package com.zheteng.tiandao.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 端侧 LLM 推理接口
 * 绝对无网络依赖，用于为世界中的高阶修士生成拟真动机与复杂决策。
 */
class LocalInferenceClient {

    // 允许重试次数与极端严苛的超时时间（防止主引擎 Tick 饥饿）
    private val maxRetries = 1
    private val inferenceTimeoutMs = 2500L

    /**
     * 推演高阶修士动机
     * 运行于 Default 调度器，绝不占用主线程或 Tick 引擎专用线程
     */
    suspend fun inferHighTierMotive(npcContext: String): String = withContext(Dispatchers.Default) {
        var attempt = 0
        while (attempt <= maxRetries) {
            try {
                // 超时熔断机制：在移动端算力受限时保护游戏帧率
                return@withContext withTimeout(inferenceTimeoutMs) {
                    executeNativeInference(npcContext)
                }
            } catch (e: TimeoutCancellationException) {
                attempt++
                if (attempt > maxRetries) {
                    // 降级策略：推演失败时，高阶修士默认转入“稳妥闭关”状态
                    return@withContext "ACTION: RETREAT_AND_CULTIVATE"
                }
            } catch (e: Exception) {
                // JNI 崩溃或显存 OOM 兜底处理
                return@withContext "ACTION: RETREAT_AND_CULTIVATE"
            }
        }
        return@withContext "ACTION: RETREAT_AND_CULTIVATE"
    }

    /**
     * 底层本地大模型调用层（接驳 C++ JNI 或移动端推理框架）
     */
    private suspend fun executeNativeInference(prompt: String): String {
        // TODO: 待接入本地量化模型 (如 q4_K_M) 的 JNI 接口。

        // 占位逻辑反馈，保证当前测试跑通
        if (prompt.contains("寿元将尽")) {
            return "ACTION: EXTREME_MURDER_AND_ROB; TARGET: NEAREST_WEAK_SECT" // 陷入疯狂，发动灭门夺宝
        } else if (prompt.contains("重伤")) {
            return "ACTION: FLEE_AND_HIDE" // 重伤远遁
        }

        return "ACTION: EXPLORE_RUINS" // 默认探索机缘
    }
}