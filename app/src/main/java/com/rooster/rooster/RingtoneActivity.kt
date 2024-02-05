package com.rooster.rooster

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast

class RingtoneActivity : AppCompatActivity() {
    private var alarmId: Long = 0
    private val RINGTONE_PICKER_REQUEST = 999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringtone)

        // Check and request permission
        checkPermission()

        // Get alarm ID from intent
        intent.extras?.let {
            alarmId = it.getLong("alarm_id")
            Log.i("RingtoneActivity", "Alarm ID: $alarmId")
        }

        // Open ringtone picker
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
        }
        startActivityForResult(intent, RINGTONE_PICKER_REQUEST)
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RINGTONE_PICKER_REQUEST) {
            val ringtoneUri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            ringtoneUri?.let {
                // Here you should update the alarm in your database with the URI
                updateAlarmRingtone(alarmId, it.toString())
                Toast.makeText(this, "Ringtone selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAlarmRingtone(alarmId: Long, ringtoneUri: String) {
        val alarmDbHelper = AlarmDbHelper(this)
        val alarm = alarmDbHelper.getAlarm(alarmId)
        Log.w("Update", "Ringtone update Intent for alarm $alarmId")
        alarm?.let {
            it.ringtoneUri = ringtoneUri
            alarmDbHelper.updateAlarm(it)
            Log.i("Update", "Ringtone updated for alarm $alarmId")
        }
        finish()
    }
}
