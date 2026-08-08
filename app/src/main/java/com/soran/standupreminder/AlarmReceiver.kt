package com.soran.standupreminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PrefsManager(context)

        // Only actually alert if the reminder cycle is still turned on
        // (guards against a stray alarm firing after the user stopped it).
        if (!prefs.getIsRunningBlocking()) return

        NotificationHelper.createChannels(context)

        val fullScreenIntent = Intent(context, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, 2001, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.ALERT_CHANNEL_ID)
            .setContentTitle("Time to stand up")
            .setContentText("Stand and walk for at least 1 minute.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(ReminderService.NOTIFICATION_ID + 1, notification)

        // Also try to launch it directly — works reliably when the app/service
        // is in the foreground or the device allows background activity starts.
        context.startActivity(fullScreenIntent)

        // Immediately queue the next cycle so it keeps repeating.
        val interval = prefs.getIntervalMinutesBlocking()
        AlarmScheduler.scheduleNext(context, interval)
    }
}
