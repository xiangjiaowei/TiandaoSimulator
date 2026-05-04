package com.zheteng.tiandao.llm

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * 本地大模型通信客户端
 * 负责将冷冰冰的 ECS 数据转化为具有“仙侠感”的文本描述。
 */
object LocalInferenceClient {

    private val client = OkHttpClient()
    private const val LOCAL_URL = "http://127.0.0.1:11434/api/generate" // 以 Ollama 为例

    /**
     * 将原始事件转化为剧情描述
     * @param rawEvent 格式如: "修士A(结丹期) 在 青石镇 杀害了 修士B(筑基期)"
     * @param callback 回调生成的文本
     */
    fun fetchStoryline(rawEvent: String, onResult: (String) -> Unit) {
        val prompt = """
            你是一个资深的仙侠小说作家。
            请将以下修仙界发生的数值事件，转化为一段充满意境、简短有力的文字描述（50字以内）。
            事件：$rawEvent
            描述：
        """.trimIndent()

        val json = JSONObject().apply {
            put("model", "qwen2.5:7b") // 假设本地运行的是 Qwen 2.5
            put("prompt", prompt)
            put("stream", false)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(LOCAL_URL).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult("【天机模糊】本地推理引擎未响应：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val responseJson = JSONObject(it.string())
                    val text = responseJson.optString("response")
                    onResult(text.trim())
                }
            }
        })
    }
}