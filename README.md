# Velocity — Android GPS Speedometer

Velocity is a modern, offline-first Android GPS speedometer and trip telemetry application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Hilt**, **Room**, **DataStore**, **Coroutines/Flow**, **FusedLocationProviderClient**, and **Google Maps Compose** (with automatic **osmdroid** fallback).

---

## Features

- **Live GPS Speed & Smoothing**: High-accuracy (`500 ms` default, configurable to `1 s` or `2 s`) GPS speed filtered through a 1D Kalman filter (`SpeedSmoother`) with `> 30 m` accuracy rejection and outlier acceleration filtering.
- **Analog & Digital Modes**: Swipe left/right between a custom Compose `Canvas` 270° analog gauge (spring-animated needle, glow gradient arc, max-speed violet marker, speed-limit red band) and a bold `96 sp` Orbitron digital display.
- **Units**: `km/h`, `mph`, and `knots` — switchable in Settings or by tapping the unit label directly.
- **Background Trip Recording**: `TrackingForegroundService` (`foregroundServiceType="location"`) with live speed/distance notification and Pause/Resume/Stop actions, plus process-death state recovery.
- **Speed Limit Alerts**: Configurable speed limit with warning zone (`>= 90%`), pulsing red over-limit state, vibration, and optional beep tone.
- **Windshield HUD Mode**: Full-screen `#000000` mirrored high-contrast night display (`#00E5FF`, `#FF1744` over limit).
- **Trip History & Detail**: Saved routes with Google Maps Compose or OpenStreetMap (`osmdroid`) fallback, speed-over-time `Canvas` chart, single/bulk delete with confirmation, and **GPX / CSV** share-sheet export.
- **Odometer & Compass**: Lifetime odometer stored in DataStore, 8-point compass heading, and color-coded GPS accuracy pill (`< 10 m` green, `10–30 m` amber, `> 30 m` / no fix red).

---

## Prerequisites

- **Android Studio Ladybug (2024.2.1)+** or **JDK 17** + **Android SDK 35**
- Android device or emulator with **API 26+** (Android 8.0 Oreo or newer) and Google Play Services

---

## Google Maps API Key Setup (Optional)

Trip Detail maps automatically use **Google Maps Compose** when a Google Maps API key is configured, and fall back seamlessly to **OpenStreetMap (`osmdroid`)** when no key is present.

1. Obtain an Android API key from the [Google Cloud Console](https://console.cloud.google.com/) with **Maps SDK for Android** enabled (restrict the key by your app package name and SHA-1 fingerprint).
2. Open (or create) `local.properties` in the **project root** (this file is listed in `.gitignore` and must **never** be committed to version control).
3. Add your key:

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_ANDROID_API_KEY
```

---

## Build & Test Commands

```bash
# Run JVM unit tests (speed conversion, Kalman filter, geo math, ViewModel)
gradle testDebugUnitTest

# Run Android Lint
gradle lintDebug

# Build the Debug APK (outputs to app/build/outputs/apk/debug/)
gradle assembleDebug

# Run Compose UI instrumentation tests (requires connected device/emulator)
gradle connectedDebugAndroidTest
```

*(If opening in Android Studio, sync Gradle and click **Run** or **Build > Build Bundle(s) / APK(s) > Build APK(s)**. GitHub Actions in `.github/workflows/android-ci.yml` also runs unit tests, Lint, and `assembleDebug` on every push to `main`.)*

---

## Architecture Overview

Velocity follows **MVVM + Clean Layers** in a single-activity architecture:

- **`domain/`**: Pure Kotlin models (`Trip`, `TrackPoint`, `UserSettings`, `LiveTelemetry`), repository/service contracts (`SettingsRepository`, `TripRepository`, `TrackingController`), and stateless utilities (`SpeedConverter`, `SpeedSmoother`, `GeoUtils`, `TripExporter`). Speeds are stored internally in `m/s` and converted strictly in the presentation layer.
- **`data/`**: Room database (`VelocityDatabase`, `TripDao`, `TripEntity`, `TrackPointEntity` with `CASCADE` delete), Preferences DataStore (`SettingsDataStoreRepository` including active-trip process-death snapshots and odometer), and `FusedLocationSource` (`callbackFlow` over `FusedLocationProviderClient`).
- **`service/`**: `TrackingManager` (singleton controller maintaining live `StateFlow<LiveTelemetry>`, Kalman smoothing, auto-pause, odometer updates, and Room writes), `TrackingForegroundService` (Android 14+ location foreground service with notification controls), and `SpeedAlertManager`.
- **`ui/`**: Jetpack Compose Material 3 screens (`PermissionScreen`, `SpeedometerScreen`, `HudScreen`, `HistoryScreen`, `TripDetailScreen`, `SettingsScreen`, `AboutScreen`), custom `Canvas` components (`AnalogGauge`, `SpeedTimeChart`), `TripRouteMap` (Google Maps Compose + `osmdroid` fallback), and `VelocityNavGraph` with `250 ms` fade/slide transitions.
