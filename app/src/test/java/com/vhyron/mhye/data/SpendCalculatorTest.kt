package com.vhyron.mhye.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpendCalculatorTest {

    @Test
    fun `monthly cost is taken as-is`() {
        assertEquals(549.0, subscription(cost = 549.0).monthlyCost()!!, TOLERANCE)
    }

    @Test
    fun `yearly cost is divided across twelve months`() {
        val monthly = subscription(cost = 1200.0, billingCycle = BillingCycle.YEARLY).monthlyCost()

        assertEquals(100.0, monthly!!, TOLERANCE)
    }

    @Test
    fun `custom cycle is scaled by average month length`() {
        val monthly = subscription(
            cost = 300.0,
            billingCycle = BillingCycle.CUSTOM_DAYS,
            customCycleDays = 90
        ).monthlyCost()

        // 300 over 90 days = 3.333/day, across an average 30.4375-day month.
        assertEquals(101.458, monthly!!, TOLERANCE)
    }

    @Test
    fun `custom cycle without a usable day count has no monthly equivalent`() {
        val missing = subscription(billingCycle = BillingCycle.CUSTOM_DAYS, customCycleDays = null)
        val zero = subscription(billingCycle = BillingCycle.CUSTOM_DAYS, customCycleDays = 0)

        assertNull(missing.monthlyCost())
        assertNull(zero.monthlyCost())
    }

    @Test
    fun `unknown billing cycle has no monthly equivalent`() {
        assertNull(subscription(billingCycle = "WEEKLY").monthlyCost())
    }

    @Test
    fun `only active subscriptions count toward spend`() {
        val spend = monthlySpend(
            listOf(
                subscription(cost = 100.0),
                subscription(cost = 50.0, status = SubscriptionStatus.PAUSED),
                subscription(cost = 25.0, status = SubscriptionStatus.CANCELLED)
            )
        )

        assertEquals(listOf(MonthlySpend("PHP", 100.0)), spend)
    }

    @Test
    fun `currencies are totalled separately and ordered by size`() {
        val spend = monthlySpend(
            listOf(
                subscription(cost = 100.0, currency = "USD"),
                subscription(cost = 200.0, currency = "PHP"),
                subscription(cost = 1200.0, currency = "PHP", billingCycle = BillingCycle.YEARLY)
            )
        )

        assertEquals(listOf("PHP", "USD"), spend.map { it.currency })
        assertEquals(300.0, spend.first().amount, TOLERANCE)
        assertEquals(100.0, spend.last().amount, TOLERANCE)
    }

    @Test
    fun `no active subscriptions yields no totals`() {
        assertEquals(emptyList<MonthlySpend>(), monthlySpend(emptyList()))
        assertEquals(
            emptyList<MonthlySpend>(),
            monthlySpend(listOf(subscription(status = SubscriptionStatus.CANCELLED)))
        )
    }

    private fun subscription(
        cost: Double = 100.0,
        currency: String = "PHP",
        billingCycle: String = BillingCycle.MONTHLY,
        customCycleDays: Int? = null,
        status: String = SubscriptionStatus.ACTIVE
    ) = Subscription(
        name = "Test",
        cost = cost,
        currency = currency,
        billingCycle = billingCycle,
        customCycleDays = customCycleDays,
        renewalDate = 0L,
        categoryId = 1,
        status = status
    )

    private companion object {
        const val TOLERANCE = 0.001
    }
}
