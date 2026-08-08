# Stand Up Reminder

A minimal Android app that replaces 25+ manual alarms with one repeating,
full-screen "stand up and walk" reminder — built per your requirements:

- Configurable interval (default 40 min, adjustable 20–90 min)
- Runs continuously all day (no fixed work-hours window)
- Full-screen alert you must actively dismiss (like an alarm clock, not a swipeable notification)
- Pick which days of the week it's active
- No tracking/stats — just the reminder
- Snooze option (default 5 min, adjustable) for meetings etc.
- Kotlin + Jetpack Compose, minSdk 26 (Android 8.0+)

## How to open

1. Open Android Studio (Koala/2024.1+ recommended).
2. **File → Open** → select this `StandUpReminder` folder.
3. Let Gradle sync (it will download dependencies from Google/Maven — requires internet).
4. Run on a device or emulator running Android 8.0+.

## How it works

- `MainActivity` — Compose settings screen: interval slider, snooze slider,
  day-of-week checkboxes, Start/Stop, and one-time permission buttons.
- `ReminderService` — a foreground service that keeps the cycle alive and
  survives Doze/app-kill better than a plain background timer.
- `AlarmScheduler` — schedules the next tick with `AlarmManager.setExactAndAllowWhileIdle`,
  skipping days you've turned off.
- `AlarmReceiver` — fires when it's time: shows a full-screen intent notification,
  launches `AlertActivity` directly, and immediately schedules the next cycle.
- `AlertActivity` — the full-screen "Time to stand up!" screen with a 60-second
  countdown, a Dismiss button, and a Snooze button.
- `BootReceiver` — restarts the service after a phone reboot if it was running.

## Important setup step on first run

Tap the three buttons under **"One-time setup"** in the app:
1. **Allow notifications** (Android 13+ requires this permission)
2. **Allow exact alarms** (Android 12+ requires this for precise timing)
3. **Disable battery optimization for this app** — on many phones (especially
   Xiaomi, Huawei, Samsung, OnePlus), Android will silently kill the reminder
   service or delay alarms unless you exempt the app from battery optimization.
   Without this step, reminders may become unreliable after a few hours.

Then press **Start reminders**.

## Notes / possible next steps (not requested, but worth knowing)

- Some OEMs (Xiaomi/MIUI, Huawei, Oppo/ColorOS) have an *additional*, separate
  "autostart" or "protected apps" setting beyond stock battery optimization —
  if reminders stop firing after a day on one of these phones, that's usually why.
- The full-screen alert currently always plays the default notification sound/vibration
  via the high-importance channel; if you want it silent/vibrate-only, that's a
  one-line change in `NotificationHelper`.
