package com.vhyron.mhye.ui.subscriptions

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vhyron.mhye.data.AppDatabase
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SubscriptionListViewModel(
    subscriptionDao: SubscriptionDao
) : ViewModel() {

    /** Soonest renewal first — the DAO query already applies that ordering. */
    val subscriptions: StateFlow<List<Subscription>> = subscriptionDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                SubscriptionListViewModel(AppDatabase.getInstance(application).subscriptionDao())
            }
        }
    }
}
