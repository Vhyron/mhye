package com.vhyron.mhye.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, Subscription::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        const val DEFAULT_CATEGORY_NAME = "Uncategorized"
        const val DEFAULT_CATEGORY_COLOR = "#9E9E9E"

        private const val DATABASE_NAME = "mhye.db"

        /**
         * Seeds a single fallback category on first creation so the add form always
         * has a valid selection.
         */
        val SEED_DEFAULT_CATEGORY = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO Category (name, colorHex) VALUES (?, ?)",
                    arrayOf(DEFAULT_CATEGORY_NAME, DEFAULT_CATEGORY_COLOR)
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(SEED_DEFAULT_CATEGORY)
                // Pre-release: the schema is still moving, so wipe rather than migrate.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
