package com.vhyron.mhye.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderLeadTimeTest {

    @Test
    fun `no override follows the app-wide default`() {
        assertEquals(7, subscription(reminderDaysBefore = null).resolveReminderDays(7))
    }

    @Test
    fun `an override beats the default`() {
        assertEquals(30, subscription(reminderDaysBefore = 30).resolveReminderDays(7))
    }

    @Test
    fun `zero is a real lead time, not an absent one`() {
        // Guards against a null-vs-zero mixup: 0 means "on the renewal date".
        assertEquals(0, subscription(reminderDaysBefore = 0).resolveReminderDays(7))
    }

    @Test
    fun `an active subscription with a lead time wants a reminder`() {
        assertTrue(subscription(reminderDaysBefore = 3).wantsReminder(7))
        assertTrue(subscription(reminderDaysBefore = null).wantsReminder(7))
    }

    @Test
    fun `turning reminders off suppresses them per subscription`() {
        assertFalse(subscription(reminderDaysBefore = REMINDERS_OFF).wantsReminder(7))
    }

    @Test
    fun `turning the default off suppresses everything that follows it`() {
        assertFalse(subscription(reminderDaysBefore = null).wantsReminder(REMINDERS_OFF))
        // ...but an explicit override still wins.
        assertTrue(subscription(reminderDaysBefore = 3).wantsReminder(REMINDERS_OFF))
    }

    @Test
    fun `paused and cancelled subscriptions never want reminders`() {
        assertFalse(
            subscription(status = SubscriptionStatus.PAUSED, reminderDaysBefore = 3)
                .wantsReminder(7)
        )
        assertFalse(
            subscription(status = SubscriptionStatus.CANCELLED, reminderDaysBefore = 3)
                .wantsReminder(7)
        )
    }

    @Test
    fun `backups carry the override across a round trip`() {
        val backup = buildBackup(
            categories = listOf(Category(id = 1, name = "A", colorHex = "#FFFFFF")),
            subscriptions = listOf(
                subscription(reminderDaysBefore = 30),
                subscription(reminderDaysBefore = null)
            ),
            exportedAt = 0L
        )

        val restored = backupJson
            .decodeFromString<Backup>(backupJson.encodeToString(backup))
            .subscriptions

        assertEquals(Backup.CURRENT_VERSION, 2)
        assertEquals(30, restored.first().reminderDaysBefore)
        assertNull(restored.last().reminderDaysBefore)
    }

    @Test
    fun `a version 1 backup still restores, with no overrides`() {
        // Written before reminderDaysBefore existed — must not fail to decode.
        val v1 = """
            {
              "version": 1,
              "exportedAt": 5,
              "categories": [{"id": 1, "name": "A", "colorHex": "#FFFFFF"}],
              "subscriptions": [{
                "name": "Netflix", "cost": 549.0, "currency": "PHP",
                "billingCycle": "MONTHLY", "renewalDate": 0,
                "categoryId": 1, "status": "ACTIVE"
              }]
            }
        """.trimIndent()

        val backup = backupJson.decodeFromString<Backup>(v1)

        assertEquals(1, backup.subscriptions.size)
        assertNull(backup.subscriptions.single().reminderDaysBefore)
    }

    private fun subscription(
        status: String = SubscriptionStatus.ACTIVE,
        reminderDaysBefore: Int? = null
    ) = Subscription(
        name = "Test",
        cost = 100.0,
        currency = "PHP",
        billingCycle = BillingCycle.MONTHLY,
        renewalDate = 0L,
        categoryId = 1,
        status = status,
        reminderDaysBefore = reminderDaysBefore
    )
}
