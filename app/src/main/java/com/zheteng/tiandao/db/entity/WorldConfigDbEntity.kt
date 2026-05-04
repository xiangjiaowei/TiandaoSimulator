package com.zheteng.tiandao.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_config")
data class WorldConfigDbEntity(
    @PrimaryKey val configId: Int = 1, // 固定为 1
    val currentTick: Long,
    val originEnergy: Long,
    val worldSeed: Long,
    val mofaEraTriggered: Boolean,
    val globalQiModifier: Float // 全局灵气倍率
)