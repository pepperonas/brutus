package com.pepperonas.brutus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AlarmEntity::class], version = 5, exportSchema = true)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        /**
         * Adds the `hardcoreMode` column introduced in v1.2.0.
         * SQLite stores Boolean as INTEGER 0/1 — Room maps automatically.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN hardcoreMode INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "brutus_alarms.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    // Pre-v1.0.0 dev versions never reached external users; if
                    // someone is still on one of them we accept a clean wipe.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
