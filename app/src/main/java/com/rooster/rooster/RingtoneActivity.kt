package com.rooster.rooster
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.rooster.rooster.R

class RingtoneActivity : AppCompatActivity() {

    private val RINGTONE_PICKER_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringtone)

        val btnSelectRingtone: Button = findViewById(R.id.btnSelectRingtone)
        btnSelectRingtone.setOnClickListener {
            checkAndRequestPermission()
            showRingtonePicker()
        }
    }

    private fun showRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone")
        }
        startActivityForResult(intent, RINGTONE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RINGTONE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                Log.e("Ringtone", "Ringtone URI: $uri")
                updateAlarmRingtone(intent.getLongExtra("alarm_id", -1), uri.toString())
            }
        }
    }

    private fun checkAndRequestPermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            // Permission has already been granted, proceed with your logic
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
