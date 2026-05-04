package com.zheteng.tiandao.ui.panels

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zheteng.tiandao.llm.LocalInferenceClient

/**
 * 世界大事记渲染面板 (流式文本)
 */
class WorldLogPanel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val recyclerView = RecyclerView(context)
    private val logAdapter = LogAdapter() // 内部适配器

    init {
        addView(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true // 日志从底部向上滚动
        }
        recyclerView.adapter = logAdapter
    }

    /**
     * 更新日志列表，并触发 LLM 剧情修饰
     */
    fun updateLogs(newLogs: List<String>) {
        if (newLogs.isEmpty()) return

        val latestRaw = newLogs.first()
        // 调用本地 LLM 客户端进行文学化包装
        LocalInferenceClient.fetchStoryline(latestRaw) { stylizedText ->
            // 在主线程更新 UI
            recyclerView.post {
                logAdapter.addLog(stylizedText)
                recyclerView.smoothScrollToPosition(logAdapter.itemCount - 1)
            }
        }
    }

    private class LogAdapter : RecyclerView.Adapter<LogViewHolder>() {
        private val items = mutableListOf<String>()

        fun addLog(log: String) {
            items.add(log)
            if (items.size > 50) items.removeAt(0)
            notifyDataSetChanged()
        }
        // standard ViewHolder pattern...
        override fun onCreateViewHolder(p0: android.view.ViewGroup, p1: Int): LogViewHolder = TODO()
        override fun onBindViewHolder(p0: LogViewHolder, p1: Int) = TODO()
        override fun getItemCount(): Int = items.size
    }
    private class LogViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view)
}