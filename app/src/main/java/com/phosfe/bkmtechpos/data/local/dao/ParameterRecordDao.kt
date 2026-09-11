package com.phosfe.bkmtechpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.phosfe.bkmtechpos.data.local.entity.ParameterRecordEntity

@Dao
abstract class ParameterRecordDao {
    @Query("SELECT * FROM parameter_table ORDER BY type")
    abstract fun all(): List<ParameterRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsert(record: ParameterRecordEntity)

    @Query("DELETE FROM parameter_table WHERE type = :type")
    abstract fun delete(type: Int)

    @Transaction
    open fun activate(records: List<ParameterRecordEntity>, deletedTypes: List<Int>) {
        deletedTypes.forEach(::delete)
        records.forEach(::upsert)
    }
}
