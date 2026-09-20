package com.fn.has.code.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fn.has.code.data.local.db.entity.BlockedDomainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDomainDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domainEntity: BlockedDomainEntity)

    @Delete
    suspend fun deleteDomain(domainEntity: BlockedDomainEntity)

    @Query("SELECT * FROM blocked_domains ORDER BY addedTimestamp DESC")
    fun getAllBlockedDomains(): Flow<List<BlockedDomainEntity>>

    @Query("SELECT * FROM blocked_domains WHERE isEnabled = 1")
    suspend fun getActiveBlockedDomains(): List<BlockedDomainEntity>

    @Query("DELETE FROM blocked_domains WHERE domain = :domain")
    suspend fun deleteByDomainName(domain: String)

    @Query("SELECT COUNT(*) FROM blocked_domains")
    suspend fun getBlockedDomainCount(): Long
}
