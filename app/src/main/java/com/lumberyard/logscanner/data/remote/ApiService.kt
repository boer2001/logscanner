package com.lumberyard.logscanner.data.remote

import com.lumberyard.logscanner.data.local.LogEntity
import com.lumberyard.logscanner.data.local.ScanEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("logs")
    suspend fun getLogs(@Query("last_updated") lastUpdated: Long): Response<List<LogEntity>>

    @POST("scans")
    suspend fun uploadScans(@Body scans: List<ScanEntity>): Response<SyncResponse>
}

data class SyncResponse(
    val success: Boolean,
    val message: String,
    val syncedIds: List<Int>
)
