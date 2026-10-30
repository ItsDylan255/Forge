# Forge

**Forge** is an offline-first Android fitness tracker for weight, steps, nutrition and gym training. Everything is stored locally on your device. No account, no cloud, no ads.

> The app's user interface is in German.

## Features

### Gym
- **Exercise library** with muscle groups and three exercise types: reps, time (e.g. plank) and assisted (e.g. assisted pull-ups)
- **Routines** with planned sets, rep ranges, rest times, unilateral exercises and supersets
- **Training plans** that group routines (e.g. *Leg Day 1* and *Leg Day 2* belong to *Fitnessbasic*), with a custom order
- **Plan progress:** current week, completed rounds, last workout, and how many of the plan's routines you've done this week
- **Duplicate plans** as a template, optionally as a deload (one set less per exercise)
- **Archive** routines and plans without losing any history, and restore them at any time
- **Live workouts** with rest timer, notification, sound and vibration, the last performance of each exercise and progression hints
- **Estimated 1RM** (Epley formula) per exercise, with a progress chart, a percentage table and a "new record" hint during your workout
- **History and progress:** volume per week and per session, personal bests, per-exercise charts

### Weight
- Daily weigh-ins with a goal weight, a target date and a forecast
- Chart with 7-day, 30-day, 90-day, 1-year and all-time views. **Swipe left and right** to move through time
- Daily reminder at a time you choose, only sent if you haven't logged your weight yet

### Steps
- Background step counting with the hardware step counter, with correct handling of reboots and midnight
- Daily goal and bar chart (7 or 30 days) that you can swipe through

### Nutrition
- Food database with portions, meals made of several ingredients, a food diary with calories and macros
- Calorie target based on your profile (height, age, sex, activity level, macro split)

### Data export
Export all your data as a ZIP file of CSV tables (**Settings → Daten → Exportieren**):

| File | Contents |
|---|---|
| `tage.csv` | One row per day: weight, steps, calories, macros, workouts, sets, volume |
| `gewicht.csv` | All weigh-ins |
| `schritte.csv` | Steps per day |
| `ernaehrung.csv` | Every food diary entry with nutrients |
| `training_einheiten.csv` | Every finished workout with plan, duration and volume |
| `training_saetze.csv` | Every logged set |
| `LIESMICH.txt` | Description of all files |

The files are standard CSV (UTF-8, comma-separated, dot as decimal separator, ISO dates), so Excel, Google Sheets, Python and R can read them directly:

```python
import pandas as pd
days = pd.read_csv("tage.csv", parse_dates=["datum"])
```

## Installation

1. Download `forge-1.0.0.apk` from the [latest release](https://github.com/ItsDylan255/Forge/releases/latest).
2. Open it on your Android phone and allow installing apps from this source.

Requires **Android 8.0 (API 26)** or newer.

## Building from source

Requirements: Android Studio (or the Android SDK) and JDK 17+.

```bash
git clone https://github.com/ItsDylan255/Forge.git
cd Forge
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Room (SQLite) with versioned migrations
- WorkManager for reminders, foreground services for workouts and step counting
- Charts drawn with Compose Canvas, no chart library

## Project structure

```
app/src/main/java/com/example/fitnesstracker/
├── data/       Room entities, DAOs, repositories, preferences, export
├── domain/     Pure Kotlin logic (calculations, parsing, formatting), unit-tested
├── service/    Workout and step services, reminders, notifications
└── ui/         Compose screens per area (gym, weight, steps, food, settings, ...)
```

All calculations live in `domain/` and don't depend on Android, so they're covered by plain JUnit tests in `app/src/test/`.

## Permissions

| Permission | Why |
|---|---|
| Physical activity | Read the step counter |
| Notifications | Workout timer, step counter and weight reminder |
| Foreground service | Keep the rest timer and step counting running |
| Run at startup | Restart step counting after a reboot |
| Ignore battery optimizations (optional) | More reliable step counting on some devices |

## Privacy

Forge has no internet permission. Your data never leaves your device unless you export it yourself.
