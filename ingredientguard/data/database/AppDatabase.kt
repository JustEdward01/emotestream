package com.ingredientguard.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.ingredientguard.data.models.*
import com.ingredientguard.data.database.dao.*
import com.ingredientguard.data.repository.ScanHistoryItem

@Database(
    entities = [
        UserProfile::class,
        UserSettings::class,
        ScanHistoryItem::class // Your existing entity
    ],
    version = 2, // Increment from your current version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun historyDao(): HistoryDao // Your existing DAO
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ingredientguard_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Migration from version 1 to 2 (adding user profile tables)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create user_profiles table
                database.execSQL("""
                    CREATE TABLE user_profiles (
                        id TEXT PRIMARY KEY NOT NULL,
                        firstName TEXT NOT NULL,
                        lastName TEXT NOT NULL,
                        email TEXT NOT NULL,
                        avatarPath TEXT,
                        dateCreated INTEGER NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        personalAllergens TEXT NOT NULL,
                        severityPreferences TEXT NOT NULL,
                        isOnboardingCompleted INTEGER NOT NULL
                    )
                """)
                
                // Create user_settings table
                database.execSQL("""
                    CREATE TABLE user_settings (
                        userId TEXT PRIMARY KEY NOT NULL,
                        notificationsEnabled INTEGER NOT NULL,
                        soundEnabled INTEGER NOT NULL,
                        vibrationEnabled INTEGER NOT NULL,
                        autoSaveScans INTEGER NOT NULL,
                        warningThreshold TEXT NOT NULL,
                        language TEXT NOT NULL,
                        theme TEXT NOT NULL
                    )
                """)
            }
        }
    }
}