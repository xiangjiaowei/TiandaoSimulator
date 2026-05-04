package com.zheteng.tiandao.db.dao

import androidx.room.*
import com.zheteng.tiandao.db.entity.WorldConfigDbEntity

@Dao
interface WorldConfigDao {
    @Query("SELECT * FROM world_config WHERE configId = 1")
    suspend fun getConfig(): WorldConfigDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: WorldConfigDbEntity)
}