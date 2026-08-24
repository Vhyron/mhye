package com.vhyron.mhye.ui.subscriptions

import android.app.Application
import android.net.Uri
import androidx.room.withTransaction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vhyron.mhye.data.AppDatabase
import com.vhyron.mhye.data.BackupRepository
import com.vhyron.mhye.data.BackupResult
import com.vhyron.mhye.data.DEFAULT_REMINDER_DAYS
import com.vhyron.mhye.data.SettingsRepository
import com.vhyron.mhye.data.Category
import com.vhyron.mhye.data.CategoryDao
import com.vhyron.mhye.data.Subscription
import com.vhyron.mhye.data.SubscriptionDao
import com.vhyron.mhye.data.monthlyCost
import com.vhyron.mhye.data.monthlySpend
import com.vhyron.mhye.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionListViewModel(
    private val application: Application,
    private val subscriptionDao: SubscriptionDao,
    private val categoryDao: CategoryDao,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val database: AppDatabase
) : ViewModel() {

    val defaultReminderDays: StateFlow<Int> = settingsRepository.defaultReminderDays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DEFAULT_REMINDER_DAYS
        )

    /** Changing the default re-times every subscription that follows it. */
    fun setDefaultReminderDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultReminderDays(days)
            rescheduleAllReminders()
        }
    }

    private val sortOrder = MutableStateFlow(SortOrder.RENEWAL_DATE)
    private val statusFilter = MutableStateFlow<String?>(null)
    private val categoryFilter = MutableStateFlow<Int?>(null)
    private val groupBy = MutableStateFlow(GroupBy.NONE)

    /** Bundled so the combine below stays within its typed arity. */
    private data class Controls(
        val order: SortOrder,
        val status: String?,
        val category: Int?,
        val grouping: GroupBy
    )

    private val controls = combine(
        sortOrder, statusFilter, categoryFilter, groupBy, ::Controls
    )

    val uiState: StateFlow<SubscriptionListUiState> = combine(
        subscriptionDao.observeAll(),
        categoryDao.observeAll(),
        controls
    ) { all, categories, (order, status, category, grouping) ->
        val visible = all
            .filter { status == null || it.status == status }
            .filter { category == null || it.categoryId == category }
            .sortedWith(comparatorFor(order))

        SubscriptionListUiState(
            subscriptions = visible,
            groups = groupsFor(visible, categories, grouping),
            monthlySpend = monthlySpend(visible),
            categories = categories,
            categoryUsage = all.groupingBy { it.categoryId }.eachCount(),
            sortOrder = order,
            groupBy = grouping,
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

    fun setGroupBy(grouping: GroupBy) {
        groupBy.value = grouping
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
            ReminderScheduler.schedule(
                application,
                subscription.copy(id = id),
                settingsRepository.defaultReminderDays.first()
            )
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionDao.update(subscription)
            // Replaces the pending reminder, or cancels it if no longer active.
            ReminderScheduler.schedule(
                application,
                subscription,
                settingsRepository.defaultReminderDays.first()
            )
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

    /**
     * [reassignTo] moves this category's subscriptions before deleting it.
     * Null is only valid when nothing uses the category — the foreign key
     * would otherwise reject the delete. Both steps share a transaction so a
     * failure can't leave subscriptions pointing at a deleted category.
     */
    fun deleteCategory(category: Category, reassignTo: Int? = null) {
        viewModelScope.launch {
            database.withTransaction {
                if (reassignTo != null) {
                    subscriptionDao.reassignCategory(category.id, reassignTo)
                }
                categoryDao.delete(category)
            }
        }
    }

    /** One-shot outcome for the UI to surface, cleared once shown. */
    private val _backupResult = MutableStateFlow<BackupResult?>(null)
    val backupResult: StateFlow<BackupResult?> = _backupResult

    fun exportTo(destination: Uri) {
        viewModelScope.launch { _backupResult.value = backupRepository.export(destination) }
    }

    fun importFrom(source: Uri) {
        viewModelScope.launch {
            val result = backupRepository.import(source)
            // Reminders were scheduled against ids that no longer exist.
            if (result is BackupResult.Imported) rescheduleAllReminders()
            _backupResult.value = result
        }
    }

    fun clearBackupResult() {
        _backupResult.value = null
    }

    private suspend fun rescheduleAllReminders() {
        val default = settingsRepository.defaultReminderDays.first()
        subscriptionDao.observeAll().first().forEach { subscription ->
            ReminderScheduler.schedule(application, subscription, default)
        }
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
                    database.categoryDao(),
                    BackupRepository(application, database),
                    SettingsRepository(application),
                    database
                )
            }
        }
    }
}
