# Architecture

## Overview

Rooster follows **MVVM + Clean Architecture** with dependency injection via Hilt.

```
UI (Activities / Custom Views)
  ↓
ViewModels (Hilt-injected)
  ↓
UseCases (Business logic)
  ↓
Repositories (Data abstraction)
  ↓
Room Database / APIs
```

## Package Structure

```
com.rooster.rooster/
├── Activities                    # UI layer (Activity-based)
│   ├── MainActivity              # Home screen with solar ring
│   ├── AlarmListActivity         # Alarm list management
│   ├── AlarmEditorActivity       # Create/edit alarms
│   ├── AlarmActivity             # Full-screen alarm display
│   ├── TimePickerActivity        # Custom time selection
│   ├── RingtoneActivity          # Ringtone selection
│   └── SettingsActivity          # App settings
│
├── presentation/viewmodel/       # ViewModels
│   ├── AlarmListViewModel
│   ├── AlarmEditorViewModel
│   ├── AlarmViewModel
│   ├── MainViewModel
│   ├── SettingsViewModel
│   ├── BackupViewModel
│   └── RingtoneViewModel
│
├── domain/usecase/               # Business logic
│   ├── ScheduleAlarmUseCase
│   └── CalculateAlarmTimeUseCase
│
├── data/
│   ├── local/
│   │   ├── AlarmDatabase         # Room DB (v6, 6 migrations)
│   │   ├── entity/               # AlarmEntity, AstronomyDataEntity, LocationEntity
│   │   └── dao/                  # AlarmDao, AstronomyDao, LocationDao
│   ├── repository/               # AlarmRepository, AstronomyRepository, LocationRepository
│   ├── mapper/                   # AlarmMapper
│   └── backup/                   # BackupManager
│
├── di/                           # Hilt modules
│   ├── AppModule
│   ├── DatabaseModule
│   └── RepositoryModule
│
├── ui/                           # Custom views
│   ├── SolarRingView             # Solar event visualization
│   ├── SolarRingTimePickerView   # Interactive time picker on ring
│   ├── WheelTimePicker           # iOS-style wheel picker
│   ├── AppleTimePickerDialog     # Three-wheel time dialog
│   └── SoundPreviewHelper        # Audio preview
│
├── widget/                       # App widgets
│   └── SolarRingWidgetProvider   # Home screen widget
│
├── worker/                       # Background tasks (WorkManager)
│   ├── LocationUpdateWorker
│   ├── AstronomyUpdateWorker
│   └── WorkManagerHelper
│
├── receiver/                     # Broadcast receivers
│   ├── AlarmclockReceiver
│   └── SnoozeReceiver
│
└── util/                         # Utilities (13 files)
    ├── AppConstants
    ├── PermissionHelper
    ├── ValidationHelper
    ├── AnimationHelper
    ├── HapticFeedbackHelper
    ├── ThemeHelper
    ├── Logger
    ├── TimeUtils
    └── Extensions
```

## Key Components

### Alarm System
- **4 modes**: At, Before, After, Between
- **9 solar events**: Astronomical/Nautical/Civil Dawn, Sunrise, Solar Noon, Sunset, Civil/Nautical/Astronomical Dusk
- Alarms are scheduled via `AlarmManager` (not WorkManager) for reliability
- Snooze uses `SnoozeReceiver` with `AlarmManager`

### Data Layer
- **Room** database (v6) with 3 tables: `alarms`, `astronomy_data`, `location_data`
- 6 migrations handling schema evolution
- `AstronomyRepository` with offline caching and freshness tracking

### Background Work
- **LocationUpdateWorker**: GPS updates every 3 hours
- **AstronomyUpdateWorker**: Solar data refresh (6-hour validity)
- Both managed by `WorkManagerHelper`

### Widget
- `SolarRingWidgetProvider` renders a solar ring visualization on the home screen
- Updates on alarm changes and periodically

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.24 |
| DI | Hilt (Dagger 2) 2.50 |
| Database | Room 2.6.1 |
| Background | WorkManager 2.9.0 |
| Async | Coroutines 1.7.3 |
| UI | Material Design 3 + Jetpack Compose (partial) |
| Location | Google Play Services 21.1.0 |
| Target SDK | 34 (Android 14) |
| Min SDK | 21 (Android 5.0) |
