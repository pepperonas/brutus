package com.pepperonas.brutus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AlarmEntity::class], version = 7, exportSchema = true)
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

        /**
         * v1.4.0: Ultra Hardcore Mode (two follow-up alarms after dismiss) + per-alarm
         * math difficulty and shake sensitivity presets.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN ultraHardcoreMode INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN mathDifficulty INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN shakeSensitivity INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /** v1.6.0: optional 10-minute Sunrise pre-alarm. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN sunriseEnabled INTEGER NOT NULL DEFAULT 0"
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    // Pre-v1.0.0 dev versions never reached external users; if
                    // someone is still on one of them we accept a clean wipe.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
