package com.rooster.rooster.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Tests for all database migrations (v1 through v6).
 * Since exportSchema=false, we create at v1 and migrate incrementally.
 */
@RunWith(AndroidJUnit4::class)
class AlarmDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AlarmDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ==================== Migration 1 -> 2: Add ringtoneUri ====================

    @Test
    @Throws(IOException::class)
    fun migrate1To2_addsRingtoneUriWithDefault() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.execSQL(
            """
            INSERT INTO alarms (id, label, mode, relative1, relative2, time1, time2,
                               calculated_time, enabled, monday, tuesday, wednesday,
                               thursday, friday, saturday, sunday)
            VALUES (1, 'Test Alarm', 'At', 'Pick Time', 'Pick Time', 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0)
            """.trimIndent()
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, AlarmDatabase.MIGRATION_1_2)
        val cursor = migratedDb.query("SELECT * FROM alarms WHERE id = 1")
        assertTrue(cursor.moveToFirst())

        val ringtoneUriIndex = cursor.getColumnIndex("ringtoneUri")
        assertTrue("ringtoneUri column should exist", ringtoneUriIndex >= 0)
        assertEquals("Default", cursor.getString(ringtoneUriIndex))
        assertEquals("Test Alarm", cursor.getString(cursor.getColumnIndex("label")))

        cursor.close()
        migratedDb.close()
    }

    // ==================== Migration 1 -> 3: Add astronomy_data table ====================

    @Test
    @Throws(IOException::class)
    fun migrate1To3_createsAstronomyDataTable() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB, 3, true,
            AlarmDatabase.MIGRATION_1_2,
            AlarmDatabase.MIGRATION_2_3
        )

        // Verify astronomy_data table exists by inserting data
        migratedDb.execSQL(
            """
            INSERT INTO astronomy_data (id, latitude, longitude, sunrise, sunset, solarNoon,
                civilDawn, civilDusk, nauticalDawn, nauticalDusk, astroDawn, astroDusk,
                lastUpdated, dayLength)
            VALUES (1, 48.85, 2.35, 1000, 2000, 1500, 900, 2100, 800, 2200, 700, 2300, 9999, 1000)
            """.trimIndent()
        )

        val cursor = migratedDb.query("SELECT * FROM astronomy_data WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(48.85f, cursor.getFloat(cursor.getColumnIndex("latitude")), 0.01f)
        assertEquals(2.35f, cursor.getFloat(cursor.getColumnIndex("longitude")), 0.01f)
        assertEquals(1000L, cursor.getLong(cursor.getColumnIndex("sunrise")))
        assertEquals(2000L, cursor.getLong(cursor.getColumnIndex("sunset")))

        cursor.close()
        migratedDb.close()
    }

    // ==================== Migration 1 -> 4: Add alarm enhancement fields ====================

    @Test
    @Throws(IOException::class)
    fun migrate1To4_addsEnhancementColumnsWithDefaults() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.execSQL(
            """
            INSERT INTO alarms (id, label, mode, relative1, relative2, time1, time2,
                               calculated_time, enabled, monday, tuesday, wednesday,
                               thursday, friday, saturday, sunday)
            VALUES (1, 'Enhanced', 'At', 'Pick Time', 'Pick Time', 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0)
            """.trimIndent()
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB, 4, true,
            AlarmDatabase.MIGRATION_1_2,
            AlarmDatabase.MIGRATION_2_3,
            AlarmDatabase.MIGRATION_3_4
        )

        val cursor = migratedDb.query("SELECT * FROM alarms WHERE id = 1")
        assertTrue(cursor.moveToFirst())

        // New columns with defaults
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("vibrate")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("snooze_enabled")))
        assertEquals(10, cursor.getInt(cursor.getColumnIndex("snooze_duration")))
        assertEquals(3, cursor.getInt(cursor.getColumnIndex("snooze_count")))
        assertEquals(80, cursor.getInt(cursor.getColumnIndex("volume")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("gradual_volume")))
        // Existing data preserved
        assertEquals("Enhanced", cursor.getString(cursor.getColumnIndex("label")))

        cursor.close()
        migratedDb.close()
    }

    // ==================== Migration 1 -> 5: Recreate table with proper types ====================

    @Test
    @Throws(IOException::class)
    fun migrate1To5_preservesDataWithProperTypes() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.execSQL(
            """
            INSERT INTO alarms (id, label, mode, relative1, relative2, time1, time2,
                               calculated_time, enabled, monday, tuesday, wednesday,
                               thursday, friday, saturday, sunday)
            VALUES (1, 'Migrated', 'Before', 'Sunrise', 'Sunset', 100, 200, 150, 1,
                    1, 0, 1, 0, 1, 0, 1)
            """.trimIndent()
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB, 5, true,
            AlarmDatabase.MIGRATION_1_2,
            AlarmDatabase.MIGRATION_2_3,
            AlarmDatabase.MIGRATION_3_4,
            AlarmDatabase.MIGRATION_4_5
        )

        val cursor = migratedDb.query("SELECT * FROM alarms WHERE id = 1")
        assertTrue(cursor.moveToFirst())

        assertEquals("Migrated", cursor.getString(cursor.getColumnIndex("label")))
        assertEquals("Before", cursor.getString(cursor.getColumnIndex("mode")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("enabled")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("monday")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("tuesday")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("wednesday")))
        // v2 default
        assertEquals("Default", cursor.getString(cursor.getColumnIndex("ringtoneUri")))
        // v4 defaults via COALESCE
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("vibrate")))
        assertEquals(80, cursor.getInt(cursor.getColumnIndex("volume")))

        cursor.close()
        migratedDb.close()
    }

    // ==================== Migration 1 -> 6: Add location_data table ====================

    @Test
    @Throws(IOException::class)
    fun migrate1To6_createsLocationDataTable() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB, 6, true,
            AlarmDatabase.MIGRATION_1_2,
            AlarmDatabase.MIGRATION_2_3,
            AlarmDatabase.MIGRATION_3_4,
            AlarmDatabase.MIGRATION_4_5,
            AlarmDatabase.MIGRATION_5_6
        )

        migratedDb.execSQL(
            """
            INSERT INTO location_data (id, latitude, longitude, altitude, lastUpdated)
            VALUES (1, 48.8566, 2.3522, 35.0, 1234567890)
            """.trimIndent()
        )

        val cursor = migratedDb.query("SELECT * FROM location_data WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(48.8566f, cursor.getFloat(cursor.getColumnIndex("latitude")), 0.001f)
        assertEquals(2.3522f, cursor.getFloat(cursor.getColumnIndex("longitude")), 0.001f)
        assertEquals(35.0f, cursor.getFloat(cursor.getColumnIndex("altitude")), 0.1f)
        assertEquals(1234567890L, cursor.getLong(cursor.getColumnIndex("lastUpdated")))

        cursor.close()
        migratedDb.close()
    }

    // ==================== Full migration: v1 -> v6 with data ====================

    @Test
    @Throws(IOException::class)
    fun migrateAll_v1ToLatest_preservesAlarmData() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.execSQL(
            """
            INSERT INTO alarms (id, label, mode, relative1, relative2, time1, time2,
                               calculated_time, enabled, monday, tuesday, wednesday,
                               thursday, friday, saturday, sunday)
            VALUES (1, 'Full Migration', 'At', 'Sunrise', 'Pick Time', 100, 0, 100, 1,
                    1, 1, 1, 1, 1, 0, 0)
            """.trimIndent()
        )
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB, 6, true,
            AlarmDatabase.MIGRATION_1_2,
            AlarmDatabase.MIGRATION_2_3,
            AlarmDatabase.MIGRATION_3_4,
            AlarmDatabase.MIGRATION_4_5,
            AlarmDatabase.MIGRATION_5_6
        )

        // Verify alarm data survived all migrations
        val cursor = migratedDb.query("SELECT * FROM alarms WHERE id = 1")
        assertTrue("Alarm should exist after full migration", cursor.moveToFirst())
        assertEquals("Full Migration", cursor.getString(cursor.getColumnIndex("label")))
        assertEquals("At", cursor.getString(cursor.getColumnIndex("mode")))
        assertEquals("Sunrise", cursor.getString(cursor.getColumnIndex("relative1")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("enabled")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("monday")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("sunday")))
        assertEquals("Default", cursor.getString(cursor.getColumnIndex("ringtoneUri")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("vibrate")))
        assertEquals(80, cursor.getInt(cursor.getColumnIndex("volume")))
        cursor.close()

        // Verify astronomy_data table exists
        val astCursor = migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='astronomy_data'")
        assertTrue("astronomy_data table should exist", astCursor.moveToFirst())
        astCursor.close()

        // Verify location_data table exists
        val locCursor = migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='location_data'")
        assertTrue("location_data table should exist", locCursor.moveToFirst())
        locCursor.close()

        migratedDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll_v1ToLatest_canOpenWithRoom() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.close()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val roomDb = Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                AlarmDatabase.MIGRATION_1_2,
                AlarmDatabase.MIGRATION_2_3,
                AlarmDatabase.MIGRATION_3_4,
                AlarmDatabase.MIGRATION_4_5,
                AlarmDatabase.MIGRATION_5_6
            )
            .build()
        roomDb.openHelper.writableDatabase.close()
    }
}
