package com.rooster.rooster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.rooster.rooster.util.HapticFeedbackHelper

class CreditsActivity : AppCompatActivity() {

    private data class AudioCredit(
        val event: String,
        val title: String,
        val author: String,
        val license: String,
        val sourceUrl: String,
    )

    private val audioCredits = listOf(
        AudioCredit(
            event = "Sunrise",
            title = "Medium rooster crowing",
            author = "alys (via PDSounds.org)",
            license = "Public domain",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Medium_rooster_crowing.ogg",
        ),
        AudioCredit(
            event = "Civil Dawn",
            title = "Sonus naturalis – Soundscape XC376330",
            author = "Taukeer Alam Lodha (xeno-canto.org)",
            license = "CC BY-SA 4.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Sonus_naturalis_-_Soundscape_XC376330.mp3",
        ),
        AudioCredit(
            event = "Nautical Dawn",
            title = "Ocean waves crushing",
            author = "Luftrum",
            license = "CC BY 3.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Oceanwavescrushing.ogg",
        ),
        AudioCredit(
            event = "Astronomical Dawn",
            title = "Windglockenspiel (Koshi)",
            author = "Membeth",
            license = "CC0 1.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Windglockenspiel.Koshi.ogg",
        ),
        AudioCredit(
            event = "Solar Noon",
            title = "Wind chimes",
            author = "Esc861",
            license = "Public domain",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Windchimes.ogg",
        ),
        AudioCredit(
            event = "Sunset",
            title = "Church bells",
            author = "Natalie (via PDSounds.org)",
            license = "Public domain",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Churchbells.ogg",
        ),
        AudioCredit(
            event = "Civil Dusk",
            title = "Field cricket",
            author = "Thatcher",
            license = "CC BY-SA 3.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Field_cricket_unedited.ogg",
        ),
        AudioCredit(
            event = "Nautical Dusk",
            title = "Howling wind",
            author = "Tvabutzku1234",
            license = "CC0 1.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Howling_wind.ogg",
        ),
        AudioCredit(
            event = "Astronomical Dusk",
            title = "Tawny Owl (Strix aluco)",
            author = "Aubrey John Williams / The British Library",
            license = "CC BY-SA 4.0",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Tawny_Owl_(Strix_aluco)_(W1CDR0001519_BD8).ogg",
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)
        setContentView(R.layout.activity_credits)

        findViewById<MaterialToolbar>(R.id.topAppBar).setNavigationOnClickListener {
            onBackPressed()
        }

        renderAudioCredits()

        findViewById<TextView>(R.id.gplLicenseLink).setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            openUrl("https://www.gnu.org/licenses/gpl-3.0.html")
        }

        findViewById<TextView>(R.id.privacyPolicyLink).setOnClickListener {
            HapticFeedbackHelper.performClick(it)
            openUrl("https://github.com/thdelmas/Rooster/blob/main/docs/PRIVACY_POLICY.md")
        }
    }

    private fun renderAudioCredits() {
        val container = findViewById<LinearLayout>(R.id.audioCreditsContainer)
        val inflater = LayoutInflater.from(this)
        for (credit in audioCredits) {
            val row = inflater.inflate(R.layout.item_audio_credit, container, false) as MaterialCardView
            row.findViewById<TextView>(R.id.creditEvent).text = credit.event
            row.findViewById<TextView>(R.id.creditLicense).text = credit.license
            row.findViewById<TextView>(R.id.creditTitle).text = credit.title
            row.findViewById<TextView>(R.id.creditAuthor).text = "by ${credit.author}"
            row.setOnClickListener {
                HapticFeedbackHelper.performClick(it)
                openUrl(credit.sourceUrl)
            }
            container.addView(row)
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
