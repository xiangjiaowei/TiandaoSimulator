package com.zheteng.tiandao.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun SectTopologyView(
    // 预期接收从 ViewModel 传来的纯粹的节点和边的数据集合
    // nodes: List<SectNode>, edges: List<KarmaEdge>
) {
    // 纯渲染层，接收只读拓扑数据并执行连线渲染
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 占位示例绘制：绘制两个对抗宗门的拓扑连线
        val sectA = Offset(size.width * 0.3f, size.height * 0.5f)
        val sectB = Offset(size.width * 0.7f, size.height * 0.5f)

        // 绘制因果仇恨连线（如：血海深仇显示红色）
        drawLine(
            color = Color.Red,
            start = sectA,
            end = sectB,
            strokeWidth = 3f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // 绘制宗门节点
        drawCircle(color = Color.Blue, radius = 20f, center = sectA)
        drawCircle(color = Color.Magenta, radius = 30f, center = sectB)
    }
}