package com.ingredientguard.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.ingredientguard.data.database.AppDatabase
import com.ingredientguard.data.models.UserProfile
import com.ingredientguard.data.models.UserSettings
import java.util.Date

class UserRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userProfileDao = database.userProfileDao()
    private val userSettingsDao = database.userSettingsDao()
    
    // Profile operations
    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getUserProfile()
    
    suspend fun createOrUpdateProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile.copy(lastUpdated = Date()))
    }
    
    suspend fun updatePersonalAllergens(allergens: List<String>) {
        userProfileDao.updatePersonalAllergens(allergens)
    }
    
    suspend fun completeOnboarding() {
        userProfileDao.setOnboardingCompleted(true)
    }
    
    suspend fun isOnboardingCompleted(): Boolean {
        return userProfileDao.getUserProfileOnce()?.isOnboardingCompleted ?: false
    }
    
    // Settings operations
    fun getUserSettings(): Flow<UserSettings?> = userSettingsDao.getUserSettings()
    
    suspend fun updateSettings(settings: UserSettings) {
        userSettingsDao.insertOrUpdateSettings(settings)
    }
    
    suspend fun getOrCreateDefaultSettings(): UserSettings {
        return userSettingsDao.getUserSettingsOnce() ?: UserSettings().also {
            userSettingsDao.insertOrUpdateSettings(it)
        }
    }
    
    // Combined operations
    suspend fun initializeNewUser(
        firstName: String,
        lastName: String,
        email: String,
        selectedAllergens: List<String>
    ) {
        val profile = UserProfile(
            firstName = firstName,
            lastName = lastName,
            email = email,
            personalAllergens = selectedAllergens,
            isOnboardingCompleted = true,
            dateCreated = Date(),
            lastUpdated = Date()
        )
        
        val settings = UserSettings()
        
        userProfileDao.insertOrUpdateProfile(profile)
        userSettingsDao.insertOrUpdateSettings(settings)
    }
    
    // Check if user's personal allergens are detected in scan
    suspend fun checkPersonalAllergens(detectedAllergens: List<String>): List<String> {
        val profile = userProfileDao.getUserProfileOnce()
        return profile?.personalAllergens?.intersect(detectedAllergens.map { it.lowercase() })?.toList() ?: emptyList()
    }
}