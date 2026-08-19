package com.vhyron.mhye.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

/** Outcome of an export or import, so the UI can report it without throwing. */
sealed interface BackupResult {
    data class Exported(val subscriptions: Int) : BackupResult
    data class Imported(val subscriptions: Int, val categories: Int) : BackupResult
    data class Failed(val reason: String) : BackupResult
}

/**
 * Reads and writes backups through the system file picker, so no storage
 * permission is needed and the user chooses where the file lives.
 */
class BackupRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun export(destination: Uri): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val categories = database.categoryDao().observeAll().first()
            val subscriptions = database.subscriptionDao().observeAll().first()
            val backup = buildBackup(categories, subscriptions, System.currentTimeMillis())

            context.contentResolver.openOutputStream(destination)?.use { stream ->
                stream.write(backupJson.encodeToString(backup).toByteArray())
            } ?: error("Couldn't open the selected file for writing")

            BackupResult.Exported(subscriptions.size)
        }.getOrElse { BackupResult.Failed(it.message ?: "Export failed") }
    }

    /**
     * Replaces everything. A merge would need identity rules the data model
     * doesn't have — two "Netflix" rows may be genuinely different — so restore
     * is deliberately all-or-nothing, wrapped in a transaction.
     */
    suspend fun import(source: Uri): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(source)?.use { stream ->
                stream.readBytes().decodeToString()
            } ?: error("Couldn't open the selected file")

            val backup = try {
                backupJson.decodeFromString<Backup>(text)
            } catch (e: SerializationException) {
                return@runCatching BackupResult.Failed("That doesn't look like a Mhye backup")
            }

            if (backup.version > Backup.CURRENT_VERSION) {
                return@runCatching BackupResult.Failed(
                    "This backup was made by a newer version of Mhye"
                )
            }
            if (backup.categories.isEmpty()) {
                return@runCatching BackupResult.Failed("Backup has no categories to restore")
            }

            database.withTransaction {
                val categoryDao = database.categoryDao()
                val subscriptionDao = database.subscriptionDao()

                subscriptionDao.observeAll().first().forEach { subscriptionDao.delete(it) }
                categoryDao.observeAll().first().forEach { categoryDao.delete(it) }

                // Old id -> newly assigned id, so subscriptions land in the
                // right category after reinsertion.
                val categoryIds = backup.categories.associate { backupCategory ->
                    backupCategory.id to
                        categoryDao.insert(backupCategory.toCategory().copy(id = 0)).toInt()
                }

                backup.subscriptions.forEach { subscription ->
                    subscriptionDao.insert(subscription.toSubscription(categoryIds))
                }
            }

            BackupResult.Imported(backup.subscriptions.size, backup.categories.size)
        }.getOrElse { BackupResult.Failed(it.message ?: "Import failed") }
    }
}
