package com.zheteng.tiandao.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "low_tier_chunks")
data class LowTierChunkDbEntity(
    @PrimaryKey val chunkId: Int,
    val dataBlob: ByteArray // 存储序列化后的炮灰数据
)