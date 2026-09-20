package com.fn.has.code.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_domains")
data class BlockedDomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val category: String = "CUSTOM",
    val isEnabled: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)
