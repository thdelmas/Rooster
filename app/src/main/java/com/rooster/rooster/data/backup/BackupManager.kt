package com.rooster.rooster.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.AlarmCreation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for backup and restore operations
 */
@Singleton
class BackupManager @Inject constructor(
    private val alarmRepository: AlarmRepository
) {
    
    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_VERSION = 2
        private const val AUTO_BACKUP_KEY = "auto_backup"
        private const val LAST_BACKUP_KEY = "last_backup_timestamp"
    }
    
    /**
     * Export all alarms to a JSON file
     */
    suspend fun exportToFile(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val alarms = alarmRepository.getAllAlarms()
            val backupData = createBackupJson(alarms)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(backupData)
                }
            }
            
            Log.i(TAG, "Backup completed: ${alarms.size} alarms exported")
            Result.success("Successfully exported ${alarms.size} alarm(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting alarms", e)
            Result.failure(Exception("Failed to export alarms: ${e.message}"))
        }
    }
    
    /**
     * Import alarms from a JSON file
     */
    suspend fun importFromFile(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Failed to read file"))
            
            val importedCount = parseAndImportBackup(jsonString)
            
            Log.i(TAG, "Import completed: $importedCount alarms imported")
            Result.success("Successfully imported $importedCount alarm(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Error importing alarms", e)
            Result.failure(Exception("Failed to import alarms: ${e.message}"))
        }
    }
    
    /**
     * Create JSON representation of alarms with validation
     */
    private fun createBackupJson(alarms: List<com.rooster.rooster.Alarm>): String {
        val jsonObject = JSONObject()
        jsonObject.put("version", BACKUP_VERSION)
        jsonObject.put("timestamp", System.currentTimeMillis())
        jsonObject.put("app_version", "1.1")
        jsonObject.put("alarm_count", alarms.size)
        
        val alarmsArray = JSONArray()
        alarms.forEach { alarm ->
            try {
                val alarmJson = JSONObject().apply {
                    put("id", alarm.id)
                    put("label", alarm.label)
                    put("enabled", alarm.enabled)
                    put("mode", alarm.mode)
                    put("ringtoneUri", alarm.ringtoneUri)
                    put("relative1", alarm.relative1)
                    put("relative2", alarm.relative2)
                    put("time1", alarm.time1)
                    put("time2", alarm.time2)
                    put("calculatedTime", alarm.calculatedTime)
                    put("monday", alarm.monday)
                    put("tuesday", alarm.tuesday)
                    put("wednesday", alarm.wednesday)
                    put("thursday", alarm.thursday)
                    put("friday", alarm.friday)
                    put("saturday", alarm.saturday)
                    put("sunday", alarm.sunday)
                }
                alarmsArray.put(alarmJson)
            } catch (e: Exception) {
                Log.e(TAG, "Error serializing alarm ${alarm.id}", e)
            }
        }
        
        jsonObject.put("alarms", alarmsArray)
        return jsonObject.toString(2) // Pretty print with indent of 2
    }
    
    /**
     * Parse JSON and import alarms
     */
    private suspend fun parseAndImportBackup(jsonString: String): Int {
        val jsonObject = JSONObject(jsonString)
        val version = jsonObject.getInt("version")
        
        if (version > BACKUP_VERSION) {
            throw Exception("Backup file version ($version) is not supported")
        }
        
        val alarmsArray = jsonObject.getJSONArray("alarms")
        var importedCount = 0
        
        for (i in 0 until alarmsArray.length()) {
            val alarmJson = alarmsArray.getJSONObject(i)
            
            try {
                val alarm = AlarmCreation(
                    label = alarmJson.getString("label"),
                    enabled = alarmJson.getBoolean("enabled"),
                    mode = alarmJson.getString("mode"),
                    ringtoneUri = alarmJson.optString("ringtoneUri", "Default"),
                    relative1 = alarmJson.getString("relative1"),
                    relative2 = alarmJson.getString("relative2"),
                    time1 = alarmJson.getLong("time1"),
                    time2 = alarmJson.getLong("time2"),
                    calculatedTime = 0 // Will be recalculated
                )
                
                val alarmId = alarmRepository.insertAlarm(alarm)
                
                // Update the day flags
                val insertedAlarm = alarmRepository.getAlarmById(alarmId)
                if (insertedAlarm != null) {
                    val updatedAlarm = insertedAlarm.copy(
                        monday = alarmJson.getBoolean("monday"),
                        tuesday = alarmJson.getBoolean("tuesday"),
                        wednesday = alarmJson.getBoolean("wednesday"),
                        thursday = alarmJson.getBoolean("thursday"),
                        friday = alarmJson.getBoolean("friday"),
                        saturday = alarmJson.getBoolean("saturday"),
                        sunday = alarmJson.getBoolean("sunday")
                    )
                    alarmRepository.updateAlarm(updatedAlarm)
                }
                
                importedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Error importing alarm $i", e)
                // Continue with next alarm
            }
        }
        
        return importedCount
    }
    
    /**
     * Create a backup as a JSON string (for quick backup to SharedPreferences)
     */
    suspend fun createQuickBackup(): String = withContext(Dispatchers.IO) {
        val alarms = alarmRepository.getAllAlarms()
        createBackupJson(alarms)
    }
    
    /**
     * Restore from a backup JSON string
     */
    suspend fun restoreFromQuickBackup(jsonString: String): Int = withContext(Dispatchers.IO) {
        parseAndImportBackup(jsonString)
    }
    
    /**
     * Validate backup file before importing
     */
    suspend fun validateBackupFile(context: Context, uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Failed to read file"))
            
            val jsonObject = JSONObject(jsonString)
            val version = jsonObject.getInt("version")
            val timestamp = jsonObject.getLong("timestamp")
            val alarmCount = jsonObject.optInt("alarm_count", 0)
            val appVersion = jsonObject.optString("app_version", "unknown")
            
            if (version > BACKUP_VERSION) {
                return@withContext Result.failure(Exception("Backup file version ($version) is not supported"))
            }
            
            val info = BackupInfo(
                version = version,
                timestamp = timestamp,
                alarmCount = alarmCount,
                appVersion = appVersion
            )
            
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Error validating backup file", e)
            Result.failure(Exception("Invalid backup file: ${e.message}"))
        }
    }
    
    /**
     * Delete all alarms before restore (optional, for clean restore)
     */
    suspend fun deleteAllAlarms(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            alarmRepository.deleteAllAlarms()
            Log.i(TAG, "All alarms deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting alarms", e)
            Result.failure(e)
        }
    }
}

/**
 * Information about a backup file
 */
data class BackupInfo(
    val version: Int,
    val timestamp: Long,
    val alarmCount: Int,
    val appVersion: String
)
