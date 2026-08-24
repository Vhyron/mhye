package com.vhyron.mhye.ui.subscriptions

import com.vhyron.mhye.data.BillingCycle
import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupingTest {

    private val entertainment = Category(id = 1, name = "Entertainment", colorHex = "#E53935")
    private val cloud = Category(id = 2, name = "Cloud", colorHex = "#5E35B1")
    private val unused = Category(id = 3, name = "Unused", colorHex = "#9E9E9E")
    private val categories = listOf(entertainment, cloud, unused)

    private val netflix = subscription("Netflix", categoryId = 1)
    private val spotify = subscription("Spotify", categoryId = 1)
    private val domain = subscription("vhyron.dev", categoryId = 2)
    private val all = listOf(netflix, spotify, domain)

    @Test
    fun `ungrouped is a single block holding everything in order`() {
        val groups = groupsFor(all, categories, GroupBy.NONE)

        assertEquals(1, groups.size)
        assertNull(groups.single().category)
        assertEquals(all, groups.single().subscriptions)
    }

    @Test
    fun `grouping by category yields one block per used category`() {
        val groups = groupsFor(all, categories, GroupBy.CATEGORY)

        assertEquals(listOf("Entertainment", "Cloud"), groups.map { it.category?.name })
        assertEquals(listOf(netflix, spotify), groups.first().subscriptions)
        assertEquals(listOf(domain), groups.last().subscriptions)
    }

    @Test
    fun `a category with nothing in it produces no block`() {
        val groups = groupsFor(all, categories, GroupBy.CATEGORY)

        assertTrue(groups.none { it.category?.name == "Unused" })
    }

    @Test
    fun `groups follow the category list order, not the subscription order`() {
        // Cloud first in the category list, but its subscription comes last.
        val reordered = listOf(cloud, entertainment)

        val groups = groupsFor(all, reordered, GroupBy.CATEGORY)

        assertEquals(listOf("Cloud", "Entertainment"), groups.map { it.category?.name })
    }

    @Test
    fun `sort order within a group is preserved`() {
        val groups = groupsFor(listOf(spotify, netflix), categories, GroupBy.CATEGORY)

        // Whatever order the caller sorted into survives grouping.
        assertEquals(listOf(spotify, netflix), groups.single().subscriptions)
    }

    @Test
    fun `an empty list produces no blocks at all`() {
        // Guards the empty state: a stray blank block would draw a card.
        assertEquals(emptyList<SubscriptionGroup>(), groupsFor(emptyList(), categories, GroupBy.NONE))
        assertEquals(
            emptyList<SubscriptionGroup>(),
            groupsFor(emptyList(), categories, GroupBy.CATEGORY)
        )
    }

    private fun subscription(name: String, categoryId: Int) = Subscription(
        name = name,
        cost = 100.0,
        currency = "PHP",
        billingCycle = BillingCycle.MONTHLY,
        renewalDate = 0L,
        categoryId = categoryId,
        status = SubscriptionStatus.ACTIVE
    )
}
