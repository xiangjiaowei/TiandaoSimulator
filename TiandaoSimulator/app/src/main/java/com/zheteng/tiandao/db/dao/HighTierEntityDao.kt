package com.zheteng.tiandao.db.dao

import androidx.room.*
import com.zheteng.tiandao.db.entity.HighTierDbEntity

@Dao
interface HighTierEntityDao {
    @Query("SELECT * FROM high_tier_entities")
    suspend fun getAll(): List<HighTierDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HighTierDbEntity>)

    @Query("DELETE FROM high_tier_entities")
    suspend fun clear()

    @Transaction
    suspend fun refreshTable(entities: List<HighTierDbEntity>) {
        clear()
        insertAll(entities)
    }
}