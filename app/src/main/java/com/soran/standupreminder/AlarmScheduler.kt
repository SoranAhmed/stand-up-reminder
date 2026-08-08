package com.soran.standupreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    const val REQUEST_CODE = 1001

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /**
     * Schedules the next reminder `minutesFromNow` minutes from now.
     * If the resulting day-of-week isn't in activeDays, it keeps rolling forward
     * day by day (same time) until it lands on an active day.
     */
    fun scheduleNext(context: Context, minutesFromNow: Int) {
        val prefs = PrefsManager(context)
        val activeDays = prefs.getActiveDaysBlocking()

        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutesFromNow)
        }

        // Roll forward to the next active day if needed, keeping the same time-of-day,
        // in case the interval pushes us into a day the user excluded.
        if (activeDays.isNotEmpty()) {
            var guard = 0
            while (activeDays.none { it == cal.get(Calendar.DAY_OF_WEEK) } && guard < 8) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                guard++
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                // Fallback: inexact, still fires eventually.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }
}
