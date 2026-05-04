package com.zheteng.tiandao.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "high_tier_entities",
    indices = [Index(value = ["sectId"]), Index(value = ["majorLevel"])]
)
data class HighTierDbEntity(
    @PrimaryKey val entityId: Int,
    val name: String,
    val age: Int,
    val maxLifespan: Int,
    val linggenType: Byte,
    val majorLevel: Byte,
    val minorLevel: Byte,
    val currentExp: Long,
    val gridX: Short,
    val gridY: Short,
    val hp: Int,
    val maxHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val boundQi: Long,
    val sectId: Int,
    val position: Byte, // 职位
    val actionState: Int // 离线前的行动状态
)