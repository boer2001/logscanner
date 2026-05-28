package com.lumberyard.logscanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE barcode = :barcode")
    suspend fun getLogByBarcode(barcode: String): LogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntity>)

    @Query("DELETE FROM logs")
    suspend fun deleteAllLogs()

    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getLogCount(): Int
}
