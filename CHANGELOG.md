# Changelog

All notable changes to the Rooster project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5] - 2025-12-05

### Added - Apple-Inspired UX
- **iOS-Style Wheel Time Picker**: Custom scrolling wheel picker with smooth inertial scrolling
  - Natural touch and drag scrolling
  - Fling gesture support with auto-snapping
  - Visual scaling and fade effects
  - Haptic feedback on value changes
- **Sound Preview System**: Tap to preview alarm sound, long press to change
  - 3-second auto-preview at 50% volume
  - Visual feedback with play icon
  - Proper resource cleanup
- **Smart Day Presets**: One-tap selection for common patterns
  - Weekdays (Mon-Fri) preset
  - Weekends (Sat-Sun) preset
  - Every Day preset
- **Smooth Transition Animations**: Fade in/out for all UI state changes
  - Mode switching with crossfade
  - Settings panels with smooth reveal/hide
  - Card transitions with proper timing
- **Enhanced Haptic Feedback**: Rich tactile feedback throughout
  - Light clicks for minor actions
  - Success patterns for completions
  - Error patterns for validation
- **Minimal Apple-Inspired Design**: Clean, polished interface
  - Better spacing and hierarchy
  - Prominent time displays
  - Grouped settings in cards
  - Clear visual affordances

### Changed
- Replaced standard Android time picker with custom wheel picker
- Redesigned alarm editor layout for better visual flow
- Enhanced ringtone button with preview and change actions
- Improved day selection with presets and animations
- Updated animations throughout using AnimationHelper

### Technical
- Created WheelTimePicker custom view component
- Added AppleTimePickerDialog with three-wheel design
- Implemented SoundPreviewHelper for ringtone previews
- Enhanced HapticFeedbackHelper with error feedback
- Added smooth animation support for all state transitions

## [1.4] - 2025-12-05

### Added
- **Vibration Control**: Per-alarm vibration settings with toggle in alarm editor
- **Snooze Functionality**: 
  - Configurable snooze duration (5-30 minutes)
  - Maximum snooze count (1-10 times)
  - Visual snooze counter on alarm screen
  - Smart snooze button that hides after max count reached
- **Gradual Volume**: Optional gradual volume increase over 30 seconds for gentler wake-up
- **Volume Control**: Per-alarm volume settings (0-100%)
- **Input Validation**: Ensures at least one day is selected before saving alarm
- **Enhanced Haptic Feedback**: Error feedback for validation failures
- **Snooze Settings UI**: Collapsible panel with +/- controls for duration and count
- **Comprehensive Documentation**: Added ALARM_IMPROVEMENTS.md

### Changed
- **Database Schema**: Updated to version 4 with new columns for alarm settings
- **Alarm Data Model**: Added fields for vibrate, snooze, volume, and gradual volume
- **Alarm Editor**: Reorganized settings with better visual hierarchy
- **Alarm Activity**: Enhanced with snooze button and gradual volume support
- **Settings Organization**: Grouped related settings in Material Design 3 cards

### Fixed
- Vibration switch now properly saves and applies settings
- Alarm validation prevents saving incomplete configurations
- Proper resource cleanup for MediaPlayer and Vibrator

### Technical
- Database migration from v3 to v4 with backward compatibility
- Added comprehensive error handling and logging
- Improved coroutine usage for gradual volume increase
- Enhanced null safety throughout alarm code

## [1.3] - 2025-12-05

### Added
- HapticFeedbackHelper utility for comprehensive haptic feedback throughout the app
- AnimationHelper utility for smooth expand/collapse and fade animations
- ThemeHelper utility for managing light/dark theme preferences
- Haptic feedback on all button presses and interactions
- Smooth expand/collapse animations for alarm cards
- Scale with bounce animation for day selection buttons
- Activity transition animations (fade and slide)
- Dark/Light theme toggle in Settings
- Material You dynamic colors support (Android 12+)
- Theme preference persistence across app restarts
- Appearance section in Settings with theme controls

