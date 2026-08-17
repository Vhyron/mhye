package com.vhyron.mhye.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    /** Sorted by soonest renewal first — the order the list screen renders. */
    @Query("SELECT * FROM Subscription ORDER BY renewalDate ASC")
    fun observeAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM Subscription WHERE status = :status ORDER BY renewalDate ASC")
    fun observeByStatus(status: String): Flow<List<Subscription>>

    @Query("SELECT * FROM Subscription WHERE id = :id")
    suspend fun getById(id: Int): Subscription?

    @Insert
    suspend fun insert(subscription: Subscription): Long

    @Update
    suspend fun update(subscription: Subscription)

    @Delete
    suspend fun delete(subscription: Subscription)
}
