package com.lumberyard.logscanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey val barcode: String,
    val length: Double,
    val diameter: Double,
    val volume: Double,
    val status: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
