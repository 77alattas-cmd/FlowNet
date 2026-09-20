package com.fn.has.code.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_logs")
data class NetworkLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val type: String, // "INFO", "SUCCESS", "WARN", "ERROR"
    val timestamp: Long = System.currentTimeMillis()
)
