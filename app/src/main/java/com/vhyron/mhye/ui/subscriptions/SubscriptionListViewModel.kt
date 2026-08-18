package com.vhyron.mhye.ui.subscriptions

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vhyron.mhye.data.AppDatabase
import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.CategoryDao
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionDao
import com.vhyron.mhye.data.monthlyCost
import com.vhyron.mhye.data.monthlySpend
import com.vhyron.mhye.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionListViewModel(
    private val application: Application,
    private val subscriptionDao: SubscriptionDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val sortOrder = MutableStateFlow(SortOrder.RENEWAL_DATE)
    private val statusFilter = MutableStateFlow<String?>(null)
    private val categoryFilter = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SubscriptionListUiState> = combine(
        subscriptionDao.observeAll(),
        categoryDao.observeAll(),
        sortOrder,
        statusFilter,
        categoryFilter
    ) { all, categories, order, status, category ->
        val visible = all
            .filter { status == null || it.status == status }
            .filter { category == null || it.categoryId == category }
            .sortedWith(comparatorFor(order))

        SubscriptionListUiState(
            subscriptions = visible,
            monthlySpend = monthlySpend(visible),
            categories = categories,
            categoryUsage = all.groupingBy { it.categoryId }.eachCount(),
            sortOrder = order,
            statusFilter = status,
            categoryFilter = category,
            hasAnySubscriptions = all.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SubscriptionListUiState()
    )

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun setStatusFilter(status: String?) {
        statusFilter.value = status
    }

    fun setCategoryFilter(categoryId: Int?) {
        categoryFilter.value = categoryId
    }

    fun addSubscription(subscription: Subscription) {
        viewModelScope.launch {
            val id = subscriptionDao.insert(subscription).toInt()
            // Room assigns the id, so schedule against the stored row.
            ReminderScheduler.schedule(application, subscription.copy(id = id))
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionDao.update(subscription)
            // Replaces the pending reminder, or cancels it if no longer active.
            ReminderScheduler.schedule(application, subscription)
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionDao.delete(subscription)
            ReminderScheduler.cancel(application, subscription.id)
        }
    }

    /** Inserts when [category] has the default id of 0, updates otherwise. */
    fun saveCategory(category: Category) {
        viewModelScope.launch {
            if (category.id == 0) {
                categoryDao.insert(category)
            } else {
                categoryDao.update(category)
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryDao.delete(category) }
    }

    private fun comparatorFor(order: SortOrder): Comparator<Subscription> = when (order) {
        SortOrder.RENEWAL_DATE -> compareBy { it.renewalDate }
        SortOrder.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        // Monthly-equivalent so cycles are comparable; unparseable cycles sink.
        SortOrder.MONTHLY_COST -> compareByDescending { it.monthlyCost() ?: 0.0 }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getInstance(application)
                SubscriptionListViewModel(
                    application,
                    database.subscriptionDao(),
                    database.categoryDao()
                )
            }
        }
    }
}
