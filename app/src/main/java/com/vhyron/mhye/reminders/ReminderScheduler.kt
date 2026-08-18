package com.vhyron.mhye.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import java.util.concurrent.TimeUnit

/**
 * Schedules one reminder per subscription, keyed by a unique work name so a
 * re-save replaces the pending reminder instead of stacking another one.
 *
 * WorkManager persists its queue across reboots, so nothing needs rescheduling
 * on BOOT_COMPLETED.
 */
object ReminderScheduler {

    /** How far ahead of the renewal date the reminder fires. */
    const val DAYS_BEFORE_RENEWAL = 3L

    fun schedule(context: Context, subscription: Subscription) {
        if (subscription.status != SubscriptionStatus.ACTIVE) {
            cancel(context, subscription.id)
            return
        }

        val delayMillis = subscription.renewalDate -
            TimeUnit.DAYS.toMillis(DAYS_BEFORE_RENEWAL) -
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
