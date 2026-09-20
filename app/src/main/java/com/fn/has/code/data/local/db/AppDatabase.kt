package com.fn.has.code.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fn.has.code.data.local.db.dao.BlockedDomainDao
import com.fn.has.code.data.local.db.dao.FlowNetDao
import com.fn.has.code.data.local.db.entity.BlockedDomainEntity
import com.fn.has.code.data.local.db.entity.CaptiveLogEntity
import com.fn.has.code.data.local.db.entity.NetworkLogEntity

@Database(entities = [BlockedDomainEntity::class, CaptiveLogEntity::class, NetworkLogEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun flowNetDao(): FlowNetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flownet_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
