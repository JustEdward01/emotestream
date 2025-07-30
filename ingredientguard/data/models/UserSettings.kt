package com.ingredientguard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val userId: String = "current_user",
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoSaveScans: Boolean = true,
    val warningThreshold: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val language: String = "ro",
    val theme: String = "SYSTEM" // LIGHT, DARK, SYSTEM
)