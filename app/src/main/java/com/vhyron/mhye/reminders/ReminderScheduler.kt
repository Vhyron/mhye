package com.vhyron.mhye.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.resolveReminderDays
import com.vhyron.mhye.data.wantsReminder
import java.util.concurrent.TimeUnit

/**
 * Schedules one reminder per subscription, keyed by a unique work name so a
 * re-save replaces the pending reminder instead of stacking another one.
 *
 * WorkManager persists its queue across reboots, so nothing needs rescheduling
 * on BOOT_COMPLETED.
 */
object ReminderScheduler {

    /**
     * [defaultDaysBefore] is the app-wide setting; a subscription's own
     * [Subscription.reminderDaysBefore] wins when set. Either may be
     * [REMINDERS_OFF], which cancels instead of scheduling.
     */
    fun schedule(context: Context, subscription: Subscription, defaultDaysBefore: Int) {
        if (!subscription.wantsReminder(defaultDaysBefore)) {
            cancel(context, subscription.id)
            return
        }
        val daysBefore = subscription.resolveReminderDays(defaultDaysBefore)

        val delayMillis = subscription.renewalDate -
            TimeUnit.DAYS.toMillis(daysBefore.toLong()) -
            System.currentTimeMillis()

        // Renewal is already within the reminder window (or past) — nothing to schedule.
        if (delayMillis <= 0) {
            cancel(context, subscription.id)
            return
        }

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_SUBSCRIPTION_ID to subscription.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(subscription.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, subscriptionId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(subscriptionId))
    }

    private fun workName(subscriptionId: Int) = "renewal-reminder-$subscriptionId"
}
