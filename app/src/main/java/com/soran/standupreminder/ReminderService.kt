package com.soran.standupreminder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderService : Service() {

    companion object {
        const val ACTION_START = "com.soran.standupreminder.action.START"
        const val ACTION_STOP = "com.soran.standupreminder.action.STOP"
        const val NOTIFICATION_ID = 1
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AlarmScheduler.cancel(this)
                scope.launch { PrefsManager(this@ReminderService).setIsRunning(false) }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildServiceNotification())
                scope.launch {
                    val prefs = PrefsManager(this@ReminderService)
                    prefs.setIsRunning(true)
                    val interval = prefs.intervalMinutesFlow.first()
                    AlarmScheduler.scheduleNext(this@ReminderService, interval)
                }
                return START_STICKY
            }
        }
    }

    private fun buildServiceNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NotificationHelper.SERVICE_CHANNEL_ID)
            .setContentTitle("Stand Up Reminder is active")
            .setContentText("You'll get a full-screen alert to stand and walk on schedule.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
