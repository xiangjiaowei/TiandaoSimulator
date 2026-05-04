package com.zheteng.tiandao.db.dao

import androidx.room.*
import com.zheteng.tiandao.db.entity.LowTierChunkDbEntity

@Dao
interface LowTierChunkDao {
    @Query("SELECT * FROM low_tier_chunks")
    suspend fun getAllChunks(): List<LowTierChunkDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<LowTierChunkDbEntity>)

    @Query("DELETE FROM low_tier_chunks")
    suspend fun clear()
}