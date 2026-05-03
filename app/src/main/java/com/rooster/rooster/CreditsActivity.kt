package com.rooster.rooster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rooster.rooster.presentation.compose.AudioCreditUi
import com.rooster.rooster.presentation.compose.CreditsScreen
import com.rooster.rooster.ui.theme.RoosterTheme
import com.rooster.rooster.util.HapticFeedbackHelper

class CreditsActivity : ComponentActivity() {

    private val audioCredits = listOf(
        AudioCreditUi(
            event = "Sunrise",
            title = "Medium rooster crowing",
            author = "alys (via PDSounds.org)",
            license = "Public domain",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Medium_rooster_crowing.ogg",
        ),
        AudioCreditUi(
            event = "Civil Dawn",
            title = "Sonus naturalis – Soundscape XC376330",
            author = "Taukeer Alam Lodha (xeno-canto.org)",
            license = "CC BY-SA 4.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Sonus_naturalis_-_Soundscape_XC376330.mp3",
        ),
        AudioCreditUi(
            event = "Nautical Dawn",
            title = "Ocean waves crushing",
            author = "Luftrum",
            license = "CC BY 3.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Oceanwavescrushing.ogg",
        ),
        AudioCreditUi(
            event = "Astronomical Dawn",
            title = "Windglockenspiel (Koshi)",
            author = "Membeth",
            license = "CC0 1.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Windglockenspiel.Koshi.ogg",
        ),
        AudioCreditUi(
            event = "Solar Noon",
            title = "Wind chimes",
            author = "Esc861",
            license = "Public domain",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Windchimes.ogg",
        ),
        AudioCreditUi(
            event = "Sunset",
            title = "Church bells",
            author = "Natalie (via PDSounds.org)",
            license = "Public domain",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Churchbells.ogg",
        ),
        AudioCreditUi(
            event = "Civil Dusk",
            title = "Field cricket",
            author = "Thatcher",
            license = "CC BY-SA 3.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Field_cricket_unedited.ogg",
        ),
        AudioCreditUi(
            event = "Nautical Dusk",
            title = "Howling wind",
            author = "Tvabutzku1234",
            license = "CC0 1.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Howling_wind.ogg",
        ),
        AudioCreditUi(
            event = "Astronomical Dusk",
            title = "Tawny Owl (Strix aluco)",
            author = "Aubrey John Williams / The British Library",
            license = "CC BY-SA 4.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Tawny_Owl_(Strix_aluco)_(W1CDR0001519_BD8).ogg",
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoosterTheme {
                CreditsScreen(
                    audioCredits = audioCredits,
                    onBack = ::handleBack,
                    onOpenUrl = ::openUrl,
                    onOpenGplLicense = {
                        HapticFeedbackHelper.performSuccessFeedback(this)
                        openUrl("https://www.gnu.org/licenses/gpl-3.0.html")
                    },
                    onOpenPrivacyPolicy = {
                        HapticFeedbackHelper.performSuccessFeedback(this)
                        openUrl("https://github.com/thdelmas/Rooster/blob/main/docs/PRIVACY_POLICY.md")
                    },
                )
            }
        }
    }

    private fun handleBack() {
        @Suppress("DEPRECATION")
        onBackPressed()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
