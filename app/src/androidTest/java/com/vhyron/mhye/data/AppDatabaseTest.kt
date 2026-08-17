package com.vhyron.mhye.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var subscriptionDao: SubscriptionDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).addCallback(AppDatabase.SEED_DEFAULT_CATEGORY).build()
        categoryDao = db.categoryDao()
        subscriptionDao = db.subscriptionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun seedsDefaultCategoryOnCreate() = runBlocking {
        val categories = categoryDao.observeAll().first()

        assertEquals(1, categories.size)
        assertEquals(AppDatabase.DEFAULT_CATEGORY_NAME, categories.single().name)
        assertEquals(AppDatabase.DEFAULT_CATEGORY_COLOR, categories.single().colorHex)
    }

    @Test
    fun insertsAndReadsBackSubscription() = runBlocking {
        val categoryId = categoryDao.observeAll().first().single().id

        val id = subscriptionDao.insert(
            Subscription(
                name = "Spotify",
                cost = 199.0,
                currency = "PHP",
                billingCycle = "MONTHLY",
                renewalDate = 1_700_000_000_000L,
                categoryId = categoryId,
                status = "ACTIVE"
            )
        ).toInt()

        val stored = subscriptionDao.getById(id)
        assertNotNull(stored)
        assertEquals("Spotify", stored!!.name)
        assertEquals(199.0, stored.cost, 0.001)
        assertEquals(categoryId, stored.categoryId)
    }

    @Test
    fun observesSubscriptionsSortedByRenewalDate() = runBlocking {
        val categoryId = categoryDao.observeAll().first().single().id
        subscriptionDao.insert(subscription("Domain", renewalDate = 300L, categoryId = categoryId))
        subscriptionDao.insert(subscription("Netflix", renewalDate = 100L, categoryId = categoryId))
        subscriptionDao.insert(subscription("Figma", renewalDate = 200L, categoryId = categoryId))

        val names = subscriptionDao.observeAll().first().map { it.name }

        assertEquals(listOf("Netflix", "Figma", "Domain"), names)
    }

    @Test
    fun updatesAndDeletesSubscription() = runBlocking {
        val categoryId = categoryDao.observeAll().first().single().id
        val id = subscriptionDao.insert(
            subscription("Notion", renewalDate = 100L, categoryId = categoryId)
        ).toInt()

        val stored = subscriptionDao.getById(id)!!
        subscriptionDao.update(stored.copy(status = "PAUSED", cost = 500.0))

        val updated = subscriptionDao.getById(id)!!
        assertEquals("PAUSED", updated.status)
        assertEquals(500.0, updated.cost, 0.001)
        assertEquals(listOf(updated), subscriptionDao.observeByStatus("PAUSED").first())

        subscriptionDao.delete(updated)
        assertEquals(emptyList<Subscription>(), subscriptionDao.observeAll().first())
    }

    private fun subscription(name: String, renewalDate: Long, categoryId: Int) = Subscription(
        name = name,
        cost = 100.0,
        currency = "PHP",
        billingCycle = "MONTHLY",
        renewalDate = renewalDate,
        categoryId = categoryId,
        status = "ACTIVE"
    )
}
