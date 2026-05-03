# Bundled audio — sources and licenses

The themed alarm sounds in [`app/src/main/res/raw/`](../app/src/main/res/raw/)
were sourced from Wikimedia Commons. Resolution mapping lives in
[`SolarEventSoundMapper`](../app/src/main/java/com/rooster/rooster/util/SolarEventSoundMapper.kt).

To swap a tone: replace the file in `res/raw/`, keeping the same basename
(e.g. `rooster_crow.ogg`). Android's `getIdentifier()` matches by basename, so
the extension can change.

| File | Solar event | Source | Author | License |
| --- | --- | --- | --- | --- |
| `rooster_crow.ogg` | Sunrise | [Medium_rooster_crowing.ogg](https://commons.wikimedia.org/wiki/File:Medium_rooster_crowing.ogg) | alys (via PDSounds.org) | Public domain |
| `morning_birds.mp3` | Civil Dawn | [Sonus naturalis – Soundscape XC376330](https://commons.wikimedia.org/wiki/File:Sonus_naturalis_-_Soundscape_XC376330.mp3) | Taukeer Alam Lodha (xeno-canto.org) | CC BY-SA 4.0 |
| `ocean_waves.ogg` | Nautical Dawn | [Oceanwavescrushing.ogg](https://commons.wikimedia.org/wiki/File:Oceanwavescrushing.ogg) | Luftrum | CC BY 3.0 |
| `gentle_chime.ogg` | Astronomical Dawn | [Windglockenspiel.Koshi.ogg](https://commons.wikimedia.org/wiki/File:Windglockenspiel.Koshi.ogg) | Membeth | CC0 1.0 |
| `wind_chime.ogg` | Solar Noon | [Windchimes.ogg](https://commons.wikimedia.org/wiki/File:Windchimes.ogg) | Esc861 | Public domain |
| `evening_bell.ogg` | Sunset | [Churchbells.ogg](https://commons.wikimedia.org/wiki/File:Churchbells.ogg) | Natalie (via PDSounds.org) | Public domain |
| `cricket_song.ogg` | Civil Dusk | [Field_cricket_unedited.ogg](https://commons.wikimedia.org/wiki/File:Field_cricket_unedited.ogg) | Thatcher | CC BY-SA 3.0 |
| `night_breeze.ogg` | Nautical Dusk | [Howling_wind.ogg](https://commons.wikimedia.org/wiki/File:Howling_wind.ogg) | Tvabutzku1234 | CC0 1.0 |
| `owl_hoot.ogg` | Astronomical Dusk | [Tawny Owl (Strix aluco), W1CDR0001519 BD8](https://commons.wikimedia.org/wiki/File:Tawny_Owl_(Strix_aluco)_(W1CDR0001519_BD8).ogg) | Aubrey John Williams / The British Library | CC BY-SA 4.0 |

## Licensing notes

Each audio file retains its own license as listed above. Bundling these assets
in the APK is treated as mere aggregation: the application code is GPL-3.0, and
the audio assets are distributed under their respective Creative Commons /
public-domain terms.

For the CC BY and CC BY-SA tracks, attribution is surfaced to end users via
the in-app **Credits** screen (Settings → Credits & Licenses). Replacing or
adding tracks must keep that screen in sync.

## Default fallback

When an alarm has no ringtone set (or a stored URI fails to resolve), the app
falls back to `rooster_crow.ogg` — the same public-domain track used for the
Sunrise theme. See [`SolarEventSoundMapper.defaultUri`](../app/src/main/java/com/rooster/rooster/util/SolarEventSoundMapper.kt).
