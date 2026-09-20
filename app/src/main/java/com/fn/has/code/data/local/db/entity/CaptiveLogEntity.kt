package com.fn.has.code.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captive_logs")
data class CaptiveLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val targetHost: String,
    val redirectUrl: String,
    val clientIp: String,
    val timestamp: Long = System.currentTimeMillis()
)
