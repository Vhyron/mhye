package com.vhyron.mhye.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.vhyron.mhye.data.AppDatabase
import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderWorkerTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private val insertedIds = mutableListOf<Int>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() = runBlocking {
        notificationManager.cancelAll()
        val dao = AppDatabase.getInstance(context).subscriptionDao()
        insertedIds.forEach { id -> dao.getById(id)?.let { dao.delete(it) } }
        insertedIds.clear()
    }

    @Test
    fun postsNotificationForActiveSubscription() = runBlocking {
        val id = insert(status = SubscriptionStatus.ACTIVE)

        val result = runWorker(id)

        assertEquals(ListenableWorker.Result.success(), result)
        val posted = notificationManager.activeNotifications
        assertEquals(1, posted.size)
        assertTrue(
            posted.single().notification.extras.getString("android.title")!!
                .contains("Reminder Test")
        )
    }

    @Test
    fun skipsPausedSubscription() = runBlocking {
        val id = insert(status = SubscriptionStatus.PAUSED)

        val result = runWorker(id)

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, notificationManager.activeNotifications.size)
    }

    @Test
    fun succeedsQuietlyWhenSubscriptionWasDeleted() = runBlocking {
        val result = runWorker(subscriptionId = 999_999)

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, notificationManager.activeNotifications.size)
    }

    @Test
    fun failsWithoutASubscriptionId() = runBlocking {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()

        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }

    private suspend fun runWorker(subscriptionId: Int): ListenableWorker.Result =
        TestListenableWorkerBuilder<ReminderWorker>(context)
            .setInputData(workDataOf(ReminderWorker.KEY_SUBSCRIPTION_ID to subscriptionId))
            .build()
            .doWork()

    private suspend fun insert(status: String): Int {
        val dao = AppDatabase.getInstance(context).subscriptionDao()
        val categoryId = AppDatabase.getInstance(context).categoryDao().insert(
            com.vhyron.mhye.data.Category(name = "Reminder Test Category", colorHex = "#FF0000")
        ).toInt()
        val id = dao.insert(
            Subscription(
                name = "Reminder Test",
                cost = 199.0,
                currency = "PHP",
                billingCycle = BillingCycle.MONTHLY,
                renewalDate = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L,
                categoryId = categoryId,
                status = status
            )
        ).toInt()
        insertedIds += id
        return id
    }
}
