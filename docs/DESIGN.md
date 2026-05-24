# Design

This document describes Rooster's visual, audio, and interaction design language. It complements [ARCHITECTURE.md](ARCHITECTURE.md), which covers code structure.

## Design principles

1. **Astral, not alarming.** Rooster wakes you with the sun. Cues should feel meditative — bell-like tones, warm gradients, slow animations — never jarring.
2. **Black as canvas.** The dark theme is deliberately solid black so solar accents (amber, coral, gold) carry the visual weight. The app reads as a piece of night sky with a sun rising through it.
3. **The ring is the app.** The solar ring is Rooster's signature affordance: it visualizes the day, picks times, anchors the home screen, and lives on the launcher as a widget. New surfaces should reuse it before inventing new metaphors.
4. **Quiet by default.** No badge counts, no upsells, no notifications outside of solar/alarm events. The app should feel calm sitting unused.
5. **Identifiable by sense alone.** A user with the phone face-down should know which solar event fired by touch; with the screen on but muted, by sight; with the speaker on, by ear. Each modality carries the same information independently.

## Color system

Colors are defined in [colors.xml](../app/src/main/res/values/colors.xml). The palette traces a single arc — deep space → twilight → golden hour → sunrise — and every accent color sits somewhere on that arc.

### Astral journey palette

| Token            | Hex       | Meaning                       |
|------------------|-----------|-------------------------------|
| `night`          | `#0A0A14` | Pre-dawn, post-dusk           |
| `astronomical`   | `#1A1A2E` | Astronomical twilight         |
| `nautical`       | `#2A2A4E` | Nautical twilight             |
| `civil`          | `#4A4A7E` | Civil twilight                |
| `sunrise`        | `#FF8E53` | Sunrise / sunset              |
| `golden_hour`    | `#FFB86C` | Golden hour                   |
| `day_sky`        | `#FFD89C` | Full day                      |

These tokens drive the solar ring gradient, time-of-day theming, and event accent colors. Use them rather than introducing new ones.

### Material 3 mapping

| Role           | Light      | Dark       |
|----------------|------------|------------|
| Primary        | `#FF6B35`  | `#FF8E53`  |
| Secondary      | `#FF8E53`  | `#FFB86C`  |
| Tertiary       | `#FFB86C`  | `#FFD89C`  |
| Background     | `#FFFBF8`  | `#000000`  |
| Surface        | `#FFFBF8`  | `#000000`  |
| On-surface     | `#1C0F0A`  | `#FFB86C`  |

Dark theme uses **solid black** (`#000000`) for background, surface, and all surface containers — including cards. Elevation is communicated by border (sun-colored stroke) and content density, not by tonal surface tinting. Do not introduce gray surface tiers in dark theme.

### Semantic accents

`success #4ADE80`, `warning #FBBF24`, `info #60A5FA`, `error #BA1A1A` (light) / `#FFB4AB` (dark). Use sparingly — most state should resolve through the solar palette.

## Typography

Defined in [themes.xml](../app/src/main/res/values/themes.xml) under `TextAppearance.Rooster.*`.

| Style          | Family               | Letter spacing |
|----------------|----------------------|----------------|
| Display Large  | sans-serif-light     | -0.02          |
| Headline Large | sans-serif-medium    | -0.01          |
| Title Large    | sans-serif-medium    |  0             |
| Body Large     | sans-serif           |  0.01          |

Time displays scale from `64sp` to `112sp` (`text_time_display_*` in [dimens.xml](../app/src/main/res/values/dimens.xml)). Negative letter spacing on display sizes keeps large numerals from feeling airy. Always lowercase or sentence-case button labels — `android:textAllCaps` is off everywhere.

## Shape, spacing, elevation

Sharp-but-soft. Radii are intentionally small for a precise, instrument-like feel:

- Small components: `4dp`
- Medium components: `6dp`
- Large components / cards: `8dp`
- FAB / pill controls: `28dp` (the only large radius)

Spacing follows the Bios-ecosystem canonical scale (see [Bios design-tokens/spacing.yaml](/home/mia/Bios/docs/design-tokens/spacing.yaml)):

```
4dp · 8dp · 12dp · 16dp · 24dp · 32dp · 48dp
```

48dp is the touch-target floor; 16dp is the default screen edge inset; 8dp is inter-item spacing in lists. Screen padding ladders 16/24/32 by breakpoint; card padding ladders 12/16/24/32. **Never** introduce a 5/10/18/20/28/40dp value — those break the ecosystem grid.

**Elevation is mostly zero.** Buttons, FAB, and cards declare `elevation 0dp`. Depth is conveyed by the sun-colored stroke (`card_border_minimal #FF8E53` at 0.5dp) on a black surface. Reserve real elevation tokens (`elevation_low 2dp` through `elevation_very_high 8dp`) for transient surfaces like dialogs and bottom sheets.

