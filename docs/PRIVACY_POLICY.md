# Privacy Policy

**Effective date:** 2026-05-03
**App:** Rooster (Android)
**Developer:** Théophile Delmas
**Contact:** contact@theophile.world

## Summary

Rooster runs entirely on your device. It does not collect, transmit, or share
your personal data. The app has no analytics, no advertising, no crash-reporting
SDKs, and makes no network requests of its own.

## What data the app accesses

| Data | Source | Why it is needed | Where it lives |
| --- | --- | --- | --- |
| Approximate / precise location | Android location services | Computing local sunrise, sunset, and twilight times for your alarms | Stored only on your device, in Rooster's private storage |
| Alarm configuration (time, label, ringtone, repeat days, sleep profile) | You, when you create or edit an alarm | Running your alarms | Stored only on your device, in Rooster's private storage |
| Theme and preference settings | You, via the Settings screen | Remembering your choices | Stored only on your device |

The app does not access your contacts, calendar, photos, microphone, accounts,
SMS, call logs, or any other personal data.

## How the data is used

- **Location** is read by Android and passed to Rooster only when the app is
  open or actively scheduling an alarm. Rooster computes sunrise / sunset /
  twilight times on your device using a built-in solar calculator. Your
  coordinates are not transmitted to any server controlled by the developer.
- **Alarm and preference data** is read and written by Rooster's local storage
  (Android's standard Room database and SharedPreferences). It is never sent off
  the device.

## Third parties

Rooster does not use any third-party SDKs for analytics, advertising, attribution,
or crash reporting. The only external code in the app is:

- **Google Play Services Location.** Rooster uses Android's
  `FusedLocationProviderClient` to receive location updates from the operating
  system. Google's handling of location data is governed by the
  [Google Privacy Policy](https://policies.google.com/privacy).
- **Google Play.** If you purchase the app, Google Play processes the payment.
  Rooster never sees your payment details. Refunds, billing, and account data
  are handled entirely by Google under their own policy.

The bundled audio tracks come from public sources documented in
[AUDIO_ATTRIBUTIONS.md](AUDIO_ATTRIBUTIONS.md); they do not transmit data and do
not interact with the network.

## Data retention and deletion

- All data created by Rooster lives only on your device.
- You can delete it at any time via Android Settings → Apps → Rooster → Storage
  → Clear data, or by uninstalling the app.
- Because the developer never receives your data, there is no server-side
  copy to delete.

## Your rights (GDPR / similar regimes)

Rooster does not store personal data on servers operated by the developer, so
there is no remote dataset to access, rectify, port, or erase. All data is
under your direct control on your device.

If you have questions about this policy or about a specific Android permission
the app requests, contact the developer at the email above.

## Children

Rooster is a general-purpose alarm clock and is not directed at children under
13. The app behaves the same way regardless of user age.

## Changes to this policy

If the policy changes, the effective date at the top of this document will be
updated and the change will be noted in the app's release notes on Google Play.

## Contact

contact@theophile.world

Source code: <https://github.com/thdelmas/Rooster>
