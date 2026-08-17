package com.vhyron.mhye.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"]
    )],
    indices = [Index("categoryId")]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val cost: Double,
    val currency: String,             // e.g. "PHP", "USD"
    val billingCycle: String,         // MONTHLY / YEARLY / CUSTOM_DAYS
    val customCycleDays: Int? = null, // used only when billingCycle == CUSTOM_DAYS
    val renewalDate: Long,            // epoch millis
    val categoryId: Int,
    val status: String,               // ACTIVE / CANCELLED / PAUSED
    val notes: String? = null
)
