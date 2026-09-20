package com.fn.has.code.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fn.has.code.data.local.db.entity.CaptiveLogEntity
import com.fn.has.code.data.local.db.entity.NetworkLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowNetDao {

    @Query("SELECT * FROM captive_logs ORDER BY timestamp DESC")
    fun getAllCaptiveLogs(): Flow<List<CaptiveLogEntity>>

    @Query("SELECT * FROM captive_logs ORDER BY timestamp DESC")
    fun observeCaptiveLogs(): Flow<List<CaptiveLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaptiveLog(log: CaptiveLogEntity)

    @Query("DELETE FROM captive_logs")
    suspend fun clearCaptiveLogs()

    @Query("SELECT * FROM network_logs ORDER BY timestamp DESC")
    fun getAllNetworkLogs(): Flow<List<NetworkLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworkLog(log: NetworkLogEntity)

    @Query("DELETE FROM network_logs")
    suspend fun clearNetworkLogs()
}
