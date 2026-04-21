# MedTracker

💊 **MedTracker** — A simple medication reminder and tracking app for Android.

## Features

- **Add Medications** — Name, dosage, frequency, time of day
- **Daily Reminders** — AlarmManager-based notifications for each dose
- **Mark Doses** — Taken, Skipped, or Missed tracking
- **Refill Tracking** — Remaining pill count with low-supply alerts
- **Drug Interactions** — Built-in database of common drug interaction warnings
- **Calendar View** — See your medication schedule by date
- **History Log** — Complete dose history with JSON export
- **Dark Theme** — Material Design 3 dark theme throughout
- **About Section** — Version info, update check, share button

## Technical Stack

- Kotlin + AndroidX
- Room database (all DB ops on Dispatchers.IO)
- AlarmManager for reminders
- NotificationCompat for dose reminders
- Material Design 3 (dark theme)
- Coroutines + Flow for reactive data

## Download

Download the latest APK from [GitHub Releases](https://github.com/jnetai-clawbot/MedTracker/releases/latest).

## License

MIT