package com.rooster.rooster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

class RingtoneActivity : AppCompatActivity() {
    private var alarmId: Long = 0

    // Register a callback for the result from opening a document
    private val openDocumentRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                // Grant temporary read permission to the content URI
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Use the Uri to access the selected file here.
                updateAlarmRingtone(alarmId, it.toString())
                Toast.makeText(this, "Ringtone file selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringtone)

        intent.extras?.let {
            alarmId = it.getLong("alarm_id")
            Log.i("RingtoneActivity", "Alarm ID: $alarmId")
        }

        // Trigger the document selection
        selectRingtoneFile()
    }

    private fun selectRingtoneFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*" // Filter to show only audio files. Adjust if needed.
        }
        openDocumentRequest.launch(intent)
    }

    private fun updateAlarmRingtone(alarmId: Long, ringtoneUri: String) {
        // Update your method to handle the ringtone file URI
        val alarmDbHelper = AlarmDbHelper(this)
        val alarm = alarmDbHelper.getAlarm(alarmId)
        Log.w("Update", "Ringtone file URI update Intent for alarm $alarmId")
        alarm?.let {
            it.ringtoneUri = ringtoneUri
            alarmDbHelper.updateAlarm(it)
            Log.i("Update", "Ringtone file URI updated for alarm $alarmId")
        }
        finish()
    }
}
