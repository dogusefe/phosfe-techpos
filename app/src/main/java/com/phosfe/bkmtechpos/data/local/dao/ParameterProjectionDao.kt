package com.phosfe.bkmtechpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phosfe.bkmtechpos.data.local.entity.ParameterProjectionEntity

@Dao
interface ParameterProjectionDao {
    @Query("SELECT * FROM parameter_projection ORDER BY type")
    fun all(): List<ParameterProjectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(record: ParameterProjectionEntity)

    @Query("DELETE FROM parameter_projection WHERE type = :type")
    fun delete(type: Int)
}
