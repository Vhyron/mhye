package com.vhyron.mhye.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vhyron.mhye.data.AppDatabase
import com.vhyron.mhye.data.SubscriptionStatus

/**
 * Posts a renewal reminder for one subscription. Re-reads the row at run time
 * rather than trusting the values captured at scheduling time, so a paused or
 * deleted subscription can't fire a stale notification.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val subscriptionId = inputData.getInt(KEY_SUBSCRIPTION_ID, INVALID_ID)
        if (subscriptionId == INVALID_ID) return Result.failure()

        val subscription = AppDatabase.getInstance(applicationContext)
            .subscriptionDao()
            .getById(subscriptionId)

        // Deleted or no longer active — nothing to remind about, but not a failure.
        if (subscription == null || subscription.status != SubscriptionStatus.ACTIVE) {
            return Result.success()
        }

        showRenewalNotification(applicationContext, subscription)
        return Result.success()
    }

    companion object {
        const val KEY_SUBSCRIPTION_ID = "subscriptionId"
        private const val INVALID_ID = -1
    }
}
