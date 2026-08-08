package com.soran.standupreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val SERVICE_CHANNEL_ID = "service_channel"
    const val ALERT_CHANNEL_ID = "alert_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Reminder running",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows while Stand Up Reminder is active"
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Stand up alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full-screen alert telling you to stand up and walk"
            enableVibration(true)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
    }
}
