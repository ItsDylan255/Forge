# Forge

**Forge** is an offline-first Android fitness tracker for weight, steps, nutrition and gym training. Everything is stored locally on your device. No account, no cloud, no ads.

> The app is available in **German** and **English**. It follows your phone's language, or you can pick one under **Settings → Language**.

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

### Wear OS companion (e.g. Galaxy Watch)
A companion app for the watch that shows what's happening on your phone. All input stays on the phone.
- **Workout on your wrist:** opens automatically when you start a workout on the phone, and shows the current exercise, set, target reps, your last performance and new 1RM records
- **Rest timer** with a progress ring that vibrates on your wrist when the rest is over. **+30 s** and **skip** are the only buttons on the watch
- **Ongoing activity:** an icon on the watch face takes you back to the workout at any time
- **Home screen and tile** with today's steps, calories eaten versus your daily target, and weight
- **Complication** that shows today's steps on your watch face
- Uses the same language as the phone app

### Data export
Export all your data as a ZIP file of CSV tables (**Settings → Data → Export**). File and column names are German in both languages, so scripts keep working:

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

1. Download `forge-1.1.1.apk` from the [latest release](https://github.com/ItsDylan255/Forge/releases/latest).
2. Open it on your Android phone and allow installing apps from this source.

Requires **Android 8.0 (API 26)** or newer.

### Watch app (optional)

Requires **Wear OS 3** or newer (e.g. Galaxy Watch 4 and later). The watch app isn't on the Play Store, so you install `forge-watch-1.1.1.apk` from the release over Wi-Fi debugging:

1. On the watch, enable developer options: **Settings → About watch → Software**, tap **Software version** 7 times.
2. Under **Settings → Developer options**, turn on **ADB debugging** and **Wireless debugging**, then tap **Pair new device**.
3. On your computer (watch and computer on the same Wi-Fi):
   ```bash
   adb pair <ip>:<pairing-port>     # code shown on the watch
   adb connect <ip>:<port>          # address shown under Wireless debugging
   adb install forge-watch-1.1.1.apk   # from the latest release
   ```

Phone and watch app must be signed with the same key, otherwise they can't talk to each other.

## Building from source

Requirements: Android Studio (or the Android SDK) and JDK 17+.

```bash
git clone https://github.com/ItsDylan255/Forge.git
cd Forge
./gradlew assembleDebug        # phone APK in app/build/outputs/apk/debug/, watch APK in wear/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Room (SQLite) with versioned migrations
- WorkManager for reminders, foreground services for workouts and step counting
- Charts drawn with Compose Canvas, no chart library
- Watch: Compose for Wear OS, Tiles, Complications, Ongoing Activities and the Wearable Data Layer

## Project structure

```
app/src/main/java/com/example/fitnesstracker/
├── data/       Room entities, DAOs, repositories, preferences, export
├── i18n/       App language (German/English) and the tr() helper
├── domain/     Pure Kotlin logic (calculations, parsing, formatting), unit-tested
├── service/    Workout and step services, reminders, notifications
├── ui/         Compose screens per area (gym, weight, steps, food, settings, ...)
└── wear/       Sync with the watch over the Wearable Data Layer

wear/src/main/java/com/example/fitnesstracker/watch/
                Watch app: workout and rest screen, tile, complication
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

Forge has no internet permission. Your data never leaves your device unless you export it yourself. If you use the watch app, the phone sends the current workout and today's numbers to your watch over Bluetooth (via the Wearable Data Layer of Google Play services). The watch only keeps today's numbers for the tile and doesn't have its own database.
