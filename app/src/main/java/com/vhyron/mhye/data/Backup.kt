package com.vhyron.mhye.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * On-disk shape of an exported backup.
 *
 * [version] exists so a future schema change can still read today's files —
 * without it, an older backup would be indistinguishable from a corrupt one.
 */
@Serializable
data class Backup(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val categories: List<BackupCategory>,
    val subscriptions: List<BackupSubscription>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupCategory(
    val id: Int,
    val name: String,
    val colorHex: String
)

@Serializable
data class BackupSubscription(
    val name: String,
    val cost: Double,
    val currency: String,
    val billingCycle: String,
    val customCycleDays: Int? = null,
    val renewalDate: Long,
    val categoryId: Int,
    val status: String,
    val notes: String? = null
)

/**
 * Lenient on unknown keys so a newer backup still restores what it can.
 * [encodeDefaults] matters: without it [Backup.version] is omitted whenever it
 * equals the default, which is exactly when it needs writing.
 */
val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun buildBackup(
    categories: List<Category>,
    subscriptions: List<Subscription>,
    exportedAt: Long
) = Backup(
    exportedAt = exportedAt,
    categories = categories.map { BackupCategory(it.id, it.name, it.colorHex) },
    subscriptions = subscriptions.map {
        BackupSubscription(
            name = it.name,
            cost = it.cost,
            currency = it.currency,
            billingCycle = it.billingCycle,
            customCycleDays = it.customCycleDays,
            renewalDate = it.renewalDate,
            categoryId = it.categoryId,
            status = it.status,
            notes = it.notes
        )
    }
)

fun BackupCategory.toCategory() = Category(id = id, name = name, colorHex = colorHex)

/**
 * Subscription ids are not restored — Room assigns fresh ones. [categoryId] is
 * remapped through [categoryIds] because the categories were reinserted too.
 */
fun BackupSubscription.toSubscription(categoryIds: Map<Int, Int>) = Subscription(
    name = name,
    cost = cost,
    currency = currency,
    billingCycle = billingCycle,
    customCycleDays = customCycleDays,
    renewalDate = renewalDate,
    categoryId = categoryIds[categoryId] ?: categoryIds.values.first(),
    status = status,
    notes = notes
)