## Iconography & emoji

The app leans on **emoji glyphs** (🌅 🌄 ☀️ 🌇 🌆 🌌) at large display sizes (`emoji_size_*` in [dimens.xml](../app/src/main/res/values/dimens.xml), 32–56sp) for solar event labels. They render consistently across Android versions and reinforce the warm, human tone. Material vector icons are reserved for utility (settings, edit, delete, location).

## The solar ring

The solar ring (see [SolarRingView.kt](../app/src/main/java/com/rooster/rooster/ui/SolarRingView.kt) and [SolarRingTimePickerView.kt](../app/src/main/java/com/rooster/rooster/ui/SolarRingTimePickerView.kt)) is Rooster's identity. It appears on:

- **Home screen** — current state of the day with upcoming events marked
- **Time picker** — drag a handle around the ring to pick an alarm time, with the ring itself colored by solar phase at that hour
- **Launcher widget** ([SolarRingWidgetProvider.kt](../app/src/main/java/com/rooster/rooster/widget/SolarRingWidgetProvider.kt)) — the same ring, glanceable

Design rules:

- The ring's circumference represents 24 hours, midnight at the top, noon at the bottom (or the locale-equivalent orientation).
- Phase colors come from the astral journey palette — never invent new colors for arcs.
- The nine solar events are marked at their exact angular positions. Their order around the ring is the same order used by the audio and vibration signatures (see below) so all three modalities reinforce each other.

## Audio cue language

Solar event sounds are synthesized in [SolarEventTone.kt](../app/src/main/java/com/rooster/rooster/util/SolarEventTone.kt). The design rationale:

- **Voice family: struck singing-bowl.** A fundamental plus three partials (octave, octave-fifth, two-octaves), each decaying faster than the one below, gives a warm meditative bell. Same timbre family as our breathing-tones player elsewhere in the portfolio — cues feel meditative, not alarming.
- **Pitch palette: C major pentatonic across two octaves** (C3, G3, C4, E4, G4, A4, C5). Any combination is consonant, so cues can layer without dissonance.
- **Signatures encode the event:**
  - Dawn arpeggios **rise** (deep → bright)
  - Dusk arpeggios **fall** (bright → deep)
  - Sunrise / sunset are wide three-note peals
  - Solar noon is a tight high-pitched crown peal at the apex
  - Astronomical events use the deepest pitches at the extremes of the day
- **Length: 3–4 seconds total**, two or three notes max. Cues should feel like a passing chime, never a ringtone.
- **Audio attributes:** `USAGE_NOTIFICATION_EVENT` + `CONTENT_TYPE_SONIFICATION`. The cue respects Do Not Disturb and rides the notification volume slider, not the alarm slider.

The actual alarm — the thing that wakes you up — is louder, longer, and uses ringtone audio attributes; that's a different surface and intentionally distinct from event cues.

## Vibration cue language

Vibration signatures in [SolarEventVibration.kt](../app/src/main/java/com/rooster/rooster/util/SolarEventVibration.kt) **mirror the audio rhythms one-to-one**:

- Pulse counts match note counts (1 pulse for astronomical events, 2 for nautical, 3 for civil/sunrise/sunset/noon).
- Pulse amplitudes track the audio loudness arc: low at twilight edges (`AMP_LOW 80/255`), bright at the day's peaks (`AMP_HIGH 230/255`).
- Pulse durations: `SHORT 80ms` taps for fast climbs, `MEDIUM 180ms` pulses for normal events, `LONG 350ms` thrums for the deepest astronomical edges.

Result: a face-down phone delivers the same information as the speaker. Sound and vibration are independently toggleable from settings — choosing one does not silently suppress the other.

## Motion

