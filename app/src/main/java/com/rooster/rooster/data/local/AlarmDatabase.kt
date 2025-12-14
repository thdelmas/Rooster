package com.rooster.rooster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rooster.rooster.data.local.dao.AlarmDao
import com.rooster.rooster.data.local.dao.AstronomyDao
import com.rooster.rooster.data.local.dao.LocationDao
import com.rooster.rooster.data.local.entity.AlarmEntity
import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import com.rooster.rooster.data.local.entity.LocationEntity

@Database(
    entities = [AlarmEntity::class, AstronomyDataEntity::class, LocationEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun astronomyDao(): AstronomyDao
    abstract fun locationDao(): LocationDao
    
    companion object {
        const val DATABASE_NAME = "alarm_db"
        
        /**
         * Migration from version 1 to 2: Add ringtoneUri column
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarms ADD COLUMN ringtoneUri TEXT NOT NULL DEFAULT 'Default'")
            }
        }
        
        /**
         * Migration from version 2 to 3: Add astronomy_data table
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS astronomy_data (
                        id INTEGER PRIMARY KEY NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        sunrise INTEGER NOT NULL,
                        sunset INTEGER NOT NULL,
                        solarNoon INTEGER NOT NULL,
                        civilDawn INTEGER NOT NULL,
                        civilDusk INTEGER NOT NULL,
                        nauticalDawn INTEGER NOT NULL,
                        nauticalDusk INTEGER NOT NULL,
                        astroDawn INTEGER NOT NULL,
                        astroDusk INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        dayLength INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        /**
         * Migration from version 3 to 4: Add alarm enhancement fields
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarms ADD COLUMN vibrate INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE alarms ADD COLUMN snooze_enabled INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE alarms ADD COLUMN snooze_duration INTEGER NOT NULL DEFAULT 10")
                database.execSQL("ALTER TABLE alarms ADD COLUMN snooze_count INTEGER NOT NULL DEFAULT 3")
                database.execSQL("ALTER TABLE alarms ADD COLUMN volume INTEGER NOT NULL DEFAULT 80")
                database.execSQL("ALTER TABLE alarms ADD COLUMN gradual_volume INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        /**
         * Migration from version 4 to 5: Convert BOOLEAN types to INTEGER and ensure proper nullability
         * Changes: Recreate table with proper Room-compatible schema
         * - Convert BOOLEAN to INTEGER type
         * - Ensure all required fields are NOT NULL
         * - Add default values for enhancement fields
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with proper Room schema (INTEGER for boolean, proper nullability)
                database.execSQL("""
                    CREATE TABLE alarms_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        mode TEXT NOT NULL,
                        ringtoneUri TEXT NOT NULL,
                        relative1 TEXT NOT NULL,
                        relative2 TEXT NOT NULL,
                        time1 INTEGER NOT NULL,
                        time2 INTEGER NOT NULL,
                        calculated_time INTEGER NOT NULL,
                        monday INTEGER NOT NULL,
                        tuesday INTEGER NOT NULL,
                        wednesday INTEGER NOT NULL,
                        thursday INTEGER NOT NULL,
                        friday INTEGER NOT NULL,
                        saturday INTEGER NOT NULL,
                        sunday INTEGER NOT NULL,
                        vibrate INTEGER NOT NULL DEFAULT 1,
                        snooze_enabled INTEGER NOT NULL DEFAULT 1,
                        snooze_duration INTEGER NOT NULL DEFAULT 10,
                        snooze_count INTEGER NOT NULL DEFAULT 3,
                        volume INTEGER NOT NULL DEFAULT 80,
                        gradual_volume INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Copy data from old table to new table, using COALESCE for null values
                // Normalize boolean values to ensure they're strictly 0 or 1
                database.execSQL("""
                    INSERT INTO alarms_new (
                        id, label, enabled, mode, ringtoneUri, relative1, relative2,
                        time1, time2, calculated_time, monday, tuesday, wednesday,
                        thursday, friday, saturday, sunday, vibrate, snooze_enabled,
                        snooze_duration, snooze_count, volume, gradual_volume
                    )
                    SELECT 
                        id, label, 
                        CASE WHEN CAST(enabled AS INTEGER) = 0 THEN 0 ELSE 1 END, 
                        mode, ringtoneUri, relative1, relative2,
                        time1, time2, calculated_time, 
                        CASE WHEN CAST(monday AS INTEGER) = 0 THEN 0 ELSE 1 END, 
                        CASE WHEN CAST(tuesday AS INTEGER) = 0 THEN 0 ELSE 1 END, 
                        CASE WHEN CAST(wednesday AS INTEGER) = 0 THEN 0 ELSE 1 END,
                        CASE WHEN CAST(thursday AS INTEGER) = 0 THEN 0 ELSE 1 END, 
                        CASE WHEN CAST(friday AS INTEGER) = 0 THEN 0 ELSE 1 END, 
                        CASE WHEN CAST(saturday AS INTEGER) = 0 THEN 0 ELSE 1 END, 
                        CASE WHEN CAST(sunday AS INTEGER) = 0 THEN 0 ELSE 1 END, 
                        CASE WHEN COALESCE(CAST(vibrate AS INTEGER), 1) = 0 THEN 0 ELSE 1 END, 
                        CASE WHEN COALESCE(CAST(snooze_enabled AS INTEGER), 1) = 0 THEN 0 ELSE 1 END,
                        COALESCE(snooze_duration, 10), 
                        COALESCE(snooze_count, 3), 
                        COALESCE(volume, 80), 
                        CASE WHEN COALESCE(CAST(gradual_volume AS INTEGER), 0) = 0 THEN 0 ELSE 1 END
                    FROM alarms
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE alarms")
                
                // Rename new table to alarms
                database.execSQL("ALTER TABLE alarms_new RENAME TO alarms")
            }
        }
        
        /**
         * Migration from version 5 to 6: Add location_data table
         * Moves location storage from SharedPreferences to Room database
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS location_data (
                        id INTEGER PRIMARY KEY NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        altitude REAL NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
