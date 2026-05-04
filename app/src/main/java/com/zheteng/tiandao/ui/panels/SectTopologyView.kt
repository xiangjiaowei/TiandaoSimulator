package com.zheteng.tiandao.ui.panels

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.zheteng.tiandao.world.grid.GridManager
import com.zheteng.tiandao.ecs.core.EntityManager

/**
 * 宗门势力与关系网谱可视化视图
 */
class SectTopologyView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 1f }
    private val sectPaint = Paint().apply { style = Paint.Style.FILL; alpha = 120 }
    private val entityPaint = Paint().apply { color = Color.CYAN; style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellW = width.toFloat() / GridManager.width
        val cellH = height.toFloat() / GridManager.height

        // 1. 绘制网格与势力底色
        for (y in 0 until GridManager.height) {
            for (x in 0 until GridManager.width) {
                val cell = GridManager.getCell(x.toShort(), y.toShort()) ?: continue
                if (cell.sectOwnerId != 0) {
                    // 根据 sectId 映射颜色（简单 Hash）
                    sectPaint.color = (cell.sectOwnerId * 0xFFFFFF).toInt() or 0xFF000000.toInt()
                    canvas.drawRect(x * cellW, y * cellH, (x + 1) * cellW, (y + 1) * cellH, sectPaint)
                }
            }
        }

        // 2. 绘制高阶修士热点 (结丹以上才可见)
        val entityIds = EntityManager.activeEntityIds
        val count = EntityManager.activeEntityCount
        for (i in 0 until count) {
            val id = entityIds[i]
            val cult = EntityManager.cultivations[id] ?: continue
            if (cult.majorLevel >= 3) { // 结丹期及以上
                val trans = EntityManager.transforms[id] ?: continue
                canvas.drawCircle(
                    trans.gridX * cellW + cellW / 2,
                    trans.gridY * cellH + cellH / 2,
                    3f,
                    entityPaint
                )
            }
        }

        // 每一帧自动重绘，实现实时动态效果
        invalidate()
    }
}