package com.zheteng.tiandao.ui.panels

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.zheteng.tiandao.databinding.PanelGodControlBinding
import com.zheteng.tiandao.heavenlydao.GodModeApi
import com.zheteng.tiandao.ecs.component.BaseInfoComponent

/**
 * 天道干预滑块与指令 UI 面板
 */
class GodControlPanel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = PanelGodControlBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 1. 降下祥瑞：大幅提升局部灵气
        binding.btnBlessing.setOnClickListener {
            // 此处坐标可通过 SectTopologyView 的选中状态传递，此处暂定示例坐标
            GodModeApi.alterRegionalQi(50, 50, 5, 2.0f)
        }

        // 2. 拨弄命运：将随机实体提升为天灵根
        binding.btnModifyFate.setOnClickListener {
            // 消耗本源能量，逆天改命
            GodModeApi.modifyEntityLinggen(100, BaseInfoComponent.LINGGEN_TIAN)
        }

        // 3. 降下雷罚：强行抹杀当前选中的“异端”
        binding.btnThunderSmite.setOnClickListener {
            GodModeApi.smiteEntity(100)
        }

        // 4. 法则锁定：限制妖族突破上限
        binding.btnLockRace.setOnClickListener {
            GodModeApi.lockRaceProgression(BaseInfoComponent.RACE_MONSTER, 2)
        }
    }
}