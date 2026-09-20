package com.fn.has.code.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fn.has.code.data.local.db.dao.FlowNetDao
import com.fn.has.code.data.local.db.entity.CaptiveLogEntity
import com.fn.has.code.data.local.db.entity.NetworkLogEntity

@Database(entities = [CaptiveLogEntity::class, NetworkLogEntity::class], version = 1, exportSchema = false)
abstract class FlowNetDatabase : RoomDatabase() {

    abstract fun flowNetDao(): FlowNetDao

    companion object {
        @Volatile
        private var INSTANCE: FlowNetDatabase? = null

        fun getDatabase(context: Context): FlowNetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlowNetDatabase::class.java,
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
