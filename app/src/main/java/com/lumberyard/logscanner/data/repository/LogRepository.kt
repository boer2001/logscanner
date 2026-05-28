package com.lumberyard.logscanner.data.repository

import com.lumberyard.logscanner.data.local.LogDao
import com.lumberyard.logscanner.data.local.LogEntity
import com.lumberyard.logscanner.data.local.ScanDao
import com.lumberyard.logscanner.data.local.ScanEntity
import com.lumberyard.logscanner.data.remote.ApiService
import kotlinx.coroutines.flow.Flow

class LogRepository(
    private val logDao: LogDao,
    private val scanDao: ScanDao,
    private val apiService: ApiService
) {
    val allLogs: Flow<List<LogEntity>> = logDao.getAllLogs()
    val allScans: Flow<List<ScanEntity>> = scanDao.getAllScans()

    suspend fun getLogByBarcode(barcode: String): LogEntity? {
        return logDao.getLogByBarcode(barcode)
    }

    suspend fun insertScan(scan: ScanEntity) {
        scanDao.insertScan(scan)
    }

    suspend fun syncData() {
        // 1. Upload unsynced scans
        val unsynced = scanDao.getUnsyncedScans()
        if (unsynced.isNotEmpty()) {
            val response = apiService.uploadScans(unsynced)
            if (response.isSuccessful && response.body()?.success == true) {
                scanDao.markAsSynced(response.body()!!.syncedIds)
            }
        }

        // 2. Download updated logs
        val lastUpdated = 0L // Should track this properly
        val logResponse = apiService.getLogs(lastUpdated)
        if (logResponse.isSuccessful) {
            logResponse.body()?.let { logs ->
                logDao.insertLogs(logs)
            }
        }
    }
}