### Changed
- Alarm card expand/collapse now uses smooth animations instead of instant visibility changes
- All button interactions now provide haptic feedback
- Delete actions provide distinctive heavy haptic feedback
- Toggle switches provide appropriate haptic feedback
- Screen transitions use smooth animations
- App now applies saved theme on startup

### Improved
- Overall UX with tactile feedback on all interactions
- Visual feedback with bounce animations on button presses
- Settings screen organization with new Appearance section
- User experience with theme customization options
- Accessibility with better feedback mechanisms

## [1.2] - 2025-12-04

### Added
- LocationUpdateWorker for efficient periodic location updates using WorkManager
- AstronomyRepository with offline caching and automatic fallback
- AstronomyDataEntity and AstronomyDao for persistent astronomy data storage
- ScheduleAlarmUseCase for clean architecture alarm scheduling
- PermissionHelper utility for centralized permission management
- ValidationHelper for comprehensive alarm data validation
- ErrorHandler utility for centralized error handling and logging
- Backup file validation before importing
- BackupInfo data class for backup metadata
- Database migration from version 2 to 3 for astronomy_data table
- Better error messages for permission dialogs
- Support for validating backup file version and contents

### Changed
- AlarmHandler now uses ScheduleAlarmUseCase via dependency injection
- Improved permission handling with clearer user messages
- AstronomyUpdateWorker now uses AstronomyRepository for caching
- BackupManager backup version upgraded to v2 with enhanced metadata
- WorkManagerHelper location update interval optimized from 15min to 3 hours
- Permission requests now version-aware (Android 13+ handling)
- Removed fallbackToDestructiveMigration from database configuration
- Enhanced backup JSON format with alarm ID and calculated time

### Fixed
- TODO in LocationUpdateService.onBind() - now returns null properly
- Deprecated READ_EXTERNAL_STORAGE permission on Android 13+
- AlarmRepository now validates alarms before inserting/updating
- Location worker handles failed updates with cached astronomy data
- Permission dialog wording improved for better user understanding

### Improved
- Offline support for astronomy data with intelligent caching
- Battery efficiency with optimized location update frequency
- Error handling throughout the application
- Alarm validation with detailed error messages
- Backup/restore functionality with validation and metadata
- Permission flow with dedicated helper utility
- Code architecture with better separation of concerns
- Logging with consistent tags and structured messages

### Security
- Enhanced input validation for alarm data
- Location coordinate validation
- Backup file validation before restoration

## [1.1] - 2025-12-04

### Added
- Kotlin Coroutines support for all background operations
- Comprehensive error handling and retry logic
- Better logging throughout the application
- Timeout configuration for network requests
- Battery-efficient location updates
- Makefile commands: `clean`, `build`, `test`, `lint`, `uninstall`
- IMPROVEMENTS.md documentation file
- CHANGELOG.md for version tracking

### Changed
- Updated Android SDK from 33 to 34
- Upgraded all dependencies to latest stable versions
- Modernized LocationUpdateService with Coroutines
- Modernized AstronomyUpdateService with Coroutines
- Improved MainActivity lifecycle management
- Enhanced AlarmHandler with better scheduling logic
- Database operations now use safe column access methods
- PendingIntents now use FLAG_IMMUTABLE for security
- Java compatibility level updated from 8 to 11

### Fixed
- Memory leak in MainActivity Handler
- Deprecated API usage (`toLowerCase()` → `lowercase()`)
- Database cursor resource leaks
- Null pointer exceptions in ringtone handling
- Alarm cancellation not properly clearing PendingIntents
- Location service not removing updates after success
- SimpleDateFormat missing Locale configuration

### Improved
- Code quality with null safety
- Performance of database queries
- Reliability of alarm scheduling
- Battery efficiency of background services
- Error messages and debugging information
- Resource cleanup and lifecycle management

### Security
- Updated PendingIntent flags for Android 12+ compliance
- Better permission checks for location access
- Enhanced security exception handling

## [1.0] - Initial Release

### Added
- Basic alarm functionality with sunrise/sunset times
- Location-based sunrise time calculation
- Multiple alarm support
- Customizable wake-up days
- Ringtone selection
- Background GPS synchronization
- Automatic sun times synchronization
- Astronomy data from sunrise-sunset.org API

