package com.lumberyard.logscanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val scannedBy: String,
    val scanTime: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val truckId: String? = null
)
