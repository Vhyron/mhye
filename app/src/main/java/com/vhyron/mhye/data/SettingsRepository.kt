package com.vhyron.mhye.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Lead times offered in the UI. Zero means "on the renewal date itself". */
val REMINDER_DAY_OPTIONS = listOf(0, 1, 3, 7, 14, 30)

/** Sentinel for "don't remind me at all", stored like any other lead time. */
const val REMINDERS_OFF = -1

const val DEFAULT_REMINDER_DAYS = 3

/**
 * App-wide preferences. Only the default reminder lead time lives here — a
 * per-subscription override is stored on the subscription itself.
 */
class SettingsRepository(private val context: Context) {

    val defaultReminderDays: Flow<Int> = context.settingsDataStore.data
        .map { it[DEFAULT_REMINDER_DAYS_KEY] ?: DEFAULT_REMINDER_DAYS }

    suspend fun setDefaultReminderDays(days: Int) {
        context.settingsDataStore.edit { it[DEFAULT_REMINDER_DAYS_KEY] = days }
    }

    private companion object {
        val DEFAULT_REMINDER_DAYS_KEY = intPreferencesKey("default_reminder_days")
    }
}

/**
 * The lead time actually used for this subscription: its own override when
 * set, otherwise the app-wide default.
 */
fun Subscription.resolveReminderDays(defaultDays: Int): Int =
    reminderDaysBefore ?: defaultDays

/** Whether a reminder should exist at all for this subscription. */
fun Subscription.wantsReminder(defaultDays: Int): Boolean =
    status == SubscriptionStatus.ACTIVE && resolveReminderDays(defaultDays) != REMINDERS_OFF
