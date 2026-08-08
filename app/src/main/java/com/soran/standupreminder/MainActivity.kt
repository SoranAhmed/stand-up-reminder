package com.soran.standupreminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)
        NotificationHelper.createChannels(this)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    prefs = prefs,
                    onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
                    onRequestExactAlarmPermission = { requestExactAlarmPermissionIfNeeded() },
                    onRequestBatteryOptimizationExemption = { requestIgnoreBatteryOptimizations() },
                    onStart = { startReminders() },
                    onStop = { stopReminders() }
                )
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored — UI re-reads permission state on resume */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun startReminders() {
        val intent = Intent(this, ReminderService::class.java).apply {
            action = ReminderService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopReminders() {
        val intent = Intent(this, ReminderService::class.java).apply {
            action = ReminderService.ACTION_STOP
        }
        startService(intent)
    }
}

private val dayLabels = listOf(
    Calendar.SUNDAY to "Sun",
    Calendar.MONDAY to "Mon",
    Calendar.TUESDAY to "Tue",
    Calendar.WEDNESDAY to "Wed",
    Calendar.THURSDAY to "Thu",
    Calendar.FRIDAY to "Fri",
    Calendar.SATURDAY to "Sat"
)

@Composable
fun SettingsScreen(
    prefs: PrefsManager,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestBatteryOptimizationExemption: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val interval by prefs.intervalMinutesFlow.collectAsStateWithLifecycle(initialValue = 40)
    val snooze by prefs.snoozeMinutesFlow.collectAsStateWithLifecycle(initialValue = 5)
    val activeDays by prefs.activeDaysFlow.collectAsStateWithLifecycle(
        initialValue = dayLabels.map { it.first }.toSet()
    )
    val isRunning by prefs.isRunningFlow.collectAsStateWithLifecycle(initialValue = false)

    var intervalSlider by remember(interval) { mutableFloatStateOf(interval.toFloat()) }
    var snoozeSlider by remember(snooze) { mutableFloatStateOf(snooze.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Stand Up Reminder", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Reminds you with a full-screen alert to stand and walk, on the schedule you set below.",
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))
        Text("Reminder interval: ${intervalSlider.toInt()} minutes", fontWeight = FontWeight.SemiBold)
        Slider(
            value = intervalSlider,
            onValueChange = { intervalSlider = it },
            onValueChangeFinished = {
                scope.launch { prefs.setIntervalMinutes(intervalSlider.toInt()) }
            },
            valueRange = 20f..90f,
            steps = 13
        )

        Spacer(Modifier.height(16.dp))
        Text("Snooze duration: ${snoozeSlider.toInt()} minutes", fontWeight = FontWeight.SemiBold)
        Slider(
            value = snoozeSlider,
            onValueChange = { snoozeSlider = it },
            onValueChangeFinished = {
                scope.launch { prefs.setSnoozeMinutes(snoozeSlider.toInt()) }
            },
            valueRange = 1f..20f,
            steps = 18
        )

        Spacer(Modifier.height(16.dp))
        Text("Active days", fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach { (dayInt, label) ->
                val checked = activeDays.contains(dayInt)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 12.sp)
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            val newSet = if (isChecked) activeDays + dayInt else activeDays - dayInt
                            scope.launch { prefs.setActiveDays(newSet) }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("One-time setup", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Grant these once so alerts fire reliably, even in Doze mode or after the phone is idle for a while.",
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
            Text("Allow notifications")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRequestExactAlarmPermission, modifier = Modifier.fillMaxWidth()) {
            Text("Allow exact alarms")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRequestBatteryOptimizationExemption, modifier = Modifier.fillMaxWidth()) {
            Text("Disable battery optimization for this app")
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { if (isRunning) onStop() else onStart() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (isRunning) "Stop reminders" else "Start reminders", fontSize = 18.sp)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (isRunning) "Status: running" else "Status: stopped",
            fontWeight = FontWeight.Medium
        )
    }
}
