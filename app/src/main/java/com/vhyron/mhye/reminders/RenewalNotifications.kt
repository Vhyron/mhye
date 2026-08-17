package com.vhyron.mhye.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vhyron.mhye.MainActivity
import com.vhyron.mhye.R
import com.vhyron.mhye.data.Subscription
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val CHANNEL_ID = "renewals"

/**
 * Posts the "renews soon" notification. Silently does nothing when the user
 * hasn't granted POST_NOTIFICATIONS — the reminder is a convenience, not
 * something worth surfacing an error for.
 */
fun showRenewalNotification(context: Context, subscription: Subscription) {
    createChannel(context)

    // Inlined rather than extracted so lint can see the guard on notify() below.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val openApp = PendingIntent.getActivity(
        context,
        subscription.id,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("${subscription.name} renews soon")
        .setContentText(notificationText(subscription))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(openApp)
        .build()

    NotificationManagerCompat.from(context).notify(subscription.id, notification)
}

private fun notificationText(subscription: Subscription): String {
    val date = Instant.ofEpochMilli(subscription.renewalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val cost = String.format(Locale.getDefault(), "%s %,.2f", subscription.currency, subscription.cost)
    return "$cost on $date"
}

private fun createChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Renewal reminders",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Reminders before a subscription renews"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
