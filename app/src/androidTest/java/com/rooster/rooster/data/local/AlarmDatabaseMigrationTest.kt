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
 * Tests for database migrations
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
    
    @Test
    @Throws(IOException::class)
    fun migrate1To2_containsCorrectData() {
        // Create the database with version 1 schema
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert data into version 1
            execSQL(
                """
                INSERT INTO alarms (id, label, mode, relative1, relative2, time1, time2, 
                                   calculated_time, enabled, monday, tuesday, wednesday, 
                                   thursday, friday, saturday, sunday)
                VALUES (1, 'Test Alarm', 'At', 'Pick Time', 'Pick Time', 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0)
                """.trimIndent()
            )
            close()
        }
        
        // Re-open the database with version 2 and provide the migration
        helper.runMigrationsAndValidate(TEST_DB, 2, true, AlarmDatabase.MIGRATION_1_2).apply {
            // Verify that data is intact and new column exists with default value
            query("SELECT * FROM alarms WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                val ringtoneUriIndex = cursor.getColumnIndex("ringtoneUri")
                assertTrue("ringtoneUri column should exist", ringtoneUriIndex >= 0)
                
                val ringtoneUri = cursor.getString(ringtoneUriIndex)
                assertEquals("Default", ringtoneUri)
                
                val labelIndex = cursor.getColumnIndex("label")
                assertEquals("Test Alarm", cursor.getString(labelIndex))
            }
            close()
        }
    }
    
    @Test
    @Throws(IOException::class)
    fun testAllMigrations() {
        // Create the database with version 1
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }
        
        // Open the latest version and verify all migrations work
        val context = ApplicationProvider.getApplicationContext<Context>()
        Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            TEST_DB
        )
            .addMigrations(AlarmDatabase.MIGRATION_1_2)
            .build().apply {
                openHelper.writableDatabase.close()
            }
    }
}
