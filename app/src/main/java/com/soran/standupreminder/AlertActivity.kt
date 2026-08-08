package com.soran.standupreminder

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure it shows over the lock screen and turns the screen on,
        // in addition to the manifest theme flags (belt-and-suspenders for older OEM skins).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Clear the notification since the full-screen UI is now showing.
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ReminderService.NOTIFICATION_ID + 1)

        val prefs = PrefsManager(this)

        setContent {
            var snoozeMinutes by remember { mutableIntStateOf(5) }

            LaunchedEffect(Unit) {
                snoozeMinutes = prefs.snoozeMinutesFlow.first()
            }

            MaterialTheme {
                AlertScreen(
                    snoozeMinutes = snoozeMinutes,
                    onDismiss = { finish() },
                    onSnooze = { minutes ->
                        AlarmScheduler.scheduleNext(this, minutes)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AlertScreen(
    snoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(60) }

    DisposableEffect(Unit) {
        val timer = object : CountDownTimer(60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsLeft = (millisUntilFinished / 1000).toInt()
            }
            override fun onFinish() {
                secondsLeft = 0
            }
        }
        timer.start()
        onDispose { timer.cancel() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1B5E20)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Time to stand up!",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Walk around for at least 1 minute to protect your health.",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (secondsLeft > 0) "$secondsLeft" else "Done!",
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (secondsLeft > 0) "seconds remaining" else "Great job",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Done, dismiss", color = Color(0xFF1B5E20), fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onSnooze(snoozeMinutes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Snooze $snoozeMinutes min (e.g. in a meeting)", fontSize = 16.sp)
            }
        }
    }
}
