# Data Safety form — answers

This document mirrors the answers to enter into the Google Play Console
**Data Safety** questionnaire. Keeping it in the repo means the form
answers stay in sync with the privacy policy and the actual code.

If anything in this document changes, also update
[PRIVACY_POLICY.md](PRIVACY_POLICY.md) and the in-app Credits screen.

## Overview answers

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** (location only) |
| Is all of the user data collected by your app encrypted in transit? | **Not applicable** — the only data collected (location) never leaves the device |
| Do you provide a way for users to request that their data be deleted? | **Yes** — uninstalling the app or clearing app data deletes everything |

## Data types — what to declare collected / shared

The Data Safety form asks one row per data type. For Rooster, only **one
row** has a non-empty answer.

### Location → Approximate location

| Field | Answer |
| --- | --- |
| Collected? | **Yes** |
| Shared? | **No** |
| Required or optional? | **Optional** — the app launches and runs without location, but solar-event scheduling produces no useful times until coordinates are available |
| Purpose(s) | **App functionality** (only) |
| Is this data ephemeral? | **No** — coordinates are persisted to local storage to compute solar times across app restarts |

### Location → Precise location

| Field | Answer |
| --- | --- |
| Collected? | **Yes** |
| Shared? | **No** |
| Required or optional? | **Optional** (same reason as approximate) |
| Purpose(s) | **App functionality** (only) |
| Is this data ephemeral? | **No** |

### All other data types

For every other data type the form lists, the answer is **No** (not
collected, not shared). Specifically:

- Personal info (name, email, address, IDs, phone, race, sexual orientation, political/religious info, other)
- Financial info (purchases handled by Google Play directly — see note below)
- Health and fitness
- Messages (email, SMS, in-app)
- Photos and videos
- Audio files (the app plays bundled & system ringtones; it does not record or upload audio)
- Files and docs
- Calendar
- Contacts
- App activity (interactions, search history, installed apps, in-app content, other actions)
- Web browsing history
- App info and performance (crash logs, diagnostics, other) — the app contains no Crashlytics, Sentry, Firebase Performance, or similar SDK
- Device or other identifiers (advertising ID, device ID, install ID)

### Note on Google Play purchases

Google Play handles all payment processing. The app never sees payment
details, billing addresses, or any financial info. The Data Safety form
**explicitly excludes Google Play purchase data from developer
disclosure** — it's covered by Google's own policy. So "Financial info"
stays **No**.

## Security practices section

| Question | Answer |
| --- | --- |
| Is data encrypted in transit? | **Not applicable** — no data is transmitted off the device |
| Do you provide a way for users to request that their data be deleted? | **Yes — Android Settings → Apps → Rooster → Storage → Clear Data, or uninstall** |
| Has your app been independently validated against a global security standard? (MASA) | **No** — optional self-attestation; consider for a future release |

## Why each "No" actually holds

A reviewer or auditor can verify these claims by reading the source:

| Claim | How to verify |
| --- | --- |
| No analytics | `grep -r "firebase\|analytics\|mixpanel\|amplitude\|segment" app/build.gradle` returns nothing |
| No advertising | Same — no ad SDKs in dependencies |
| No crash reporting | No `Crashlytics`, `Sentry`, `Bugsnag` references in code or build.gradle |
| No network calls of our own | `grep -r "okhttp\|retrofit\|HttpURLConnection" app/src/main/java` returns nothing; `INTERNET` permission was removed |
| Location stays on device | `AstronomyRepository.fetchAndCacheAstronomyData` is a misnomer — after the v1.5 refactor it computes locally via `SolarCalculator` |
| Only third-party SDK is Google Play Services Location | `grep "implementation '" app/build.gradle` shows the full list — only `play-services-location` connects to anything outside the app's own process |

## Permissions disclosure (separate Play Console section)

The "Permissions" section in Play Console asks about each permission.
Brief justifications, mapped to the manifest:

| Permission | Justification |
| --- | --- |
| `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` | Computing local sunrise / sunset / twilight times on-device. Without coordinates, solar-event alarms cannot fire at the right time for your place. |
| `POST_NOTIFICATIONS` | Showing the alarm notification when the device wakes the user. |
| `USE_EXACT_ALARM` (API 33+) / `SCHEDULE_EXACT_ALARM` (API ≤32) | An alarm clock must fire at exactly the user's chosen time, not "around" it. Declared per Google Play's "alarm or calendar" use case. |
| `USE_FULL_SCREEN_INTENT` | Showing the full-screen alarm UI on the lock screen so the user can dismiss or snooze. |
| `VIBRATE` | Optional per-alarm vibration. |
| `WAKE_LOCK` | Keeping the CPU awake while the alarm is ringing so it doesn't get cut off. |
| `RECEIVE_BOOT_COMPLETED` | Re-scheduling alarms after a device reboot — without this, your alarms would disappear after a reboot. |
| `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤32) | Listing your existing system ringtones in the ringtone picker via Android's `RingtoneManager`. |
