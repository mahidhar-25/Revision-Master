package com.example.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "revision_master_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val STUDY_STREAK = intPreferencesKey("study_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val LAST_STUDY_DATE = stringPreferencesKey("last_study_date")
        val STUDY_TIME_MS = longPreferencesKey("study_time_ms")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    }

    val studyStreakFlow: Flow<Int> = context.dataStore.data.map { it[STUDY_STREAK] ?: 0 }
    val longestStreakFlow: Flow<Int> = context.dataStore.data.map { it[LONGEST_STREAK] ?: 0 }
    val lastStudyDateFlow: Flow<String> = context.dataStore.data.map { it[LAST_STUDY_DATE] ?: "" }
    val totalStudyTimeFlow: Flow<Long> = context.dataStore.data.map { it[STUDY_TIME_MS] ?: 0L }
    val reminderHourFlow: Flow<Int> = context.dataStore.data.map { it[REMINDER_HOUR] ?: 9 }
    val reminderMinuteFlow: Flow<Int> = context.dataStore.data.map { it[REMINDER_MINUTE] ?: 0 }
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val darkModeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE_ENABLED] ?: false }

    suspend fun saveStreak(streak: Int) {
        context.dataStore.edit { prefs ->
            prefs[STUDY_STREAK] = streak
            val longest = prefs[LONGEST_STREAK] ?: 0
            if (streak > longest) {
                prefs[LONGEST_STREAK] = streak
            }
        }
    }

    suspend fun saveLastStudyDate(dateStr: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_STUDY_DATE] = dateStr
        }
    }

    suspend fun incrementStudyTime(ms: Long) {
        context.dataStore.edit { prefs ->
            val cur = prefs[STUDY_TIME_MS] ?: 0L
            prefs[STUDY_TIME_MS] = cur + ms
        }
    }

    suspend fun saveReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_HOUR] = hour
            prefs[REMINDER_MINUTE] = minute
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun toggleDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_ENABLED] = enabled
        }
    }
}
