# Roadmap

## Current State (v1.5)

Rooster is a functional solar alarm clock with location-based sunrise/sunset scheduling, custom time pickers, a solar ring widget, and per-alarm settings (volume, vibration, snooze). See [CHANGELOG.md](CHANGELOG.md) for full version history.

---

## Short-term (v1.6 - v1.7)

### Code Health
- [ ] Refactor `AlarmEditorActivity` (1,676 lines) — extract logic into fragments or Compose screens
- [ ] Refactor `SolarRingWidgetProvider` (864 lines) — split rendering and data logic
- [ ] Remove legacy `AlarmHandler` — consolidate all scheduling through `ScheduleAlarmUseCase`
- [ ] Fix remaining audit issues: WakeLock leak in `AlarmActivity`, Handler-based snooze (use `AlarmManager`)
- [ ] Fix memory leak: replace `CoroutineScope(Dispatchers.Main)` with `lifecycleScope` in `AlarmActivity`

### Reliability
- [ ] Add MediaPlayer fallback to default ringtone on playback failure
- [ ] Validate alarms on boot before scheduling (filter past times)
- [ ] Persist snooze state in database (survive app kill / reboot)
- [ ] Add crash reporting (Firebase Crashlytics or Sentry)

### Testing
- [ ] Increase test coverage to 70%+ on business logic
- [ ] Add database migration tests
- [ ] Add end-to-end alarm flow tests on real devices

---

## Medium-term (v2.0)

### Full Compose Migration
- [ ] Migrate alarm editor to Jetpack Compose
- [ ] Migrate alarm list to Compose with LazyColumn
- [ ] Migrate settings screen to Compose
- [ ] Replace XML layouts progressively

### New Features
- [ ] Alarm history & statistics (fired, dismissed, snoozed counts)
- [ ] Alarm preview / test mode
- [ ] Weather-aware wake-up (show weather at alarm time)
- [ ] Multiple ringtone playlists per alarm
- [ ] Bedtime reminder / wind-down notification

### UX Improvements
- [ ] Onboarding flow for first-time users (permissions, location setup)
- [ ] Alarm grouping (e.g., "Work", "Weekend")
- [ ] Quick alarm creation from widget
- [ ] Accessibility improvements (TalkBack support, contrast)

---

## Long-term (v2.x+)

### Platform
- [ ] Wear OS companion app (alarm dismiss/snooze from watch)
- [ ] Google Play Store release
- [ ] Tablet-optimized layouts
- [ ] Widgets: additional widget styles (minimal, detailed)

### Intelligence
- [ ] Smart wake window — learn optimal wake time from user patterns
- [ ] Sleep phase integration (Google Health Connect)
- [ ] Adaptive volume based on ambient noise

### Social & Sharing
- [ ] Share alarm configurations
- [ ] Community solar event photos feed
- [ ] Family alarm sync (shared household schedules)

### Security & Privacy
- [ ] Encrypted backup files
- [ ] Backup password protection
- [ ] On-device astronomy calculation (remove API dependency)

---

## Non-goals
- iOS version (Android-first, no current plans for cross-platform)
- Music streaming integration (keep the app focused on solar alarms)
- Social alarm / multiplayer wake-up games

---

## How to Suggest Features

Open an issue with the `feature-request` label, or join the [Discord](https://discord.gg/WZUC4wE5MV) to discuss ideas.
