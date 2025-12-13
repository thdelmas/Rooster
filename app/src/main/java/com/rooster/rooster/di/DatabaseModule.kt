package com.rooster.rooster.di

import android.content.Context
import androidx.room.Room
import com.rooster.rooster.data.local.AlarmDatabase
import com.rooster.rooster.data.local.dao.AlarmDao
import com.rooster.rooster.data.local.dao.AstronomyDao
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
    fun provideAlarmDatabase(
        @ApplicationContext context: Context
    ): AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            AlarmDatabase.DATABASE_NAME
        )
            .addMigrations(
                AlarmDatabase.MIGRATION_1_2,
                AlarmDatabase.MIGRATION_2_3,
                AlarmDatabase.MIGRATION_3_4,
                AlarmDatabase.MIGRATION_4_5
            )
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }
    
    @Provides
    @Singleton
    fun provideAstronomyDao(database: AlarmDatabase): AstronomyDao {
        return database.astronomyDao()
    }
}
