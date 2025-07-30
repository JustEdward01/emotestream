package com.ingredientguard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ingredientguard.data.database.Converters
import java.util.Date

@Entity(tableName = "user_profiles")
@TypeConverters(Converters::class)
data class UserProfile(
    @PrimaryKey
    val id: String = "current_user", // Single user app
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val avatarPath: String? = null,
    val dateCreated: Date = Date(),
    val lastUpdated: Date = Date(),
    val personalAllergens: List<String> = emptyList(), // User's known allergies
    val severityPreferences: Map<String, String> = emptyMap(), // allergen -> severity mapping
    val isOnboardingCompleted: Boolean = false
)