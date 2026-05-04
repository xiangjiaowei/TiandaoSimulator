package com.zheteng.tiandao.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zheteng.tiandao.db.dao.*
import com.zheteng.tiandao.db.entity.*
import com.zheteng.tiandao.db.converter.JsonTypeConverters

@Database(
    entities = [
        SectDbEntity::class,
        HighTierDbEntity::class,
        LowTierChunkDbEntity::class,
        WorldConfigDbEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(JsonTypeConverters::class)
abstract class TiandaoDatabase : RoomDatabase() {
    abstract fun sectDao(): SectDao
    abstract fun highTierEntityDao(): HighTierEntityDao
    abstract fun lowTierChunkDao(): LowTierChunkDao
    abstract fun worldConfigDao(): WorldConfigDao
}