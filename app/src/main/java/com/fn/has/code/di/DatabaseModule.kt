package com.fn.has.code.di

import android.content.Context
import com.fn.has.code.data.local.db.FlowNetDatabase
import com.fn.has.code.data.local.db.dao.FlowNetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlowNetDatabase {
        return FlowNetDatabase.getDatabase(context)
    }

    @Provides
    fun provideFlowNetDao(database: FlowNetDatabase): FlowNetDao {
        return database.flowNetDao()
    }
}
