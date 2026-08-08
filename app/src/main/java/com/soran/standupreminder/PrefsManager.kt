package com.soran.standupreminder

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Days are stored as Calendar.DAY_OF_WEEK ints (1=Sunday .. 7=Saturday) in string form,
 * since DataStore preferences don't support Set<Int> directly.
 */
object PrefsKeys {
    val INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
    val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
    val ACTIVE_DAYS = stringSetPreferencesKey("active_days")
    val IS_RUNNING = booleanPreferencesKey("is_running")
}

class PrefsManager(private val context: Context) {

    val intervalMinutesFlow: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.INTERVAL_MINUTES] ?: 40
    }

    val snoozeMinutesFlow: Flow<Int> = context.dataStore.data.map {
        it[PrefsKeys.SNOOZE_MINUTES] ?: 5
    }

    val activeDaysFlow: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[PrefsKeys.ACTIVE_DAYS]?.map { it.toInt() }?.toSet()
            ?: setOf(
                Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
    }

    val isRunningFlow: Flow<Boolean> = context.dataStore.data.map {
        it[PrefsKeys.IS_RUNNING] ?: false
    }

    suspend fun setIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[PrefsKeys.INTERVAL_MINUTES] = minutes }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[PrefsKeys.SNOOZE_MINUTES] = minutes }
    }

    suspend fun setActiveDays(days: Set<Int>) {
        context.dataStore.edit { it[PrefsKeys.ACTIVE_DAYS] = days.map { d -> d.toString() }.toSet() }
    }

    suspend fun setIsRunning(running: Boolean) {
        context.dataStore.edit { it[PrefsKeys.IS_RUNNING] = running }
    }

    // Blocking-style helpers for use from non-suspend contexts (receivers/services).
    fun getIntervalMinutesBlocking(): Int = kotlinx.coroutines.runBlocking { intervalMinutesFlow.first() }
    fun getSnoozeMinutesBlocking(): Int = kotlinx.coroutines.runBlocking { snoozeMinutesFlow.first() }
    fun getActiveDaysBlocking(): Set<Int> = kotlinx.coroutines.runBlocking { activeDaysFlow.first() }
    fun getIsRunningBlocking(): Boolean = kotlinx.coroutines.runBlocking { isRunningFlow.first() }
}
