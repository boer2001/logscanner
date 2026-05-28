package com.lumberyard.logscanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY scanTime DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE synced = 0")
    suspend fun getUnsyncedScans(): List<ScanEntity>

    @Insert
    suspend fun insertScan(scan: ScanEntity)

    @Query("UPDATE scans SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)

    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()
}
