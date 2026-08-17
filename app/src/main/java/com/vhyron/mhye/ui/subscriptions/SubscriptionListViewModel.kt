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
import com.vhyron.mhye.data.MonthlySpend
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionDao
import com.vhyron.mhye.data.monthlySpend
import com.vhyron.mhye.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionListViewModel(
    private val application: Application,
    private val subscriptionDao: SubscriptionDao,
    categoryDao: CategoryDao
) : ViewModel() {

    /** Soonest renewal first — the DAO query already applies that ordering. */
    val subscriptions: StateFlow<List<Subscription>> = subscriptionDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    /** Monthly-equivalent spend per currency, derived from the same query. */
    val monthlySpend: StateFlow<List<MonthlySpend>> = subscriptions
        .map { monthlySpend(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    /** Backs the category dropdown in the add/edit sheet. */
    val categories: StateFlow<List<Category>> = categoryDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

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
