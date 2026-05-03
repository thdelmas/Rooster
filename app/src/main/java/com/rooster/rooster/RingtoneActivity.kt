package com.rooster.rooster

import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rooster.rooster.presentation.compose.RingtoneItemUi
import com.rooster.rooster.presentation.compose.RingtoneScreen
import com.rooster.rooster.presentation.viewmodel.RingtoneViewModel
import com.rooster.rooster.ui.SoundPreviewHelper
import com.rooster.rooster.ui.theme.RoosterTheme
import com.rooster.rooster.util.HapticFeedbackHelper
import com.rooster.rooster.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RingtoneActivity : ComponentActivity() {

    private val viewModel: RingtoneViewModel by viewModels()
    private lateinit var soundPreviewHelper: SoundPreviewHelper
    private val previewingUri = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundPreviewHelper = SoundPreviewHelper(this)

        val alarmId = intent.getLongExtra("alarm_id", -1L)
        val ringtones = getAvailableRingtones()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateResult.collectLatest { result ->
                    if (result != null) {
                        when (result) {
                            is RingtoneViewModel.UpdateResult.Success ->
                                Logger.i(TAG, "Ringtone updated successfully")
                            is RingtoneViewModel.UpdateResult.Error ->
                                Logger.e(TAG, "Error updating ringtone: ${result.message}")
                        }
                        viewModel.resetUpdateResult()
                        finish()
                    }
                }
            }
        }

        setContent {
            RoosterTheme {
                val previewing by previewingUri.collectAsState()
                RingtoneScreen(
                    ringtones = ringtones,
                    previewingUri = previewing,
                    onBack = ::handleBack,
                    onPreview = ::previewRingtone,
                    onSelect = { item ->
                        HapticFeedbackHelper.performClick(window.decorView)
                        selectRingtone(alarmId, item)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPreviewHelper.cleanup()
    }

    private fun handleBack() {
        soundPreviewHelper.cleanup()
        finish()
    }

    private fun getAvailableRingtones(): List<RingtoneItemUi> {
        val results = mutableListOf(RingtoneItemUi(title = "Default Ringtone", uri = null))
        val ringtoneManager = RingtoneManager(this).apply { setType(RingtoneManager.TYPE_ALARM) }
        try {
            val cursor = ringtoneManager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneManager.getRingtoneUri(cursor.position)
                results.add(RingtoneItemUi(title = title, uri = uri))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting ringtones", e)
        }
        return results
    }

    private fun previewRingtone(item: RingtoneItemUi) {
        HapticFeedbackHelper.performClick(window.decorView)
        if (soundPreviewHelper.isPreviewPlaying()) {
            soundPreviewHelper.stopPreview()
            if (previewingUri.value == item.uri) {
                previewingUri.value = null
                return
            }
        }
        previewingUri.value = item.uri
        val uriString = item.uri?.toString() ?: "Default"
        soundPreviewHelper.previewSound(uriString, durationMs = PREVIEW_DURATION_MS)
        lifecycleScope.launch {
            while (soundPreviewHelper.isPreviewPlaying() && previewingUri.value == item.uri) {
                delay(POLL_INTERVAL_MS)
            }
            if (previewingUri.value == item.uri) {
                previewingUri.value = null
            }
        }
    }

    private fun selectRingtone(alarmId: Long, item: RingtoneItemUi) {
        if (alarmId == -1L) {
            Logger.w(TAG, "Invalid alarm ID")
            finish()
            return
        }
        soundPreviewHelper.stopPreview()
        Logger.i(TAG, "Updating ringtone for alarm $alarmId")
        viewModel.updateAlarmRingtone(alarmId, item.uri?.toString() ?: "Default")
    }

    companion object {
        private const val TAG = "RingtoneActivity"
        private const val PREVIEW_DURATION_MS = 5000L
        private const val POLL_INTERVAL_MS = 500L
    }
}
