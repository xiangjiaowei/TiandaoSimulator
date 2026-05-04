package com.zheteng.tiandao.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sects")
data class SectDbEntity(
    @PrimaryKey val sectId: Int,
    val name: String,
    var rank: Int,
    val baseX: Short,
    val baseY: Short,
    var foundationQi: Long,
    var reputation: Int,
    val alignment: Int, // 0:中立, 1:正道, 2:魔道
    var isExtinct: Boolean = false
)