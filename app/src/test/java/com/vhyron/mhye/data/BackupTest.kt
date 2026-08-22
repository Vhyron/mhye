package com.vhyron.mhye.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private val categories = listOf(
        Category(id = 1, name = "Uncategorized", colorHex = "#9E9E9E"),
        Category(id = 2, name = "Entertainment", colorHex = "#E53935")
    )

    private val subscriptions = listOf(
        Subscription(
            id = 7,
            name = "Netflix",
            cost = 549.0,
            currency = "PHP",
            billingCycle = BillingCycle.MONTHLY,
            renewalDate = 1_787_616_000_000L,
            categoryId = 2,
            status = SubscriptionStatus.ACTIVE,
            notes = "shared plan"
        ),
        Subscription(
            id = 8,
            name = "Proxy",
            cost = 300.0,
            currency = "PHP",
            billingCycle = BillingCycle.CUSTOM_DAYS,
            customCycleDays = 90,
            renewalDate = 1_790_726_400_000L,
            categoryId = 1,
            status = SubscriptionStatus.PAUSED
        )
    )

    @Test
    fun `written backup records its schema version`() {
        val json = backupJson.encodeToString(
            buildBackup(categories, subscriptions, exportedAt = 1L)
        )

        // Without encodeDefaults the version silently vanishes when it matches
        // the default — which is precisely when it must be present.
        assertTrue(json.contains("\"version\": ${Backup.CURRENT_VERSION}"))
    }

    @Test
    fun `backup survives a round trip through json`() {
        val original = buildBackup(categories, subscriptions, exportedAt = 42L)

        val restored = backupJson.decodeFromString<Backup>(backupJson.encodeToString(original))

        assertEquals(original, restored)
    }

    @Test
    fun `every field is carried across`() {
        val restored = backupJson
            .decodeFromString<Backup>(
                backupJson.encodeToString(buildBackup(categories, subscriptions, 0L))
            )
            .subscriptions

        assertEquals("shared plan", restored.first().notes)
        assertEquals(90, restored.last().customCycleDays)
        assertEquals(SubscriptionStatus.PAUSED, restored.last().status)
        assertEquals(BillingCycle.CUSTOM_DAYS, restored.last().billingCycle)
    }

    @Test
    fun `restored subscriptions point at the reinserted categories`() {
        val backup = buildBackup(categories, subscriptions, 0L)
        // Room hands out fresh ids on reinsert; old id 2 becomes 55.
        val remapped = mapOf(1 to 44, 2 to 55)

        val restored = backup.subscriptions.map { it.toSubscription(remapped) }

        assertEquals(55, restored.first().categoryId)
        assertEquals(44, restored.last().categoryId)
        // Ids are not restored — Room assigns them.
        assertTrue(restored.all { it.id == 0 })
    }

    @Test
    fun `a subscription with an unknown category falls back rather than failing`() {
        val orphan = BackupSubscription(
            name = "Orphan",
            cost = 10.0,
            currency = "PHP",
            billingCycle = BillingCycle.MONTHLY,
            renewalDate = 0L,
            categoryId = 999,
            status = SubscriptionStatus.ACTIVE
        )

        val restored = orphan.toSubscription(mapOf(1 to 44))

        assertEquals(44, restored.categoryId)
    }

    @Test
    fun `unknown fields from a newer backup are ignored`() {
        val json = """
            {
              "version": 1,
              "exportedAt": 5,
              "somethingNew": "ignore me",
              "categories": [{"id": 1, "name": "A", "colorHex": "#FFFFFF"}],
              "subscriptions": []
            }
        """.trimIndent()

        val backup = backupJson.decodeFromString<Backup>(json)

        assertEquals(1, backup.categories.size)
        assertEquals(5L, backup.exportedAt)
    }
}