- **Slow over snappy.** Solar phase transitions on the ring animate over hundreds of milliseconds, not tens. The app should never feel hurried.
- **Easing: standard Material easing** for incidental UI; **linear for time-tracking elements** (the ring's "current time" indicator advances linearly with the clock).
- **No bounce.** No spring overshoot, no rubber-banding. The aesthetic is astronomical, not playful.

## Surfaces & screens

Each screen is documented in [ARCHITECTURE.md](ARCHITECTURE.md#package-structure); design intent per surface:

- **Home / [MainActivity](../app/src/main/java/com/rooster/rooster/MainActivity.kt)** — solar ring centered, current event glyph + label below, minimal chrome.
- **Alarm list / [AlarmListActivity](../app/src/main/java/com/rooster/rooster/AlarmListActivity.kt)** — black canvas, alarms as bordered cards with the relative event ("30 min before sunrise") as the headline rather than the absolute time.
- **Alarm editor / [AlarmEditorActivity](../app/src/main/java/com/rooster/rooster/AlarmEditorActivity.kt)** — Compose-first ([ROADMAP.md](ROADMAP.md) tracks the migration), each setting on its own card with generous breathing room.
- **Time picker / [TimePickerActivity](../app/src/main/java/com/rooster/rooster/TimePickerActivity.kt)** — solar ring time picker by default; the wheel picker is the secondary affordance.
- **Alarm display / [AlarmActivity](../app/src/main/java/com/rooster/rooster/AlarmActivity.kt)** — full-screen, single time display at `text_time_display_xlarge` (112sp), dismiss/snooze affordances at the bottom edge of the screen so they require a deliberate reach.
- **Settings / [SettingsActivity](../app/src/main/java/com/rooster/rooster/SettingsActivity.kt)** — Compose, simple sectioned list. Solar event cues toggles live here.

## Contribution popup

A separate, portfolio-wide design contract — see the project memory and [PLAY_LISTING.md](PLAY_LISTING.md) for placement context. Hard rules: no Play Billing, no feature gates, no donor differentiation, three parallel buttons (donate / review / feedback), no "don't ask again" toggle.

## Accessibility

- All interactive controls meet 48dp minimum target size (`button_height 56dp`, `fab_size 56dp`, `list_item_height 72dp`).
- Color is never the sole information channel — solar events are also identified by glyph, label, audio signature, and vibration signature.
- TalkBack support is on the [ROADMAP.md](ROADMAP.md). When adding new surfaces, ensure content descriptions on the solar ring's interactive parts so the picker remains usable without sight.
- Dark theme contrast: text on black uses the warm palette (`#FFB86C`, `#FFD89C`) — verify ≥ 4.5:1 against `#000000` for body text and ≥ 3:1 for large display text.

## Compliance with the Bios ecosystem spec

Rooster ships inside the [Bios ecosystem design system](/home/mia/Bios/docs/DESIGN_SYSTEM.md). The shared spec governs structure (typography roles, spacing scale, semantic color roles, component anatomy, Bios-integration UI); each app keeps its own identity (palette, mode, voice).

**Adopted (no divergence)**
- **Spacing scale** — 4 / 8 / 12 / 16 / 24 / 32 / 48dp. Encoded in [dimens.xml](../app/src/main/res/values/dimens.xml).
- **Touch-target floor** — 48dp (`button_height`, `fab_size`, `list_item_height`).
- **Instrument readout** — mandatory cross-app monospace style for data-bearing text (times, durations, quantities). Implemented as `TextAppearance.Rooster.Instrument(.Small|.Large)` in [themes.xml](../app/src/main/res/values/themes.xml). Apply only to measurements, never to body copy.
- **Semantic accents** — `success`, `warning`, `error` defined in [colors.xml](../app/src/main/res/values/colors.xml).
- **RTL / accessibility floors** — `start`/`end` attributes throughout, WCAG AA contrast on warm-on-black pairs.

**Intentional divergences (identity-bound)**
- **Card corner radius.** Bios spec pins cards at 12 or 14dp. Rooster uses 4–8dp for a sharp, instrument-like feel — see "Shape, spacing, elevation" above. This is the central piece of Rooster's identity and is locked.
- **Card elevation.** Bios spec for dark themes says `elevation 1dp` with no stroke. Rooster uses `elevation 0dp` + 0.5dp sun-colored stroke on solid black — depth is conveyed by border, not tonal tint. Locked.
- **Mode.** Rooster locks to dark (`Theme.Material3.Dark`) so the cosmic-arc palette can carry the visual weight. Light tokens exist in [colors.xml](../app/src/main/res/values/colors.xml) but the app does not toggle to them.

**Not applicable**
- **Bios-integration UI block** (status pill, pending banner, "Open Bios" button). Rooster does not write to the Bios companion URI today; if/when it does, the Settings screen must adopt the block per the spec.

Rooster's identity-hue mapping is registered in [Bios design-tokens/colors.yaml](/home/mia/Bios/docs/design-tokens/colors.yaml) under `apps.rooster`.

## Adding new design elements

Before introducing a new color, shape, motion, or sound:

1. Can it reuse a token from the astral palette, an existing radius, or an existing audio voice? Use that.
2. If genuinely new, add it as a token in [colors.xml](../app/src/main/res/values/colors.xml), [dimens.xml](../app/src/main/res/values/dimens.xml), or [themes.xml](../app/src/main/res/values/themes.xml) — never inline.
3. Does it sit on the cosmic-journey arc (deep space → sunrise) or break it? Breaking the arc requires a deliberate reason documented here.
