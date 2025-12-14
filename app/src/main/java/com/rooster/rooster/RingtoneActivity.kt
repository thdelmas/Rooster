package com.rooster.rooster
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rooster.rooster.R
import com.rooster.rooster.data.repository.AlarmRepository
import com.rooster.rooster.ui.SoundPreviewHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RingtoneActivity : AppCompatActivity() {
    
    @Inject
    lateinit var alarmRepository: AlarmRepository

    private lateinit var soundPreviewHelper: SoundPreviewHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RingtoneAdapter
    private var currentlyPreviewingUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringtone)

        soundPreviewHelper = SoundPreviewHelper(this)

        // Setup toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            soundPreviewHelper.cleanup()
            finish()
        }

        // Get alarm ID from intent
        val alarmId = intent.getLongExtra("alarm_id", -1)

        // Setup RecyclerView
        recyclerView = findViewById(R.id.ringtoneRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get available ringtones
        val ringtones = getAvailableRingtones()

        // Create adapter
        adapter = RingtoneAdapter(
            ringtones,
            onRingtoneSelect = { ringtoneItem ->
                // Select ringtone
                val uriString = ringtoneItem.uri?.toString() ?: "Default"
                updateAlarmRingtone(alarmId, uriString)
            },
            onRingtonePreview = { ringtoneItem ->
                // Preview ringtone
                previewRingtone(ringtoneItem)
            },
            isCurrentlyPreviewing = { ringtoneItem ->
                currentlyPreviewingUri == ringtoneItem.uri && soundPreviewHelper.isPreviewPlaying()
            }
        )

        recyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPreviewHelper.cleanup()
    }

    private fun getAvailableRingtones(): List<RingtoneItem> {
        val ringtones = mutableListOf<RingtoneItem>()

        // Add default option
        ringtones.add(RingtoneItem("Default Ringtone", null))

        // Get system alarm ringtones
        val ringtoneManager = RingtoneManager(this)
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM)

        try {
            val cursor = ringtoneManager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneManager.getRingtoneUri(cursor.position)
                ringtones.add(RingtoneItem(title, uri))
            }
        } catch (e: Exception) {
            Log.e("RingtoneActivity", "Error getting ringtones", e)
        }

        return ringtones
    }

    private fun previewRingtone(ringtoneItem: RingtoneItem) {
        // Stop current preview if playing
        if (soundPreviewHelper.isPreviewPlaying()) {
            soundPreviewHelper.stopPreview()
            // If clicking the same ringtone again, just stop
            if (currentlyPreviewingUri == ringtoneItem.uri) {
                currentlyPreviewingUri = null
                adapter.notifyDataSetChanged()
                return
            }
        }

        // Start preview
        currentlyPreviewingUri = ringtoneItem.uri
        val uriString = ringtoneItem.uri?.toString() ?: "Default"
        soundPreviewHelper.previewSound(uriString, durationMs = 5000) // Preview for 5 seconds
        
        // Notify adapter to update UI
        adapter.notifyDataSetChanged()
        
        // Update UI when preview stops (check periodically)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val checkPreview = object : Runnable {
            override fun run() {
                if (!soundPreviewHelper.isPreviewPlaying() && currentlyPreviewingUri == ringtoneItem.uri) {
                    currentlyPreviewingUri = null
                    adapter.notifyDataSetChanged()
                } else if (soundPreviewHelper.isPreviewPlaying() && currentlyPreviewingUri == ringtoneItem.uri) {
                    // Check again in 500ms
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.postDelayed(checkPreview, 500)
    }

    private fun checkAndRequestPermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun updateAlarmRingtone(alarmId: Long, ringtoneUri: String) {
        if (alarmId == -1L) {
            Log.w("RingtoneActivity", "Invalid alarm ID")
            finish()
            return
        }

        soundPreviewHelper.stopPreview()
        Log.i("RingtoneActivity", "Updating ringtone for alarm $alarmId")
        
        // Use Repository to update alarm ringtone
        lifecycleScope.launch {
            try {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    alarm.ringtoneUri = ringtoneUri
                    alarmRepository.updateAlarm(alarm)
                    Log.i("RingtoneActivity", "Ringtone updated for alarm $alarmId")
                } else {
                    Log.e("RingtoneActivity", "Alarm not found: $alarmId")
                }
            } catch (e: Exception) {
                Log.e("RingtoneActivity", "Error updating ringtone", e)
            } finally {
                finish()
            }
        }
    }
}
