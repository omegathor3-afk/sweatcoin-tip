# MotionLab

Android app for local step-rate simulation and device sensor diagnostics.

## Features
- Adjustable local simulated cadence profile (1–20 km/h).
- Start, stop, and reset controls.
- Detects whether common hardware sensors are present.
- Clearly separates simulated counters from Android's real sensor stream.

This app does not inject fabricated sensor events into third-party apps or bypass their activity verification.

## Build
Open the repository in Android Studio and build the :app debug variant with JDK 17 and Android SDK 35.
