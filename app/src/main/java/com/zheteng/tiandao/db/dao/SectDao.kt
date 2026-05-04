package com.zheteng.tiandao.db.dao

import androidx.room.*
import com.zheteng.tiandao.db.entity.SectDbEntity

@Dao
interface SectDao {
    @Query("SELECT * FROM sects WHERE isExtinct = 0")
    suspend fun getAllActiveSects(): List<SectDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSects(sects: List<SectDbEntity>)

    @Update
    suspend fun updateSect(sect: SectDbEntity)

    @Query("DELETE FROM sects")
    suspend fun clear()
}